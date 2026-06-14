/**
 * 表情/嘴型控制处理器
 *
 * - 表情：默认使用关键词模式；可通过 VITE_ENABLE_SENTIMENT_MODEL=true 启用本地 Transformers.js 情感模型
 * - 嘴型：接收 TTS 音频的音量值，映射为 ParamMouthOpenY
 * - 不需要后端对用户文本做任何表情处理
 */
import type { AvatarManifest, ExpressionId } from './avatarManifest.ts';

type SentimentResult = {
  stars: number;
  score: number;
};

const ENABLE_SENTIMENT_MODEL = import.meta.env.VITE_ENABLE_SENTIMENT_MODEL === 'true';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
let sentimentPipeline: ((text: string) => Promise<Array<{ label: string; score: number }>>) | null = null;
let pipelineReady = false;

export async function initSentimentPipeline(): Promise<void> {
  if (pipelineReady) return;

  if (!ENABLE_SENTIMENT_MODEL) {
    pipelineReady = true;
    return;
  }

  try {
    const mod = await import('@huggingface/transformers');
    const pipe = await mod.pipeline(
      'sentiment-analysis',
      'Xenova/bert-base-multilingual-uncased-sentiment',
    );
    sentimentPipeline = (text: string) => pipe(text) as Promise<Array<{ label: string; score: number }>>;
    pipelineReady = true;
    console.log('[ExpressionController] sentiment pipeline ready');
  } catch (e) {
    console.warn('[ExpressionController] failed to load sentiment model, fallback to keyword mode', e);
    pipelineReady = true;
  }
}

async function inferSentiment(text: string): Promise<SentimentResult> {
  if (!sentimentPipeline) {
    return { stars: 3, score: 0 };
  }

  try {
    const results = await sentimentPipeline(text);
    const top = results[0];
    if (!top) return { stars: 3, score: 0 };

    const match = top.label.match(/(\d+)\s*stars?/i);
    const stars = match ? Number(match[1]) : 3;
    return { stars, score: top.score };
  } catch {
    return { stars: 3, score: 0 };
  }
}

function keywordSentiment(text: string): 'positive' | 'negative' | 'neutral' {
  const positive = /(?:开心|高兴|哈哈|太棒|喜欢|快乐|nice|great|love|happy|wonderful|awesome|完美|赞|棒|好)/;
  const negative = /(?:难过|伤心|生气|可恶|讨厌|生气|sad|angry|bad|terrible|hate|awful|烦|累|哭)/;

  const posCount = (text.match(new RegExp(positive, 'gi')) ?? []).length;
  const negCount = (text.match(new RegExp(negative, 'gi')) ?? []).length;

  if (posCount > negCount) return 'positive';
  if (negCount > posCount) return 'negative';
  return 'neutral';
}

const SENTIMENT_TO_EXPRESSION: Record<number, string> = {
  5: 'starry_eyes',
  4: 'happy',
  3: 'neutral',
  2: 'tear',
  1: 'black',
};

const KEYWORD_TO_EXPRESSION: Record<string, string> = {
  positive: 'happy',
  negative: 'black',
  neutral: 'neutral',
};

export async function inferExpression(
  avatar: AvatarManifest,
  text: string,
): Promise<{ id: ExpressionId; file: string }> {
  await initSentimentPipeline();

  let expressionKey: string;

  if (ENABLE_SENTIMENT_MODEL && sentimentPipeline) {
    const { stars } = await inferSentiment(text);
    expressionKey = SENTIMENT_TO_EXPRESSION[stars] ?? 'neutral';
  } else {
    const sentiment = keywordSentiment(text);
    expressionKey = KEYWORD_TO_EXPRESSION[sentiment] ?? 'neutral';
  }

  const expr = avatar.expressions.find((e) => e.id === expressionKey);

  return expr && expr.file
    ? { id: expr.id, file: expr.file }
    : { id: 'neutral', file: '' };
}

export function volumeToMouthOpen(volume: number): number {
  return Math.min(0.95, Math.max(0, volume / 255));
}
