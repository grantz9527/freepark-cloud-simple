/**
 * 普通浏览器选择微信支付：尽量走公众号网页授权进微信；
 * 否则提示用户使用微信扫码付款（不展示二维码）。
 *
 * 微信网页授权 10039：redirect_uri 协议必须与公众号后台一致，现网一律用 https。
 */

function isIpHost(host: string): boolean {
  return /^\d{1,3}(\.\d{1,3}){3}$/.test(host) || host === '[::1]' || host === '::1'
}

function isLoopbackHost(host: string): boolean {
  return host === 'localhost' || host === '127.0.0.1' || host === '[::1]' || host === '::1'
}

function samePublicHost(a: string, b: string): boolean {
  return a.replace(/^www\./i, '').toLowerCase() === b.replace(/^www\./i, '').toLowerCase()
}

/** 公众号网页授权只认 https；反代/微信内若仍显示 http，回调也要升到 https，否则报 10039。 */
function applyWeChatHttps(url: URL): void {
  if (isLoopbackHost(url.hostname) || isIpHost(url.hostname)) return
  if (url.protocol === 'http:') {
    url.protocol = 'https:'
  }
  if (url.port === '80' || url.port === '443') {
    url.port = ''
  }
}

function configuredPublicBase(value?: string | null): string | null {
  const raw = (value ?? '').trim()
  if (!raw) return null
  try {
    const url = new URL(raw.includes('://') ? raw : `https://${raw}`)
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return null
    url.username = ''
    url.password = ''
    url.search = ''
    url.hash = ''
    if (isLoopbackHost(url.hostname) || isIpHost(url.hostname)) return null
    applyWeChatHttps(url)
    const path = url.pathname.replace(/\/+$/, '')
    const suffix = !path || path === '/' ? '' : path
    return `${url.protocol}//${url.host}${suffix}`
  } catch {
    return null
  }
}

function normalizeOAuthRedirect(redirectUri: string): string {
  const url = new URL(redirectUri)
  url.hash = ''
  applyWeChatHttps(url)
  return url.toString()
}

function applySessionQuery(url: URL, sessionIds?: number[] | null): void {
  url.searchParams.delete('sessions')
  const ids = (sessionIds ?? []).filter((id) => Number.isFinite(id) && id > 0)
  if (ids.length) {
    url.searchParams.set('sessions', ids.join(','))
  }
}

/** 从配置或当前页取出可用于微信网页授权的 https 根地址（可含子路径）。 */
export function resolveWeChatPageOrigin(userBaseUrl?: string | null): string {
  const configured = configuredPublicBase(userBaseUrl)
  if (configured && configured.startsWith('https://')) return configured
  const here = configuredPublicBase(window.location.origin)
  return here && here.startsWith('https://') ? here : window.location.origin
}

/**
 * 微信内静默授权（snsapi_base）回调地址：
 * 留在当前域名+路径（含 /freepark-user/），协议强制 https，避免 10039。
 */
export function currentPageUrlForOAuth(
  plate?: string | null,
  plateColor?: string | null,
  userBaseUrl?: string | null,
  sessionIds?: number[] | null
): string {
  const page = new URL(window.location.href)
  page.hash = ''
  const configured = configuredPublicBase(userBaseUrl)
  if (configured) {
    try {
      const base = new URL(configured.endsWith('/') ? configured : `${configured}/`)
      if (samePublicHost(base.hostname, page.hostname)) {
        page.protocol = base.protocol
        page.host = base.host
      }
    } catch {
      // 配置解析失败时仍用当前页
    }
  }
  applyWeChatHttps(page)
  page.searchParams.delete('code')
  page.searchParams.delete('state')
  page.searchParams.delete('sessions')
  const plateNo = (plate ?? '').trim()
  if (plateNo) page.searchParams.set('plate', plateNo)
  const color = (plateColor ?? '').trim()
  if (color) page.searchParams.set('plateColor', color)
  page.searchParams.set('openPay', '1')
  applySessionQuery(page, sessionIds)
  return page.toString()
}

/** 该车牌在用户端的查询缴费页（进入后自动查费；微信/支付宝内可带上 openPay 自动弹出缴费）。 */
export function plateQueryAbsoluteUrl(
  userBaseUrl: string | null | undefined,
  plate: string,
  plateColor?: string | null,
  sessionIds?: number[] | null
): string {
  const configured = configuredPublicBase(userBaseUrl)
  const url = configured
    ? new URL(configured.endsWith('/') ? configured : `${configured}/`)
    : new URL(import.meta.env.BASE_URL || '/', `${window.location.origin}/`)
  applyWeChatHttps(url)
  if (plate) url.searchParams.set('plate', plate)
  if (plateColor) url.searchParams.set('plateColor', plateColor)
  url.searchParams.set('openPay', '1')
  applySessionQuery(url, sessionIds)
  return url.toString()
}

export function canUseWeChatOAuth(appId: string, redirectUri: string): boolean {
  if (!appId.trim() || !redirectUri.trim()) return false
  try {
    const target = new URL(redirectUri)
    applyWeChatHttps(target)
    if (target.protocol !== 'https:') return false
    if (isLoopbackHost(target.hostname) || isIpHost(target.hostname)) return false
    return true
  } catch {
    return false
  }
}

export function buildWeChatOAuthUrl(appId: string, redirectUri: string, state = 'pay'): string {
  const params = new URLSearchParams({
    appid: appId.trim(),
    redirect_uri: normalizeOAuthRedirect(redirectUri),
    response_type: 'code',
    scope: 'snsapi_base',
    state
  })
  return `https://open.weixin.qq.com/connect/oauth2/authorize?${params.toString()}#wechat_redirect`
}

export type WeChatLaunchMode = 'oauth' | 'guide'

/**
 * 尽量直接在微信中打开指定页面。oauth 会离开当前页；否则留在本页提示扫码付款。
 */
export function launchWeChat(appId: string, pageUrl: string): WeChatLaunchMode {
  if (canUseWeChatOAuth(appId, pageUrl)) {
    window.location.href = buildWeChatOAuthUrl(appId, pageUrl)
    return 'oauth'
  }
  return 'guide'
}

export async function copyText(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch {
    // 走 execCommand 兜底
  }
  try {
    const input = document.createElement('textarea')
    input.value = text
    input.setAttribute('readonly', '')
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    document.body.appendChild(input)
    input.select()
    const ok = document.execCommand('copy')
    input.remove()
    return ok
  } catch {
    return false
  }
}
