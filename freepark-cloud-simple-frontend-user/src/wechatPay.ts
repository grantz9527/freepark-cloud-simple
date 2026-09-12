/**
 * 微信内调起 JSAPI 支付（WeixinJSBridge.getBrandWCPayRequest）。
 */

export interface WeChatJsapiPayParams {
  appId: string
  timeStamp: string
  nonceStr: string
  package: string
  signType: string
  paySign: string
}

declare global {
  interface Window {
    WeixinJSBridge?: {
      invoke: (
        api: string,
        params: Record<string, string>,
        callback: (res: { err_msg?: string }) => void
      ) => void
    }
  }
}

function waitBridge(): Promise<void> {
  if (typeof window.WeixinJSBridge !== 'undefined') {
    return Promise.resolve()
  }
  return new Promise((resolve) => {
    let done = false
    const finish = () => {
      if (done) return
      done = true
      document.removeEventListener('WeixinJSBridgeReady', onReady)
      resolve()
    }
    const onReady = () => finish()
    document.addEventListener('WeixinJSBridgeReady', onReady, false)
    window.setTimeout(finish, 1500)
  })
}

export type WeChatPayInvokeResult = 'ok' | 'cancel' | 'fail'

/** 调起微信支付收银台；结果以异步通知为准，ok 仅表示前端回调成功。 */
export async function invokeWeChatJsapiPay(params: WeChatJsapiPayParams): Promise<WeChatPayInvokeResult> {
  if (!params?.appId || !params.timeStamp || !params.nonceStr || !params.package || !params.paySign) {
    return 'fail'
  }
  await waitBridge()
  if (typeof window.WeixinJSBridge === 'undefined') {
    return 'fail'
  }
  return new Promise((resolve) => {
    window.WeixinJSBridge!.invoke(
      'getBrandWCPayRequest',
      {
        appId: params.appId,
        timeStamp: params.timeStamp,
        nonceStr: params.nonceStr,
        package: params.package,
        signType: params.signType || 'RSA',
        paySign: params.paySign
      },
      (res) => {
        const msg = (res?.err_msg || '').toLowerCase()
        if (msg.includes(':ok')) {
          resolve('ok')
          return
        }
        if (msg.includes(':cancel') || msg.includes('cancel')) {
          resolve('cancel')
          return
        }
        resolve('fail')
      }
    )
  })
}

const OAUTH_CODE_KEY = 'fp-wx-oauth-code'

/** 取出并清除一次性 OAuth code（授权回调写入）。 */
export function takeWeChatOAuthCode(): string {
  try {
    const code = sessionStorage.getItem(OAUTH_CODE_KEY) || ''
    if (code) sessionStorage.removeItem(OAUTH_CODE_KEY)
    return code
  } catch {
    return ''
  }
}

export function peekWeChatOAuthCode(): string {
  try {
    return sessionStorage.getItem(OAUTH_CODE_KEY) || ''
  } catch {
    return ''
  }
}
