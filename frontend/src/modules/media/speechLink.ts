import type { LipFrameParams } from '../live2d/live2dEngine.ts';

type AudioPlaybackOptions = {
  interrupt?: boolean;
  onVolume?: (volume: number) => void;
};

type Viseme = 'sil' | 'A' | 'I' | 'U' | 'E' | 'O';

type LipTimelineFrame = {
  timeSec: number;
  envelope: number;
  viseme: Viseme;
};

type SeededMouthFrame = {
  viseme: Exclude<Viseme, 'sil'>;
  envelope: number;
  mouthScale: number;
};

const SAMPLE_RATE = 24000;
const FRAME_MS = 10;
const FRAME_SIZE = SAMPLE_RATE * FRAME_MS / 1000;
const SCHEDULE_AHEAD_SEC = 0.025;
const RELEASE_FACTOR = 0.28;
const ATTACK_FACTOR = 0.68;
const NOISE_GATE = 0.035;
const SEEDED_MOUTH_STEP_SEC = 0.2;
const SEEDED_MOUTH_STEP_FRAMES = Math.max(1, Math.round(SEEDED_MOUTH_STEP_SEC * 1000 / FRAME_MS));
const SEEDED_MOUTH_PATTERN_SIZE = 96;
const SEEDED_MOUTH_BASE_SEED = 0x51ee_d622;
const SEEDED_VISEMES: Array<Exclude<Viseme, 'sil'>> = ['A', 'I', 'U', 'E', 'O'];

const SILENCE: LipFrameParams = {
  ParamMouthOpenY: 0,
  ParamJawOpen: 0,
  ParamMouthForm: 0,
  ParamMouthpucker: 0,
  ParamMouthShrug: 0,
  ParamMouthX: 0,
};

const VISEME_SHAPES: Record<Viseme, LipFrameParams> = {
  sil: SILENCE,
  A: {
    ParamMouthOpenY: 1.0,
    ParamJawOpen: 0.9,
    ParamMouthForm: 0,
    ParamMouthpucker: 0,
    ParamMouthShrug: 0,
    ParamMouthX: 0,
  },
  I: {
    ParamMouthOpenY: 0.35,
    ParamJawOpen: 0.15,
    ParamMouthForm: 1.0,
    ParamMouthpucker: -0.3,
    ParamMouthShrug: 0,
    ParamMouthX: 0.05,
  },
  U: {
    ParamMouthOpenY: 0.35,
    ParamJawOpen: 0.25,
    ParamMouthForm: -0.2,
    ParamMouthpucker: 1.0,
    ParamMouthShrug: 0.2,
    ParamMouthX: 0,
  },
  E: {
    ParamMouthOpenY: 0.55,
    ParamJawOpen: 0.35,
    ParamMouthForm: 0.7,
    ParamMouthpucker: -0.2,
    ParamMouthShrug: 0,
    ParamMouthX: 0.04,
  },
  O: {
    ParamMouthOpenY: 0.75,
    ParamJawOpen: 0.55,
    ParamMouthForm: -0.2,
    ParamMouthpucker: 0.85,
    ParamMouthShrug: 0,
    ParamMouthX: 0,
  },
};

const SEEDED_MOUTH_PATTERN = buildSeededMouthPattern(
  SEEDED_MOUTH_BASE_SEED,
  SEEDED_MOUTH_PATTERN_SIZE,
);

let outputCtx: AudioContext | null = null;
let outputAnalyser: AnalyserNode | null = null;
let analyserMonitorGain: GainNode | null = null;
let analyserBuffer: Float32Array | null = null;
let nextStartTime = 0;
let sources = new Set<AudioBufferSourceNode>();
let lipFrames: LipTimelineFrame[] = [];
let rafId: number | null = null;
let playbackGeneration = 0;
let lipFrameSink: ((params: LipFrameParams) => void) | null = null;
let volumeSink: ((volume: number) => void) | null = null;
let textVisemes: Viseme[] = [];
let currentViseme: Viseme = 'A';
let visemeHoldFrames = 0;
let lastEnvelope = 0;
let liveEnvelope = 0;
let lastRenderedViseme: Viseme = 'A';
let playbackSeedStartTime = 0;

export function setLipFrameSink(sink: ((params: LipFrameParams) => void) | null): void {
  lipFrameSink = sink;
  if (sink) {
    sink(SILENCE);
  }
}

