<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { launchAlipay } from '../alipayLaunch'
import { invokeAlipayWapPay } from '../alipayPay'
import {
  closePayment,
  createPayment,
  type PaymentOrder,
  type PlateFeeItem
} from '../api/client'
import { t } from '../i18n'
import { detectPayClientEnv, isPayMethodEnabledInEnv } from '../payEnv'
import {
  buildWeChatOAuthUrl,
  canUseWeChatOAuth,
  currentPageUrlForOAuth,
  launchWeChat,
  plateQueryAbsoluteUrl
} from '../wechatLaunch'
import { invokeWeChatJsapiPay, peekWeChatOAuthCode, takeWeChatOAuthCode } from '../wechatPay'
import AlipayLaunchGuide from './AlipayLaunchGuide.vue'
import WechatLaunchGuide from './WechatLaunchGuide.vue'

const props = defineProps<{
  open: boolean
  amount: number
  currencySymbol: string
  methods: string[]
  plate: string
  plateColor: string | null
  items?: PlateFeeItem[]
  wechatMpAppId?: string
  userBaseUrl?: string
  forcePayAll?: boolean
  sessionIds?: number[]
}>()

const emit = defineEmits<{
  close: []
  created: [order: PaymentOrder]
}>()

const selected = ref('')
const submitting = ref(false)
const error = ref('')
const showWechatHandoff = ref(false)
const showAlipayHandoff = ref(false)
const handoffUrl = ref('')
const clientEnv = detectPayClientEnv()

const hasMethods = computed(() => props.methods.length > 0)
const enabledMethods = computed(() =>
  props.methods.filter((code) => isPayMethodEnabledInEnv(code, clientEnv))
)
const envHint = computed(() => {
  if (clientEnv === 'wechat') return t('pay.env.wechat')
  if (clientEnv === 'alipay') return t('pay.env.alipay')
  if (selected.value === 'WECHAT_PAY') return t('pay.env.launchWechat')
  if (selected.value === 'ALIPAY_PAY') return t('pay.env.launchAlipay')
  return ''
})
const items = computed(() => (props.items ?? []).filter((item) => item.amount > 0))

function methodEnabled(code: string): boolean {
  return isPayMethodEnabledInEnv(code, clientEnv)
}

function methodLabel(code: string): string {
  const key = `pay.method.${code}`
  const label = t(key)
  return label === key ? code : label
}

function money(value: number): string {
  return value.toFixed(2)
}

function pickDefaultMethod(methods: string[]): string {
  return methods.find((code) => isPayMethodEnabledInEnv(code, clientEnv)) ?? ''
}

function reset() {
  error.value = ''
  submitting.value = false
  showWechatHandoff.value = false
  showAlipayHandoff.value = false
  handoffUrl.value = ''
  selected.value = pickDefaultMethod(props.methods)
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      reset()
      // 网页授权回调回来后，若已有 code 且默认选中微信支付，自动继续唤起收银台
      if (
        clientEnv === 'wechat' &&
        selected.value === 'WECHAT_PAY' &&
        peekWeChatOAuthCode()
      ) {
        void startPay()
      }
    }
  }
)

watch(
  () => props.methods,
  (methods) => {
    if (!selected.value || !methodEnabled(selected.value) || !methods.includes(selected.value)) {
      selected.value = pickDefaultMethod(methods)
    }
  }
)

function dismiss() {
  if (submitting.value) return
  emit('close')
}

function selectMethod(code: string) {
  if (submitting.value || !methodEnabled(code)) return
  selected.value = code
}

/** 微信内真实支付前先拿网页授权 code；已在微信中则用当前页回调，不再用 localhost/IP 规则拦截。 */
function ensureWeChatOauthCode(): { code?: string; redirecting?: boolean; error?: string } {
  const existing = takeWeChatOAuthCode()
  if (existing) return { code: existing }
  const appId = (props.wechatMpAppId ?? '').trim()
  if (!appId) {
    return { error: t('pay.wechat.needAppId') }
  }
  const redirectUri = currentPageUrlForOAuth(
    props.plate,
    props.plateColor,
    props.userBaseUrl,
    props.forcePayAll === false ? props.sessionIds : undefined
  )
  if (!canUseWeChatOAuth(appId, redirectUri)) {
    return { error: t('pay.wechat.needHttps') }
  }
  window.location.href = buildWeChatOAuthUrl(appId, redirectUri)
  return { redirecting: true }
}

const payButtonLabel = computed(() => {
  if (submitting.value) return t('pay.paying')
  if (selected.value === 'WECHAT_PAY' && clientEnv === 'browser') return t('pay.wechat.openInWechat')
  if (selected.value === 'ALIPAY_PAY' && clientEnv === 'browser') return t('pay.alipay.openInAlipay')
  return t('pay.submit')
})

