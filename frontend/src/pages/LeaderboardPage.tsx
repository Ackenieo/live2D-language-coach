import { useEffect, useState } from 'react';
import { RefreshCw } from 'lucide-react';
import { TopNav } from '../components/TopNav.tsx';
import { isAuthRequiredError } from '../lib/api.ts';
import { fetchLeaderboard } from '../lib/dashboard.ts';
import type { LeaderboardData, LeaderboardEntry } from '../types/dashboard.ts';

type LeaderboardPageProps = {
  onAuthRequired: () => void;
};

function AvatarMark({ entry }: { entry: LeaderboardEntry }) {
  const name = entry.nickname || 'User';
  if (entry.avatarUrl) {
    return <img src={entry.avatarUrl} alt={`${name} 的头像`} />;
  }
  return <span>{name.slice(0, 1).toUpperCase()}</span>;
}

function rankLabel(rank: number) {
  if (rank <= 3) return `#${rank}`;
  return String(rank);
}

export function LeaderboardPage({ onAuthRequired }: LeaderboardPageProps) {
  const [data, setData] = useState<LeaderboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function loadLeaderboard() {
    setLoading(true);
    setError('');
    try {
      const nextData = await fetchLeaderboard(1, 20);
      setData(nextData);
    } catch (loadError) {
      if (isAuthRequiredError(loadError)) {
        onAuthRequired();
        return;
      }
      setError(loadError instanceof Error ? loadError.message : '排行榜加载失败');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadLeaderboard();
  }, []);

  const records = data?.records ?? [];

  return (
    <main className="app-page">
      <TopNav />
      <section className="page-section" aria-labelledby="leaderboard-title">
        <div className="page-heading">
          <p>Leaderboard</p>
          <h1 id="leaderboard-title">排行榜</h1>
          <span>按已结束会话的平均表现聚合展示</span>
        </div>

        {error ? (
          <div className="state-block error-state">
            <span>{error}</span>
            <button type="button" onClick={loadLeaderboard}>
              <RefreshCw size={16} aria-hidden="true" />
              重试
            </button>
          </div>
        ) : null}

        {loading ? <div className="state-block">正在加载排行榜...</div> : null}

        {!loading && !error && records.length === 0 ? (
          <div className="state-block">暂无排行数据，结束会话后会开始累计。</div>
        ) : null}

        {records.length > 0 ? (
          <div className="data-list" aria-label="排行榜列表">
            {records.map((entry) => (
              <div className="data-row leaderboard-row" key={entry.userId}>
                <b>{rankLabel(entry.rank)}</b>
                <div className="avatar-mark">
                  <AvatarMark entry={entry} />
                </div>
                <div>
                  <strong>{entry.nickname || '未命名用户'}</strong>
                  <span>{entry.sessionCount ?? 0} 次会话</span>
                </div>
                <strong>{entry.avgGrade || '-'}</strong>
              </div>
            ))}
          </div>
        ) : null}

        {data?.myRank ? (
          <div className="my-rank" aria-label="我的排名">
            <span>我的排名</span>
            <b>{rankLabel(data.myRank.rank)}</b>
            <strong>{data.myRank.nickname || '我'}</strong>
            <em>{data.myRank.avgGrade || '-'}</em>
          </div>
        ) : null}
      </section>
    </main>
  );
}
