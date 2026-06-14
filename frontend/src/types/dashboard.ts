export type ChatHistoryRecord = {
  sessionId: string;
  scene?: string;
  difficulty?: string;
  overallGrade?: string;
  durationSeconds?: number;
  messageCount?: number;
  createdAt?: string;
};

export type ChatHistoryData = {
  records: ChatHistoryRecord[];
  total: number;
  page: number;
  pageSize: number;
};

export type LeaderboardEntry = {
  rank: number;
  userId: string;
  nickname?: string;
  avatarUrl?: string | null;
  avgGrade?: string;
  sessionCount?: number;
};

export type LeaderboardData = {
  records: LeaderboardEntry[];
  myRank?: LeaderboardEntry | null;
};

export type UserProfile = {
  id: string;
  phone: string;
  nickname: string;
  avatarUrl?: string | null;
};
