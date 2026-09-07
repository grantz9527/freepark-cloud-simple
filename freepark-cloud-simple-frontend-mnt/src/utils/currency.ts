import { computed, ref } from 'vue'
import { useLocaleStore } from '../stores/locale'
import request from './request'

/**
 * ISO 4217 币种元数据（与后端 SystemSettingsOptions.SUPPORTED_CURRENCIES 保持一致）。
 * 站点「收费金额单位」的展示：中文界面显示本地单位（元/美元…），英文界面显示货币代码（CNY/USD…）。
 */
interface CurrencyMeta {
  /** 币种符号（用于配置页与金额附近简洁标注） */
  symbol: string
  /** 中文单位名称 */
  zhUnit: string
  /** 中文币种全称（配置下拉候选用） */
  zhName: string
  /** 英文币种全称（配置下拉候选用） */
  enName: string
}

const CURRENCY_META: Record<string, CurrencyMeta> = {
  CNY: { symbol: '¥', zhUnit: '元', zhName: '人民币', enName: 'Chinese Yuan' },
  USD: { symbol: '$', zhUnit: '美元', zhName: '美元', enName: 'US Dollar' },
  HKD: { symbol: 'HK$', zhUnit: '港币', zhName: '港币', enName: 'Hong Kong Dollar' },
  TWD: { symbol: 'NT$', zhUnit: '新台币', zhName: '新台币', enName: 'New Taiwan Dollar' },
  MOP: { symbol: 'MOP$', zhUnit: '澳门元', zhName: '澳门元', enName: 'Macanese Pataca' },
  JPY: { symbol: '¥', zhUnit: '日元', zhName: '日元', enName: 'Japanese Yen' },
  KRW: { symbol: '₩', zhUnit: '韩元', zhName: '韩元', enName: 'South Korean Won' },
  SGD: { symbol: 'S$', zhUnit: '新加坡元', zhName: '新加坡元', enName: 'Singapore Dollar' },
  MYR: { symbol: 'RM', zhUnit: '林吉特', zhName: '马来西亚林吉特', enName: 'Malaysian Ringgit' },
  THB: { symbol: '฿', zhUnit: '泰铢', zhName: '泰铢', enName: 'Thai Baht' },
  VND: { symbol: '₫', zhUnit: '越南盾', zhName: '越南盾', enName: 'Vietnamese Dong' },
  EUR: { symbol: '€', zhUnit: '欧元', zhName: '欧元', enName: 'Euro' },
  GBP: { symbol: '£', zhUnit: '英镑', zhName: '英镑', enName: 'British Pound' },
  CHF: { symbol: 'Fr', zhUnit: '瑞士法郎', zhName: '瑞士法郎', enName: 'Swiss Franc' },
  AUD: { symbol: 'A$', zhUnit: '澳元', zhName: '澳元', enName: 'Australian Dollar' },
  CAD: { symbol: 'C$', zhUnit: '加元', zhName: '加元', enName: 'Canadian Dollar' },
  NZD: { symbol: 'NZ$', zhUnit: '新西兰元', zhName: '新西兰元', enName: 'New Zealand Dollar' }
}

/** 默认币种：CNY（与后端 DEFAULT_CURRENCY 一致） */
export const DEFAULT_CURRENCY_CODE = 'CNY'

export interface CurrencyView {
  code: string
  meta: CurrencyMeta
  /** 金额单位：中文=本地单位（元/美元…），英文=货币代码（CNY/USD…） */
  unit: string
  symbol: string
  /** 下拉候选中展示的名称，如 “CNY · 人民币” */
  optionLabel: string
}

export function currencyMetaOf(code: string): CurrencyMeta {
  return (
    CURRENCY_META[code] ?? {
      symbol: code,
      zhUnit: code,
      zhName: code,
      enName: code
    }
  )
}

/** 币种名称：中文返回中文名（人民币…），英文返回英文名（Chinese Yuan…） */
export function currencyNameOf(code: string, locale: string): string {
  const meta = currencyMetaOf(code)
  return locale === 'en' ? meta.enName : meta.zhName
}

/** 系统配置下拉候选项标签，如 “CNY · 人民币” */
export function currencyOptionLabel(code: string, locale: string): string {
  return `${code} · ${currencyNameOf(code, locale)}`
}

/** 站点当前生效的默认币种；页面挂载时调用 load() 刷新 */
export function useDefaultCurrency() {
  const localeStore = useLocaleStore()
  const code = ref(DEFAULT_CURRENCY_CODE)
  const view = computed<CurrencyView>(() => {
    const meta = currencyMetaOf(code.value)
    const zh = localeStore.locale === 'zh-CN'
    return {
      code: code.value,
      meta,
      unit: zh ? meta.zhUnit : code.value,
      symbol: meta.symbol,
      optionLabel: `${code.value} · ${zh ? meta.zhName : meta.enName}`
    }
  })

  async function load(): Promise<string> {
    try {
      const settings = await request.get<never, { defaultCurrency?: string }>('/system/settings')
      if (settings?.defaultCurrency) {
        code.value = settings.defaultCurrency
      }
    } catch {
      // 读取失败回退默认币种，不阻塞页面主流程
    }
    return code.value
  }

  return { code, view, load }
}
