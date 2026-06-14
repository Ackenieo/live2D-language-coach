import { fetchWithAuth } from './api.ts';
import type { AuthTokens } from './auth.ts';
import type {
  ChatHistoryData,
  LeaderboardData,
  UserProfile,
} from '../types/dashboard.ts';

type AvatarUploadResponse = {
  avatarUrl: string;
};

export function fetchChatHistory(page = 1, pageSize = 20) {
  return fetchWithAuth<ChatHistoryData>(`/api/chat/history?page=${page}&pageSize=${pageSize}`);
}

export function fetchLeaderboard(page = 1, pageSize = 20) {
  return fetchWithAuth<LeaderboardData>(`/api/leaderboard?page=${page}&pageSize=${pageSize}`);
}

export function fetchUserProfile() {
  return fetchWithAuth<UserProfile>('/api/user/profile');
}

export function updateUserProfile(nickname: string) {
  return fetchWithAuth<UserProfile>('/api/user/profile', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ nickname }),
  });
}

export function changeUserPhone(newPhone: string, code: string) {
  return fetchWithAuth<AuthTokens>('/api/user/phone', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ newPhone, code }),
  });
}

export async function uploadUserAvatar(file: File) {
  const formData = new FormData();
  formData.append('file', file);

  const data = await fetchWithAuth<AvatarUploadResponse>('/api/user/avatar', {
    method: 'POST',
    body: formData,
  });

  return data.avatarUrl;
}
