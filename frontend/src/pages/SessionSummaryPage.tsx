import { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { TopNav } from '../components/TopNav.tsx';
import { isAuthRequiredError } from '../lib/api.ts';
import { fetchSessionReport } from '../lib/session.ts';
import type { SessionSummary } from '../types/session.ts';

function SummaryCard({
  label,
  value,
  emphasize = false,
}: {
  label: string;
  value: string;
  emphasize?: boolean;
}) {
  return (
    <div style={{
      minHeight: emphasize ? 178 : 154,
      padding: emphasize ? '24px 26px' : '22px 24px',
      borderRadius: 28,
      background: emphasize ? 'linear-gradient(180deg, rgba(255,255,255,0.09), rgba(255,255,255,0.05))' : 'rgba(255,255,255,0.05)',
      border: '1px solid rgba(255,255,255,0.09)',
      boxShadow: 'inset 0 1px 0 rgba(255,255,255,0.02)',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between',
    }}>
      <div style={{ fontSize: 12, letterSpacing: 0, textTransform: 'uppercase', color: '#8f9aac' }}>{label}</div>
      <div style={{ fontSize: emphasize ? 72 : 48, fontWeight: 780, lineHeight: 0.92, color: '#f3f6fb', letterSpacing: 0 }}>{value || '-'}</div>
    </div>
  );
}

function InfoCard({ label, value }: { label: string; value: string }) {
  return (
    <div style={{
      padding: '22px 24px',
      borderRadius: 24,
      background: 'rgba(255,255,255,0.045)',
      border: '1px solid rgba(255,255,255,0.08)',
    }}>
      <div style={{ fontSize: 13, color: '#96a0af', marginBottom: 12 }}>{label}</div>
      <div style={{ fontSize: 28, fontWeight: 680, color: '#f3f6fb', letterSpacing: 0 }}>{value}</div>
    </div>
  );
}

function SuggestionsCard({ suggestions }: { suggestions: string[] }) {
  return (
    <div style={{
      padding: '22px 24px',
      borderRadius: 24,
      background: 'rgba(255,255,255,0.045)',
      border: '1px solid rgba(255,255,255,0.08)',
      minHeight: 180,
    }}>
      <div style={{ fontSize: 13, color: '#96a0af', marginBottom: 14 }}>改进建议</div>
      {suggestions.length ? (
        <ul style={{ margin: 0, paddingLeft: 24, color: '#eef3f9', lineHeight: 1.9, fontSize: 16 }}>
          {suggestions.map((item, index) => (
            <li key={`${item}-${index}`} style={{ marginBottom: 10 }}>{item}</li>
          ))}
        </ul>
      ) : (
        <div style={{ color: '#bcc5d2', fontSize: 16, lineHeight: 1.8 }}>暂无建议。</div>
      )}
    </div>
  );
}

function SummarySkeleton() {
  return (
    <div style={{
      padding: '22px 24px',
      borderRadius: 24,
      background: 'rgba(255,255,255,0.035)',
      border: '1px solid rgba(255,255,255,0.06)',
      color: '#8f9aac',
      fontSize: 14,
      lineHeight: 1.8,
    }}>
      正在生成本次对话总结...
    </div>
  );
}

function formatSummaryDuration(durationSeconds: number) {
  if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) return '-';
  const minutes = Math.floor(durationSeconds / 60);
  const seconds = durationSeconds % 60;
  return `${minutes} 分 ${String(seconds).padStart(2, '0')} 秒`;
}

