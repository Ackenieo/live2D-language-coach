export type RealtimeConfig = {
  lang: 'en' | 'zh';
  role: string;
  vision: 'on' | 'off';
};

export type RealtimeMessage = {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  type: 'user_subtitle' | 'ai_subtitle' | 'ai_subtitle_complete' | 'system';
  turnId?: string;
  grammarCorrection?: string;
  pronunciationScore?: string;
  isStreaming?: boolean;
};

export type GenericRealtimeInboundMessage = {
  type: string;
  text?: string;
  turnId?: string;
  sessionId?: string;
  responseId?: string;
  itemId?: string;
  sampleRate?: number;
  reconnect?: boolean;
  suggestedGrade?: string;
  overallGrade?: string;
  accuracyGrade?: string;
  fluencyGrade?: string;
  completionGrade?: string;
  completenessGrade?: string;
  summary?: string;
  suggestions?: string[];
  durationSeconds?: number;
  turnCount?: number;
};

export type SessionSummary = {
  sessionId: string;
  overallGrade: string;
  accuracyGrade: string;
  fluencyGrade: string;
  completenessGrade: string;
  durationSeconds: number;
  turns: number;
  summary: string;
  suggestions: string[];
};

export type SessionReportResponse = {
  success: boolean;
  data?: {
    sessionId: string;
    durationSeconds?: number;
    messageCount?: number;
    overallGrade?: string;
    accuracyGrade?: string;
    fluencyGrade?: string;
    completenessGrade?: string;
    suggestions?: string[];
  };
  message?: string | null;
};