export function appendSpeechText(text: string): void {
  textVisemes.push(...textToVisemes(text));
}

export function resetSpeechText(): void {
  textVisemes = [];
  currentViseme = 'A';
  visemeHoldFrames = 0;
}

export function prepareSpeechPlayback(): void {
  void getOutputCtx().resume();
}

export function playSpeechAudioChunk(data: ArrayBuffer, options?: AudioPlaybackOptions): void {
  if (options?.interrupt) {
    interruptSpeechPlayback();
  }
  if (options?.onVolume) {
    volumeSink = options.onVolume;
  }

  const ctx = getOutputCtx();
  const audio = int16ToFloat32(data);
  if (audio.length === 0) {
    return;
  }

  const startTime = Math.max(ctx.currentTime + SCHEDULE_AHEAD_SEC, nextStartTime || 0);
  const durationSec = audio.length / SAMPLE_RATE;
  if (nextStartTime === 0) {
    playbackSeedStartTime = startTime;
  }
  nextStartTime = startTime + durationSec;

  lipFrames.push(...buildLipFrames(audio, startTime));

  const buffer = ctx.createBuffer(1, audio.length, SAMPLE_RATE);
  buffer.getChannelData(0).set(audio);

  const source = ctx.createBufferSource();
  const generation = playbackGeneration;
  source.buffer = buffer;
  source.connect(ctx.destination);
  source.connect(getOutputAnalyser(ctx));
  sources.add(source);
  source.onended = () => {
    sources.delete(source);
    if (generation === playbackGeneration) {
      startFrameLoop();
    }
  };
  source.start(startTime);
  startFrameLoop();
}

export function interruptSpeechPlayback(): void {
  stopPlaybackInternal(false, false);
}

export function stopSpeechPlayback(): void {
  stopPlaybackInternal(true, true);
}

function getOutputCtx(): AudioContext {
  if (!outputCtx) {
    outputCtx = new AudioContext({ sampleRate: SAMPLE_RATE });
  }
  if (outputCtx.state === 'suspended') {
    void outputCtx.resume();
  }
  return outputCtx;
}

function getOutputAnalyser(ctx: AudioContext): AnalyserNode {
  if (!outputAnalyser) {
    outputAnalyser = ctx.createAnalyser();
    outputAnalyser.fftSize = 1024;
    outputAnalyser.smoothingTimeConstant = 0;
    analyserMonitorGain = ctx.createGain();
    analyserMonitorGain.gain.value = 0;
    outputAnalyser.connect(analyserMonitorGain);
    analyserMonitorGain.connect(ctx.destination);
    analyserBuffer = new Float32Array(outputAnalyser.fftSize);
  }
  return outputAnalyser;
}

function int16ToFloat32(data: ArrayBuffer): Float32Array {
  const int16 = new Int16Array(data);
  const f32 = new Float32Array(int16.length);
  for (let i = 0; i < int16.length; i += 1) {
    f32[i] = int16[i] / 32768;
  }
  return f32;
}

function buildLipFrames(audio: Float32Array, startTime: number): LipTimelineFrame[] {
  const frames: LipTimelineFrame[] = [];
  for (let offset = 0; offset < audio.length; offset += FRAME_SIZE) {
    const end = Math.min(offset + FRAME_SIZE, audio.length);
    const frameTime = startTime + offset / SAMPLE_RATE;
    const rawEnvelope = normalizeRms(rms(audio, offset, end));
    const envelope = smoothEnvelope(rawEnvelope);
    const viseme = envelope > NOISE_GATE ? nextViseme(frameTime) : 'sil';
    frames.push({
      timeSec: frameTime,
      envelope,
      viseme,
    });
  }
  return frames;
}

function rms(audio: Float32Array, start: number, end: number): number {
  let sum = 0;
  const length = Math.max(1, end - start);
  for (let i = start; i < end; i += 1) {
    sum += audio[i] * audio[i];
  }
  return Math.sqrt(sum / length);
}

function normalizeRms(value: number): number {
  return Math.max(0, Math.min(1, (value - 0.008) * 16));
}

function smoothEnvelope(next: number): number {
  const factor = next > lastEnvelope ? ATTACK_FACTOR : RELEASE_FACTOR;
  lastEnvelope = lastEnvelope + (next - lastEnvelope) * factor;
  return lastEnvelope < NOISE_GATE ? 0 : lastEnvelope;
}

