import net from 'node:net'
import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'

const PREFERRED_PORT = 5173

function normalizeBase(raw?: string): string {
  const value = (raw ?? '/').trim() || '/'
  if (value === '/') return '/'
  const withLead = value.startsWith('/') ? value : `/${value}`
  return withLead.endsWith('/') ? withLead : `${withLead}/`
}

/** Windows 上 IPv4 / IPv6 可各绑一次同一端口，Vite 默认探测会漏掉。 */
function probeConnect(port: number, host: string): Promise<boolean> {
  return new Promise((resolve) => {
    const socket = net.connect({ port, host })
    const done = (used: boolean) => {
      socket.removeAllListeners()
      socket.destroy()
      resolve(used)
    }
    socket.setTimeout(250)
    socket.once('connect', () => done(true))
    socket.once('timeout', () => done(false))
    socket.once('error', () => done(false))
  })
}

async function isPortOccupied(port: number): Promise<boolean> {
  if (await probeConnect(port, '127.0.0.1')) return true
  if (await probeConnect(port, '::1')) return true
  return false
}

async function findAvailablePort(start: number, span = 20): Promise<number> {
  const occupied: number[] = []
  for (let port = start; port < start + span; port++) {
    if (await isPortOccupied(port)) {
      occupied.push(port)
      continue
    }
    if (occupied.length) {
      console.warn(
        `[vite] 端口 ${occupied.join(', ')} 已被占用（含 IPv4/IPv6），改用 ${port}`,
      )
    }
    return port
  }
  throw new Error(`[vite] ${start}-${start + span - 1} 没有空闲端口`)
}

export default defineConfig(async ({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080'
  const base = normalizeBase(env.VITE_BASE)
  const port = await findAvailablePort(PREFERRED_PORT)

  return {
    base,
    plugins: [vue()],
    server: {
      host: true,
      port,
      strictPort: true,
      proxy: {
        // 开发/测试将 /api 转发到云端后端（目标见对应 .env.*）
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        }
      }
    },
    preview: {
      host: true,
      port,
      strictPort: true
    }
  }
})
