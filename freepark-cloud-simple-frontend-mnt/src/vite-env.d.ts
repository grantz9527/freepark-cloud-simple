/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** development | test | production（或自定义 mode） */
  readonly VITE_APP_ENV: string
  /** 静态资源与路由基路径，如 / 或 /freepark-mnt/ */
  readonly VITE_BASE?: string
  /** 后端 API 前缀，如 /api 或 https://api.example.com/api */
  readonly VITE_API_BASE_URL: string
  /** 开发代理目标，仅 vite.config 读取 */
  readonly VITE_DEV_PROXY_TARGET?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