function nextViseme(now: number): Viseme {
  if (visemeHoldFrames > 0) {
    visemeHoldFrames -= 1;
    return currentViseme;
  }

  let usedSeedFallback = false;
  const next = textVisemes.shift();
  if (next && next !== 'sil') {
    currentViseme = next;
  } else if (next === 'sil') {
    currentViseme = 'sil';
  } else {
    currentViseme = seededMouthFrame(now).viseme;
    usedSeedFallback = true;
  }

  visemeHoldFrames = currentViseme === 'sil'
    ? 2
    : usedSeedFallback
      ? SEEDED_MOUTH_STEP_FRAMES - 1
      : 6;
  return currentViseme;
}

function textToVisemes(text: string): Viseme[] {
  const visemes: Viseme[] = [];
  const lower = text.toLowerCase();
  for (let i = 0; i < lower.length; i += 1) {
    const ch = lower[i];
    const next = lower[i + 1] ?? '';
    if (/[,.!?;:]/.test(ch)) {
      visemes.push('sil');
    } else if (ch === 'o') {
      visemes.push(next === 'o' ? 'U' : 'O');
    } else if (ch === 'u') {
      visemes.push('U');
    } else if (ch === 'i' || ch === 'y') {
      visemes.push('I');
    } else if (ch === 'e') {
      visemes.push(next === 'e' ? 'I' : 'E');
    } else if (ch === 'a') {
      visemes.push('A');
    }
  }
  return visemes;
}

function applyEnvelope(shape: LipFrameParams, envelope: number, now: number): LipFrameParams {
  const params: LipFrameParams = {};
  const mouthEnvelope = scaleMouthEnvelope(envelope, now);
  for (const [key, value] of Object.entries(SILENCE)) {
    if (typeof value === 'number') {
      params[key as keyof LipFrameParams] = value;
    }
  }
  for (const [key, value] of Object.entries(shape)) {
    if (typeof value === 'number') {
      const frameEnvelope = isMouthOpenParam(key) ? mouthEnvelope : envelope;
      params[key as keyof LipFrameParams] = value * frameEnvelope;
    }
  }
  return params;
}

function readLivePlaybackEnvelope(): number {
  if (!outputAnalyser) {
    return 0;
  }
  if (!analyserBuffer || analyserBuffer.length !== outputAnalyser.fftSize) {
    analyserBuffer = new Float32Array(outputAnalyser.fftSize);
  }

  outputAnalyser.getFloatTimeDomainData(analyserBuffer);
  let sum = 0;
  for (let i = 0; i < analyserBuffer.length; i += 1) {
    sum += analyserBuffer[i] * analyserBuffer[i];
  }

  const next = normalizeRms(Math.sqrt(sum / analyserBuffer.length));
  const factor = next > liveEnvelope ? ATTACK_FACTOR : RELEASE_FACTOR;
  liveEnvelope += (next - liveEnvelope) * factor;
  return liveEnvelope < NOISE_GATE ? 0 : liveEnvelope;
}

function scaleMouthEnvelope(envelope: number, now: number): number {
  if (envelope <= 0) {
    return 0;
  }

  return Math.max(0, Math.min(1, envelope * seededMouthFrame(now).mouthScale));
}

function isMouthOpenParam(paramId: string): boolean {
  return paramId === 'ParamMouthOpenY' || paramId === 'ParamJawOpen';
}

function buildSeededMouthPattern(seed: number, size: number): SeededMouthFrame[] {
  let state = seed >>> 0;
  const pattern: SeededMouthFrame[] = [];
  let previousViseme: Exclude<Viseme, 'sil'> | null = null;

  for (let i = 0; i < size; i += 1) {
    state = nextSeed(state);
    let viseme = SEEDED_VISEMES[state % SEEDED_VISEMES.length];
    if (viseme === previousViseme) {
      viseme = SEEDED_VISEMES[(SEEDED_VISEMES.indexOf(viseme) + 1) % SEEDED_VISEMES.length];
    }

    state = nextSeed(state);
    const envelope = 0.22 + normalizedSeed(state) * 0.28;

    state = nextSeed(state);
    const mouthScale = 0.86 + normalizedSeed(state) * 0.34;

    pattern.push({ viseme, envelope, mouthScale });
    previousViseme = viseme;
  }

  return pattern;
}

