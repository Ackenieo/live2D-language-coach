/**
 * 摄像头视觉采集（从 front-mvp0/talktic 提取）
 * 定时截图并通过 WebSocket 发送，带 pHash 去重
 */

const PHASH_SIZE = 8;
const DEFAULT_THRESHOLD = 0.98;
const CAPTURE_FPS = 10;
const CAPTURE_INTERVAL_MS = 1000 / CAPTURE_FPS;
const SCREEN_WIDTH = 240;
const SCREEN_HEIGHT = 180;
const SCREEN_QUALITY = 0.2;

type VisionCaptureOptions = {
  similarityThreshold?: number;
};

let cameraStream: MediaStream | null = null;
let visionTimer: ReturnType<typeof setInterval> | null = null;
let lastPHash: string | null = null;

function computePHash(imageData: ImageData): string {
  const { data, width: w, height: h } = imageData;
  const grays: number[] = [];
  for (let gy = 0; gy < PHASH_SIZE; gy++) {
    for (let gx = 0; gx < PHASH_SIZE; gx++) {
      const px = Math.floor((gx + 0.5) * w / PHASH_SIZE);
      const py = Math.floor((gy + 0.5) * h / PHASH_SIZE);
      const idx = (py * w + px) * 4;
      grays.push(0.299 * data[idx] + 0.587 * data[idx + 1] + 0.114 * data[idx + 2]);
    }
  }
  const avg = grays.reduce((a, b) => a + b, 0) / grays.length;
  return grays.map((g) => (g >= avg ? '1' : '0')).join('');
}

function hashSimilarity(a: string, b: string): number {
  if (!a || !b || a.length !== b.length) return 0;
  let same = 0;
  for (let i = 0; i < a.length; i++) if (a[i] === b[i]) same++;
  return same / a.length;
}

export async function startVisionCapture(
  videoElement: HTMLVideoElement,
  sendFrame: (base64: string) => void,
  options: VisionCaptureOptions = {},
): Promise<void> {
  const threshold = options.similarityThreshold ?? DEFAULT_THRESHOLD;

  cameraStream = await navigator.mediaDevices.getUserMedia({
    video: { width: 640, height: 480, facingMode: 'user' },
  });
  videoElement.srcObject = cameraStream;
  await new Promise<void>((resolve) => { videoElement.onloadeddata = () => resolve(); });

  const tick = () => {
    if (!cameraStream) return;
    if (videoElement.readyState < 2) return;

    const hashCanvas = document.createElement('canvas');
    hashCanvas.width = PHASH_SIZE * 4;
    hashCanvas.height = PHASH_SIZE * 4;
    const hCtx = hashCanvas.getContext('2d')!;
    hCtx.drawImage(videoElement, 0, 0, hashCanvas.width, hashCanvas.height);

    const currentHash = computePHash(hCtx.getImageData(0, 0, hashCanvas.width, hashCanvas.height));
    if (lastPHash && hashSimilarity(lastPHash, currentHash) >= threshold) return;
    lastPHash = currentHash;

    const sendCanvas = document.createElement('canvas');
    sendCanvas.width = SCREEN_WIDTH;
    sendCanvas.height = SCREEN_HEIGHT;
    const sCtx = sendCanvas.getContext('2d')!;
    sCtx.drawImage(videoElement, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
    sendFrame(sendCanvas.toDataURL('image/jpeg', SCREEN_QUALITY));
  };

  tick();
  visionTimer = setInterval(tick, CAPTURE_INTERVAL_MS);
}

export function stopVisionCapture(videoElement: HTMLVideoElement): void {
  if (visionTimer) { clearInterval(visionTimer); visionTimer = null; }
  if (cameraStream) { cameraStream.getTracks().forEach((t) => t.stop()); cameraStream = null; }
  videoElement.srcObject = null;
  lastPHash = null;
}

export function isVisionActive(): boolean {
  return visionTimer !== null;
}
