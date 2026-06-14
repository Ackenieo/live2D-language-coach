import { Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { LoginPage } from './modules/auth/LoginPage.tsx';
import { getStoredAuth, logout as doLogout } from './lib/auth.ts';
import type { AuthTokens } from './lib/auth.ts';
import { type ReactNode, useCallback, useState } from 'react';
import { CallPage } from './pages/CallPage.tsx';
import { SessionSummaryPage } from './pages/SessionSummaryPage.tsx';
import { WelcomePage } from './pages/WelcomePage.tsx';
import { HistoryPage } from './pages/HistoryPage.tsx';
import { LeaderboardPage } from './pages/LeaderboardPage.tsx';
import { ProfilePage } from './pages/ProfilePage.tsx';
import type { SessionSummary } from './types/session.ts';

function ProtectedRoute({ isLoggedIn, children }: { isLoggedIn: boolean; children: ReactNode }) {
  const location = useLocation();

  if (!isLoggedIn) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: `${location.pathname}${location.search}` }}
      />
    );
  }

  return children;
}

function AppShell() {
  const navigate = useNavigate();
  const location = useLocation();
  const [auth, setAuth] = useState<AuthTokens | null>(() => getStoredAuth());
  const [latestSessionSummary, setLatestSessionSummary] = useState<SessionSummary | null>(null);
  const isLoggedIn = !!auth?.accessToken;

  function handleLogin(tokens: AuthTokens) {
    setAuth(tokens);
    const from = (location.state as { from?: string } | null)?.from;
    navigate(from || '/welcome', { replace: true });
  }

  function handleAuthUpdate(tokens: AuthTokens) {
    setAuth(tokens);
  }

  const handleLogout = useCallback(() => {
    doLogout();
    setAuth(null);
    setLatestSessionSummary(null);
    navigate('/welcome');
  }, [navigate]);

  const handleSessionEnd = useCallback((summary: SessionSummary) => {
    setLatestSessionSummary(summary);
    navigate(`/summary/${summary.sessionId}`);
  }, [navigate]);

  const handleStartNewSession = useCallback(() => {
    setLatestSessionSummary(null);
    navigate('/chat');
  }, [navigate]);

  const handleAuthRequired = useCallback(() => {
    doLogout();
    setAuth(null);
    navigate('/login', {
      replace: true,
      state: { from: `${location.pathname}${location.search}` },
    });
  }, [location.pathname, location.search, navigate]);

  return (
    <Routes>
      <Route path="/" element={<WelcomePage />} />
      <Route path="/welcome" element={<WelcomePage />} />
      <Route path="/login" element={isLoggedIn ? <Navigate to="/welcome" replace /> : <LoginPage onLogin={handleLogin} />} />
      <Route
        path="/chat"
        element={(
          <ProtectedRoute isLoggedIn={isLoggedIn}>
            <CallPage auth={auth} onLogout={handleLogout} onSessionEnd={handleSessionEnd} />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/history"
        element={(
          <ProtectedRoute isLoggedIn={isLoggedIn}>
            <HistoryPage onAuthRequired={handleAuthRequired} />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/leaderboard"
        element={(
          <ProtectedRoute isLoggedIn={isLoggedIn}>
            <LeaderboardPage onAuthRequired={handleAuthRequired} />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/profile"
        element={(
          <ProtectedRoute isLoggedIn={isLoggedIn}>
            <ProfilePage
              onAuthRequired={handleAuthRequired}
              onAuthUpdate={handleAuthUpdate}
              onLogout={handleLogout}
            />
          </ProtectedRoute>
        )}
      />
      <Route
        path="/summary/:sessionId"
        element={(
          <ProtectedRoute isLoggedIn={isLoggedIn}>
            <SessionSummaryPage
              initialSummary={latestSessionSummary}
              onStartNewSession={handleStartNewSession}
              onAuthRequired={handleAuthRequired}
            />
          </ProtectedRoute>
        )}
      />
      <Route path="*" element={<Navigate to="/welcome" replace />} />
    </Routes>
  );
}

export default function App() {
  return <AppShell />;
}
