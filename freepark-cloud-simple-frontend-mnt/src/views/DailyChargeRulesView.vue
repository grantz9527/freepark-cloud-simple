<script setup lang="ts">
import { useBiText, type BiDict } from '../utils/biText'

const d: BiDict = {
  title: { 'zh-CN': '每日制计费规则', en: 'Daily Billing Rules' },
  body: {
    'zh-CN': '该功能页面为占位，后续将接入真实计费能力。',
    en: 'This page is a placeholder. Real billing rule features will be connected later.'
  }
}

const { t } = useBiText(d)
</script>

<template>
  <section class="charge-page">
    <el-card class="panel" shadow="never">
      <div class="placeholder">
        <h2 class="placeholder-title">{{ t('title') }}</h2>
        <p class="placeholder-body">{{ t('body') }}</p>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.charge-page {
  max-width: 720px;
  animation: fade-up 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.panel {
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-soft);
}

.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 56px 20px;
  text-align: center;
}

.placeholder-title {
  margin: 0;
  font-family: var(--fp-font-display);
  font-size: 1.15rem;
  color: var(--fp-ink);
}

.placeholder-body {
  margin: 0;
  max-width: 42ch;
  line-height: 1.65;
  color: var(--fp-muted);
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