export function SessionSummaryPage({
  initialSummary,
  onStartNewSession,
  onAuthRequired,
}: {
  initialSummary: SessionSummary | null;
  onStartNewSession: () => void;
  onAuthRequired: () => void;
}) {
  const { sessionId = '' } = useParams();
  const [summary, setSummary] = useState<SessionSummary | null>(initialSummary && initialSummary.sessionId === sessionId ? initialSummary : null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setSummary(initialSummary && initialSummary.sessionId === sessionId ? initialSummary : null);
  }, [initialSummary, sessionId]);

  useEffect(() => {
    let cancelled = false;

    async function loadReport() {
      if (!sessionId) {
        setError('缺少会话 ID，无法加载总结页面。');
        return;
      }

      setLoading(true);
      setError('');
      try {
        const report = await fetchSessionReport(sessionId);
        if (cancelled) return;
        setSummary((prev) => ({
          sessionId,
          overallGrade: report.overallGrade ?? prev?.overallGrade ?? '-',
          accuracyGrade: report.accuracyGrade ?? prev?.accuracyGrade ?? '-',
          fluencyGrade: report.fluencyGrade ?? prev?.fluencyGrade ?? '-',
          completenessGrade: report.completenessGrade ?? prev?.completenessGrade ?? '-',
          durationSeconds: report.durationSeconds ?? prev?.durationSeconds ?? 0,
          turns: prev?.turns || report.turns || 0,
          summary: prev?.summary ?? '本次会话暂无总结。',
          suggestions: report.suggestions?.length ? report.suggestions : (prev?.suggestions ?? []),
        }));
      } catch (loadError) {
        if (cancelled) return;
        if (isAuthRequiredError(loadError)) {
          onAuthRequired();
          return;
        }
        console.error(loadError);
        setError(loadError instanceof Error ? loadError.message : '获取会话总结失败。');
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void loadReport();
    return () => {
      cancelled = true;
    };
  }, [onAuthRequired, sessionId]);

  const suggestions = summary?.suggestions ?? [];
  const introText = useMemo(() => {
    if (summary?.summary && summary.summary !== '本次会话暂无总结。') {
      return '本次对话已完成，下面是你的评分、时长和下一步练习建议。';
    }
    return '正在整理本次对话的评分、时长和反馈建议。';
  }, [summary?.summary]);

  const shellPadding = summary ? '34px 40px 30px' : '30px 26px 26px';
  const headingFontSize = summary ? 56 : 42;
  const topGridColumns = summary ? 'minmax(220px, 1.15fr) repeat(3, minmax(160px, 0.9fr))' : 'repeat(auto-fit, minmax(180px, 1fr))';
  const lowerGridColumns = summary ? 'repeat(auto-fit, minmax(280px, 1fr))' : '1fr';

  return (
    <div style={{
      minHeight: '100vh',
      background: 'radial-gradient(circle at 12% 8%, rgba(74, 94, 132, 0.22), transparent 26%), radial-gradient(circle at 88% 100%, rgba(38, 52, 78, 0.2), transparent 30%), linear-gradient(180deg, #091018 0%, #0b1219 100%)',
      color: '#eaf0f8',
      fontFamily: 'system-ui, sans-serif',
      padding: '24px 16px',
      boxSizing: 'border-box',
    }}>
      <TopNav />
      <div style={{
        width: 'min(980px, 100%)',
        margin: '0 auto',
        padding: shellPadding,
        borderRadius: 36,
        background: 'rgba(13,19,27,0.88)',
        border: '1px solid rgba(255,255,255,0.08)',
        boxShadow: '0 40px 90px rgba(0,0,0,0.36)',
        backdropFilter: 'blur(10px)',
        boxSizing: 'border-box',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 18, marginBottom: 18, flexWrap: 'wrap' }}>
          <div style={{ minWidth: 0, flex: '1 1 420px' }}>
            <h1 style={{ margin: 0, fontSize: headingFontSize, lineHeight: 0.96, letterSpacing: 0, color: '#f6f8fb', wordBreak: 'break-word' }}>会话总结</h1>
            <p style={{ margin: '18px 0 0', maxWidth: 620, fontSize: summary ? 18 : 16, lineHeight: 1.7, color: '#98a3b4' }}>
              {introText}
            </p>
          </div>
          <div style={{
            padding: '12px 22px',
            borderRadius: 999,
            border: '1px solid rgba(148,162,184,0.22)',
            background: 'rgba(255,255,255,0.03)',
            color: '#aeb7c4',
            fontSize: 13,
            letterSpacing: 0,
            textTransform: 'uppercase',
            whiteSpace: 'nowrap',
            flexShrink: 0,
          }}>
            已完成
          </div>
        </div>

        {error && (
          <div style={{ marginBottom: 18, padding: '12px 16px', borderRadius: 14, background: 'rgba(255,120,120,0.12)', border: '1px solid rgba(255,120,120,0.22)', color: '#ffb4b4', fontSize: 13 }}>
            {error}
          </div>
        )}

        {!summary && loading ? (
          <div style={{ marginBottom: 18 }}>
            <SummarySkeleton />
          </div>
        ) : null}

        <div style={{ display: 'grid', gridTemplateColumns: topGridColumns, gap: 18, marginBottom: 18 }}>
          <SummaryCard label="综合评分" value={summary?.overallGrade ?? '-'} emphasize />
          <SummaryCard label="准确度" value={summary?.accuracyGrade ?? '-'} />
          <SummaryCard label="流利度" value={summary?.fluencyGrade ?? '-'} />
          <SummaryCard label="完整度" value={summary?.completenessGrade ?? '-'} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 18, marginBottom: 18 }}>
          <InfoCard label="通话时长" value={formatSummaryDuration(summary?.durationSeconds ?? 0)} />
          <InfoCard label="对话轮次" value={summary?.turns ? `${summary.turns} 次` : '-'} />
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: lowerGridColumns, gap: 18, marginBottom: 20 }}>
          <SuggestionsCard suggestions={suggestions} />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
          <button
            onClick={onStartNewSession}
            style={{
              padding: '12px 22px',
              borderRadius: 14,
              border: 0,
              background: 'linear-gradient(135deg, #667eea, #764ba2)',
              color: '#fff',
              fontSize: 15,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            开始新对话
          </button>
          {loading && <span style={{ fontSize: 13, color: '#94a0b2' }}>正在同步最终报告...</span>}
        </div>
      </div>
    </div>
  );
}
