import { createI18n } from 'vue-i18n'
import en from './locales/en'
import zhCN from './locales/zh-CN'

export type AppLocale = 'zh-CN' | 'en'

export const LOCALE_STORAGE_KEY = 'fp_admin_locale'

export function resolveInitialLocale(): AppLocale {
  const saved = localStorage.getItem(LOCALE_STORAGE_KEY)
  if (saved === 'zh-CN' || saved === 'en') {
    return saved
  }
  const nav = navigator.language?.toLowerCase() ?? ''
  return nav.startsWith('zh') ? 'zh-CN' : 'en'
}

const i18n = createI18n({
  legacy: false,
  locale: resolveInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    en
  }
})

export default i18n
