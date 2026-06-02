import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/auctions/*/bids': 'http://localhost:8082',
      '/api': 'http://localhost:8080',
    },
  },
})
