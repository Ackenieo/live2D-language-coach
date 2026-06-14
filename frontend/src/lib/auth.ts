/**
 * 认证模块
 * - 短信验证码发送
 * - 手机号+验证码登录
 * - Token 管理（存储、刷新）
 */

const STORAGE_KEY = 'tt_auth';

export type AuthTokens = {
  accessToken: string;
  refreshToken: string;
  userId: string;
  phone: string;
};

function loadTokens(): AuthTokens | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as AuthTokens;
  } catch {
    return null;
  }
}

function saveTokens(tokens: AuthTokens) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
}

function clearTokens() {
  localStorage.removeItem(STORAGE_KEY);
}

export async function sendSms(phone: string): Promise<void> {
  const res = await fetch('/api/auth/sms/send', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phone }),
  });
  const json = await res.json();
  if (!json.success) {
    throw new Error(json.message || '发送验证码失败');
  }
}

export async function login(phone: string, code: string): Promise<AuthTokens> {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ phone, code }),
  });
  const json = await res.json();
  if (!json.success) {
    throw new Error(json.message || '登录失败');
  }
  const tokens = json.data as AuthTokens;
  saveTokens(tokens);
  return tokens;
}

export async function refreshAccessToken(): Promise<string> {
  const tokens = loadTokens();
  if (!tokens?.refreshToken) {
    throw new Error('无刷新令牌');
  }
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: tokens.refreshToken }),
  });
  const json = await res.json();
  if (!json.success) {
    clearTokens();
    throw new Error(json.message || '刷新令牌失效');
  }
  const newTokens = json.data as AuthTokens;
  saveTokens(newTokens);
  return newTokens.accessToken;
}

export function getAccessToken(): string | null {
  return loadTokens()?.accessToken ?? null;
}

export function getStoredAuth(): AuthTokens | null {
  return loadTokens();
}

export function storeAuthTokens(tokens: AuthTokens) {
  saveTokens(tokens);
}

export function logout() {
  clearTokens();
}
