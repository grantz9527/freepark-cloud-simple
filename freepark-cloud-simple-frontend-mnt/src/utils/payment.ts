/**
 * 站点收费方式元数据（与后端 SystemSettingsOptions.SUPPORTED_PAYMENT_METHODS 保持一致）。
 */
interface PaymentMethodMeta {
  /** 中文方式名 */
  zhName: string
  /** 英文方式名 */
  enName: string
}

const PAYMENT_METHOD_META: Record<string, PaymentMethodMeta> = {
  WECHAT_PAY: { zhName: '微信支付', enName: 'WeChat Pay' }
}

/** 收费方式名称：中文界面显示中文名（微信支付…），英文界面显示英文名（WeChat Pay…） */
export function paymentMethodNameOf(code: string, locale: string): string {
  const meta = PAYMENT_METHOD_META[code]
  if (!meta) {
    return code
  }
  return locale === 'en' ? meta.enName : meta.zhName
}

/** 配置页候选项标签，如 “WECHAT_PAY · 微信支付” */
export function paymentMethodOptionLabel(code: string, locale: string): string {
  return `${code} · ${paymentMethodNameOf(code, locale)}`
}
