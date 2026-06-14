export type ExpressionId = string;

export type AvatarExpression = {
  id: ExpressionId;
  label: string;
  file: string;
  kind: 'emotion' | 'pose' | 'prop' | 'effect';
};

export type AvatarManifest = {
  id: string;
  name: string;
  modelJson: string;
  scaleMultiplier: number;
  verticalOffset: number;
  modelTransform: {
    scale: number;
    offsetX: number;
    offsetY: number;
  };
  transformDefaults: {
    scale: number;
    offsetX: number;
    offsetY: number;
  };
  expressions: AvatarExpression[];
};

function publicAsset(path: string) {
  return `${import.meta.env.BASE_URL}${path.replace(/^\/+/, '')}`;
}

const yumiFolder = publicAsset('live2D/yumi');

export const avatarManifests: Record<string, AvatarManifest> = {
  yumi: {
    id: 'yumi',
    name: 'Yumi',
    modelJson: `${yumiFolder}/yumi.model3.json`,
    scaleMultiplier: 0.27,
    verticalOffset: 0.08,
    modelTransform: { scale: 8, offsetX: 0, offsetY: 1.3 },
    transformDefaults: { scale: 1, offsetX: 0, offsetY: 0 },
    expressions: [
      { id: 'neutral', label: 'Neutral', kind: 'emotion', file: '' },
      { id: 'starry_eyes', label: '星星眼', kind: 'emotion', file: `${yumiFolder}/星星眼.exp3.json` },
      { id: 'happy', label: '爱心眼', kind: 'emotion', file: `${yumiFolder}/爱心眼.exp3.json` },
      { id: 'tear', label: '泪汪汪', kind: 'emotion', file: `${yumiFolder}/泪汪汪.exp3.json` },
      { id: 'black', label: '黑脸', kind: 'emotion', file: `${yumiFolder}/黑脸.exp3.json` },
      { id: 'smirk', label: '歪嘴', kind: 'emotion', file: `${yumiFolder}/歪嘴.exp3.json` },
      { id: 'wave', label: '抬手', kind: 'pose', file: `${yumiFolder}/抬手右.exp3.json` },
    ],
  },
};

export const defaultAvatarId = 'yumi';

export function getAvatar(id: string): AvatarManifest {
  return avatarManifests[id] ?? avatarManifests[defaultAvatarId];
}

export function getExpression(avatar: AvatarManifest, id: ExpressionId): AvatarExpression | undefined {
  return avatar.expressions.find((e) => e.id === id);
}

export function listAvatars(): { id: string; name: string }[] {
  return Object.entries(avatarManifests).map(([id, a]) => ({ id, name: a.name }));
}
