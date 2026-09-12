import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

function normalizeBase(raw?: string): string {
  const value = (raw ?? '/').trim() || '/'
  if (value === '/') return '/'
  const withLead = value.startsWith('/') ? value : `/${value}`
  return withLead.endsWith('/') ? withLead : `${withLead}/`
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080'
  const base = normalizeBase(env.VITE_BASE)

  return {
    base,
    plugins: [vue()],
    server: {
      host: true,
      proxy: {
        // 开发/测试将 /api 转发到云端后端（目标见对应 .env.*）
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    },
    preview: {
      host: true
    }
  }
})
