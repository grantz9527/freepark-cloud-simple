/**
 * 用户端（C 端网页）请求封装。
 *
 * 首页调用云端免鉴权公开接口（无需登录）：
 *   GET /api/public/plate-fee?plateNumber=粤B12345
 *   GET /api/public/site-settings
 *   POST /api/public/payment
 *
 * 后端地址由环境变量 VITE_API_BASE_URL 决定（见 .env.development / .env.production），
 * 缺省使用同源 /api。
 */
import { activeLocale } from '../i18n'

const BASE_URL: string = import.meta.env.VITE_API_BASE_URL || '/api'

/** 云端统一失败响应结构（4xx/5xx 或 HTTP 200 包一层 code/message）。 */
interface ApiErrorBody {
  code?: number
  message?: string
  data?: unknown
}

/** 单条费用明细：ONGOING 在场（含免费停放中）/ SETTLED 已出场未结。 */
export interface PlateFeeItem {
  sessionId: number
  type: 'ONGOING' | 'SETTLED'
  lotName: string | null
  /** 车牌颜色枚举名（BLUE/GREEN/YELLOW/…）；记录缺失为 null */
  plateColor: string | null
  entryText: string
  exitText: string | null
  durationText: string
  amount: number
}

/** C 端查费结果。 */
export interface PlateFeeQuote {
  plateNumber: string
  items: PlateFeeItem[]
  totalAmount: number
  /** 该车牌存在记录的颜色枚举名（按 BLUE/YELLOW/GREEN/… 序），用于提示“同车牌多颜色/可切换颜色” */
  colors: string[]
}

/** 将输入车牌规整为大写并去除空白；返回空串表示未填。 */
export function normalizePlate(raw: string): string {
  return raw.replace(/\s+/g, '').toUpperCase()
}

/**
 * 云端站点级公开配置（免鉴权）：决定查费页采用的语言/车牌版式、金额币种与开放缴费方式。
 */
export interface SiteSettings {
  plateRegion: string
  defaultLocale: string
  /** ISO 4217 货币代码（如 CNY/USD/HKD…），决定金额前的货币符号 */
  defaultCurrency: string
  /** 系统配置中开放的线上缴费方式，如 WECHAT_PAY / ALIPAY_PAY */
  allowedPaymentMethods: string[]
  /** 用户端基础地址（公网根，无尾斜杠）；未配置为空串 */
  userBaseUrl: string
  /** 缴费授权公众号 AppID；未配置为空串 */
  wechatMpAppId: string
  /** true 必须一次缴清该车牌全部欠费；false 允许勾选指定停车记录 */
  forcePayAll: boolean
}

export interface PaymentItem {
  sessionId: number
  lotName: string | null
  plateColor: string | null
  sessionStatus: string | null
  entryTime: string | null
  amountYuan: number
}

export interface WeChatJsapiPayParams {
  appId: string
  timeStamp: string
  nonceStr: string
  package: string
  signType: string
  paySign: string
}

export interface PaymentOrder {
  payNo: string
  plateNumber: string
  plateColor: string | null
  amountYuan: number
  method: string
  status: 'PENDING' | 'PAID' | 'CLOSED' | string
  mock: boolean
  transactionId: string | null
  /** 渠道同步跳回地址（用户端基础地址 + /pay/{payNo}） */
  returnUrl: string | null
  items: PaymentItem[]
  /** 微信 JSAPI 调起参数（仅真实下单返回） */
  wxPay: WeChatJsapiPayParams | null
}

function localeHeader(): string {
  return activeLocale.value === 'en' ? 'en' : 'zh-CN'
}

function asNumber(value: unknown): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && value.trim()) {
    const n = Number(value)
    if (Number.isFinite(n)) return n
  }
  return NaN
}

function isApiErrorBody(body: unknown): body is ApiErrorBody {
  return !!body && typeof body === 'object' && 'code' in body && 'message' in body && !('payNo' in body)
}

async function publicRequest(path: string, init?: RequestInit): Promise<unknown> {
  let resp: Response
  try {
    resp = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        'Accept-Language': localeHeader(),
        ...(init?.headers ?? {})
      }
    })
  } catch {
    throw new Error('网络请求失败，请检查网络后重试')
  }

  const text = await resp.text()
  let body: unknown = null
  if (text) {
    try {
      body = JSON.parse(text)
    } catch {
      // 非 JSON：走下方统一错误
    }
  }

  if (isApiErrorBody(body) && typeof body.code === 'number' && body.code !== 200) {
    throw new Error(body.message || `请求失败（${body.code}）`)
  }
  if (!resp.ok) {
    const message = (body as ApiErrorBody | null)?.message
    throw new Error(message || `请求失败（HTTP ${resp.status}）`)
  }
  return body
}

