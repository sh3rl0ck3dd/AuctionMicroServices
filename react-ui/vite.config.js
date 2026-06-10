import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '^/api/auctions/[^/]+/bids': process.env.VITE_BIDDING_SERVICE_URL || 'http://localhost:8082',
      '/api/notifications': process.env.VITE_NOTIFICATION_SERVICE_URL || 'http://localhost:8083',
      '/api': process.env.VITE_AUCTION_SERVICE_URL || 'http://localhost:8080',
    },
  },
})
