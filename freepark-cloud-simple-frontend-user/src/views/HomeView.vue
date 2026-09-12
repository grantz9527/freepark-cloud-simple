<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchSiteSettings,
  normalizePlate,
  queryPlateFee,
  type PaymentOrder,
  type PlateFeeItem,
  type PlateFeeQuote
} from '../api/client'
import PlateInput from '../components/PlateInput.vue'
import PaySheet from '../components/PaySheet.vue'
import {
  localeButtonText,
  localeFromSite,
  setLocaleIfUnset,
  toggleLocale,
  t,
  tColor,
  tpl,
  tRegion
} from '../i18n'
import {
  PLATE_COLOR_META,
  detectPlateColor,
  plateColorSwatch,
  plateInkColor
} from '../plateColor'
import { regionDefaultTone } from '../plateRegion'
import { detectPayClientEnv } from '../payEnv'

const route = useRoute()
const router = useRouter()

/** URL 携带的车牌（扫码 / 分享链接 / 缴费返回）；无则返回空串 */
function plateFromUrl(): string {
  const raw = route.query.plate
  return normalizePlate(Array.isArray(raw) ? raw[0] ?? '' : (raw ?? ''))
}

/** search：输入查询页；result：结果页 */
const view = ref<'search' | 'result'>(plateFromUrl() ? 'result' : 'search')
const plateInput = ref('')
const loading = ref(false)
const error = ref('')
const quote = ref<PlateFeeQuote | null>(null)
const paySheetOpen = ref(false)
/** 系统配置中开放的缴费方式（WECHAT_PAY / ALIPAY_PAY） */
const paymentMethods = ref<string[]>([])
/** 系统配置：true 强制缴清全部欠费；false 允许勾选指定停车记录 */
const forcePayAll = ref(true)
/** 勾选缴费时选中的停车流水 ID */
const selectedSessionIds = ref<number[]>([])
/** 手动指定车牌颜色；null = 自动（不按颜色过滤） */
const manualColor = ref<string | null>(null)
/** 车牌版式区域（云端 system_settings.plateRegion），拉取前按 CN 渲染 */
const region = ref('CN')
const userBaseUrl = ref('')
const wechatMpAppId = ref('')
const isCn = computed(() => region.value === 'CN')
/** 金额币种（云端 system_settings.defaultCurrency），拉取前按 CNY */
const currency = ref('CNY')
const currencySymbol = computed(() => currencySymbolOf(currency.value))

/** ISO 4217 → 货币符号；未知币种回退显示代码本身 */
function currencySymbolOf(code: string): string {
  const symbol: Record<string, string> = {
    CNY: '¥',
    USD: '$',
    HKD: 'HK$',
    TWD: 'NT$',
    MOP: 'MOP$',
    JPY: '¥',
    KRW: '₩',
    SGD: 'S$',
    MYR: 'RM',
    THB: '฿',
    VND: '₫',
    EUR: '€',
    GBP: '£',
    CHF: 'Fr.',
    AUD: 'A$',
    CAD: 'C$',
    NZD: 'NZ$'
  }
  return symbol[code] ?? code
}

/** 最近查询过的车牌（仅存本机），点击可直接复查，无需重新输入 */
interface RecentPlate {
  plate: string
  /** 当时选择的颜色；null = 自动 */
  color: string | null
}
const RECENT_STORAGE_KEY = 'freepark.user.recent-plates.v1'
const RECENT_MAX = 3
const recentPlates = ref<RecentPlate[]>([])

function loadRecents() {
  try {
    const raw = localStorage.getItem(RECENT_STORAGE_KEY)
    if (!raw) return
    const list = JSON.parse(raw)
    if (Array.isArray(list)) {
      recentPlates.value = list
        .filter((r): r is RecentPlate => !!r && typeof r.plate === 'string')
        .slice(0, RECENT_MAX)
    }
  } catch {
    // 存储损坏则忽略，从空列表开始
  }
}

function persistRecents() {
  try {
    localStorage.setItem(RECENT_STORAGE_KEY, JSON.stringify(recentPlates.value))
  } catch {
    // 隐私模式等写失败可忽略
  }
}

function addRecent(plate: string, color: string | null) {
  const p = normalizePlate(plate)
  if (!p) return
  recentPlates.value = [
    { plate: p, color: color ?? null },
    ...recentPlates.value.filter((r) => r.plate !== p)
  ].slice(0, RECENT_MAX)
  persistRecents()
}

function removeRecent(plate: string) {
  recentPlates.value = recentPlates.value.filter((r) => r.plate !== plate)
  persistRecents()
}

function clearRecents() {
  recentPlates.value = []
  persistRecents()
}

/** 点击最近车牌：按记录颜色直接查询 */
async function useRecent(r: RecentPlate) {
  if (loading.value) return
  plateInput.value = r.plate
  manualColor.value = r.color
  await runQuery(r.plate, r.color)
}