async function startPay() {
  if (!selected.value || submitting.value || !methodEnabled(selected.value)) return
  error.value = ''
  if (selected.value === 'WECHAT_PAY' && clientEnv === 'browser') {
    const pageUrl = plateQueryAbsoluteUrl(
      props.userBaseUrl,
      props.plate,
      props.plateColor,
      props.forcePayAll === false ? props.sessionIds : undefined
    )
    handoffUrl.value = pageUrl
    showWechatHandoff.value = true
    launchWeChat(props.wechatMpAppId ?? '', pageUrl)
    return
  }
  if (selected.value === 'ALIPAY_PAY' && clientEnv === 'browser') {
    const pageUrl = plateQueryAbsoluteUrl(
      props.userBaseUrl,
      props.plate,
      props.plateColor,
      props.forcePayAll === false ? props.sessionIds : undefined
    )
    handoffUrl.value = pageUrl
    showAlipayHandoff.value = true
    launchAlipay(pageUrl)
    return
  }

  let wxCode: string | undefined
  if (selected.value === 'WECHAT_PAY' && clientEnv === 'wechat') {
    const gate = ensureWeChatOauthCode()
    if (gate.redirecting) return
    if (gate.error) {
      error.value = gate.error
      return
    }
    wxCode = gate.code
  }

  submitting.value = true
  try {
    const order = await createPayment(
      props.plate,
      selected.value,
      props.plateColor,
      wxCode,
      props.forcePayAll === false ? props.sessionIds : undefined
    )
    if (order.method === 'WECHAT_PAY' && !order.mock) {
      if (!order.wxPay) {
        error.value = t('pay.wechat.invokeFailed')
        try {
          await closePayment(order.payNo)
        } catch {
          // ignore
        }
        return
      }
      const result = await invokeWeChatJsapiPay(order.wxPay)
      if (result !== 'ok') {
        try {
          await closePayment(order.payNo)
        } catch {
          // 关闭失败不阻断：下次下单会清理待缴单
        }
        error.value = result === 'cancel' ? t('pay.wechat.cancelled') : t('pay.wechat.invokeFailed')
        return
      }
    }
    if (order.method === 'ALIPAY_PAY' && !order.mock) {
      if (!order.aliPay?.formHtml) {
        error.value = t('pay.alipay.invokeFailed')
        try {
          await closePayment(order.payNo)
        } catch {
          // ignore
        }
        return
      }
      // 提交后浏览器跳转支付宝收银台，成功则不再 emit
      if (!invokeAlipayWapPay(order.aliPay.formHtml)) {
        error.value = t('pay.alipay.invokeFailed')
        try {
          await closePayment(order.payNo)
        } catch {
          // ignore
        }
        return
      }
      return
    }
    emit('created', order)
  } catch (e) {
    error.value = e instanceof Error ? e.message : t('pay.failed')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div v-if="open" class="mask" @click.self="dismiss">
    <section class="sheet" role="dialog" aria-modal="true" :aria-label="t('pay.title')">
      <div class="sheet-grip" />
      <h3>{{ t('pay.title') }}</h3>
      <p class="amount">
        <span class="cny">{{ currencySymbol }}</span>{{ money(amount) }}
      </p>
      <p class="hint">
        {{ plate }} · {{ props.forcePayAll === false ? t('pay.hintSelected') : t('pay.hint') }}
      </p>
      <p v-if="items.length" class="section-label">{{ t('pay.items') }}</p>
      <ul v-if="items.length" class="pay-items">
        <li v-for="(item, idx) in items" :key="idx" class="pay-item">
          <div class="pay-item-main">
            <span class="pay-item-lot">{{ item.lotName || t('list.unknown') }}</span>
            <span class="pay-item-meta">{{ item.entryText }}</span>
          </div>
          <span class="pay-item-amt">
            <span class="cny">{{ currencySymbol }}</span>{{ money(item.amount) }}
          </span>
        </li>
      </ul>
      <p v-if="envHint && !showWechatHandoff && !showAlipayHandoff" class="env-hint">{{ envHint }}</p>

      <template v-if="!hasMethods">
        <p class="empty">{{ t('pay.none') }}</p>
        <button class="btn btn-ghost" type="button" @click="dismiss">{{ t('sheet.ok') }}</button>
      </template>

      <template v-else-if="showWechatHandoff">
        <WechatLaunchGuide />
        <button class="btn btn-ghost" type="button" @click="dismiss">
          {{ t('pay.cancel') }}
        </button>
      </template>

      <template v-else-if="showAlipayHandoff">
        <AlipayLaunchGuide :page-url="handoffUrl" />
        <button class="btn btn-ghost" type="button" @click="dismiss">
          {{ t('pay.cancel') }}
        </button>
      </template>

      <template v-else>
        <p class="section-label">{{ t('pay.choose') }}</p>
        <div class="methods">
          <button
            v-for="code in methods"
            :key="code"
            class="method"
            :class="{
              'method-on': selected === code && methodEnabled(code),
              'method-off': !methodEnabled(code)
            }"
            type="button"
            :disabled="submitting || !methodEnabled(code)"
            @click="selectMethod(code)"
          >
            <span class="method-mark" :class="'mark-' + code.toLowerCase()" aria-hidden="true">
              {{ code === 'ALIPAY_PAY' ? '支' : '微' }}
            </span>
            <span class="method-text">
              <span class="method-name">{{ methodLabel(code) }}</span>
              <span v-if="!methodEnabled(code)" class="method-lock">{{ t('pay.env.locked') }}</span>
            </span>
            <span class="method-check" aria-hidden="true" />
          </button>
        </div>
        <p v-if="enabledMethods.length === 0" class="form-error" role="alert">{{ t('pay.env.noMatch') }}</p>
        <p v-else-if="error" class="form-error" role="alert">{{ error }}</p>
        <button
          class="btn btn-pay"
          type="button"
          :disabled="submitting || !selected || enabledMethods.length === 0"
          @click="startPay"
        >
          <span v-if="submitting" class="spinner" aria-hidden="true" />
          {{ payButtonLabel }}
        </button>
        <button class="btn btn-ghost" type="button" :disabled="submitting" @click="dismiss">
          {{ t('pay.cancel') }}
        </button>
      </template>
    </section>
  </div>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(10, 14, 20, 0.45);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.sheet {
  width: 100%;
  max-width: 500px;
  background: var(--fp-card);
  border-radius: 24px 24px 0 0;
  padding: 10px 22px calc(24px + env(safe-area-inset-bottom));
  box-sizing: border-box;
  text-align: center;
  animation: slide-up 0.22s ease-out;
}
@keyframes slide-up {
  from {
    transform: translateY(40px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
.sheet-grip {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: var(--fp-line);
  margin: 0 auto 16px;
}
.sheet :deep(.wx-guide) {
  margin-top: 10px;
}
.sheet :deep(.ali-guide) {
  margin-top: 8px;
  padding: 8px 0 0;
  border: none;
  box-shadow: none;
  background: transparent;
}
.sheet h3 {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 800;
  color: var(--fp-ink);
}
.amount {
  margin: 0;
  font-size: 40px;
  font-weight: 800;
  color: var(--fp-ink);
  font-variant-numeric: tabular-nums;
  line-height: 1.15;
}
.cny {
  font-size: 20px;
  margin-right: 3px;
}
.hint,
.empty,
.mock-note,
.env-hint {
  margin: 8px auto 0;
  max-width: 320px;
  color: var(--fp-muted);
  font-size: 13px;
  line-height: 1.7;
}
.env-hint {
  color: var(--fp-accent-deep);
  font-weight: 600;
}
.mock-note {
  color: var(--fp-accent-deep);
  font-weight: 600;
}
.section-label {
  margin: 16px 0 10px;
  text-align: left;
  font-size: 13px;
  font-weight: 700;
  color: var(--fp-ink-soft);
}
.pay-items {
  list-style: none;
  margin: 0 0 8px;
  padding: 0;
  max-height: 180px;
  overflow: auto;
  text-align: left;
}
.pay-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--fp-line);
}
.pay-item:last-child {
  border-bottom: none;
}
.pay-item-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.pay-item-lot {
  font-size: 14px;
  font-weight: 700;
  color: var(--fp-ink);
}
.pay-item-meta {
  font-size: 12px;
  color: var(--fp-muted);
}
.pay-item-amt {
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}
.methods {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.method {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-height: 54px;
  padding: 10px 14px;
  border-radius: 14px;
  border: 1.5px solid var(--fp-line);
  background: var(--fp-chip);
  color: var(--fp-ink);
  cursor: pointer;
  text-align: left;
}
.method-on {
  border-color: var(--fp-accent);
  background: var(--fp-gradient-soft);
  box-shadow: 0 0 0 1px var(--fp-accent);
}
.method-off {
  opacity: 0.45;
  cursor: not-allowed;
  filter: grayscale(1);
}
.method-mark {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  flex-shrink: 0;
}
.mark-wechat_pay {
  background: #07c160;
}
.mark-alipay_pay {
  background: #1677ff;
}
.method-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.method-name {
  font-size: 15px;
  font-weight: 700;
}
.method-lock {
  font-size: 11px;
  font-weight: 600;
  color: var(--fp-muted);
}
.method-check {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid var(--fp-line);
  flex-shrink: 0;
}
.method-on .method-check {
  border-color: var(--fp-accent);
  background: radial-gradient(circle, var(--fp-accent) 0 5px, transparent 6px);
}
.method-off .method-check {
  visibility: hidden;
}
.form-error {
  margin: 12px 0 0;
  color: var(--fp-danger);
  font-size: 13px;
  font-weight: 600;
}
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
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
.btn-pay {
  background: var(--fp-gradient-accent);
  color: #fff;
  box-shadow: 0 10px 22px var(--fp-glow-accent);
}
.btn-ghost {
  background: var(--fp-gradient-soft);
  color: var(--fp-accent-deep);
  margin-top: 10px;
}
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.45);
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
