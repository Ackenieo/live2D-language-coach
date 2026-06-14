import * as PIXI from 'pixi.js';
import { Live2DModel } from 'pixi-live2d-display/cubism4';
import type { AvatarManifest } from './avatarManifest.ts';

declare global {
  interface Window {
    PIXI: typeof PIXI;
    Live2DCubismCore?: object;
  }
}

export type StageTransform = {
  scale: number;
  offsetX: number;
  offsetY: number;
};

export type LipParamId =
  | 'ParamMouthOpenY'
  | 'ParamJawOpen'
  | 'ParamMouthForm'
  | 'ParamMouthpucker'
  | 'ParamMouthShrug'
  | 'ParamMouthX';

export type LipFrameParams = Partial<Record<LipParamId, number>>;

type RuntimeState = {
  model: Live2DModel;
  app: PIXI.Application;
  avatar: AvatarManifest;
  modelBaseWidth: number;
  modelBaseHeight: number;
  currentTransform: StageTransform;
};

type CubismModelSettingJson = {
  url: string;
  Version: number;
  FileReferences: Record<string, unknown>;
  Groups?: Array<Record<string, unknown>>;
};

type CubismCoreModel = {
  getParameterValueById(parameterId: string): number;
  setParameterValueById(parameterId: string, value: number, weight?: number): void;
};

type FocusControllerLike = {
  focus(x: number, y: number, instant?: boolean): void;
};

const LIP_PARAM_LIMITS: Record<LipParamId, [number, number]> = {
  ParamMouthOpenY: [0, 1],
  ParamJawOpen: [0, 1],
  ParamMouthForm: [-1, 1],
  ParamMouthpucker: [-1, 1],
  ParamMouthShrug: [0, 1],
  ParamMouthX: [-1, 1],
};

const LIP_PARAM_IDS = Object.keys(LIP_PARAM_LIMITS) as LipParamId[];

function getCoreModel(runtime: RuntimeState) {
  return runtime.model.internalModel.coreModel as CubismCoreModel;
}

function getFocusController(runtime: RuntimeState) {
  return runtime.model.internalModel.focusController as FocusControllerLike;
}

async function fetchJson<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) throw new Error(`Failed to fetch ${url}`);
  return (await response.json()) as T;
}

function toModelSettings(avatar: AvatarManifest, settings: CubismModelSettingJson): CubismModelSettingJson {
  return {
    ...settings,
    url: avatar.modelJson,
  };
}

export async function createLive2DRuntime(
  container: HTMLElement,
  avatar: AvatarManifest,
): Promise<RuntimeState> {
  if (!window.Live2DCubismCore) {
    throw new Error('live2dcubismcore.min.js is not loaded.');
  }

  window.PIXI = PIXI;

  const app = new PIXI.Application({
    autoStart: true,
    resizeTo: container,
    backgroundAlpha: 0,
    antialias: true,
  });

  container.replaceChildren(app.view as HTMLCanvasElement);

  const rawSettings = await fetchJson<CubismModelSettingJson>(avatar.modelJson);
  const modelSettings = toModelSettings(avatar, rawSettings);
  const model = await Live2DModel.from(modelSettings, { autoInteract: false });

  app.stage.addChild(model);
  model.scale.set(1);

  const localBounds = model.getLocalBounds();
  const modelBaseWidth = Math.max(localBounds.width, 1);
  const modelBaseHeight = Math.max(localBounds.height, 1);

  const runtime: RuntimeState = {
    model,
    app,
    avatar,
    modelBaseWidth,
    modelBaseHeight,
    currentTransform: avatar.transformDefaults,
  };

  fitModel(runtime, container);
  getFocusController(runtime).focus(0, 0, true);
  return runtime;
}

function fitModel(runtime: RuntimeState, container: HTMLElement, transform?: StageTransform) {
  const { model, avatar, app } = runtime;
  const width = container.clientWidth || 800;
  const height = container.clientHeight || 800;
  const t = transform ?? runtime.currentTransform;

  app.renderer.resize(width, height);

  const baseScale = Math.min(width / runtime.modelBaseWidth, height / runtime.modelBaseHeight);
  const scale = baseScale * avatar.scaleMultiplier * avatar.modelTransform.scale * t.scale;

  model.scale.set(scale);
  model.anchor.set(0.5, 1);
  model.x = width * (0.5 + avatar.modelTransform.offsetX + t.offsetX);
  model.y = height * (1 - avatar.verticalOffset + avatar.modelTransform.offsetY + t.offsetY);
  runtime.currentTransform = t;
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), max);
}

export function resizeRuntime(runtime: RuntimeState, container: HTMLElement) {
  fitModel(runtime, container);
}

export function updateStageTransform(runtime: RuntimeState, container: HTMLElement, transform: StageTransform) {
  fitModel(runtime, container, transform);
}

/** 设置单个 Live2D 参数值 */
export function setParameter(runtime: RuntimeState, paramId: string, value: number) {
  getCoreModel(runtime).setParameterValueById(paramId, value, 1);
}

export function setLipFrame(runtime: RuntimeState, params: LipFrameParams) {
  for (const paramId of LIP_PARAM_IDS) {
    const value = params[paramId];
    if (typeof value !== 'number' || Number.isNaN(value)) {
      continue;
    }
    const [min, max] = LIP_PARAM_LIMITS[paramId];
    setParameter(runtime, paramId, clamp(value, min, max));
  }
}

/** 设置嘴型开合度 */
export function setMouthOpen(runtime: RuntimeState, value: number) {
  setLipFrame(runtime, { ParamMouthOpenY: value });
}

/** 设置表情（应用 .exp3.json 文件） */
export async function applyExpression(runtime: RuntimeState, _label: string, expressionFile: string) {
  try {
    const payload = await fetchJson<{
      Parameters?: Array<{ Id?: string; Value?: number }>;
    }>(expressionFile);

    for (const param of payload.Parameters ?? []) {
      if (param.Id && typeof param.Value === 'number') {
        setParameter(runtime, param.Id, param.Value);
      }
    }
  } catch {
    // expression file not found or invalid, ignore
  }
}

export function focusRuntime(runtime: RuntimeState, container: HTMLElement, clientX: number, clientY: number) {
  const rect = container.getBoundingClientRect();
  runtime.model.focus(clientX - rect.left, clientY - rect.top);
}

export function resetRuntimeFocus(runtime: RuntimeState) {
  getFocusController(runtime).focus(0, 0);
}

export function destroyRuntime(runtime: RuntimeState) {
  runtime.app.destroy(true, { children: true, texture: false, baseTexture: false });
}