const ongoingItems = computed<PlateFeeItem[]>(() =>
  quote.value?.items.filter((i) => i.type === 'ONGOING') ?? []
)
const settledItems = computed<PlateFeeItem[]>(() =>
  quote.value?.items.filter((i) => i.type === 'SETTLED') ?? []
)
const hasDue = computed<boolean>(() => (quote.value?.totalAmount ?? 0) > 0)
const payableItems = computed<PlateFeeItem[]>(() =>
  (quote.value?.items ?? []).filter((item) => item.amount > 0 && Number(item.sessionId) > 0)
)
const canSelectPay = computed<boolean>(() => !forcePayAll.value && payableItems.value.length > 0)
const selectedAmount = computed<number>(() => {
  const picked = new Set(selectedSessionIds.value)
  return payableItems.value
    .filter((item) => picked.has(item.sessionId))
    .reduce((sum, item) => sum + item.amount, 0)
})
const payAmount = computed<number>(() =>
  forcePayAll.value ? (quote.value?.totalAmount ?? 0) : selectedAmount.value
)
const payItems = computed<PlateFeeItem[]>(() => {
  if (forcePayAll.value) return payableItems.value
  const picked = new Set(selectedSessionIds.value)
  return payableItems.value.filter((item) => picked.has(item.sessionId))
})
const allPayableSelected = computed<boolean>(
  () =>
    payableItems.value.length > 0 &&
    payableItems.value.every((item) => selectedSessionIds.value.includes(item.sessionId))
)
/** 有在场记录但当前免费/未产生费用 */
const ongoingFree = computed<boolean>(
  () =>
    ongoingItems.value.length > 0 &&
    ongoingItems.value.every((i) => i.amount <= 0) &&
    !hasDue.value
)
const noRecords = computed<boolean>(() => !!quote.value && quote.value.items.length === 0)
/** 车牌外观配色：手动选择优先，否则 CN 按号码自动识别、其它区域按当地默认色 */
const effectiveTone = computed<string>(() => {
  if (manualColor.value) return manualColor.value
  return isCn.value ? detectPlateColor(plateInput.value) : regionDefaultTone(region.value)
})
const autoDetected = computed<string>(() => detectPlateColor(plateInput.value))
/** 结果页顶部车牌徽章底色：按当前生效的“查询颜色”展示 */
const badgeTone = computed<string>(() => {
  if (manualColor.value) return manualColor.value
  return isCn.value ? detectPlateColor(plateInput.value) : regionDefaultTone(region.value)
})
/** 该车牌在云端实际存在的颜色（用于结果页切换筛选） */
const quoteColors = computed<string[]>(() => quote.value?.colors ?? [])
/** 其它颜色提示文本（选了某颜色但无结果） */
const otherColorsText = computed<string>(() =>
  quoteColors.value
    .filter((c) => c !== manualColor.value)
    .map((c) => tColor(c))
    .join(' / ')
)
/** Hero 副标题：国内/地区版式双语文案 */
const heroSub = computed<string>(() =>
  isCn.value
    ? t('hero.sub.cn')
    : tpl('hero.sub.region', { region: tRegion(region.value) })
)

function validatePlate(): string {
  const plate = normalizePlate(plateInput.value)
  if (!plate) return t('err.needPlate')
  if (isCn.value) {
    if (!/^[\u4e00-\u9fa5A-Z0-9]{5,10}$/.test(plate)) return t('err.format')
  } else if (!/^[A-Z0-9]{2,12}$/.test(plate)) {
    return t('err.chars')
  }
  return ''
}

/** 拉取云端站点配置：车牌版式区域 + 金额币种 + 初始语言（仅当用户未手动选过） */
async function applySiteSettings() {
  try {
    const settings = await fetchSiteSettings()
    region.value = settings.plateRegion || 'CN'
    wechatMpAppId.value = settings.wechatMpAppId || ''
    userBaseUrl.value = settings.userBaseUrl || ''
    currency.value = settings.defaultCurrency || 'CNY'
    paymentMethods.value = settings.allowedPaymentMethods ?? []
    forcePayAll.value = settings.forcePayAll !== false
    setLocaleIfUnset(localeFromSite(settings.defaultLocale))
  } catch {
    // 拉取失败保持 CN/CNY 兜底，不影响查询主流程
  }
}

function selectColor(code: string | null) {
  manualColor.value = code
}

async function runQuery(plate: string, color: string | null = manualColor.value) {
  error.value = ''
  loading.value = true
  try {
    quote.value = await queryPlateFee(plate, color ?? undefined)
    plateInput.value = plate
    addRecent(plate, color)
    syncSelection(sessionIdsFromQuery())
    view.value = 'result'
  } catch (e) {
    error.value = e instanceof Error ? e.message : t('err.failed')
  } finally {
    loading.value = false
  }
}

