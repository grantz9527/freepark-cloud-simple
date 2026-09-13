<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  confirmSandboxPayment,
  fetchPayment,
  fetchSiteSettings,
  type PaymentOrder
} from '../api/client'
import { launchAlipay } from '../alipayLaunch'
import AlipayLaunchGuide from '../components/AlipayLaunchGuide.vue'
import WechatLaunchGuide from '../components/WechatLaunchGuide.vue'
import { localeButtonText, toggleLocale, t } from '../i18n'
import { detectPayClientEnv } from '../payEnv'
import { launchWeChat, plateQueryAbsoluteUrl } from '../wechatLaunch'

const POLL_MS = 2000
const POLL_TIMEOUT_MS = 60_000

const route = useRoute()
const router = useRouter()

const payNo = computed(() => String(route.params.payNo ?? '').trim())
const order = ref<PaymentOrder | null>(null)
const error = ref('')
const loading = ref(true)
const currencySymbol = ref('¥')
const wechatMpAppId = ref('')
const userBaseUrl = ref('')
const clientEnv = detectPayClientEnv()

let timer: ReturnType<typeof setTimeout> | null = null
let stopped = false
let pollStartedAt = 0

const status = computed(() => order.value?.status ?? '')
const isPending = computed(() => status.value === 'PENDING')
const isPaid = computed(() => status.value === 'PAID')
const isClosed = computed(() => status.value === 'CLOSED')
const timedOut = ref(false)
const isTimedOut = computed(() => timedOut.value && isPending.value)
const showWechatGuide = computed(
  () =>
    !loading.value &&
    isPending.value &&
    !isTimedOut.value &&
    order.value?.method === 'WECHAT_PAY' &&
    clientEnv === 'browser' &&
    !order.value?.mock
)
const showAlipayGuide = computed(
  () =>
    !loading.value &&
    isPending.value &&
    !isTimedOut.value &&
    order.value?.method === 'ALIPAY_PAY' &&
    clientEnv === 'browser' &&
    !order.value?.mock
)
const showMockConfirm = computed(
  () =>
    !loading.value &&
    isPending.value &&
    !isTimedOut.value &&
    !!order.value?.mock &&
    // 支付宝已接真实支付，结果页不应再出现联调「确认已支付」
    order.value.method !== 'ALIPAY_PAY'
)
const confirming = ref(false)
const handoffPageUrl = computed(() => {
  if (!order.value) return ''
  return plateQueryAbsoluteUrl(userBaseUrl.value, order.value.plateNumber, order.value.plateColor)
})

function launchedStorageKey(channel: 'wx' | 'ali') {
  return `fp-${channel}-launched-${payNo.value}`
}

function consumeWeChatOAuthQuery() {
  const code = route.query.code
  if (typeof code !== 'string' || !code || !payNo.value) return
  sessionStorage.setItem(`fp-wx-oauth-code-${payNo.value}`, code)
  const nextQuery: Record<string, string> = {}
  for (const [key, value] of Object.entries(route.query)) {
    if (key === 'code' || key === 'state') continue
    if (typeof value === 'string' && value) nextQuery[key] = value
  }
  void router.replace({
    name: 'pay-status',
    params: { payNo: payNo.value },
    query: nextQuery
  })
}

function autoLaunchWeChatOnce() {
  if (!showWechatGuide.value || !payNo.value || !handoffPageUrl.value) return
  if (sessionStorage.getItem(launchedStorageKey('wx'))) return
  sessionStorage.setItem(launchedStorageKey('wx'), '1')
  launchWeChat(wechatMpAppId.value, handoffPageUrl.value)
}

function autoLaunchAlipayOnce() {
  if (!showAlipayGuide.value || !payNo.value || !handoffPageUrl.value) return
  if (sessionStorage.getItem(launchedStorageKey('ali'))) return
  sessionStorage.setItem(launchedStorageKey('ali'), '1')
  launchAlipay(handoffPageUrl.value)
}

function money(value: number): string {
  return value.toFixed(2)
}

