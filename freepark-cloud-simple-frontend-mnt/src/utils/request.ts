import axios from 'axios'
import { clearAuth, getToken } from './auth'
import { LOCALE_STORAGE_KEY, type AppLocale } from '../i18n'

/**
 * 后端统一响应结构。
 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
}

/**
 * 业务层错误：携带后端返回的 code/message。
 */
export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

function currentLocale(): AppLocale {
  const saved = localStorage.getItem(LOCALE_STORAGE_KEY)
  if (saved === 'zh-CN' || saved === 'en') {
    return saved
  }
  return 'zh-CN'
}

// 统一的后端请求实例，baseURL 由 .env.* 中的 VITE_API_BASE_URL 决定
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

// 请求头携带 token 与语言
request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['Accept-Language'] = currentLocale()
  return config
})

function rejectWithApiBody(body: unknown, fallbackMessage: string): Promise<never> {
  if (body && typeof body === 'object' && 'code' in body) {
    const result = body as ApiResult
    if (result.code === 401) {
      clearAuth()
    }
    return Promise.reject(new ApiError(result.code, result.message || fallbackMessage))
  }
  return Promise.reject(new ApiError(-1, fallbackMessage))
}

// 解包 ApiResult，成功返回 data，失败抛出 ApiError；登录态失效时清除本地 token
request.interceptors.response.use(
  (response) => {
    // 拦截器实际解包后返回业务数据，类型由调用方通过泛型声明
    const body: any = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      return rejectWithApiBody(body, body.message || 'Request failed')
    }
    return body
  },
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.data) {
      return rejectWithApiBody(error.response.data, error.message)
    }
    return Promise.reject(error)
  }
)

export default request
