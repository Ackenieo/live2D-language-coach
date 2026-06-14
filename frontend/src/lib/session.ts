import { fetchWithAuth } from './api.ts';
import type { GenericRealtimeInboundMessage, SessionReportResponse, SessionSummary } from '../types/session.ts';

export function formatDuration(durationSeconds: number) {
  if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) return '-';
  const minutes = Math.floor(durationSeconds / 60);
  const seconds = durationSeconds % 60;
  return `${minutes}m ${String(seconds).padStart(2, '0')}s`;
}

export function normalizeSessionSummary(msg: GenericRealtimeInboundMessage): SessionSummary | null {
  if (!msg.sessionId) return null;
  return {
    sessionId: msg.sessionId,
    overallGrade: msg.overallGrade ?? msg.suggestedGrade ?? '-',
    accuracyGrade: msg.accuracyGrade ?? '-',
    fluencyGrade: msg.fluencyGrade ?? '-',
    completenessGrade: msg.completenessGrade ?? msg.completionGrade ?? '-',
    durationSeconds: typeof msg.durationSeconds === 'number' ? msg.durationSeconds : 0,
    turns: typeof msg.turnCount === 'number' ? msg.turnCount : 0,
    summary: msg.summary?.trim() || '本次会话已结束。',
    suggestions: Array.isArray(msg.suggestions) ? msg.suggestions : [],
  };
}

export async function fetchSessionReport(sessionId: string): Promise<Partial<SessionSummary>> {
  const data = await fetchWithAuth<SessionReportResponse['data']>(`/api/chat/report/${sessionId}`);

  return {
    sessionId: data?.sessionId ?? sessionId,
    overallGrade: data?.overallGrade ?? '-',
    accuracyGrade: data?.accuracyGrade ?? '-',
    fluencyGrade: data?.fluencyGrade ?? '-',
    completenessGrade: data?.completenessGrade ?? '-',
    durationSeconds: typeof data?.durationSeconds === 'number' ? data.durationSeconds : 0,
    turns: typeof data?.messageCount === 'number' ? data.messageCount : 0,
    suggestions: Array.isArray(data?.suggestions) ? data.suggestions : [],
  };
}