async function handleQuery() {
  if (loading.value) return
  const invalid = validatePlate()
  if (invalid) {
    error.value = invalid
    return
  }
  await runQuery(normalizePlate(plateInput.value))
}

/** 结果页按颜色切换重查 */
async function switchColor(code: string | null) {
  if (loading.value || !quote.value) return
  selectColor(code)
  await runQuery(quote.value.plateNumber, code)
}

function goBackToSearch() {
  view.value = 'search'
  quote.value = null
  selectedSessionIds.value = []
  error.value = ''
  paySheetOpen.value = false
}

/** 查询车牌自动带入并查询：用于现场扫码 / 分享链接（?plate=粤B12345[&plateColor=GREEN]） */
async function autoQueryFromUrl() {
  const plateRaw = route.query.plate
  const plate = normalizePlate(Array.isArray(plateRaw) ? plateRaw[0] ?? '' : (plateRaw ?? ''))
  if (!plate) return
  plateInput.value = plate
  const invalid = validatePlate()
  if (invalid) {
    // 车牌格式不合法：退回查询页并提示，避免停在结果页占位
    error.value = invalid
    view.value = 'search'
    return
  }
  const colorRaw = route.query.plateColor
  const color = Array.isArray(colorRaw) ? colorRaw[0] ?? '' : (colorRaw ?? '')
  const known = PLATE_COLOR_META.find((m) => m.code === color)
  if (known) manualColor.value = known.code
  await runQuery(plate, known ? known.code : null)
  maybeAutoOpenPay()
}

function shouldAutoOpenPay(): boolean {
  const env = detectPayClientEnv()
  if (env !== 'wechat' && env !== 'alipay') return false
  const flag = String(Array.isArray(route.query.openPay) ? route.query.openPay[0] : route.query.openPay ?? '')
  if (flag === '1' || flag === 'wechat' || flag === 'alipay') return true
  try {
    // 微信静默授权回来后：URL 可能仍带车牌，用已保存的 code 自动打开并继续支付
    return env === 'wechat' && !!sessionStorage.getItem('fp-wx-oauth-code')
  } catch {
    return false
  }
}

function maybeAutoOpenPay() {
  if (!shouldAutoOpenPay()) return
  if ((payAmount.value ?? 0) > 0) gotoPay()
}

async function consumeWeChatOAuthQuery() {
  const code = route.query.code
  if (typeof code !== 'string' || !code) return
  sessionStorage.setItem('fp-wx-oauth-code', code)
  const query: Record<string, string> = {}
  for (const [key, value] of Object.entries(route.query)) {
    if (key === 'code' || key === 'state') continue
    if (typeof value === 'string' && value) query[key] = value
  }
  await router.replace({ name: 'home', query })
}

function money(value: number): string {
  return value.toFixed(2)
}

function gotoPay() {
  if (payAmount.value <= 0) {
    error.value = t('pay.needSelect')
    return
  }
  paySheetOpen.value = true
}

function sessionIdsFromQuery(): number[] {
  const raw = route.query.sessions
  const text = Array.isArray(raw) ? String(raw[0] ?? '') : String(raw ?? '')
  if (!text) return []
  return text
    .split(/[,\s]+/)
    .map((part) => Number(part))
    .filter((id) => Number.isFinite(id) && id > 0)
}

function syncSelection(preferred?: number[]) {
  const ids = payableItems.value.map((item) => item.sessionId)
  if (forcePayAll.value) {
    selectedSessionIds.value = ids
    return
  }
  if (preferred && preferred.length) {
    const allow = new Set(ids)
    const keep = preferred.filter((id) => allow.has(id))
    selectedSessionIds.value = keep.length ? keep : ids
    return
  }
  selectedSessionIds.value = ids
}

function isItemSelected(item: PlateFeeItem): boolean {
  return selectedSessionIds.value.includes(item.sessionId)
}

function toggleItem(item: PlateFeeItem) {
  if (!canSelectPay.value || item.amount <= 0 || !item.sessionId) return
  if (isItemSelected(item)) {
    selectedSessionIds.value = selectedSessionIds.value.filter((id) => id !== item.sessionId)
  } else {
    selectedSessionIds.value = [...selectedSessionIds.value, item.sessionId]
  }
}

function toggleSelectAll() {
  if (!canSelectPay.value) return
  selectedSessionIds.value = allPayableSelected.value
    ? []
    : payableItems.value.map((item) => item.sessionId)
}

function onPayCreated(order: PaymentOrder) {
  paySheetOpen.value = false
  router.push({
    name: 'pay-status',
    params: { payNo: order.payNo },
    query: {
      plate: order.plateNumber,
      ...(order.plateColor ? { plateColor: order.plateColor } : {})
    }
  })
}

