import { useNavigate } from 'react-router-dom';
import { TopNav } from '../components/TopNav.tsx';

const floatingBlocks = Array.from({ length: 30 }, (_, index) => index);

const capabilityTags = ['实时口语指导', '实时字幕记录', '发音评分反馈'];

export function WelcomePage() {
  const navigate = useNavigate();

  return (
    <main className="welcome-page">
      <TopNav />
      <div className="welcome-floating-blocks" aria-hidden="true">
        {floatingBlocks.map((block) => (
          <span key={block} />
        ))}
      </div>

      <section className="welcome-hero" aria-labelledby="welcome-title">
        <div className="welcome-copy">
          <p className="welcome-kicker">TalkTic · Live2D-Language-Coach</p>
          <h1 id="welcome-title">自然开口，稳步提升。</h1>
          <p className="welcome-subtitle">
            进入一场沉浸式英语口语对话练习。TalkTic 结合 Live2D 互动、实时字幕、
            发音反馈与历史评分，让练习过程更自然，也更容易坚持。
          </p>

          <div className="welcome-tags" aria-label="核心能力">
            {capabilityTags.map((tag) => (
              <span key={tag}>{tag}</span>
            ))}
          </div>
        </div>

        <div className="welcome-action">
          <button className="start-talk-link" type="button" onClick={() => navigate('/chat?start=1')}>
            <span>Start To Talk</span>
            <span className="start-talk-arrow" aria-hidden="true">→</span>
          </button>
        </div>
      </section>
    </main>
  );
}
