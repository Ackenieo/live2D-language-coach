/**
 * WebSocket 连接管理（从 front-mvp0/talktic 提取）
 *
 * 职责：
 * - 建立/断开 WebSocket 连接
 * - 处理二进制音频数据
 * - 处理 JSON 消息
 */
type WsCallbacks = {
  onOpen?: () => void;
  onClose?: () => void;
  onAudio?: (data: ArrayBuffer) => void;
  onMessage?: (msg: Record<string, unknown>) => void;
  onError?: (e: Event) => void;
};

export function connectWebSocket(path: string, callbacks: WsCallbacks, token?: string): WebSocket {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  let url = `${protocol}//${window.location.host}${path}`;
  if (token) {
    url += `?token=${encodeURIComponent(token)}`;
  }
  const socket = new WebSocket(url);
  socket.binaryType = 'arraybuffer';

  socket.onopen = () => callbacks.onOpen?.();
  socket.onclose = () => callbacks.onClose?.();
  socket.onerror = (e) => callbacks.onError?.(e);

  socket.onmessage = (event) => {
    if (event.data instanceof ArrayBuffer) {
      callbacks.onAudio?.(event.data);
    } else if (typeof event.data === 'string') {
      try {
        callbacks.onMessage?.(JSON.parse(event.data));
      } catch {
        // ignore parse errors
      }
    }
  };

  return socket;
}

export function sendJson(ws: WebSocket | null, data: Record<string, unknown>) {
  if (ws?.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(data));
  }
}

export function sendBinary(ws: WebSocket | null, buffer: ArrayBuffer) {
  if (ws?.readyState === WebSocket.OPEN) {
    ws.send(buffer);
  }
}

export function closeWs(ws: WebSocket | null) {
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    ws.close();
  }
}