onMounted(async () => {
  loadRecents()
  await consumeWeChatOAuthQuery()
  // 先取站点配置（区域/默认颜色），再处理 URL 直达查询
  await applySiteSettings()
  await autoQueryFromUrl()
})
</script>

<template>
  <div class="app">
    <div class="bg-blobs" aria-hidden="true">
      <i class="blob blob-a" />
      <i class="blob blob-b" />
      <i class="blob blob-c" />
    </div>

    <!-- 顶部导航 -->
    <header class="topbar">
      <button
        v-if="view === 'result'"
        class="icon-btn"
        type="button"
        :aria-label="t('act.back')"
        @click="goBackToSearch"
      >
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
      <span v-else class="icon-btn-placeholder" />
      <span class="brand">FreePark<em>{{ t('brand') }}</em></span>
      <button class="lang-btn" type="button" @click="toggleLocale">{{ localeButtonText }}</button>
    </header>

    <!-- 查询页 -->
    <main v-if="view === 'search'" class="page">
      <!-- 渐变 Hero -->
      <section class="hero">
        <svg class="hero-car" viewBox="0 0 64 40" width="78" height="48" aria-hidden="true">
          <path
            d="M8 26l6-10c1.6-2.6 4.4-4 7.4-4h21.2c3 0 5.8 1.4 7.4 4l6 10v10a3 3 0 0 1-3 3h-2a4 4 0 0 1-8 0H23a4 4 0 0 1-8 0h-2a3 3 0 0 1-3-3V26z"
            fill="rgba(255,255,255,.92)"
          />
          <circle cx="20" cy="30" r="4.4" fill="#1d3557" />
          <circle cx="46" cy="30" r="4.4" fill="#1d3557" />
          <path d="M22 22l10-1.4M26 22l1.6-1.4M34 21l10 1.2" stroke="#ffd257" stroke-width="1.8" stroke-linecap="round" fill="none" />
        </svg>
        <span class="hero-pill">{{ t('hero.pill') }}</span>
        <h1>{{ t('hero.title') }}</h1>
        <p>{{ heroSub }}</p>
      </section>

      <!-- 查询卡片（叠在 Hero 上） -->
      <section class="card query-card">
        <label class="field-label" for="plate-input">{{ t('field.plate') }}</label>
        <PlateInput v-model="plateInput" :disabled="loading" :tone="effectiveTone" :region="region" />

        <!-- 最近查询（本机记录，点击直接查询） -->
        <div v-if="recentPlates.length" class="recent">
          <div class="recent-head">
            <span class="recent-title">{{ t('recent.title') }}</span>
            <button class="recent-clear" type="button" @click="clearRecents">{{ t('recent.clear') }}</button>
          </div>
          <div class="recent-list">
            <button
              v-for="r in recentPlates"
              :key="r.plate"
              class="recent-item"
              type="button"
              :disabled="loading"
              :style="{
                background: plateColorSwatch(r.color ?? detectPlateColor(r.plate)),
                color: plateInkColor(r.color ?? detectPlateColor(r.plate))
              }"
              @click="useRecent(r)"
            >
              {{ r.plate }}
              <i class="recent-x" role="button" :aria-label="t('recent.remove')" @click.stop="removeRecent(r.plate)">×</i>
            </button>
          </div>
        </div>

        <!-- 车牌颜色选择 -->
        <div class="color-pick">
          <div class="color-caption">
            <span>{{ t('color.title') }}</span>
            <small v-if="!manualColor && plateInput.length >= 2">
              <template v-if="isCn">{{ tpl('color.autoHint', { label: tColor(autoDetected) }) }}</template>
              <template v-else>{{ tpl('color.regionHint', { label: tColor(effectiveTone) }) }}</template>
            </small>
          </div>
          <div class="chip-scroll">
            <button
              class="chip"
              :class="{ 'chip-on': !manualColor }"
              type="button"
              @click="selectColor(null)"
            >
              {{ t('color.auto') }}
            </button>
            <button
              v-for="m in PLATE_COLOR_META"
              :key="m.code"
              class="chip"
              :class="{ 'chip-on': manualColor === m.code }"
              type="button"
              @click="selectColor(m.code)"
            >
              <i class="dot" :style="{ background: plateColorSwatch(m.code) }" />
              {{ tColor(m.code) }}
            </button>
          </div>
          <p class="color-hint">{{ t('color.hint') }}</p>
        </div>

        <p v-if="error" class="form-error" role="alert">{{ error }}</p>
        <button class="btn btn-primary" type="button" :disabled="loading" @click="handleQuery">
          <span v-if="loading" class="spinner" aria-hidden="true" />
          {{ loading ? t('btn.querying') : t('btn.query') }}
        </button>
      </section>

      <div class="trust">
        <span><b>{{ t('trust.noLogin') }}</b>{{ t('trust.noLoginSub') }}</span>
        <span><b>{{ t('trust.all') }}</b>{{ t('trust.allSub') }}</span>
        <span><b>{{ t('trust.live') }}</b>{{ t('trust.liveSub') }}</span>
      </div>
    </main>

    <!-- 结果页 -->
    <main v-else class="page result-page">
      <!-- 带车牌直达（扫码 / 缴费返回）时先占位，避免闪回查询页 -->
      <section v-if="!quote" class="card state-card">
        <template v-if="error">
          <p class="state-text">{{ error }}</p>
          <button class="btn btn-primary btn-full" type="button" @click="goBackToSearch">
            {{ t('btn.other') }}
          </button>
        </template>
        <template v-else>
          <span class="spinner spinner-ink" aria-hidden="true" />
          <p class="state-text">{{ t('btn.querying') }}</p>
        </template>
      </section>
      <template v-else>
        <!-- 金额汇总卡 -->
        <section
          class="card summary"
          :class="hasDue ? 'summary-due' : ongoingFree ? 'summary-free' : 'summary-clear'"
        >
          <div class="summary-top">
            <span
              class="plate-badge"
              :style="{ background: plateColorSwatch(badgeTone) }"
            >{{ quote.plateNumber }}</span>
            <span class="state-chip">
              {{ hasDue ? t('state.due') : ongoingFree ? t('state.free') : noRecords ? t('state.none') : t('state.clear') }}
            </span>
          </div>

          <template v-if="hasDue">
            <p class="summary-label">{{ t('sum.due') }}</p>
            <p class="summary-amount"><span class="cny">{{ currencySymbol }}</span>{{ money(quote.totalAmount) }}</p>
            <p class="summary-note">{{ canSelectPay ? t('sum.selectNote') : t('sum.dueNote') }}</p>
          </template>
          <template v-else-if="ongoingFree">
            <p class="summary-label">{{ t('sum.free') }}</p>
            <p class="summary-amount"><span class="cny">{{ currencySymbol }}</span>0.00</p>
            <p class="summary-note">{{ t('sum.freeNote') }}</p>
          </template>
          <template v-else>
            <p class="summary-label">{{ t('sum.due') }}</p>
            <p class="summary-amount"><span class="cny">{{ currencySymbol }}</span>0.00</p>
            <p v-if="noRecords" class="summary-note">{{ t('sum.noRecords') }}</p>
            <p v-else class="summary-note">{{ t('sum.clearNote') }}</p>
          </template>
        </section>

        <!-- 颜色切换（同一车牌多颜色） -->
        <section
          v-if="quoteColors.length > 0 || manualColor"
          class="card color-bar"
        >
          <p class="color-bar-title">
            <template v-if="!manualColor && quoteColors.length > 1">
              {{ t('color.multi') }}
            </template>
            <template v-else-if="manualColor">
              {{ tpl('color.querying', { label: tColor(manualColor) }) }}
              <span v-if="noRecords && otherColorsText">{{ tpl('color.noRecords', { list: otherColorsText }) }}</span>
              <span v-else>{{ tpl('color.count', { n: quote.items.length }) }}</span>
            </template>
            <template v-else>{{ t('color.title') }}</template>
          </p>
          <div class="chip-scroll">
            <button
              class="chip"
              :class="{ 'chip-on': !manualColor }"
              type="button"
              :disabled="loading"
              @click="switchColor(null)"
            >
              {{ t('color.all') }}
            </button>
            <button
              v-for="c in quoteColors"
              :key="c"
              class="chip"
              :class="{ 'chip-on': manualColor === c }"
              type="button"
              :disabled="loading"
              @click="switchColor(c)"
            >
              <i class="dot" :style="{ background: plateColorSwatch(c) }" />
              {{ tColor(c) }}
            </button>
          </div>
        </section>

        <!-- 明细 -->
        <section v-if="ongoingItems.length" class="card list-card">
          <h2 class="list-title">
            {{ t('list.ongoing') }} <span class="list-count">{{ ongoingItems.length }}</span>
            <button
              v-if="canSelectPay"
              class="select-all"
              type="button"
              @click="toggleSelectAll"
            >
              {{ allPayableSelected ? t('list.unselectAll') : t('list.selectAll') }}
            </button>
          </h2>
          <ul class="list">
            <li
              v-for="item in ongoingItems"
              :key="'o' + item.sessionId"
              class="list-item"
              :class="{ selectable: canSelectPay && item.amount > 0, selected: canSelectPay && isItemSelected(item) }"
              @click="toggleItem(item)"
            >
              <span
                v-if="canSelectPay && item.amount > 0"
                class="pick"
                :class="{ on: isItemSelected(item) }"
                aria-hidden="true"
              />
              <div class="item-main">
                <div class="item-line1">
                  <span class="lot-name">{{ item.lotName || t('list.unknown') }}</span>
                  <span class="tag tag-ongoing">
                    <i class="dot dot-sm" :style="{ background: plateColorSwatch(item.plateColor) }" v-if="item.plateColor" />
                    {{ t('tag.ongoing') }}
                  </span>
                </div>
                <p class="item-meta">{{ tpl('meta.ongoing', { t: item.entryText, d: item.durationText || '—' }) }}</p>
              </div>
              <div class="item-amount" :class="{ 'amount-free': item.amount <= 0 }">
                <template v-if="item.amount > 0">
                  <span class="cny">{{ currencySymbol }}</span>{{ money(item.amount) }}
                  <span class="item-sub">{{ t('list.est') }}</span>
                </template>
                <span v-else class="free-text">{{ t('list.free') }}</span>
              </div>
            </li>
          </ul>
        </section>

        <section v-if="settledItems.length" class="card list-card">
          <h2 class="list-title">
            {{ t('list.settled') }} <span class="list-count">{{ settledItems.length }}</span>
            <button
              v-if="canSelectPay && !ongoingItems.length"
              class="select-all"
              type="button"
              @click="toggleSelectAll"
            >
              {{ allPayableSelected ? t('list.unselectAll') : t('list.selectAll') }}
            </button>
          </h2>
          <ul class="list">
            <li
              v-for="item in settledItems"
              :key="'s' + item.sessionId"
              class="list-item"
              :class="{ selectable: canSelectPay && item.amount > 0, selected: canSelectPay && isItemSelected(item) }"
              @click="toggleItem(item)"
            >
              <span
                v-if="canSelectPay && item.amount > 0"
                class="pick"
                :class="{ on: isItemSelected(item) }"
                aria-hidden="true"
              />
              <div class="item-main">
                <div class="item-line1">
                  <span class="lot-name">{{ item.lotName || t('list.unknown') }}</span>
                  <span class="tag tag-unpaid">
                    <i class="dot dot-sm" :style="{ background: plateColorSwatch(item.plateColor) }" v-if="item.plateColor" />
                    {{ t('tag.unpaid') }}
                  </span>
                </div>
                <p class="item-meta">
                  {{ tpl('meta.period', { a: item.entryText, b: item.exitText || '—', d: item.durationText || '—' }) }}
                </p>
              </div>
              <div class="item-amount">
                <span class="cny">{{ currencySymbol }}</span>{{ money(item.amount) }}
              </div>
            </li>
          </ul>
        </section>
      </template>
    </main>

    <!-- 底部操作条（结果页） -->
    <footer v-if="view === 'result' && quote" class="actionbar">
      <button
        v-if="hasDue"
        class="btn btn-pay"
        type="button"
        :disabled="payAmount <= 0"
        @click="gotoPay"
      >
        {{ t('btn.pay') }} {{ currencySymbol }}{{ money(payAmount) }}
      </button>
      <button v-else class="btn btn-primary btn-full" type="button" @click="goBackToSearch">
        {{ t('btn.other') }}
      </button>
    </footer>

    <!-- 缴费：按系统配置开放的支付方式下单 -->
    <PaySheet
      :open="paySheetOpen"
      :amount="payAmount"
      :currency-symbol="currencySymbol"
      :methods="paymentMethods"
      :plate="quote?.plateNumber ?? ''"
      :plate-color="manualColor"
      :items="payItems"
      :wechat-mp-app-id="wechatMpAppId"
      :user-base-url="userBaseUrl"
      :force-pay-all="forcePayAll"
      :session-ids="selectedSessionIds"
      @close="paySheetOpen = false"
      @created="onPayCreated"
    />
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

