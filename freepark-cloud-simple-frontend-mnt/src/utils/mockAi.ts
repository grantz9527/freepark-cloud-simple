import type { Composer } from 'vue-i18n'

type Translate = Composer['t']

export function mockAiReply(input: string, t: Translate): string {
  const text = input.toLowerCase()
  if (/车场|泊位|停车|lot|parking|stall|occup/.test(text)) {
    return String(t('ai.replies.parking'))
  }
  if (/订单|计费|退款|order|billing|refund/.test(text)) {
    return String(t('ai.replies.order'))
  }
  if (/设备|道闸|相机|device|barrier|camera|sensor/.test(text)) {
    return String(t('ai.replies.device'))
  }
  if (/帮助|怎么|如何|help|how|what/.test(text)) {
    return String(t('ai.replies.help'))
  }
  return String(t('ai.replies.fallback'))
}
