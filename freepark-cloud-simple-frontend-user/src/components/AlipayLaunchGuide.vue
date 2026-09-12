<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { buildAlipayOpenScheme } from '../alipayLaunch'
import { t } from '../i18n'
import { copyText } from '../wechatLaunch'

const props = defineProps<{
  pageUrl: string
}>()

const qrSrc = ref('')
const copied = ref(false)

async function renderQr() {
  if (!props.pageUrl) {
    qrSrc.value = ''
    return
  }
  try {
    const QRCode = await import('qrcode')
    qrSrc.value = await QRCode.toDataURL(props.pageUrl, {
      width: 220,
      margin: 1,
      color: { dark: '#111111', light: '#ffffff' }
    })
  } catch {
    qrSrc.value = ''
  }
}

function openAlipay() {
  if (!props.pageUrl) return
  window.location.href = buildAlipayOpenScheme(props.pageUrl)
}

async function copyLink() {
  copied.value = await copyText(props.pageUrl)
  window.setTimeout(() => {
    copied.value = false
  }, 2000)
}

onMounted(() => {
  void renderQr()
})

watch(
  () => props.pageUrl,
  () => {
    void renderQr()
  }
)
</script>

<template>
  <section class="ali-guide" aria-live="polite">
    <h2>{{ t('pay.alipay.guideTitle') }}</h2>
    <p class="ali-body">{{ t('pay.alipay.guideBody') }}</p>
    <img v-if="qrSrc" class="ali-qr" :src="qrSrc" :alt="t('pay.alipay.scan')" />
    <p class="ali-scan">{{ t('pay.alipay.scan') }}</p>
    <button class="btn btn-ali" type="button" @click="openAlipay">
      {{ t('pay.alipay.openApp') }}
    </button>
    <button class="btn btn-copy" type="button" @click="copyLink">
      {{ copied ? t('pay.alipay.copied') : t('pay.alipay.copyLink') }}
    </button>
  </section>
</template>

<style scoped>
.ali-guide {
  margin-top: 14px;
  padding: 18px 16px 16px;
  border-radius: 20px;
  background: var(--fp-card);
  border: 1px solid var(--fp-line);
  box-shadow: var(--fp-shadow-card);
  text-align: center;
}
.ali-guide h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: var(--fp-ink);
}
.ali-body,
.ali-scan {
  margin: 8px auto 0;
  max-width: 320px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--fp-muted);
}
.ali-qr {
  display: block;
  width: 180px;
  height: 180px;
  margin: 14px auto 0;
  border-radius: 12px;
  background: #fff;
}
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 48px;
  margin-top: 12px;
  border: none;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}
.btn-ali {
  background: #1677ff;
  color: #fff;
}
.btn-copy {
  background: var(--fp-gradient-soft);
  color: var(--fp-accent-deep);
  margin-top: 8px;
}
</style>