/* ===== 背景装饰 ===== */
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
  width: 260px;
  height: 260px;
  top: -90px;
  right: -70px;
  background: radial-gradient(circle at 30% 30%, var(--fp-pink), transparent 68%);
  opacity: 0.55;
}
.blob-b {
  width: 220px;
  height: 220px;
  top: 90px;
  left: -100px;
  background: radial-gradient(circle at 30% 30%, var(--fp-sky), transparent 68%);
  opacity: 0.5;
}
.blob-c {
  width: 190px;
  height: 190px;
  bottom: -60px;
  right: -40px;
  background: radial-gradient(circle at 30% 30%, var(--fp-mint), transparent 68%);
  opacity: 0.6;
}

/* ===== 顶部导航 ===== */
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
  -webkit-backdrop-filter: blur(12px);
}
.icon-btn,
.icon-btn-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 50%;
}
.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: var(--fp-icon-bg);
  color: var(--fp-ink);
  cursor: pointer;
  box-shadow: var(--fp-shadow-soft);
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
  letter-spacing: 0.2px;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--fp-line);
  background: var(--fp-icon-bg);
  color: var(--fp-accent-deep);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.5px;
  cursor: pointer;
  box-shadow: var(--fp-shadow-soft);
  transition: transform 0.15s;
}
.lang-btn:active {
  transform: scale(0.94);
}