function parsePayment(body: unknown): PaymentOrder {
  const raw = body as Record<string, unknown> | null
  const amountYuan = asNumber(raw?.amountYuan)
  if (
    !raw ||
    typeof raw.payNo !== 'string' ||
    typeof raw.plateNumber !== 'string' ||
    !Number.isFinite(amountYuan) ||
    typeof raw.method !== 'string' ||
    typeof raw.status !== 'string'
  ) {
    throw new Error('返回数据格式异常，请稍后重试')
  }
  const items = Array.isArray(raw.items)
    ? raw.items.map((item) => {
        const row = item as Record<string, unknown>
        return {
          sessionId: asNumber(row.sessionId),
          lotName: typeof row.lotName === 'string' ? row.lotName : null,
          plateColor: typeof row.plateColor === 'string' ? row.plateColor : null,
          sessionStatus: typeof row.sessionStatus === 'string' ? row.sessionStatus : null,
          entryTime: typeof row.entryTime === 'string' ? row.entryTime : null,
          amountYuan: asNumber(row.amountYuan)
        }
      })
    : []
  return {
    payNo: raw.payNo,
    plateNumber: raw.plateNumber,
    plateColor: typeof raw.plateColor === 'string' ? raw.plateColor : null,
    amountYuan,
    method: raw.method,
    status: raw.status,
    mock: raw.mock === true,
    transactionId: typeof raw.transactionId === 'string' ? raw.transactionId : null,
    returnUrl: typeof raw.returnUrl === 'string' && raw.returnUrl ? raw.returnUrl : null,
    items,
    wxPay: parseWxPay(raw.wxPay)
  }
}

function parseWxPay(value: unknown): WeChatJsapiPayParams | null {
  if (!value || typeof value !== 'object') return null
  const row = value as Record<string, unknown>
  const appId = typeof row.appId === 'string' ? row.appId : ''
  const timeStamp = typeof row.timeStamp === 'string' ? row.timeStamp : ''
  const nonceStr = typeof row.nonceStr === 'string' ? row.nonceStr : ''
  const pkg = typeof row.package === 'string' ? row.package : ''
  const signType = typeof row.signType === 'string' ? row.signType : 'RSA'
  const paySign = typeof row.paySign === 'string' ? row.paySign : ''
  if (!appId || !timeStamp || !nonceStr || !pkg || !paySign) return null
  return { appId, timeStamp, nonceStr, package: pkg, signType, paySign }
}

/**
 * 拉取站点级公开配置。
 * 接口 404 等异常不影响主流程：由调用方捕获并按 CN/CNY 兜底。
 */
export async function fetchSiteSettings(): Promise<SiteSettings> {
  const body = (await publicRequest('/public/site-settings', { method: 'GET' })) as Record<
    string,
    unknown
  >
  const region = typeof body?.plateRegion === 'string' ? body.plateRegion : 'CN'
  const locale = typeof body?.defaultLocale === 'string' ? body.defaultLocale : 'zh-CN'
  const currency =
    typeof body?.defaultCurrency === 'string' && body.defaultCurrency ? body.defaultCurrency : 'CNY'
  const methods = Array.isArray(body?.allowedPaymentMethods)
    ? body.allowedPaymentMethods.filter((item): item is string => typeof item === 'string' && !!item)
    : []
  const userBaseUrl = typeof body?.userBaseUrl === 'string' ? body.userBaseUrl : ''
  const wechatMpAppId = typeof body?.wechatMpAppId === 'string' ? body.wechatMpAppId : ''
  return {
    plateRegion: region,
    defaultLocale: locale,
    defaultCurrency: currency,
    allowedPaymentMethods: methods,
    userBaseUrl,
    wechatMpAppId,
    forcePayAll: body?.forcePayAll !== false
  }
}

/**
 * 查询车牌当前的费用信息。
 * @param plateColor 可选：同一车牌存在不同颜色记录时，传入颜色枚举名仅统计该颜色
 */
export async function queryPlateFee(plateNumber: string, plateColor?: string): Promise<PlateFeeQuote> {
  const params = new URLSearchParams({ plateNumber })
  if (plateColor) {
    params.set('plateColor', plateColor)
  }
  const body = await publicRequest(`/public/plate-fee?${params.toString()}`, { method: 'GET' })
  const quote = body as PlateFeeQuote | null
  if (
    !quote ||
    typeof quote.plateNumber !== 'string' ||
    !Array.isArray(quote.items) ||
    typeof quote.totalAmount !== 'number' ||
    !Array.isArray(quote.colors)
  ) {
    throw new Error('返回数据格式异常，请稍后重试')
  }
  return quote
}

/** 按车牌下单：强制全部支付时缴清全部未结；允许勾选时传入 sessionIds。 */
export async function createPayment(
  plateNumber: string,
  method: string,
  plateColor?: string | null,
  wxCode?: string | null,
  sessionIds?: number[] | null
): Promise<PaymentOrder> {
  const body = await publicRequest('/public/payment', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      plateNumber,
      method,
      plateColor: plateColor || undefined,
      wxCode: wxCode || undefined,
      sessionIds: sessionIds && sessionIds.length ? sessionIds : undefined
    })
  })
  return parsePayment(body)
}

/** 查询缴款单当前状态（待缴页轮询）。 */
export async function fetchPayment(payNo: string): Promise<PaymentOrder> {
  const body = await publicRequest(`/public/payment/${encodeURIComponent(payNo)}`, { method: 'GET' })
  return parsePayment(body)
}

/** 本地联调确认（模拟渠道回调）。success=false 表示取消/失败。 */
export async function confirmSandboxPayment(payNo: string, success = true): Promise<PaymentOrder> {
  const params = new URLSearchParams({ success: success ? 'true' : 'false' })
  const body = await publicRequest(`/public/payment/${encodeURIComponent(payNo)}/sandbox-confirm?${params}`, {
    method: 'POST'
  })
  return parsePayment(body)
}

/** 关闭待缴单（放弃本次支付）。 */
export async function closePayment(payNo: string): Promise<void> {
  await publicRequest(`/public/payment/${encodeURIComponent(payNo)}/close`, { method: 'POST' })
}
