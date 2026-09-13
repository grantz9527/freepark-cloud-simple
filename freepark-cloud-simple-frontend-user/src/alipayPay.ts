/**
 * 支付宝手机网站支付：提交后端签名的自动跳转表单，进入支付宝收银台。
 */

/** 将表单 HTML 写入当前文档并自动提交；成功时页面会离开当前站。 */
export function invokeAlipayWapPay(formHtml: string): boolean {
  const html = (formHtml || '').trim()
  if (!html || !html.includes('alipaysubmit')) {
    return false
  }
  const holder = document.createElement('div')
  holder.style.display = 'none'
  holder.innerHTML = html
  document.body.appendChild(holder)
  const form = holder.querySelector('form') as HTMLFormElement | null
  if (!form) {
    holder.remove()
    return false
  }
  form.submit()
  return true
}