/* ===== 页面骨架 ===== */
.page {
  position: relative;
  z-index: 1;
  flex: 1;
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
  box-sizing: border-box;
  padding: 4px 16px calc(20px + env(safe-area-inset-bottom));
}

/* ===== 查询页 ===== */
.hero {
  position: relative;
  margin: 8px 0 0;
  padding: 26px 22px 74px;
  border-radius: 24px;
  color: #fff;
  background: var(--fp-gradient-deep);
  box-shadow: 0 14px 30px var(--fp-glow-deep);
  overflow: hidden;
}
.hero::after {
  content: '';
  position: absolute;
  right: -40px;
  top: -40px;
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}
.hero-car {
  position: absolute;
  right: 18px;
  bottom: 18px;
  opacity: 0.98;
}
.hero-pill {
  display: inline-flex;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.5px;
}
.hero h1 {
  margin: 12px 0 0;
  font-size: 28px;
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: 1px;
}
.hero p {
  margin: 6px 0 0;
  font-size: 13px;
  opacity: 0.92;
}

.card {
  background: var(--fp-card);
  border: 1px solid var(--fp-line);
  border-radius: 20px;
  padding: 18px;
  box-shadow: var(--fp-shadow-card);
}
.card + .card {
  margin-top: 14px;
}

.query-card {
  position: relative;
  margin-top: -52px;
  z-index: 2;
}

