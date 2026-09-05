import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5174,
    proxy: {
      '/app/api': {
        target: 'http://localhost:8888',
        changeOrigin: true
      },
      // 静态图片资源：上传图片由 brand-api 托管，经网关访问
      '/static/upload': {
        target: 'http://localhost:8888',
        changeOrigin: true
      }
    }
  }
})
