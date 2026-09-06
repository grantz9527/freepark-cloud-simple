import { computed } from 'vue'
import { useLocaleStore } from '../stores/locale'

/** 页内双语字典：每个词条提供中英文两套文案。 */
export type BiDict = Record<string, { 'zh-CN': string; en: string }>

/**
 * 基于当前应用语言解析页内双语字典。
 * 页面文案集中在各自 .vue 内维护，避免向全局 i18n 注册大量页面级 key。
 *
 * @example
 * const d = { title: { 'zh-CN': '停车场', en: 'Parking Lots' } }
 * const { t } = useBiText(d)
 */
export function useBiText(dict: BiDict) {
  const localeStore = useLocaleStore()
  const locale = computed(() => localeStore.locale)

  function t(key: string): string {
    const entry = dict[key]
    if (!entry) {
      return key
    }
    return entry[locale.value] ?? entry['zh-CN'] ?? key
  }

  return { t, locale }
}
