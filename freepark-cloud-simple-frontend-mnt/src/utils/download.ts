import { getToken } from './auth'

/**
 * 携带鉴权与语言请求后端文件（导入模板/导出），并以附件方式保存。
 * 后端统一返回二进制，需要绕过 JSON 拦截器，故此处直接使用 fetch。
 *
 * @param path        接口路径（相对 /api 根，如 /lots/1/internal-vehicles/export?plate=）
 * @param fallbackName 服务端未返回 Content-Disposition 时的默认文件名
 */
export async function downloadFile(path: string, fallbackName: string): Promise<void> {
  const base = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '')
  const response = await fetch(`${base}${path}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
  const blob = await response.blob()
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const match = /filename="?([^";]+)"?/i.exec(disposition)
  const filename = match && match[1] ? match[1] : fallbackName
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(url)
}
