/**
 * 用户端（C 端网页）请求封装。
 *
 * 首页调用云端免鉴权公开接口（无需登录）：
 *   GET /api/public/plate-fee?plateNumber=粤B12345
 * 返回车牌当前的费用信息：在停（估算/免费中）与历史未结明细 + 合计金额。
 *
 * 后端地址可通过环境变量 VITE_API_BASE_URL 覆盖（如 http://localhost:8080/api），
 * 缺省使用同源 /api。
 */
const BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** 云端统一失败响应结构（4xx/5xx 时返回，携带 code/message）。 */
interface ApiErrorBody {
  code?: number
  message?: string
}

/** 单条费用明细：ONGOING 在场（含免费停放中）/ SETTLED 已出场未结。 */
export interface PlateFeeItem {
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
 * 云端站点级公开配置（免鉴权）：决定查费页采用的语言/车牌版式与金额币种等交互。
 */
export interface SiteSettings {
  plateRegion: string
  defaultLocale: string
  /** ISO 4217 货币代码（如 CNY/USD/HKD…），决定金额前的货币符号 */
  defaultCurrency: string
}

/**
 * 拉取站点级公开配置。
 * 接口 404 等异常不影响主流程：由调用方捕获并按 CN/CNY 兜底。
 */
export async function fetchSiteSettings(): Promise<SiteSettings> {
  const resp = await fetch(`${BASE_URL}/public/site-settings`, { method: 'GET' })
  if (!resp.ok) {
    throw new Error(`站点配置拉取失败（HTTP ${resp.status}）`)
  }
  const text = await resp.text()
  const body = text ? (JSON.parse(text) as Record<string, unknown>) : {}
  const region = typeof body.plateRegion === 'string' ? body.plateRegion : 'CN'
  const locale = typeof body.defaultLocale === 'string' ? body.defaultLocale : 'zh-CN'
  const currency =
    typeof body.defaultCurrency === 'string' && body.defaultCurrency ? body.defaultCurrency : 'CNY'
  return { plateRegion: region, defaultLocale: locale, defaultCurrency: currency }
}

/**
 * 查询车牌当前的费用信息。
 * @param plateColor 可选：同一车牌存在不同颜色记录时，传入颜色枚举名仅统计该颜色
 */
export async function queryPlateFee(plateNumber: string, plateColor?: string): Promise<PlateFeeQuote> {
  let resp: Response
  try {
    const params = new URLSearchParams({ plateNumber })
    if (plateColor) {
      params.set('plateColor', plateColor)
    }
    resp = await fetch(`${BASE_URL}/public/plate-fee?${params.toString()}`, {
      method: 'GET'
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
      // 非 JSON 响应：统一走下方错误提示
    }
  }

  if (!resp.ok) {
    const message = (body as ApiErrorBody | null)?.message
    throw new Error(message || `查询失败（HTTP ${resp.status}）`)
  }

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