.field-label {
  display: block;
  font-size: 13px;
  color: var(--fp-muted);
  margin-bottom: 10px;
  font-weight: 600;
}

/* 颜色选择 */
.color-pick {
  margin-top: 14px;
}
.color-caption {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.color-caption span {
  font-size: 12px;
  font-weight: 700;
  color: var(--fp-ink-soft);
}
.color-caption small {
  font-size: 11px;
  color: var(--fp-accent);
  font-weight: 600;
}
.chip-scroll {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 2px 2px 6px;
}
.chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  flex-shrink: 0;
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1.5px solid var(--fp-line);
  background: var(--fp-chip);
  color: var(--fp-ink-soft);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.chip:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.chip-on {
  background: var(--fp-gradient);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 6px 14px var(--fp-glow);
}
.chip-on:disabled {
  opacity: 1;
  cursor: default;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.12);
  flex-shrink: 0;
}
.chip-on .dot {
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.35);
}
.color-hint {
  margin: 6px 0 0;
  font-size: 11px;
  color: var(--fp-muted);
}

/* ===== 最近查询 ===== */
.recent {
  margin: 12px 0 2px;
  padding: 10px 12px 12px;
  border-radius: 14px;
  background: var(--fp-soft);
}
.recent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.recent-title {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.4px;
  color: var(--fp-ink-soft);
}
.recent-clear {
  border: 0;
  background: none;
  padding: 4px;
  font-size: 11px;
  font-weight: 600;
  color: var(--fp-muted);
  cursor: pointer;
}
.recent-clear:active {
  color: var(--fp-accent);
}
.recent-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.recent-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 34px;
  padding: 0 6px 0 12px;
  border: 0;
  border-radius: 9px;
  font-family: 'PingFang SC', 'Microsoft YaHei', monospace;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 1px;
  cursor: pointer;
  box-shadow: 0 3px 8px rgba(24, 36, 66, 0.16);
  transition: transform 0.15s;
}
.recent-item:active {
  transform: scale(0.95);
}
.recent-item:disabled {
  opacity: 0.55;
  cursor: default;
}
.recent-x {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  font-size: 13px;
  font-style: normal;
  line-height: 1;
  background: rgba(255, 255, 255, 0.34);
  opacity: 0.85;
}
.recent-x:active {
  opacity: 1;
}

.form-error {
  margin: 12px 0 0;
  color: var(--fp-danger);
  font-size: 13px;
  font-weight: 600;
}

/* ===== 按钮 ===== */
.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 52px;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  transition: transform 0.1s, opacity 0.2s, box-shadow 0.2s;
}
.btn:active {
  transform: scale(0.985);
}
.btn-primary {
  margin-top: 16px;
  background: var(--fp-gradient);
  color: #fff;
  box-shadow: 0 10px 20px var(--fp-glow);
}
.btn-primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.btn-pay {
  background: var(--fp-gradient-accent);
  color: #fff;
  box-shadow: 0 10px 22px var(--fp-glow-accent);
}
.btn-pay:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  box-shadow: none;
}
.btn-full {
  margin-top: 0;
}
.btn-ghost {
  background: var(--fp-gradient-soft);
  color: var(--fp-accent-deep);
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

.trust {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin: 18px 4px 0;
  font-size: 11px;
  color: var(--fp-muted);
}
.trust span {
  padding: 7px 10px;
  border-radius: 10px;
  background: var(--fp-chip);
}
.trust b {
  color: var(--fp-accent-deep);
  font-weight: 800;
  margin-right: 2px;
}

/* ===== 结果页 ===== */
.result-page {
  padding-top: 6px;
}

/* 直达查询（扫码 / 缴费返回）加载或失败时的占位卡 */
.state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  min-height: 180px;
  text-align: center;
}
.state-text {
  margin: 0;
  font-size: 13px;
  color: var(--fp-muted);
}
.spinner-ink {
  border-color: var(--fp-line);
  border-top-color: var(--fp-accent);
}

