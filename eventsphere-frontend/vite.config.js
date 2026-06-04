import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5460,
    strictPort: false,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            if (proxyRes.headers['x-captcha-token']) {
              proxyRes.headers['access-control-expose-headers'] = 'X-Captcha-Token'
            }
          })
        },
      },
    },
  },
})
