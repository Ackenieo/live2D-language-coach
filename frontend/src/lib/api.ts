import { getAccessToken } from './auth.ts';

type ApiEnvelope<T> = {
  success: boolean;
  data?: T;
  message?: string | null;
};

export class AuthRequiredError extends Error {
  constructor(message = '登录已过期，请重新登录') {
    super(message);
    this.name = 'AuthRequiredError';
  }
}

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export function isAuthRequiredError(error: unknown) {
  return error instanceof AuthRequiredError;
}

export async function fetchWithAuth<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getAccessToken();
  if (!token) {
    throw new AuthRequiredError();
  }

  const headers = new Headers(options.headers);
  headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(path, {
    ...options,
    headers,
  });

  let json: ApiEnvelope<T> | null = null;
  try {
    json = await res.json() as ApiEnvelope<T>;
  } catch {
    json = null;
  }

  if (res.status === 401) {
    throw new AuthRequiredError(json?.message || undefined);
  }

  if (!res.ok || !json?.success) {
    throw new ApiError(json?.message || `请求失败 (${res.status})`, res.status);
  }

  return json.data as T;
}
