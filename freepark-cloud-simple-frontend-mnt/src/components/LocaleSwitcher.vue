<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { AppLocale } from '../i18n'
import { useLocaleStore } from '../stores/locale'

const { t } = useI18n()
const localeStore = useLocaleStore()

function onChange(value: string | number | boolean | undefined) {
  if (value === 'zh-CN' || value === 'en') {
    localeStore.setLocale(value as AppLocale)
  }
}
</script>

<template>
  <el-select
    class="locale-switcher"
    :model-value="localeStore.locale"
    size="small"
    :aria-label="t('common.language')"
    @change="onChange"
  >
    <el-option :label="t('common.zhCN')" value="zh-CN" />
    <el-option :label="t('common.en')" value="en" />
  </el-select>
</template>

<style scoped>
.locale-switcher {
  width: 118px;
}
</style>
