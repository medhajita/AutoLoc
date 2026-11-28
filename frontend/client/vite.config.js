import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Site client servi sous /client/ (Spring Boot en production, vite en développement)
const api = { '/api': 'http://127.0.0.1:8080' }

export default defineConfig({
  base: '/client/',
  plugins: [react()],
  server: { port: 5173, proxy: api },
  preview: { port: 5173, proxy: api },
  build: { sourcemap: false, chunkSizeWarningLimit: 600 },
})
