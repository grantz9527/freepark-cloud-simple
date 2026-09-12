/**
 * 普通浏览器选择支付宝：把「该车牌的查询缴费页」交给支付宝内置浏览器打开。
 * 使用 alipays:// URL Scheme 唤起 App；唤起失败时页面留在引导态，可扫码打开。
 */

/** 支付宝内置浏览器（H5 容器）AppId */
const ALIPAY_BROWSER_APP_ID = '20000067'

/** 构造在支付宝中打开指定 H5 页的 scheme。 */
export function buildAlipayOpenScheme(pageUrl: string): string {
  const params = new URLSearchParams({
    appId: ALIPAY_BROWSER_APP_ID,
    url: pageUrl
  })
  return `alipays://platformapi/startapp?${params.toString()}`
}

/**
 * 尽量直接在支付宝中打开指定页面。
 * 未安装支付宝时 scheme 无效，调用方应同时展示扫码引导。
 */
export function launchAlipay(pageUrl: string): void {
  const target = pageUrl.trim()
  if (!target) return
  window.location.href = buildAlipayOpenScheme(target)
}