function methodLabel(code?: string): string {
  if (!code) return ''
  const key = `pay.method.${code}`
  const label = t(key)
  return label === key ? code : label
}

function fmtDateTime(value: string | null | undefined): string {
  if (!value) return '—'
  const text = value.includes('T') ? value.replace('T', ' ') : value
  const clean = text.split('.')[0]
  return clean.length === 16 ? `${clean}:00` : clean
}

function homeQuery() {
  const plate = order.value?.plateNumber || String(route.query.plate ?? '')
  const plateColor = order.value?.plateColor || String(route.query.plateColor ?? '')
  const query: Record<string, string> = {}
  if (plate) query.plate = plate
  if (plateColor) query.plateColor = plateColor
  return query
}

function goHome() {
  stopPolling()
  router.push({ name: 'home', query: homeQuery() })
}

function stopPolling() {
  stopped = true
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

function hasPollTimedOut() {
  return pollStartedAt > 0 && Date.now() - pollStartedAt >= POLL_TIMEOUT_MS
}

function markTimedOut() {
  timedOut.value = true
  stopPolling()
}

function scheduleNext() {
  if (stopped || !isPending.value) return
  if (hasPollTimedOut()) {
    markTimedOut()
    return
  }
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    void refresh()
  }, POLL_MS)
}

async function refresh() {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
  if (!payNo.value) {
    error.value = t('pay.wait.loadFailed')
    loading.value = false
    return
  }
  try {
    order.value = await fetchPayment(payNo.value)
    error.value = ''
  } catch (e) {
    error.value = e instanceof Error ? e.message : t('pay.wait.loadFailed')
  } finally {
    loading.value = false
    if (!stopped && isPending.value && !order.value?.mock) {
      if (hasPollTimedOut()) markTimedOut()
      else scheduleNext()
    }
  }
}

async function confirmMock() {
  if (!payNo.value || confirming.value) return
  confirming.value = true
  error.value = ''
  try {
    order.value = await confirmSandboxPayment(payNo.value, true)
  } catch (e) {
    error.value = e instanceof Error ? e.message : t('pay.failed')
  } finally {
    confirming.value = false
  }
}

onMounted(async () => {
  consumeWeChatOAuthQuery()
  try {
    const settings = await fetchSiteSettings()
    wechatMpAppId.value = settings.wechatMpAppId || ''
    userBaseUrl.value = settings.userBaseUrl || ''
    const code = settings.defaultCurrency || 'CNY'
    const symbols: Record<string, string> = {
      CNY: '¥',
      USD: '$',
      HKD: 'HK$',
      TWD: 'NT$',
      EUR: '€',
      GBP: '£'
    }
    currencySymbol.value = symbols[code] ?? code
  } catch {
    // 币种兜底 ¥
  }
  stopped = false
  timedOut.value = false
  pollStartedAt = Date.now()
  await refresh()
  autoLaunchWeChatOnce()
  autoLaunchAlipayOnce()
})

watch(showWechatGuide, (show) => {
  if (show) autoLaunchWeChatOnce()
})

