/**
 * 音频采集与播放（从 front-mvp0/talktic 提取）
 */

type AudioCaptureCallbacks = {
  onAudioData?: (int16Buffer: ArrayBuffer) => void;
};

type AudioPlaybackOptions = {
  onVolume?: (volume: number) => void;
  interrupt?: boolean;
};

let audioCtx: AudioContext | null = null;
let mediaStream: MediaStream | null = null;
let scriptProcessor: ScriptProcessorNode | null = null;

/** 开始麦克风采集 */
export async function startAudioCapture(callbacks: AudioCaptureCallbacks): Promise<void> {
  mediaStream = await navigator.mediaDevices.getUserMedia({
    audio: { sampleRate: 16000, channelCount: 1, echoCancellation: true, noiseSuppression: true },
  });

  audioCtx = new AudioContext({ sampleRate: 16000 });
  const source = audioCtx.createMediaStreamSource(mediaStream);
  scriptProcessor = audioCtx.createScriptProcessor(4096, 1, 1);

  scriptProcessor.onaudioprocess = (e) => {
    const inputData = e.inputBuffer.getChannelData(0);
    const int16 = new Int16Array(inputData.length);
    for (let i = 0; i < inputData.length; i++) {
      const s = Math.max(-1, Math.min(1, inputData[i]));
      int16[i] = s < 0 ? s * 0x8000 : s * 0x7fff;
    }
    callbacks.onAudioData?.(int16.buffer);
  };

  source.connect(scriptProcessor);
  scriptProcessor.connect(audioCtx.destination);
}

/** 停止采集 */
export function stopAudioCapture(): void {
  scriptProcessor?.disconnect();
  scriptProcessor = null;
  audioCtx?.close();
  audioCtx = null;
  mediaStream?.getTracks().forEach((t) => t.stop());
  mediaStream = null;
}

// --- 播放 ---

let outputCtx: AudioContext | null = null;
let audioQueue: Float32Array[] = [];
let isPlaying = false;
let currentSource: AudioBufferSourceNode | null = null;
let playbackGeneration = 0;

function getOutputCtx(): AudioContext {
  if (!outputCtx) outputCtx = new AudioContext({ sampleRate: 24000 });
  if (outputCtx.state === 'suspended') void outputCtx.resume();
  return outputCtx;
}

function stopCurrentPlayback(closeContext: boolean): void {
  playbackGeneration += 1;
  audioQueue = [];
  isPlaying = false;

  const source = currentSource;
  currentSource = null;

  if (source) {
    source.onended = null;
    try {
      source.stop();
    } catch {
      // Source may already have ended.
    }
    try {
      source.disconnect();
    } catch {
      // Source may already be disconnected by the browser.
    }
  }

  if (closeContext && outputCtx) {
    void outputCtx.close();
    outputCtx = null;
  }
}

/** 播放 PCM Int16 音频数据，回调音量 */
export function playAudioChunk(data: ArrayBuffer, options?: AudioPlaybackOptions): void {
  if (options?.interrupt) {
    stopCurrentPlayback(false);
  }

  const int16 = new Int16Array(data);
  const f32 = new Float32Array(int16.length);
  for (let i = 0; i < int16.length; i++) {
    f32[i] = int16[i] / 32768;
  }

  // 计算音量
  if (options?.onVolume) {
    let sum = 0;
    for (let i = 0; i < f32.length; i++) sum += Math.abs(f32[i]);
    const avg = sum / f32.length;
    options.onVolume(Math.min(1, avg * 8));
  }

  audioQueue.push(f32);
  if (!isPlaying) playNext(options, playbackGeneration);
}

function playNext(options?: AudioPlaybackOptions, generation = playbackGeneration): void {
  if (generation !== playbackGeneration) return;
  if (audioQueue.length === 0) {
    isPlaying = false;
    currentSource = null;
    options?.onVolume?.(0);
    return;
  }
  isPlaying = true;

  const f32 = audioQueue.shift()!;
  const ctx = getOutputCtx();
  const buf = ctx.createBuffer(1, f32.length, 24000);
  buf.getChannelData(0).set(f32);

  const src = ctx.createBufferSource();
  src.buffer = buf;
  src.connect(ctx.destination);
  currentSource = src;
  src.onended = () => {
    if (generation !== playbackGeneration) return;
    if (currentSource === src) currentSource = null;
    playNext(options, generation);
  };
  src.start();
}

export function stopPlayback(): void {
  stopCurrentPlayback(true);
}
