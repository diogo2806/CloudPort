import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    outDir: 'dist/servico-navio-siderurgico',
    emptyOutDir: true,
    sourcemap: true
  },
  server: {
    host: '0.0.0.0',
    port: 4201
  },
  preview: {
    host: '0.0.0.0',
    port: 4201
  }
});
