import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Back-office servi sous /admin/ (Spring Boot en production, vite en développement)
const api = { '/api': 'http://127.0.0.1:8080' }

export default defineConfig({
  base: '/admin/',
  plugins: [react()],
  server: { port: 5174, proxy: api },
  preview: { port: 5174, proxy: api },
  build: { sourcemap: false, chunkSizeWarningLimit: 600 },
})
