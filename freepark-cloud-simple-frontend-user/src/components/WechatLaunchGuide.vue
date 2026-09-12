<script setup lang="ts">
import { t } from '../i18n'
</script>

<template>
  <section class="wx-guide" aria-live="polite">
    <div class="wx-badge" aria-hidden="true">
      <!-- WeChat speech-bubble mark -->
      <svg class="wx-logo" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
        <path
          fill="#fff"
          d="M9.5 4C5.91 4 3 6.69 3 10c0 1.89.97 3.57 2.48 4.7l-.62 2.33 2.55-1.28c.9.28 1.86.44 2.87.45-.14-.52-.22-1.06-.22-1.62 0-3.31 2.97-6 6.63-6 .31 0 .62.02.92.05C16.7 5.82 13.38 4 9.5 4zm-2.25 3.38a.94.94 0 1 1 0 1.88.94.94 0 0 1 0-1.88zm4.5 0a.94.94 0 1 1 0 1.88.94.94 0 0 1 0-1.88zM16.69 9.58c-2.94 0-5.32 2.1-5.32 4.7 0 2.59 2.38 4.69 5.32 4.69.62 0 1.21-.1 1.76-.27l1.9.95-.47-1.74c1.12-.9 1.84-2.2 1.84-3.63 0-2.6-2.38-4.7-5.03-4.7zm-1.88 3.28a.75.75 0 1 1 0 1.5.75.75 0 0 1 0-1.5zm3.75 0a.75.75 0 1 1 0 1.5.75.75 0 0 1 0-1.5z"
        />
      </svg>
    </div>
    <h2 class="wx-title">{{ t('pay.wechat.scanPay') }}</h2>
    <p class="wx-hint">{{ t('pay.wechat.scanPayHint') }}</p>
  </section>
</template>

<style scoped>
.wx-guide {
  margin-top: 14px;
  padding: 22px 18px 20px;
  border-radius: 18px;
  text-align: center;
  background:
    linear-gradient(180deg, rgba(7, 193, 96, 0.14) 0%, rgba(7, 193, 96, 0.05) 100%);
  border: 1.5px solid rgba(7, 193, 96, 0.35);
  box-shadow: 0 10px 24px rgba(7, 193, 96, 0.12);
}
.wx-badge {
  width: 56px;
  height: 56px;
  margin: 0 auto 14px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: #07c160;
  box-shadow: 0 8px 18px rgba(7, 193, 96, 0.35);
}
.wx-logo {
  width: 30px;
  height: 30px;
  display: block;
}
.wx-title {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.35;
  color: #065f37;
  letter-spacing: 0.01em;
}
.wx-hint {
  margin: 8px auto 0;
  max-width: 280px;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.6;
  color: #2f6b4d;
}
</style>
