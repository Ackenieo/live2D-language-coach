import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Eye } from 'lucide-react';
import { Live2DStage, type Live2DStageApi } from '../modules/live2d/Live2DStage.tsx';
import { ChatPanel } from '../modules/chat/ChatPanel.tsx';
import { getAccessToken } from '../lib/auth.ts';
import type { AuthTokens } from '../lib/auth.ts';
import {
  defaultAvatarId,
  getAvatar,
} from '../modules/live2d/avatarManifest.ts';
import type { LipFrameParams, StageTransform } from '../modules/live2d/live2dEngine.ts';
import { inferExpression } from '../modules/live2d/expressionController.ts';
import {
  connectWebSocket,
  sendBinary,
  sendJson,
  closeWs,
} from '../modules/media/useWebSocket.ts';
import {
  startAudioCapture,
  stopAudioCapture,
} from '../modules/media/useAudioCapture.ts';
import {
  appendSpeechText,
  interruptSpeechPlayback,
  playSpeechAudioChunk,
  prepareSpeechPlayback,
  resetSpeechText,
  setLipFrameSink,
  stopSpeechPlayback,
} from '../modules/media/speechLink.ts';
import {
  isVisionActive,
  startVisionCapture,
  stopVisionCapture,
} from '../modules/media/useVisionCapture.ts';
import { normalizeSessionSummary } from '../lib/session.ts';
import type { GenericRealtimeInboundMessage, RealtimeConfig, RealtimeMessage, SessionSummary } from '../types/session.ts';

export function formatPronunciationScore(msg: {
  suggestedGrade?: string;
  accuracyGrade?: string;
  fluencyGrade?: string;
  completionGrade?: string;
}) {
  return `总评 ${msg.suggestedGrade ?? '-'} / 准确度 ${msg.accuracyGrade ?? '-'} / 流利度 ${msg.fluencyGrade ?? '-'} / 完整度 ${msg.completionGrade ?? '-'}`;
}

function appendSystemMessage(setMessages: React.Dispatch<React.SetStateAction<RealtimeMessage[]>>, content: string) {
  setMessages((prev) => [...prev, {
    id: crypto.randomUUID(),
    role: 'system',
    type: 'system',
    content,
  }]);
}

function formatClock(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
}

function applyRealtimeMessage(prev: RealtimeMessage[], msg: GenericRealtimeInboundMessage): RealtimeMessage[] {
  if (msg.type === 'connected' || msg.type === 'reconnected' || msg.type === 'config_updated' || msg.type === 'session_end') {
    return prev;
  }

  if (msg.type === 'ai_subtitle') {
    const text = typeof msg.text === 'string' ? msg.text : '';
    const last = prev[prev.length - 1];
    if (last?.role === 'assistant' && last.isStreaming) {
      const updatedLast: RealtimeMessage = {
        ...last,
        type: 'ai_subtitle',
        isStreaming: true,
        content: `${last.content}${text}`,
      };
      return [...prev.slice(0, -1), updatedLast];
    }
    return [...prev, {
      id: crypto.randomUUID(),
      role: 'assistant',
      type: 'ai_subtitle',
      content: text,
      isStreaming: true,
    }];
  }

  if (msg.type === 'ai_subtitle_complete') {
    const text = typeof msg.text === 'string' ? msg.text : '';
    const last = prev[prev.length - 1];
    if (last?.role === 'assistant') {
      const updatedLast: RealtimeMessage = {
        ...last,
        type: 'ai_subtitle_complete',
        isStreaming: false,
        content: text || last.content,
      };
      return [...prev.slice(0, -1), updatedLast];
    }
    return [...prev, {
      id: crypto.randomUUID(),
      role: 'assistant',
      type: 'ai_subtitle_complete',
      content: text,
      isStreaming: false,
    }];
  }

  if (msg.type === 'user_subtitle') {
    return [...prev, {
      id: msg.turnId || crypto.randomUUID(),
      turnId: msg.turnId,
      role: 'user',
      type: 'user_subtitle',
      content: typeof msg.text === 'string' ? msg.text : '',
      grammarCorrection: '',
      pronunciationScore: '',
    }];
  }

  if (msg.type === 'grammar_correction' && msg.turnId) {
    return prev.map((item) => (
      item.turnId === msg.turnId
        ? { ...item, grammarCorrection: typeof msg.text === 'string' ? msg.text : '' }
        : item
    ));
  }

  if (msg.type === 'pronunciation_score' && msg.turnId) {
    const scoreText = formatPronunciationScore({
      suggestedGrade: msg.suggestedGrade,
      accuracyGrade: msg.accuracyGrade,
      fluencyGrade: msg.fluencyGrade,
      completionGrade: msg.completionGrade,
    });
    return prev.map((item) => (
      item.turnId === msg.turnId
        ? { ...item, pronunciationScore: scoreText }
        : item
    ));
  }

  if (typeof msg.text === 'string' && msg.text) {
    return [...prev, {
      id: crypto.randomUUID(),
      role: 'system',
      type: 'system',
      content: `[${msg.type}] ${msg.text}`,
    }];
  }

  return prev;
}

