<script setup lang="ts">
import { computed } from 'vue'
import { ElConfigProvider } from 'element-plus'
import { useLocaleStore } from './stores/locale'

const localeStore = useLocaleStore()
const elementLocale = computed(() => localeStore.elementLocale)
</script>

<template>
  <ElConfigProvider :locale="elementLocale">
    <RouterView />
  </ElConfigProvider>
</template>
