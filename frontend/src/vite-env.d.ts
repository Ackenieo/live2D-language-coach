/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ENABLE_SENTIMENT_MODEL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

interface Document {
  startViewTransition?: (callback: () => void) => void;
}
