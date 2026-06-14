import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { RefreshCw } from 'lucide-react';
import { TopNav } from '../components/TopNav.tsx';
import { isAuthRequiredError } from '../lib/api.ts';
import { fetchChatHistory } from '../lib/dashboard.ts';
import { formatDuration } from '../lib/session.ts';
import type { ChatHistoryData, ChatHistoryRecord } from '../types/dashboard.ts';

type ProtectedDataPageProps = {
  onAuthRequired: () => void;
};

const pageSize = 20;

function formatDate(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function sceneLabel(record: ChatHistoryRecord) {
  const scene = record.scene || '自由对话';
  const difficulty = record.difficulty ? ` · ${record.difficulty}` : '';
  return `${scene}${difficulty}`;
}

export function HistoryPage({ onAuthRequired }: ProtectedDataPageProps) {
  const [data, setData] = useState<ChatHistoryData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadHistory() {
    setLoading(true);
    setError('');
    try {
      const nextData = await fetchChatHistory(1, pageSize);
      setData(nextData);
    } catch (loadError) {
      if (isAuthRequiredError(loadError)) {
        onAuthRequired();
        return;
      }
      setError(loadError instanceof Error ? loadError.message : '历史记录加载失败');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadHistory();
  }, []);

  const records = data?.records ?? [];

  return (
    <main className="app-page">
      <TopNav />
      <section className="page-section" aria-labelledby="history-title">
        <div className="page-heading">
          <p>History</p>
          <h1 id="history-title">历史聊天得分</h1>
          <span>{data ? `共 ${data.total} 条已结束会话` : '读取你的已结束会话评分'}</span>
        </div>

        {error ? (
          <div className="state-block error-state">
            <span>{error}</span>
            <button type="button" onClick={loadHistory}>
              <RefreshCw size={16} aria-hidden="true" />
              重试
            </button>
          </div>
        ) : null}

        {loading ? (
          <div className="state-block">正在加载历史得分...</div>
        ) : null}

        {!loading && !error && records.length === 0 ? (
          <div className="state-block">还没有已结束的会话，完成一次对话后会显示在这里。</div>
        ) : null}

        {records.length > 0 ? (
          <div className="data-list" aria-label="历史会话列表">
            {records.map((record) => (
              <Link className="data-row history-row" key={record.sessionId} to={`/summary/${record.sessionId}`}>
                <div>
                  <strong>{sceneLabel(record)}</strong>
                  <span>{formatDate(record.createdAt)}</span>
                </div>
                <div>
                  <span>{formatDuration(record.durationSeconds ?? 0)}</span>
                  <span>{record.messageCount ?? 0} 条消息</span>
                </div>
                <b>{record.overallGrade || '-'}</b>
              </Link>
            ))}
          </div>
        ) : null}
      </section>
    </main>
  );
}
