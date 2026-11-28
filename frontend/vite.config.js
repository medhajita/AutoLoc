import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
    },
  },
  // Version « production » (bundle minifié, ~75 ko gzip) : npm run build && npm run preview
  preview: {
    port: 5173,
    proxy: {
      '/api': 'http://127.0.0.1:8080',
    },
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 600,
  },
})
