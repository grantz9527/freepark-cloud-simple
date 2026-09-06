import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import type { Language } from 'element-plus/es/locale'
import i18n, { LOCALE_STORAGE_KEY, type AppLocale, resolveInitialLocale } from '../i18n'

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<AppLocale>(resolveInitialLocale())

  const elementLocale = computed<Language>(() => (locale.value === 'en' ? en : zhCn))

  function setLocale(next: AppLocale) {
    locale.value = next
  }

  function toggleLocale() {
    setLocale(locale.value === 'zh-CN' ? 'en' : 'zh-CN')
  }

  watch(
    locale,
    (value) => {
      i18n.global.locale.value = value
      localStorage.setItem(LOCALE_STORAGE_KEY, value)
      document.documentElement.lang = value
    },
    { immediate: true }
  )

  return {
    locale,
    elementLocale,
    setLocale,
    toggleLocale
  }
})
