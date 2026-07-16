import { defineConfig } from 'vite';

export default defineConfig({
  publicDir: 'public',
  server: {
    host: '0.0.0.0',
    port: 4200
  },
  preview: {
    host: '0.0.0.0',
    port: 4200
  },
  build: {
    outDir: 'dist/cloudport',
    emptyOutDir: true,
    sourcemap: false
  }
});
