/**
 * LLM 对话（简化版：纯文本，不要求 JSON）
 */
export type LlmSettings = {
  apiUrl: string;
  apiKey: string;
  model: string;
};

export function getDefaultLlmSettings(): LlmSettings {
  return {
    apiUrl: import.meta.env.VITE_LLM_API_URL ?? '',
    apiKey: import.meta.env.VITE_LLM_API_KEY ?? '',
    model: import.meta.env.VITE_LLM_MODEL ?? '',
  };
}

export function loadLlmSettings(): LlmSettings {
  try {
    const raw = localStorage.getItem('llm:settings');
    if (raw) return { ...getDefaultLlmSettings(), ...JSON.parse(raw) };
  } catch { /* ignore */ }
  return getDefaultLlmSettings();
}

export function saveLlmSettings(s: LlmSettings) {
  localStorage.setItem('llm:settings', JSON.stringify(s));
}

async function fetchCompletion(settings: LlmSettings, messages: Array<{ role: string; content: string }>): Promise<string> {
  const res = await fetch(settings.apiUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${settings.apiKey}`,
    },
    body: JSON.stringify({
      model: settings.model,
      temperature: 0.8,
      max_tokens: 400,
      messages,
    }),
  });

  if (!res.ok) throw new Error(`LLM error ${res.status}`);
  const data = await res.json() as { choices?: Array<{ message?: { content?: string } }> };
  return data.choices?.[0]?.message?.content ?? '';
}

export async function sendChatMessage(
  settings: LlmSettings,
  history: Array<{ role: 'user' | 'assistant'; content: string }>,
  systemPrompt: string,
): Promise<string> {
  const messages = [
    { role: 'system', content: systemPrompt },
    ...history.map((m) => ({ role: m.role, content: m.content })),
  ];
  return fetchCompletion(settings, messages);
}