watch(showAlipayGuide, (show) => {
  if (show) autoLaunchAlipayOnce()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="app">
    <div class="bg-blobs" aria-hidden="true">
      <i class="blob blob-a" />
      <i class="blob blob-b" />
    </div>

    <header class="topbar">
      <button class="icon-btn" type="button" :aria-label="t('act.back')" @click="goHome">
        <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
          <path
            d="M15 5l-7 7 7 7"
            fill="none"
            stroke="currentColor"
            stroke-width="2.4"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
      <span class="brand">FreePark<em>{{ t('pay.wait.title') }}</em></span>
      <button class="lang-btn" type="button" @click="toggleLocale">{{ localeButtonText }}</button>
    </header>

    <main class="page">
      <section
        class="card status-card"
        :class="isPaid ? 'is-paid' : isTimedOut || isClosed ? 'is-closed' : 'is-pending'"
      >
        <div class="status-icon" aria-hidden="true">
          <span v-if="(loading || isPending) && !isTimedOut" class="spinner" />
          <svg v-else-if="isPaid" viewBox="0 0 24 24" width="36" height="36">
            <path
              d="M6 12.5l4 4 8-9"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <svg v-else viewBox="0 0 24 24" width="36" height="36">
            <path
              d="M8 8l8 8M16 8l-8 8"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
            />
          </svg>
        </div>
        <h1>
          {{
            loading
              ? t('pay.paying')
              : isPaid
                ? t('pay.wait.paid')
                : isTimedOut
                  ? t('pay.wait.failed')
                  : isClosed
                    ? t('pay.wait.closed')
                    : t('pay.wait.pending')
          }}
        </h1>
        <p v-if="order" class="amount">
          <span class="cny">{{ currencySymbol }}</span>{{ money(order.amountYuan) }}
        </p>
        <p class="note">
          {{
            isPaid
              ? t('pay.wait.paidNote')
              : isTimedOut
                ? t('pay.wait.failedNote')
                : isClosed
                  ? t('pay.wait.closedNote')
                  : t('pay.wait.pendingNote')
          }}
        </p>
      </section>

      <section v-if="order" class="card meta-card">
        <p>
          <span>{{ t('pay.wait.payNo') }}</span>
          <strong>{{ order.payNo }}</strong>
        </p>
        <p>
          <span>{{ t('field.plate') }}</span>
          <strong>{{ order.plateNumber }}</strong>
        </p>
        <p>
          <span>{{ t('pay.choose') }}</span>
          <strong>{{ methodLabel(order.method) }}</strong>
        </p>
      </section>

      <section v-if="order?.items?.length" class="card items-card">
        <h2>{{ t('pay.wait.items') }}</h2>
        <ul class="pay-items">
          <li v-for="item in order.items" :key="item.sessionId" class="pay-item">
            <div class="pay-item-main">
              <div class="pay-item-line">
                <span class="lot-name">{{ item.lotName || t('list.unknown') }}</span>
                <span class="tag">{{
                  item.sessionStatus === 'OPEN' ? t('pay.wait.sessionOpen') : t('pay.wait.sessionClosed')
                }}</span>
              </div>
              <p v-if="item.entryTime" class="pay-item-meta">
                {{ t('pay.wait.entry') }} {{ fmtDateTime(item.entryTime) }}
              </p>
            </div>
            <div class="item-amount">
              <span class="cny">{{ currencySymbol }}</span>{{ money(item.amountYuan) }}
            </div>
          </li>
        </ul>
      </section>

      <p v-if="error" class="form-error" role="alert">{{ error }}</p>

      <template v-if="showMockConfirm">
        <p class="mock-note">{{ t('pay.mockNote') }}</p>
        <button class="btn btn-primary" type="button" :disabled="confirming" @click="confirmMock">
          {{ confirming ? t('pay.paying') : t('pay.mockConfirm') }}
        </button>
      </template>

      <WechatLaunchGuide v-if="showWechatGuide" />

      <AlipayLaunchGuide
        v-if="showAlipayGuide && handoffPageUrl"
        :page-url="handoffPageUrl"
      />

      <button
        v-if="isPaid || isClosed || isTimedOut || (isPending && order?.method === 'ALIPAY_PAY' && order.mock)"
        class="btn btn-primary"
        type="button"
        @click="goHome"
      >
        {{ t('pay.wait.back') }}
      </button>
      <button v-else-if="error && !order" class="btn btn-primary" type="button" @click="goHome">
        {{ t('pay.wait.retry') }}
      </button>
    </main>
  </div>
</template>

<style scoped>
.app {
  position: relative;
  min-height: 100svh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, var(--fp-bg-top) 0%, var(--fp-bg-bottom) 100%);
  overflow-x: hidden;
}
.bg-blobs {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}
.blob {
  position: absolute;
  display: block;
  border-radius: 50%;
  filter: blur(6px);
}
.blob-a {
  width: 240px;
  height: 240px;
  top: -80px;
  right: -60px;
  background: radial-gradient(circle at 30% 30%, var(--fp-pink), transparent 68%);
  opacity: 0.5;
}
.blob-b {
  width: 200px;
  height: 200px;
  bottom: -50px;
  left: -70px;
  background: radial-gradient(circle at 30% 30%, var(--fp-sky), transparent 68%);
  opacity: 0.45;
}
.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 12px;
  background: color-mix(in srgb, var(--fp-bg-top) 78%, transparent);
  backdrop-filter: blur(12px);
}
.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: var(--fp-icon-bg);
  color: var(--fp-ink);
  cursor: pointer;
}
.brand {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  font-size: 17px;
  font-weight: 800;
  background: var(--fp-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.brand em {
  font-style: normal;
  color: var(--fp-muted);
  font-weight: 500;
  font-size: 13px;
  margin-left: 6px;
  -webkit-text-fill-color: var(--fp-muted);
}
.lang-btn {
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--fp-line);
  background: var(--fp-icon-bg);
  color: var(--fp-accent-deep);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}
.page {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  box-sizing: border-box;
  padding: 12px 16px calc(24px + env(safe-area-inset-bottom));
}
.card {
  background: var(--fp-card);
  border: 1px solid var(--fp-line);
  border-radius: 20px;
  padding: 18px;
  box-shadow: var(--fp-shadow-card);
}
.status-card {
  text-align: center;
  color: #fff;
  border: none;
  padding: 28px 18px 24px;
}
.is-pending {
  background: var(--fp-gradient-deep);
  box-shadow: 0 16px 34px var(--fp-glow-deep);
}
.is-paid {
  background: linear-gradient(135deg, #1dbf73, #0d9f8a);
  box-shadow: 0 16px 34px rgba(29, 191, 115, 0.35);
}
.is-closed {
  background: var(--fp-gradient-accent);
  box-shadow: 0 16px 34px var(--fp-glow-accent);
}
.status-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  margin-bottom: 12px;
}
.status-card h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
}
.amount {
  margin: 10px 0 0;
  font-size: 40px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.cny {
  font-size: 20px;
  margin-right: 3px;
}
.note {
  margin: 10px auto 0;
  max-width: 280px;
  font-size: 13px;
  line-height: 1.7;
  opacity: 0.92;
}
.meta-card {
  margin-top: 14px;
  text-align: left;
}
.meta-card p {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  padding: 10px 0;
  border-bottom: 1px dashed var(--fp-line);
  font-size: 14px;
  color: var(--fp-muted);
}
.meta-card p:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.meta-card strong {
  color: var(--fp-ink);
  font-weight: 700;
  word-break: break-all;
  text-align: right;
}
.items-card {
  margin-top: 14px;
  text-align: left;
}
.items-card h2 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 800;
  color: var(--fp-ink);
}
.pay-items {
  list-style: none;
  margin: 0;
  padding: 0;
}
.pay-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--fp-line);
}
.pay-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.pay-item-main {
  min-width: 0;
}
.pay-item-line {
  display: flex;
  align-items: center;
  gap: 8px;
}
.lot-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--fp-ink);
}
.tag {
  font-size: 11px;
  font-weight: 700;
  color: var(--fp-muted);
  background: color-mix(in srgb, var(--fp-muted) 12%, transparent);
  border-radius: 999px;
  padding: 1px 8px;
}
.pay-item-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--fp-muted);
}
.item-amount {
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.form-error {
  margin: 14px 0 0;
  color: var(--fp-danger);
  font-size: 13px;
  font-weight: 600;
}
.mock-note {
  margin: 14px 0 10px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--fp-muted);
  text-align: center;
}
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 52px;
  margin-top: 14px;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
}
.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.btn-primary {
  background: var(--fp-gradient);
  color: #fff;
  box-shadow: 0 10px 20px var(--fp-glow);
}
.spinner {
  width: 22px;
  height: 22px;
  border: 2.5px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
