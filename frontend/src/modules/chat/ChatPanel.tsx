import { useEffect, useRef } from 'react';
import type { RealtimeMessage } from '../../types/session.ts';

type ChatPanelProps = {
  messages: RealtimeMessage[];
};

function labelForMessage(msg: RealtimeMessage) {
  if (msg.role === 'user') return 'You';
  if (msg.role === 'assistant') return msg.isStreaming ? 'Assistant · streaming' : 'Assistant';
  return 'System';
}

function bubbleClassName(msg: RealtimeMessage) {
  return `transcript-bubble ${msg.role}`;
}

export function ChatPanel({ messages }: ChatPanelProps) {
  const listRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const el = listRef.current;
    if (!el) return;
    el.scrollTop = el.scrollHeight;
  }, [messages]);

  return (
    <div className="transcript-panel">
      <div className="transcript-header">
        <div>实时字幕</div>
        <span>展示语音识别、AI 回复、语法纠错与发音评分</span>
      </div>

      <div ref={listRef} className="transcript-list">
        {messages.map((msg) => (
          <div key={msg.id} className={bubbleClassName(msg)}>
            <div className="transcript-label">{labelForMessage(msg)}</div>
            <div>{msg.content || '...'}</div>
            {msg.grammarCorrection && (
              <div className="transcript-meta">
                语法纠正：{msg.grammarCorrection}
              </div>
            )}
            {msg.pronunciationScore && (
              <div className="transcript-score">
                {msg.pronunciationScore}
              </div>
            )}
          </div>
        ))}
        {messages.length === 0 && (
          <div className="transcript-empty">
            开始通话后，实时字幕和评分会显示在这里
          </div>
        )}
      </div>
    </div>
  );
}