export function CallPage({
  auth,
  onLogout,
  onSessionEnd,
}: {
  auth: AuthTokens | null;
  onLogout: () => void;
  onSessionEnd: (summary: SessionSummary) => void;
}) {
  const navigate = useNavigate();
  const location = useLocation();

  const sessionIdRef = useRef<string | null>(null);
  const finishTimeoutsRef = useRef<Map<WebSocket, number>>(new Map());
  const [avatarId] = useState(defaultAvatarId);
  const avatar = getAvatar(avatarId);
  const [transform, setTransform] = useState<StageTransform>(avatar.transformDefaults);
  const [messages, setMessages] = useState<RealtimeMessage[]>([]);
  const [expressionId, setExpressionId] = useState('neutral');
  const [expressionFile, setExpressionFile] = useState('');
  const [realtimeConfig, setRealtimeConfig] = useState<RealtimeConfig>({ lang: 'en', role: 'English Coach', vision: 'on' });

  const wsRef = useRef<WebSocket | null>(null);
  const live2dApiRef = useRef<Live2DStageApi | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [callStatus, setCallStatus] = useState<'idle' | 'connecting' | 'connected' | 'calling' | 'error'>('idle');
  const [visionOn, setVisionOn] = useState(false);
  const [visionThreshold] = useState(0.98);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const manualStopRef = useRef(false);
  const autoStartRef = useRef(false);
  const interruptNextAudioRef = useRef(false);
  const assistantStreamingRef = useRef(false);
  const visionStartingRef = useRef(false);
  const speechResponseIdRef = useRef<string | null>(null);
  const subtitleDeltaSeenRef = useRef(false);

  const wsConnected = callStatus === 'connected' || callStatus === 'calling';
  const isCalling = callStatus === 'calling';
  const isConnecting = callStatus === 'connecting';

  const handleLive2DReady = useCallback((api: Live2DStageApi) => {
    live2dApiRef.current = api;
    setLipFrameSink((params: LipFrameParams) => {
      live2dApiRef.current?.setLipFrame(params);
    });
  }, []);

  const handleLive2DGone = useCallback(() => {
    live2dApiRef.current = null;
    setLipFrameSink(null);
  }, []);

  const resetSpeechLinkState = useCallback(() => {
    stopSpeechPlayback();
    resetSpeechText();
    speechResponseIdRef.current = null;
    subtitleDeltaSeenRef.current = false;
  }, []);

  const beginSpeechResponse = useCallback((responseId?: string) => {
    if (!responseId) {
      return false;
    }
    if (speechResponseIdRef.current === responseId) {
      return false;
    }
    speechResponseIdRef.current = responseId;
    subtitleDeltaSeenRef.current = false;
    resetSpeechText();
    return true;
  }, []);

  function clearFinishTimeout(ws?: WebSocket | null) {
    if (ws) {
      const timeoutId = finishTimeoutsRef.current.get(ws);
      if (timeoutId !== undefined) {
        window.clearTimeout(timeoutId);
        finishTimeoutsRef.current.delete(ws);
      }
      return;
    }

    finishTimeoutsRef.current.forEach((timeoutId) => window.clearTimeout(timeoutId));
    finishTimeoutsRef.current.clear();
  }

  useEffect(() => {
    setTransform(avatar.transformDefaults);
    setExpressionId('neutral');
    setExpressionFile('');
  }, [avatarId, avatar.transformDefaults]);

  useEffect(() => () => {
    clearFinishTimeout();
    setLipFrameSink(null);
    stopSpeechPlayback();
  }, []);

  useEffect(() => {
    if (!wsConnected) {
      setElapsedSeconds(0);
      return;
    }

    const startedAt = Date.now();
    setElapsedSeconds(0);
    const timer = window.setInterval(() => {
      setElapsedSeconds(Math.floor((Date.now() - startedAt) / 1000));
    }, 1000);

    return () => window.clearInterval(timer);
  }, [wsConnected]);

  function cleanupCallState(nextStatus: 'idle' | 'connecting' | 'connected' | 'calling' | 'error' = 'idle', ws?: WebSocket | null) {
    clearFinishTimeout(ws);
    stopAudioCapture();
    resetSpeechLinkState();
    if (videoRef.current) {
      stopVisionCapture(videoRef.current);
    }
    setVisionOn(false);
    setRealtimeConfig((prev) => ({ ...prev, vision: 'off' }));
    setCallStatus(nextStatus);
    interruptNextAudioRef.current = false;
    assistantStreamingRef.current = false;
    visionStartingRef.current = false;
    if (!ws || wsRef.current === ws) {
      wsRef.current = null;
    }
  }

  function sendRealtimeConfig(ws: WebSocket | null, config = realtimeConfig) {
    sendJson(ws, {
      type: 'config',
      lang: config.lang,
      role: config.role,
      vision: config.vision,
    });
  }

  function enableVisionMode(ws: WebSocket | null = wsRef.current, config = realtimeConfig) {
    if (!videoRef.current || !ws || ws.readyState !== WebSocket.OPEN || visionStartingRef.current || visionOn || isVisionActive()) return;

    const videoElement = videoRef.current;
    visionStartingRef.current = true;
    void startVisionCapture(
      videoElement,
      (base64) => {
        sendJson(wsRef.current, { type: 'screenshot', image: base64 });
      },
      { similarityThreshold: visionThreshold },
    ).then(() => {
      if (manualStopRef.current || wsRef.current !== ws) {
        stopVisionCapture(videoElement);
        return;
      }
      setVisionOn(true);
      const nextConfig = { ...config, vision: 'on' as const };
      setRealtimeConfig(nextConfig);
      sendRealtimeConfig(wsRef.current, nextConfig);
    }).catch((error) => {
      console.error(error);
      appendSystemMessage(setMessages, '无法获取摄像头权限');
      setVisionOn(false);
      const nextConfig = { ...config, vision: 'off' as const };
      setRealtimeConfig(nextConfig);
      sendRealtimeConfig(wsRef.current, nextConfig);
    }).finally(() => {
      visionStartingRef.current = false;
    });
  }

  function disableVisionMode(ws: WebSocket | null = wsRef.current, config = realtimeConfig) {
    if (videoRef.current) {
      stopVisionCapture(videoRef.current);
    }
    visionStartingRef.current = false;
    setVisionOn(false);
    const nextConfig = { ...config, vision: 'off' as const };
    setRealtimeConfig(nextConfig);
    sendRealtimeConfig(ws, nextConfig);
  }

  useEffect(() => {
    if (wsConnected && wsRef.current) {
      sendRealtimeConfig(wsRef.current);
    }
  }, [realtimeConfig, wsConnected]);

  const callStatusText = useMemo(() => {
    if (callStatus === 'connecting') return '连接中';
    if (callStatus === 'calling') return '通话中';
    if (callStatus === 'connected') return '已连接';
    if (callStatus === 'error') return 'ERROR';
    return 'DISCONNECTED';
  }, [callStatus]);

  async function updateExpressionFromText(text: string) {
    if (!text.trim()) return;
    const expr = await inferExpression(avatar, text);
    setExpressionId(expr.id);
    setExpressionFile(expr.file);
  }

  async function startCall() {
    if (isCalling || isConnecting) return;

    const token = getAccessToken();
    if (!token) {
      appendSystemMessage(setMessages, '登录已过期，请重新登录');
      onLogout();
      return;
    }

    manualStopRef.current = false;
    interruptNextAudioRef.current = false;
    assistantStreamingRef.current = false;
    visionStartingRef.current = false;
    const startConfig = { ...realtimeConfig, vision: 'on' as const };
    setRealtimeConfig(startConfig);
    setCallStatus('connecting');
    setMessages([]);
    resetSpeechLinkState();
    prepareSpeechPlayback();
    if (location.pathname !== '/chat') {
      navigate('/chat');
    }

    const ws = connectWebSocket('/ws/bailian', {
      onOpen: () => {
        if (wsRef.current !== ws) return;
        wsRef.current = ws;
        setCallStatus('connected');
        sendRealtimeConfig(ws, startConfig);
        sendJson(ws, { type: 'start' });
        setCallStatus('calling');
        enableVisionMode(ws, startConfig);
      },
      onClose: () => {
        if (wsRef.current !== ws) return;
        const wasUnexpected = !manualStopRef.current;
        cleanupCallState(wasUnexpected ? 'error' : 'idle', ws);
        if (wasUnexpected) {
          appendSystemMessage(setMessages, '实时通话已断开');
        }
        manualStopRef.current = false;
      },
      onError: () => {
        if (wsRef.current !== ws) return;
        setCallStatus('error');
        appendSystemMessage(setMessages, 'WebSocket 连接异常');
      },
      onAudio: (data) => {
        if (wsRef.current !== ws) return;
        const shouldInterrupt = interruptNextAudioRef.current;
        interruptNextAudioRef.current = false;
        playSpeechAudioChunk(data, { interrupt: shouldInterrupt });
      },
      onMessage: (msg) => {
        if (wsRef.current !== ws) return;
        const nextMsg = msg as GenericRealtimeInboundMessage;

        if (nextMsg.type === 'connected' || nextMsg.type === 'reconnected') {
          if (nextMsg.sessionId) {
            sessionIdRef.current = nextMsg.sessionId;
          }
        }

        if (nextMsg.type === 'session_end') {
          const summary = normalizeSessionSummary(nextMsg);
          if (summary) {
            manualStopRef.current = true;
            closeWs(ws);
            cleanupCallState('idle', ws);
            onSessionEnd(summary);
          }
          return;
        }

        if (nextMsg.type === 'ai_audio_start') {
          const startedNewResponse = beginSpeechResponse(nextMsg.responseId);
          if (startedNewResponse || !nextMsg.responseId) {
            interruptNextAudioRef.current = true;
          }
        }

        if (nextMsg.type === 'ai_audio_done') {
          if (!nextMsg.responseId || nextMsg.responseId === speechResponseIdRef.current) {
            assistantStreamingRef.current = false;
            subtitleDeltaSeenRef.current = false;
          }
        }

        if (nextMsg.type === 'user_subtitle') {
          interruptSpeechPlayback();
          resetSpeechText();
          speechResponseIdRef.current = null;
          subtitleDeltaSeenRef.current = false;
          interruptNextAudioRef.current = true;
          assistantStreamingRef.current = false;
        }

        if (nextMsg.type === 'ai_subtitle') {
          const startedNewResponse = beginSpeechResponse(nextMsg.responseId);
          const legacyNewStream = !nextMsg.responseId && !assistantStreamingRef.current;
          if (startedNewResponse || legacyNewStream) {
            interruptNextAudioRef.current = true;
            if (!startedNewResponse) {
              resetSpeechText();
            }
          }
          if (typeof nextMsg.text === 'string') {
            appendSpeechText(nextMsg.text);
            subtitleDeltaSeenRef.current = true;
          }
          assistantStreamingRef.current = true;
        }

        if (nextMsg.type === 'ai_subtitle_complete') {
          const startedNewResponse = beginSpeechResponse(nextMsg.responseId);
          const legacyNewStream = !nextMsg.responseId && !assistantStreamingRef.current;
          if (startedNewResponse || legacyNewStream) {
            interruptNextAudioRef.current = true;
            if (!startedNewResponse) {
              resetSpeechText();
            }
          }
          if (!subtitleDeltaSeenRef.current && typeof nextMsg.text === 'string') {
            appendSpeechText(nextMsg.text);
          }
          assistantStreamingRef.current = false;
          subtitleDeltaSeenRef.current = false;
        }

        setMessages((prev) => applyRealtimeMessage(prev, nextMsg));

        if ((nextMsg.type === 'ai_subtitle' || nextMsg.type === 'ai_subtitle_complete') && typeof nextMsg.text === 'string') {
          void updateExpressionFromText(nextMsg.text);
        }
      },
    }, token);

    wsRef.current = ws;

    try {
      await startAudioCapture({
        onAudioData: (buffer) => sendBinary(wsRef.current, buffer),
      });
    } catch (error) {
      console.error(error);
      appendSystemMessage(setMessages, '无法获取麦克风权限或启动语音采集');
      manualStopRef.current = true;
      closeWs(ws);
      cleanupCallState('error', ws);
    }
  }

  function stopCall() {
    if (!wsRef.current && !isCalling && !isConnecting) return;

    manualStopRef.current = true;

    const ws = wsRef.current;
    disableVisionMode(ws);
    sendJson(ws, { type: 'finish' });

    stopAudioCapture();
    resetSpeechLinkState();
    setCallStatus('idle');
    interruptNextAudioRef.current = false;
    assistantStreamingRef.current = false;
    visionStartingRef.current = false;

    if (ws) {
      clearFinishTimeout(ws);
      const timeoutId = window.setTimeout(() => {
        closeWs(ws);
        if (wsRef.current === ws) {
          wsRef.current = null;
        }
        finishTimeoutsRef.current.delete(ws);
      }, 1200);
      finishTimeoutsRef.current.set(ws, timeoutId);
    }
  }

  function toggleVision() {
    if (!videoRef.current || !wsRef.current || !wsConnected) return;

    if (visionOn) {
      disableVisionMode();
      return;
    }

    enableVisionMode();
  }

  useEffect(() => {
    const shouldAutoStart = new URLSearchParams(location.search).get('start') === '1';
    if (!shouldAutoStart || autoStartRef.current || isCalling || isConnecting) return;

    autoStartRef.current = true;
    void startCall();
  }, [isCalling, isConnecting, location.search]);

  return (
    <main className="call-page">
      <section className="call-stage-shell" aria-label="Live2D 通话舞台">
        <div className="call-topbar">
          <div className="call-controls">
            <button
              className="call-pill call-pill-end"
              type="button"
              onClick={isCalling || isConnecting ? stopCall : startCall}
              disabled={isConnecting}
              title={auth?.phone ?? ''}
            >
              {isCalling ? 'STOP' : 'START'}
            </button>
            <button
              className="call-pill call-pill-vision"
              type="button"
              onClick={toggleVision}
              disabled={!wsConnected}
              aria-label={visionOn ? '关闭视觉' : '开启视觉'}
              title={visionOn ? '关闭视觉' : '开启视觉'}
            >
              {visionOn ? (
                <span className="vision-eye-off-icon" aria-hidden="true">
                  <Eye size={34} strokeWidth={2.4} />
                </span>
              ) : (
                <Eye className="vision-eye-icon" size={35} strokeWidth={2.35} aria-hidden="true" />
              )}
            </button>
          </div>

          <div className="call-time" aria-label={`通话时间 ${formatClock(elapsedSeconds)}`}>
            <span>Time</span>
            <b>{formatClock(elapsedSeconds)}</b>
          </div>
        </div>

        <div className="call-stage-card">
          <Live2DStage
            avatar={avatar}
            expressionId={expressionId}
            expressionFile={expressionFile}
            transform={transform}
            onTransformChange={setTransform}
            onRuntimeReady={handleLive2DReady}
            onRuntimeGone={handleLive2DGone}
            showStatus={false}
          />
        </div>

        <div className={`call-connection ${wsConnected ? 'connected' : callStatus === 'error' ? 'error' : ''}`}>
          <span aria-hidden="true" />
          <b>{wsConnected ? 'CONNECTED' : callStatusText.toUpperCase()}</b>
        </div>
      </section>

      <aside className="call-transcript" aria-label="实时字幕">
        <ChatPanel messages={messages} />
      </aside>

      <video ref={videoRef} autoPlay playsInline muted style={{ display: 'none' }} />
    </main>
  );
}
