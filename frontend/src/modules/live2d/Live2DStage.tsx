import { useEffect, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react';
import type { AvatarManifest } from './avatarManifest.ts';
import {
  applyExpression,
  createLive2DRuntime,
  destroyRuntime,
  focusRuntime,
  resetRuntimeFocus,
  resizeRuntime,
  setLipFrame as engineSetLipFrame,
  setMouthOpen as engineSetMouthOpen,
  updateStageTransform,
  type LipFrameParams,
  type StageTransform,
} from './live2dEngine.ts';

export type Live2DStageApi = {
  setLipFrame: (params: LipFrameParams) => void;
  setMouthOpen: (value: number) => void;
};

type Live2DStageProps = {
  avatar: AvatarManifest;
  expressionId: string;
  expressionFile: string;
  transform: StageTransform;
  onTransformChange: (t: StageTransform) => void;
  onRuntimeReady?: (api: Live2DStageApi) => void;
  onRuntimeGone?: () => void;
  showStatus?: boolean;
};

type DragState = {
  pointerId: number;
  startX: number;
  startY: number;
  origin: StageTransform;
  mode: 'move' | 'scale';
};

function clamp(v: number, min: number, max: number) {
  return Math.min(Math.max(v, min), max);
}

export function Live2DStage({
  avatar,
  expressionId,
  expressionFile,
  transform,
  onTransformChange,
  onRuntimeReady,
  onRuntimeGone,
  showStatus = true,
}: Live2DStageProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const runtimeRef = useRef<Awaited<ReturnType<typeof createLive2DRuntime>> | null>(null);
  const transformRef = useRef(transform);
  const expressionRef = useRef({ expressionFile, expressionId });
  const runtimeReadyRef = useRef(onRuntimeReady);
  const runtimeGoneRef = useRef(onRuntimeGone);
  const dragRef = useRef<DragState | null>(null);
  const [status, setStatus] = useState('Loading');

  useEffect(() => { transformRef.current = transform; }, [transform]);
  useEffect(() => { expressionRef.current = { expressionFile, expressionId }; }, [expressionFile, expressionId]);
  useEffect(() => { runtimeReadyRef.current = onRuntimeReady; }, [onRuntimeReady]);
  useEffect(() => { runtimeGoneRef.current = onRuntimeGone; }, [onRuntimeGone]);

  // 初始化运行时
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    let cancelled = false;
    setStatus(`Loading ${avatar.name}`);

    void createLive2DRuntime(container, avatar).then((runtime) => {
      if (cancelled) { destroyRuntime(runtime); return; }
      runtimeRef.current = runtime;
      runtimeReadyRef.current?.({
        setLipFrame: (params) => {
          if (runtimeRef.current === runtime) {
            engineSetLipFrame(runtime, params);
          }
        },
        setMouthOpen: (value) => {
          if (runtimeRef.current === runtime) {
            engineSetMouthOpen(runtime, value);
          }
        },
      });
      if (expressionRef.current.expressionFile) {
        void applyExpression(runtime, expressionRef.current.expressionId, expressionRef.current.expressionFile);
      }
      console.info('[Live2D] runtime ready', {
        avatar: avatar.name,
        width: runtime.modelBaseWidth,
        height: runtime.modelBaseHeight,
        transform: runtime.currentTransform,
      });
      setStatus('Ready');
    }).catch((e) => {
      console.error(e);
      setStatus(`Load failed: ${e instanceof Error ? e.message : 'unknown error'}`);
    });

    const observer = new ResizeObserver(() => {
      if (runtimeRef.current && containerRef.current) {
        resizeRuntime(runtimeRef.current, containerRef.current);
      }
    });
    observer.observe(container);

    return () => {
      cancelled = true;
      observer.disconnect();
      runtimeGoneRef.current?.();
      if (runtimeRef.current) { destroyRuntime(runtimeRef.current); runtimeRef.current = null; }
    };
  }, [avatar]);

  // 表情切换
  useEffect(() => {
    if (!runtimeRef.current || !expressionFile) return;
    void applyExpression(runtimeRef.current, expressionId, expressionFile);
  }, [expressionId, expressionFile]);

  // 嘴型同步
  // 变换同步
  useEffect(() => {
    if (!runtimeRef.current || !containerRef.current) return;
    updateStageTransform(runtimeRef.current, containerRef.current, transform);
  }, [transform]);

  // --- 拖拽/缩放手势 ---
  function handlePointerDown(e: ReactPointerEvent<HTMLDivElement>) {
    const c = containerRef.current;
    if (!c) return;

    dragRef.current = {
      pointerId: e.pointerId,
      startX: e.clientX,
      startY: e.clientY,
      origin: transformRef.current,
      mode: e.shiftKey ? 'scale' : 'move',
    };
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  }

  function handlePointerMove(e: ReactPointerEvent<HTMLDivElement>) {
    const c = containerRef.current;
    const r = runtimeRef.current;
    const d = dragRef.current;
    if (!c) return;

    if (r && (e.pointerType === 'mouse' || e.pointerType === 'pen')) {
      focusRuntime(r, c, e.clientX, e.clientY);
    }
    if (!d || d.pointerId !== e.pointerId) return;

    const dx = e.clientX - d.startX;
    const dy = e.clientY - d.startY;

    if (d.mode === 'scale') {
      onTransformChange({ ...d.origin, scale: clamp(d.origin.scale - dy / 90, 0.05, 8) });
    } else {
      onTransformChange({
        ...d.origin,
        offsetX: clamp(d.origin.offsetX + (dx / c.clientWidth) * 4.2, -2.4, 2.4),
        offsetY: clamp(d.origin.offsetY + (dy / c.clientHeight) * 4.2, -1.8, 1.8),
      });
    }
  }

  function handlePointerUp(e: ReactPointerEvent<HTMLDivElement>) {
    if (dragRef.current?.pointerId !== e.pointerId) return;
    dragRef.current = null;
    (e.target as HTMLElement).releasePointerCapture(e.pointerId);
  }

  return (
    <div
      style={{ position: 'relative', flex: 1, minHeight: 420, borderRadius: 24, overflow: 'hidden', background: 'radial-gradient(circle at 50% 20%, rgba(212,168,92,0.16), transparent 36%), linear-gradient(180deg, rgba(38,44,57,0.92), rgba(10,15,22,0.96))' }}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
      onPointerLeave={() => { if (runtimeRef.current) resetRuntimeFocus(runtimeRef.current); }}
    >
      <div ref={containerRef} style={{ width: '100%', height: '100%' }} />
      {showStatus ? (
        <div style={{ position: 'absolute', bottom: 18, right: 18, padding: '10px 14px', borderRadius: 999, background: 'rgba(10,12,16,0.65)', color: '#d7cabb', fontSize: 13 }}>
          {status}
        </div>
      ) : null}
    </div>
  );
}
