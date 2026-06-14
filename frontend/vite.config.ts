import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  base: './',
  server: {
    proxy: {
      '/ws': {
        target: 'http://localhost:8520',
        ws: true,
      },
      '/api': {
        target: 'http://localhost:8520',
      },
    },
  },
});
