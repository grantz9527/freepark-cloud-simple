/**
 * 扫码容器环境：微信内置浏览器只能走微信支付，支付宝内置浏览器只能走支付宝。
 * 普通浏览器不限制，两种方式都可选。
 */
export type PayClientEnv = 'wechat' | 'alipay' | 'browser'

export function detectPayClientEnv(ua = navigator.userAgent): PayClientEnv {
  if (/MicroMessenger/i.test(ua)) return 'wechat'
  if (/AlipayClient|AliApp\(AP/i.test(ua)) return 'alipay'
  return 'browser'
}

/** 当前容器下该缴费方式是否可用 */
export function isPayMethodEnabledInEnv(method: string, env: PayClientEnv = detectPayClientEnv()): boolean {
  if (env === 'wechat') return method !== 'ALIPAY_PAY'
  if (env === 'alipay') return method !== 'WECHAT_PAY'
  return true
}
