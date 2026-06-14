import { useState } from 'react';
import { TopNav } from '../../components/TopNav';
import { sendSms, login, type AuthTokens } from '../../lib/auth';

type LoginPageProps = {
  onLogin: (tokens: AuthTokens) => void;
};

export function LoginPage({ onLogin }: LoginPageProps) {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [sending, setSending] = useState(false);
  const [countdown, setCountdown] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSendSms() {
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      setError('请输入正确的手机号');
      return;
    }
    setError('');
    setSending(true);
    try {
      await sendSms(phone);
      setCountdown(60);
      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) { clearInterval(timer); return 0; }
          return prev - 1;
        });
      }, 1000);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '发送失败');
    } finally {
      setSending(false);
    }
  }

  async function handleLogin() {
    if (!phone || !code) {
      setError('请输入手机号和验证码');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const tokens = await login(phone, code);
      onLogin(tokens);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '登录失败');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="app-page login-page">
      <TopNav />
      <section className="login-shell" aria-label="登录">
      <div style={{
        width: 360, padding: '32px 28px',
        borderRadius: 16, background: 'rgba(255,255,255,0.05)',
        border: '1px solid rgba(255,255,255,0.08)',
      }}>
        <h2 style={{ margin: '0 0 8px', fontSize: 22, textAlign: 'center' }}>TalkTic</h2>
        <p style={{ margin: '0 0 24px', fontSize: 13, color: '#888', textAlign: 'center' }}>
          登录后开始 AI 口语对话
        </p>

        <div style={{ marginBottom: 16 }}>
          <label style={{ fontSize: 13, color: '#aaa', display: 'block', marginBottom: 6 }}>手机号</label>
          <input
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            placeholder="请输入手机号"
            maxLength={11}
            style={{
              width: '100%', padding: '10px 12px', boxSizing: 'border-box',
              borderRadius: 10, border: '1px solid rgba(255,255,255,0.15)',
              background: 'rgba(255,255,255,0.05)', color: '#eee', fontSize: 15,
            }}
          />
        </div>

        <div style={{ marginBottom: 20 }}>
          <label style={{ fontSize: 13, color: '#aaa', display: 'block', marginBottom: 6 }}>验证码</label>
          <div style={{ display: 'flex', gap: 10 }}>
            <input
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="请输入验证码"
              maxLength={6}
              style={{
                flex: 1, padding: '10px 12px',
                borderRadius: 10, border: '1px solid rgba(255,255,255,0.15)',
                background: 'rgba(255,255,255,0.05)', color: '#eee', fontSize: 15,
              }}
            />
            <button
              onClick={handleSendSms}
              disabled={sending || countdown > 0}
              style={{
                padding: '10px 16px', borderRadius: 10, border: 0,
                background: countdown > 0 ? 'rgba(255,255,255,0.08)' : 'linear-gradient(135deg, #667eea, #764ba2)',
                color: countdown > 0 ? '#888' : '#fff', cursor: countdown > 0 ? 'default' : 'pointer',
                fontSize: 13, whiteSpace: 'nowrap',
              }}
            >
              {sending ? '发送中' : countdown > 0 ? `${countdown}s` : '获取验证码'}
            </button>
          </div>
        </div>

        {error && (
          <div style={{ marginBottom: 16, padding: '8px 12px', borderRadius: 8, background: 'rgba(255,80,80,0.15)', color: '#f66', fontSize: 13 }}>
            {error}
          </div>
        )}

        <button
          onClick={handleLogin}
          disabled={loading}
          style={{
            width: '100%', padding: '12px', borderRadius: 12, border: 0,
            background: 'linear-gradient(135deg, #667eea, #764ba2)',
            color: '#fff', cursor: loading ? 'progress' : 'pointer',
            fontSize: 16, fontWeight: 600,
            opacity: loading ? 0.7 : 1,
          }}
        >
          {loading ? '登录中...' : '登录'}
        </button>
      </div>
      </section>
    </main>
  );
}
