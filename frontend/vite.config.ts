import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// To use environment variables (like process.env.*) in TS here we rely on @types/node.
// Vitest plugin options are supplied via the `test` field when using Vitest 1.x; however
// type definitions might lag. Cast to any to avoid build-time config typing friction.

export default defineConfig(({ mode }) => ({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
  target: process.env.VITE_API_BASE || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: { outDir: 'dist' },
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.ts']
  } as any
}));