.summary {
  text-align: center;
  padding: 20px 18px 22px;
  color: #fff;
  border: none;
  box-shadow: none;
}
.summary-due {
  background: var(--fp-gradient-accent);
  box-shadow: 0 16px 34px var(--fp-glow-accent);
}
.summary-free {
  background: linear-gradient(135deg, #ffc93c, #f5a623);
  color: #4a3400;
  box-shadow: 0 16px 34px rgba(245, 166, 35, 0.4);
}
.summary-clear {
  background: var(--fp-gradient);
  box-shadow: 0 16px 34px var(--fp-glow-deep);
}
.summary-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
}
.plate-badge {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 2px;
  color: #fff;
  border-radius: 9px;
  padding: 6px 13px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.18);
}
.state-chip {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 999px;
  font-weight: 700;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.24);
  color: currentColor;
}
.summary-label {
  margin: 0 0 4px;
  font-size: 13px;
  opacity: 0.85;
}
.summary-amount {
  margin: 0;
  font-size: 52px;
  font-weight: 800;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
}
.cny {
  font-size: 24px;
  font-weight: 700;
  margin-right: 3px;
}
.summary-note {
  margin: 12px auto 0;
  max-width: 320px;
  font-size: 12px;
  line-height: 1.7;
  opacity: 0.9;
}

/* 颜色切换条 */
.color-bar {
  padding: 14px 16px;
}
.color-bar-title {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--fp-muted);
  line-height: 1.6;
}
.color-bar-title span {
  color: var(--fp-accent-deep);
  font-weight: 600;
}

/* 明细 */
.list-card {
  padding: 16px 18px 8px;
}
.list-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 800;
  color: var(--fp-ink);
}
.list-title::before {
  content: '';
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: var(--fp-gradient);
}
.list-count {
  font-size: 11px;
  color: var(--fp-muted);
  background: var(--fp-chip);
  padding: 1px 8px;
  border-radius: 999px;
}
.select-all {
  margin-left: auto;
  border: 0;
  background: none;
  color: var(--fp-accent-deep);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px dashed var(--fp-line);
}
.list-item:first-child {
  padding-top: 10px;
}
.list-item:last-child {
  border-bottom: none;
  padding-bottom: 8px;
}
.list-item.selectable {
  cursor: pointer;
}
.list-item.selected {
  background: color-mix(in srgb, var(--fp-sky) 22%, transparent);
  margin-inline: -10px;
  padding-inline: 10px;
  border-radius: 12px;
}
.pick {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid #c9d4ce;
  box-sizing: border-box;
}
.pick.on {
  border-color: var(--fp-accent-deep);
  background: var(--fp-accent-deep);
  box-shadow: inset 0 0 0 3px #fff;
}
.item-main {
  min-width: 0;
}
.item-line1 {
  display: flex;
  align-items: center;
  gap: 8px;
}
.lot-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--fp-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tag {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  font-weight: 700;
}
.dot-sm {
  width: 8px;
  height: 8px;
}
.tag-ongoing {
  color: var(--fp-accent-deep);
  background: var(--fp-gradient-soft);
}
.tag-unpaid {
  color: #b45309;
  background: #fef3cd;
}
.item-meta {
  margin: 5px 0 0;
  color: var(--fp-muted);
  font-size: 12px;
  line-height: 1.6;
}
.item-amount {
  flex-shrink: 0;
  color: var(--fp-danger);
  font-size: 17px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}
.item-amount .item-sub {
  display: block;
  font-size: 10px;
  font-weight: 500;
  color: var(--fp-muted);
  text-align: right;
  margin-top: 1px;
}
.amount-free {
  color: var(--fp-accent-deep);
}
.free-text {
  font-size: 14px;
  font-weight: 700;
}

/* ===== 底部操作条 ===== */
.actionbar {
  position: sticky;
  bottom: 0;
  z-index: 30;
  padding: 10px 16px calc(12px + env(safe-area-inset-bottom));
  background: color-mix(in srgb, var(--fp-card) 86%, transparent);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid var(--fp-line);
}

</style>