function nextSeed(seed: number): number {
  return (Math.imul(seed, 1664525) + 1013904223) >>> 0;
}

function normalizedSeed(seed: number): number {
  return seed / 0x100000000;
}

function seededMouthFrame(now: number): SeededMouthFrame {
  const elapsed = Math.max(0, now - playbackSeedStartTime);
  const index = Math.floor(elapsed / SEEDED_MOUTH_STEP_SEC) % SEEDED_MOUTH_PATTERN.length;
  return SEEDED_MOUTH_PATTERN[index];
}

function startFrameLoop(): void {
  if (rafId !== null) {
    return;
  }

  const generation = playbackGeneration;
  const tick = () => {
    rafId = null;
    if (generation !== playbackGeneration || !outputCtx) {
      return;
    }

    const now = outputCtx.currentTime;
    const playbackActive = isPlaybackActive(now);
    while (lipFrames.length > 1 && lipFrames[1].timeSec <= now) {
      lipFrames.shift();
    }

    const frame = lipFrames[0];
    const hasTimelineFrame = Boolean(frame && frame.timeSec <= now);
    const livePlaybackEnvelope = readLivePlaybackEnvelope();
    if (hasTimelineFrame || livePlaybackEnvelope > 0 || playbackActive) {
      const seededFrame = seededMouthFrame(now);
      const timelineEnvelope = hasTimelineFrame && frame ? frame.envelope : 0;
      let envelope = Math.max(timelineEnvelope, livePlaybackEnvelope);
      if (playbackActive && envelope <= NOISE_GATE) {
        envelope = seededFrame.envelope;
      }
      const frameViseme = hasTimelineFrame && frame ? frame.viseme : seededFrame.viseme;
      const viseme = envelope > NOISE_GATE ? nonSilentViseme(frameViseme) : 'sil';
      lastRenderedViseme = viseme === 'sil' ? lastRenderedViseme : viseme;
      lipFrameSink?.(applyEnvelope(VISEME_SHAPES[viseme], envelope, now));
      volumeSink?.(envelope);
      if (lipFrames.length > 1 || playbackActive) {
        requestNextFrame(tick);
        return;
      }
    }

    if (playbackActive) {
      requestNextFrame(tick);
      return;
    }

    lipFrames = [];
    nextStartTime = 0;
    lastEnvelope = 0;
    liveEnvelope = 0;
    lastRenderedViseme = 'A';
    playbackSeedStartTime = 0;
    lipFrameSink?.(SILENCE);
    volumeSink?.(0);
  };

  requestNextFrame(tick);
}

function requestNextFrame(callback: FrameRequestCallback): void {
  rafId = window.requestAnimationFrame(callback);
}

function isPlaybackActive(now: number): boolean {
  return sources.size > 0 || (nextStartTime > 0 && now < nextStartTime + 0.08);
}

function nonSilentViseme(viseme: Viseme): Viseme {
  return viseme === 'sil' ? lastRenderedViseme : viseme;
}

function stopPlaybackInternal(closeContext: boolean, resetText: boolean): void {
  playbackGeneration += 1;
  if (rafId !== null) {
    window.cancelAnimationFrame(rafId);
    rafId = null;
  }

  for (const source of sources) {
    source.onended = null;
    try {
      source.stop();
    } catch {
      // The source may already have ended.
    }
    try {
      source.disconnect();
    } catch {
      // The source may already have disconnected.
    }
  }

  sources = new Set<AudioBufferSourceNode>();
  lipFrames = [];
  nextStartTime = 0;
  lastEnvelope = 0;
  liveEnvelope = 0;
  lastRenderedViseme = 'A';
  playbackSeedStartTime = 0;
  if (resetText) {
    resetSpeechText();
  }
  lipFrameSink?.(SILENCE);
  volumeSink?.(0);

  if (closeContext && outputCtx) {
    try {
      outputAnalyser?.disconnect();
    } catch {
      // The analyser may already have disconnected.
    }
    try {
      analyserMonitorGain?.disconnect();
    } catch {
      // The monitor gain may already have disconnected.
    }
    void outputCtx.close();
    outputCtx = null;
    outputAnalyser = null;
    analyserMonitorGain = null;
    analyserBuffer = null;
  }
}
