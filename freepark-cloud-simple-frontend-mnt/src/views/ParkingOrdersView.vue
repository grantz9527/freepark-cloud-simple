<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { useDefaultCurrency } from '../utils/currency'

const d: BiDict = {
  lot: { 'zh-CN': '车场', en: 'Lot' },
  allLots: { 'zh-CN': '全部车场', en: 'All lots' },
  keyword: { 'zh-CN': '关键词', en: 'Keyword' },
  keywordPlaceholder: { 'zh-CN': '订单号 / 缴款单号 / 车牌 / 车场', en: 'Order no / pay no / plate / lot' },
  status: { 'zh-CN': '订单状态', en: 'Order status' },
  allStatus: { 'zh-CN': '全部状态', en: 'All status' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  tip: {
    'zh-CN': '每次收费/缴费请求都会为该停车流水生成一笔订单并记录支付金额。订单金额 = 当前应收 − 流水累计已支付 − 待付订单合计；再次缴费只会收取新产生的金额，不会重复计费。已支付订单支持全部退款或部分退款，退款金额会从流水累计已支付中回冲。',
    en: 'Each payment/charge request creates an order for the parking session and records the amount paid. Order amount = current receivable − session paid − pending orders; later payments only collect the newly accrued amount. Paid orders support full or partial refunds, which reverse the credited session paid amount.'
  },
  orderNo: { 'zh-CN': '订单号', en: 'Order No' },
  sessionStatus: { 'zh-CN': '流水状态', en: 'Session' },
  sessionOpen: { 'zh-CN': '在场', en: 'Open' },
  sessionClosed: { 'zh-CN': '已出场', en: 'Closed' },
  plate: { 'zh-CN': '车牌', en: 'Plate' },
  plateColor: { 'zh-CN': '车牌颜色', en: 'Plate color' },
  entryTime: { 'zh-CN': '入场时间', en: 'Entry time' },
  receivable: { 'zh-CN': '应收金额', en: 'Fee due' },
  receivableTip: { 'zh-CN': '下单时的应收口径：已出场取流水应收快照，在场按下单时刻估算', en: 'Receivable at order time: fee snapshot for closed sessions, estimated for open ones' },
  paidBefore: { 'zh-CN': '下单前已付', en: 'Paid before' },
  paidBeforeTip: { 'zh-CN': '下单前该流水累计已支付的金额', en: 'Total already paid for the session before this order' },
  pendingBefore: { 'zh-CN': '下单前待付', en: 'Pending before' },
  pendingBeforeTip: { 'zh-CN': '下单前该流水未支付/待支付订单的金额合计', en: 'Total pending-order amount of the session before this order' },
  amount: { 'zh-CN': '本次应付', en: 'This order' },
  amountTip: { 'zh-CN': '本单应付金额 = 应收 − 已支付 − 待付订单，收款后累加到流水累计已支付', en: 'This order amount = receivable − paid − pending; credited on payment' },
  orderStatus: { 'zh-CN': '订单状态', en: 'Status' },
  orderPending: { 'zh-CN': '待支付', en: 'Pending' },
  orderPaid: { 'zh-CN': '已支付', en: 'Paid' },
  orderPartialRefund: { 'zh-CN': '部分退款', en: 'Partially refunded' },
  orderRefunded: { 'zh-CN': '已退款', en: 'Refunded' },
  orderCancelled: { 'zh-CN': '已取消', en: 'Cancelled' },
  refunded: { 'zh-CN': '已退金额', en: 'Refunded' },
  refundedTip: { 'zh-CN': '本单累计已退金额；剩余可退 = 本次应付 − 已退', en: 'Cumulative refunded amount; remaining = order amount − refunded' },
  payTime: { 'zh-CN': '支付时间', en: 'Paid at' },
  createdAt: { 'zh-CN': '下单时间', en: 'Created at' },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  registerPay: { 'zh-CN': '登记收款', en: 'Register payment' },
  cancel: { 'zh-CN': '取消订单', en: 'Cancel' },
  cancelText: { 'zh-CN': '取消', en: 'Cancel' },
  refund: { 'zh-CN': '退款', en: 'Refund' },
  refundTitle: { 'zh-CN': '订单退款', en: 'Refund order' },
  refundMode: { 'zh-CN': '退款方式', en: 'Refund type' },
  refundFull: { 'zh-CN': '全部退款', en: 'Full refund' },
  refundPartial: { 'zh-CN': '部分退款', en: 'Partial refund' },
  refundAmount: { 'zh-CN': '退款金额', en: 'Refund amount' },
  refundReason: { 'zh-CN': '退款原因', en: 'Reason' },
  refundReasonPlaceholder: { 'zh-CN': '选填，最多 200 字', en: 'Optional, up to 200 characters' },
  remainingRefundable: { 'zh-CN': '剩余可退', en: 'Refundable' },
  refundSubmit: { 'zh-CN': '确认退款', en: 'Confirm refund' },
  refundSuccess: { 'zh-CN': '退款成功', en: 'Refund completed' },
  refundInvalidAmount: { 'zh-CN': '请输入大于 0 且不超过剩余可退的金额', en: 'Enter an amount greater than 0 and within the remaining refundable amount' },
  refundRecords: { 'zh-CN': '退款记录', en: 'Refund records' },
  refundNo: { 'zh-CN': '退款单号', en: 'Refund No' },
  refundType: { 'zh-CN': '类型', en: 'Type' },
  typePartial: { 'zh-CN': '部分退款', en: 'Partial' },
  typeFull: { 'zh-CN': '全部退款', en: 'Full' },
  operator: { 'zh-CN': '操作人', en: 'Operator' },
  refundTime: { 'zh-CN': '退款时间', en: 'Refunded at' },
  remainingAfter: { 'zh-CN': '剩余可退', en: 'Remaining' },
  paymentNo: { 'zh-CN': '缴款单号', en: 'Payment no' },
  paymentNoTip: { 'zh-CN': '线上一次缴清多条流水时共用此缴款单号；管理端人工下单为空', en: 'Shared when one online payment covers several sessions; empty for manual orders' },
  paymentSessions: { 'zh-CN': '缴费流水', en: 'Sessions paid' },
  sessionId: { 'zh-CN': '流水 ID', en: 'Session ID' },
  payConfirmTitle: { 'zh-CN': '登记收款', en: 'Register payment' },
  payConfirmMsg: {
    'zh-CN': '确认车牌 {plate} 的订单 {orderNo}（{amount}）已收款吗？收款后金额将累加到关联停车流水的累计已支付。',
    en: 'Confirm order {orderNo} of plate {plate} ({amount}) is received? The amount will be credited to the session.'
  },
  paySuccess: { 'zh-CN': '收款成功，已入账', en: 'Payment received and credited' },
  cancelConfirmTitle: { 'zh-CN': '取消订单', en: 'Cancel order' },
  cancelConfirmMsg: {
    'zh-CN': '确定取消订单 {orderNo}（{amount}）吗？取消后金额占用将被释放，可再次下单。',
    en: 'Cancel order {orderNo} ({amount})? Its reserved amount will be released.'
  },
  cancelSuccess: { 'zh-CN': '订单已取消', en: 'Order cancelled' },
  startDate: { 'zh-CN': '开始日期', en: 'Start date' },
  endDate: { 'zh-CN': '结束日期', en: 'End date' },
  rangeMaxYear: { 'zh-CN': '查询区间不能超过 1 年', en: 'Query range cannot exceed 1 year' },
  noData: { 'zh-CN': '暂无数据', en: 'No data' },
  loadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load, try again' },
  reqFailed: { 'zh-CN': '操作失败，请重试', en: 'Operation failed, try again' },
  colorBlue: { 'zh-CN': '蓝', en: 'Blue' },
  colorYellow: { 'zh-CN': '黄', en: 'Yellow' },
  colorGreen: { 'zh-CN': '绿', en: 'Green' },
  colorYellowGreen: { 'zh-CN': '黄绿', en: 'Yellow-green' },
  colorBlack: { 'zh-CN': '黑', en: 'Black' },
  colorWhite: { 'zh-CN': '白', en: 'White' },
  colorOther: { 'zh-CN': '其他', en: 'Other' }
}

const { t } = useBiText(d)

interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

interface LotOption {
  id: number
  name: string
}

type OrderStatus = 'PENDING' | 'PAID' | 'PARTIAL_REFUND' | 'REFUNDED' | 'CANCELLED'

interface OrderRow {
  id: number
  orderNo: string
  paymentNo: string | null
  sessionId: number
  sessionStatus: 'OPEN' | 'CLOSED' | 'VOIDED' | null
  lotId: number
  lotName: string
  plateNumber: string
  plateColor: string | null
  entryTime: string | null
  receivableYuan: number | null
  paidBeforeYuan: number | null
  pendingBeforeYuan: number | null
  amountYuan: number | null
  refundedYuan: number | null
  refundableYuan: number | null
  refundReason: string | null
  refundTime: string | null
  status: OrderStatus
  payTime: string | null
  createdAt: string
  updatedAt: string | null
}

const route = useRoute()

const lots = ref<LotOption[]>([])
const rows = ref<OrderRow[]>([])
const total = ref(0)
const loading = ref(false)

interface Filters {
  lotId: number | undefined
  keyword: string
  status: '' | OrderStatus
  sessionId: number | undefined
}

const filters = reactive<Filters>({
  lotId: undefined,
  keyword: '',
  status: '',
  sessionId: undefined
})

const pager = reactive({ page: 1, size: 10 })

/** 站点时区（系统配置，缓存），默认 Asia/Shanghai；用于墙钟日期换算避免浏览器时区偏差。 */
let systemTimezone = 'Asia/Shanghai'

/** 查询时间区间（按入场日期过滤；默认最近 1 个月，最长可查 1 年）。 */
const timeRange = ref<[string, string] | null>(defaultRange())
let lastValidRange: [string, string] = defaultRange()

const statusOptions = computed(() => [
  { value: 'PENDING' as const, label: t('orderPending') },
  { value: 'PAID' as const, label: t('orderPaid') },
  { value: 'PARTIAL_REFUND' as const, label: t('orderPartialRefund') },
  { value: 'REFUNDED' as const, label: t('orderRefunded') },
  { value: 'CANCELLED' as const, label: t('orderCancelled') }
])

const colorOptions = computed(() => [
  { value: 'BLUE', label: t('colorBlue') },
  { value: 'YELLOW', label: t('colorYellow') },
  { value: 'GREEN', label: t('colorGreen') },
  { value: 'YELLOW_GREEN', label: t('colorYellowGreen') },
  { value: 'BLACK', label: t('colorBlack') },
  { value: 'WHITE', label: t('colorWhite') },
  { value: 'OTHER', label: t('colorOther') }
])

function plateColorLabel(value: string | null | undefined): string {
  const found = colorOptions.value.find((item) => item.value === value)
  return found ? found.label : (value ?? '—')
}

/** 车牌底色样式：按车牌颜色渲染仿真实车牌（蓝牌/黄牌/新能源渐变绿等）。 */
const PLATE_STYLES: Record<string, { background: string; color: string; boxShadow: string }> = {
  BLUE: {
    background: 'linear-gradient(135deg, #2b6ae0, #0d3fa8)',
    color: '#ffffff',
    boxShadow: 'inset 0 0 0 1px rgba(255,255,255,0.22)'
  },
  YELLOW: {
    background: 'linear-gradient(135deg, #ffd83d, #f2a900)',
    color: '#332400',
    boxShadow: 'inset 0 0 0 1px rgba(0,0,0,0.15)'
  },
  GREEN: {
    background: 'linear-gradient(160deg, #2fce7d 0%, #0d9a58 55%, #0b7f49 100%)',
    color: '#ffffff',
    boxShadow: 'inset 0 0 0 1px rgba(255,255,255,0.25)'
  },
  YELLOW_GREEN: {
    background: 'linear-gradient(135deg, #b6e24b, #7cb305)',
    color: '#243a00',
    boxShadow: 'inset 0 0 0 1px rgba(0,0,0,0.14)'
  },
  BLACK: {
    background: 'linear-gradient(135deg, #3d4450, #161a20)',
    color: '#ffffff',
    boxShadow: 'inset 0 0 0 1px rgba(255,255,255,0.16)'
  },
  WHITE: {
    background: '#ffffff',
    color: '#1f2937',
    boxShadow: 'inset 0 0 0 1px rgba(0,0,0,0.16)'
  },
  OTHER: {
    background: 'linear-gradient(135deg, #e8edf2, #cbd5e1)',
    color: '#334155',
    boxShadow: 'inset 0 0 0 1px rgba(0,0,0,0.08)'
  }
}

function plateBadgeStyle(color: string | null | undefined): { background: string; color: string; boxShadow: string } {
  return (color && PLATE_STYLES[color]) || PLATE_STYLES.BLUE
}

function orderTagType(value: OrderRow['status']): 'warning' | 'success' | 'info' | 'danger' {
  if (value === 'PENDING') return 'warning'
  if (value === 'PAID') return 'success'
  if (value === 'PARTIAL_REFUND') return 'danger'
  return 'info'
}

function orderLabel(value: OrderRow['status']): string {
  if (value === 'PENDING') return t('orderPending')
  if (value === 'PAID') return t('orderPaid')
  if (value === 'PARTIAL_REFUND') return t('orderPartialRefund')
  if (value === 'REFUNDED') return t('orderRefunded')
  return t('orderCancelled')
}

function sessionTagType(value: OrderRow['sessionStatus']): 'success' | 'info' {
  return value === 'OPEN' ? 'success' : 'info'
}

function sessionLabel(value: OrderRow['sessionStatus']): string {
  if (value === 'OPEN') return t('sessionOpen')
  if (value === 'CLOSED') return t('sessionClosed')
  return '—'
}

/** 站点「收费金额单位」：中文=元…，英文=币种代码，随系统配置联动 */
const currency = useDefaultCurrency()
const moneyUnit = computed(() => currency.view.value.unit)

/** 金额展示：空值显示 —，有值去掉多余小数 0 后拼接单位 */
function moneyText(value: number | null): string {
  if (value == null) {
    return '—'
  }
  const fixed = Number(value).toFixed(2)
  const amount = fixed.replace(/\.?0+$/, '').replace(/\.$/, '') || '0'
  return `${amount} ${moneyUnit.value}`
}

/** 时间展示：去掉 ISO 的 T、统一精确到秒（yyyy-MM-dd HH:mm:ss）。 */
function fmtDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const text = value.includes('T') ? value.replace('T', ' ') : value
  const clean = text.split('.')[0]
  return clean.length === 16 ? `${clean}:00` : clean
}

async function loadSiteTimezone(): Promise<void> {
  try {
    const settings = await request.get<never, { timezone: string }>('/system/settings')
    if (settings?.timezone) {
      systemTimezone = settings.timezone
    }
  } catch {
    // 读取失败时按默认时区处理，不阻塞页面
  }
}

/** 日期区间工具（与停车流水页口径一致）：站点时区下默认最近 1 个自然月，最长跨度 1 年。 */
function pad2(value: number): string {
  return String(value).padStart(2, '0')
}

function parseDate(text: string): Date {
  const [y, m, d] = text.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function siteTodayParts(): { y: number; m: number; d: number } {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: systemTimezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(new Date())
  const map = new Map(parts.map((part) => [part.type, part.value]))
  return { y: Number(map.get('year')), m: Number(map.get('month')), d: Number(map.get('day')) }
}

function lastDayOfMonth(y: number, m: number): number {
  return new Date(Date.UTC(y, m, 0)).getUTCDate()
}

function defaultRange(): [string, string] {
  const { y, m, d } = siteTodayParts()
  const index = y * 12 + (m - 1) - 1
  const py = Math.floor(index / 12)
  const pm = (index % 12) + 1
  return [`${py}-${pad2(pm)}-${pad2(Math.min(d, lastDayOfMonth(py, pm)))}`, `${y}-${pad2(m)}-${pad2(d)}`]
}

function exceedsOneYear(start: string, end: string): boolean {
  const startDate = parseDate(start)
  const endDate = parseDate(end)
  if (startDate.getTime() > endDate.getTime()) {
    return true
  }
  const minStart = new Date(endDate.getFullYear(), endDate.getMonth(), 1)
  minStart.setFullYear(endDate.getFullYear() - 1)
  minStart.setDate(Math.min(endDate.getDate(), lastDayOfMonth(minStart.getFullYear(), minStart.getMonth() + 1)))
  return startDate.getTime() < minStart.getTime()
}

function handleRangeChange(value: [string, string] | null) {
  if (!value || !value[0] || !value[1]) {
    timeRange.value = defaultRange()
    runSearch()
    return
  }
  const [start, end] = value
  if (exceedsOneYear(start, end)) {
    ElMessage.warning(t('rangeMaxYear'))
    timeRange.value = lastValidRange
    return
  }
  lastValidRange = [start, end]
  runSearch()
}

async function loadLots() {
  try {
    lots.value = await request.get<never, LotOption[]>('/lots')
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('loadFailed'))
  }
}

async function loadRows() {
  loading.value = true
  try {
    const range = timeRange.value ?? defaultRange()
    const data = await request.get<never, PageResult<OrderRow>>('/parking-orders', {
      params: {
        sessionId: filters.sessionId,
        lotId: filters.lotId,
        keyword: filters.keyword.trim() || undefined,
        status: filters.status || undefined,
        startDate: range[0],
        endDate: range[1],
        page: pager.page,
        size: pager.size
      }
    })
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('loadFailed'))
  } finally {
    loading.value = false
  }
}

function runSearch() {
  pager.page = 1
  loadRows()
}

function handleReset() {
  filters.lotId = undefined
  filters.keyword = ''
  filters.status = ''
  filters.sessionId = undefined
  timeRange.value = defaultRange()
  lastValidRange = [...timeRange.value] as [string, string]
  runSearch()
}

function handleSizeChange(size: number) {
  pager.size = size
  pager.page = 1
  loadRows()
}

function handlePageChange(page: number) {
  pager.page = page
  loadRows()
}

/** 登记收款（待支付 → 已支付并入账到流水累计已支付）。 */
async function handlePay(row: OrderRow) {
  const message = t('payConfirmMsg')
    .replace('{orderNo}', row.orderNo)
    .replace('{plate}', row.plateNumber)
    .replace('{amount}', moneyText(row.amountYuan))
  try {
    await ElMessageBox.confirm(message, t('payConfirmTitle'), {
      type: 'warning',
      confirmButtonText: t('registerPay'),
      cancelButtonText: t('cancelText')
    })
  } catch {
    return
  }
  try {
    await request.post(`/parking-orders/${row.id}/pay`)
    ElMessage.success(t('paySuccess'))
    loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  }
}

/** 取消待支付订单（释放金额占用）。 */
async function handleCancel(row: OrderRow) {
  const message = t('cancelConfirmMsg')
    .replace('{orderNo}', row.orderNo)
    .replace('{amount}', moneyText(row.amountYuan))
  try {
    await ElMessageBox.confirm(message, t('cancelConfirmTitle'), {
      type: 'warning',
      confirmButtonText: t('cancel'),
      cancelButtonText: t('cancelText')
    })
  } catch {
    return
  }
  try {
    await request.post(`/parking-orders/${row.id}/cancel`)
    ElMessage.success(t('cancelSuccess'))
    loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  }
}

function refundableOf(row: OrderRow): number {
  if (row.refundableYuan != null) {
    return Number(Number(row.refundableYuan).toFixed(2))
  }
  const amount = Number(row.amountYuan ?? 0)
  const refunded = Number(row.refundedYuan ?? 0)
  return Math.max(0, Number((amount - refunded).toFixed(2)))
}

function canRefund(row: OrderRow): boolean {
  return (row.status === 'PAID' || row.status === 'PARTIAL_REFUND') && refundableOf(row) > 0
}

function hasRefundRecords(row: OrderRow): boolean {
  return row.status === 'PARTIAL_REFUND' || row.status === 'REFUNDED' || Number(row.refundedYuan ?? 0) > 0
}

interface RefundRecord {
  id: number
  refundNo: string
  amountYuan: number | null
  remainingAfterYuan: number | null
  refundType: 'PARTIAL' | 'FULL'
  reason: string | null
  operatorUsername: string | null
  operatorNickname: string | null
  createdAt: string
}

const recordsVisible = ref(false)
const recordsLoading = ref(false)
const recordsRow = ref<OrderRow | null>(null)
const records = ref<RefundRecord[]>([])

function operatorText(row: RefundRecord): string {
  return row.operatorNickname || row.operatorUsername || '—'
}

function refundTypeLabel(value: RefundRecord['refundType']): string {
  return value === 'FULL' ? t('typeFull') : t('typePartial')
}

async function openRecords(row: OrderRow) {
  recordsRow.value = row
  recordsVisible.value = true
  recordsLoading.value = true
  try {
    records.value = await request.get<never, RefundRecord[]>(`/parking-orders/${row.id}/refunds`)
  } catch (error) {
    records.value = []
    ElMessage.error((error as Error)?.message ?? t('loadFailed'))
  } finally {
    recordsLoading.value = false
  }
}

const sessionsVisible = ref(false)
const sessionsLoading = ref(false)
const sessionsRow = ref<OrderRow | null>(null)
const sessionRows = ref<OrderRow[]>([])

async function openSessions(row: OrderRow) {
  sessionsRow.value = row
  sessionsVisible.value = true
  sessionsLoading.value = true
  try {
    sessionRows.value = await request.get<never, OrderRow[]>(`/parking-orders/${row.id}/payment-sessions`)
  } catch (error) {
    sessionRows.value = []
    ElMessage.error((error as Error)?.message ?? t('loadFailed'))
  } finally {
    sessionsLoading.value = false
  }
}

const refundVisible = ref(false)
const refundSaving = ref(false)
const refundRow = ref<OrderRow | null>(null)
const refundMode = ref<'full' | 'partial'>('full')
const refundAmount = ref<number>(0)
const refundReason = ref('')

function openRefund(row: OrderRow) {
  refundRow.value = row
  refundMode.value = 'full'
  refundAmount.value = refundableOf(row)
  refundReason.value = ''
  refundVisible.value = true
}

function handleRefundModeChange(mode: 'full' | 'partial') {
  const remaining = refundRow.value ? refundableOf(refundRow.value) : 0
  if (mode === 'full') {
    refundAmount.value = remaining
  } else if (!refundAmount.value || refundAmount.value <= 0 || refundAmount.value > remaining) {
    refundAmount.value = remaining
  }
}

async function submitRefund() {
  const row = refundRow.value
  if (!row) {
    return
  }
  const remaining = refundableOf(row)
  const amount = refundMode.value === 'full' ? remaining : Number(Number(refundAmount.value ?? 0).toFixed(2))
  if (!(amount > 0) || amount > remaining) {
    ElMessage.warning(t('refundInvalidAmount'))
    return
  }
  refundSaving.value = true
  try {
    await request.post(`/parking-orders/${row.id}/refund`, {
      amountYuan: refundMode.value === 'full' ? null : amount,
      reason: refundReason.value.trim() || null
    })
    ElMessage.success(t('refundSuccess'))
    refundVisible.value = false
    loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  } finally {
    refundSaving.value = false
  }
}

onMounted(async () => {
  currency.load()
  await loadSiteTimezone()
  const raw = route.query.sessionId
  if (typeof raw === 'string' && /^\d+$/.test(raw)) {
    filters.sessionId = Number(raw)
  }
  await loadLots()
  runSearch()
})
</script>

<template>
  <section class="page-stack">
    <div class="page-card toolbar">
      <el-select
        v-model="filters.lotId"
        :placeholder="t('lot')"
        clearable
        style="width: 200px"
        @change="runSearch"
      >
        <el-option v-for="item in lots" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-input
        v-model="filters.keyword"
        :placeholder="t('keywordPlaceholder')"
        clearable
        style="width: 220px"
        @keyup.enter="runSearch"
      />
      <el-select
        v-model="filters.status"
        :placeholder="t('status')"
        clearable
        style="width: 150px"
        @change="runSearch"
      >
        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-date-picker
        v-model="timeRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        :start-placeholder="t('startDate')"
        :end-placeholder="t('endDate')"
        range-separator="~"
        :disabled-date="(date: Date) => date.getTime() > Date.now()"
        style="width: 250px"
        @change="handleRangeChange"
      />
      <el-button type="primary" @click="runSearch">{{ t('search') }}</el-button>
      <el-button @click="handleReset">{{ t('reset') }}</el-button>
    </div>

    <el-alert :title="t('tip')" type="info" :closable="false" show-icon />

    <div class="page-card">
      <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
        <el-table-column prop="orderNo" :label="t('orderNo')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('paymentNo')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tooltip :content="t('paymentNoTip')" placement="top">
              <span v-if="row.paymentNo" class="order-no">{{ row.paymentNo }}</span>
              <span v-else>—</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column :label="t('plate')" min-width="140">
          <template #default="{ row }">
            <el-tooltip :disabled="!row.plateColor" :content="plateColorLabel(row.plateColor)" placement="top">
              <span class="plate-badge" :style="plateBadgeStyle(row.plateColor)">
                {{ row.plateNumber }}
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="lotName" :label="t('lot')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="t('entryTime')" min-width="165">
          <template #default="{ row }">
            {{ fmtDateTime(row.entryTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('receivable')" min-width="110" align="right">
          <template #default="{ row }">
            <el-tooltip :content="t('receivableTip')" placement="top">
              <span>{{ moneyText(row.receivableYuan) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column :label="t('paidBefore')" min-width="115" align="right">
          <template #default="{ row }">
            <el-tooltip :content="t('paidBeforeTip')" placement="top">
              <span>{{ moneyText(row.paidBeforeYuan) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column :label="t('pendingBefore')" min-width="115" align="right">
          <template #default="{ row }">
            <el-tooltip :content="t('pendingBeforeTip')" placement="top">
              <span>{{ moneyText(row.pendingBeforeYuan) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column :label="t('amount')" min-width="115" align="right">
          <template #default="{ row }">
            <el-tooltip :content="t('amountTip')" placement="top">
              <span class="amount-cell">{{ moneyText(row.amountYuan) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column :label="t('refunded')" min-width="115" align="right">
          <template #default="{ row }">
            <el-tooltip :content="t('refundedTip')" placement="top">
              <span>{{ moneyText(row.refundedYuan ?? 0) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column :label="t('orderStatus')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="orderTagType(row.status)" disable-transitions>
              {{ orderLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('payTime')" min-width="165">
          <template #default="{ row }">
            {{ fmtDateTime(row.payTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('createdAt')" min-width="165">
          <template #default="{ row }">
            {{ fmtDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('actions')" width="280" fixed="right">
          <template #default="{ row }">
            <div class="actions-cell">
              <el-button v-if="row.status === 'PENDING'" link type="primary" @click="handlePay(row)">
                {{ t('registerPay') }}
              </el-button>
              <el-button v-if="row.status === 'PENDING'" link type="warning" @click="handleCancel(row)">
                {{ t('cancelText') }}
              </el-button>
              <el-button link type="primary" @click="openSessions(row)">
                {{ t('paymentSessions') }}
              </el-button>
              <el-button v-if="canRefund(row)" link type="danger" @click="openRefund(row)">
                {{ t('refund') }}
              </el-button>
              <el-button v-if="hasRefundRecords(row)" link type="primary" @click="openRecords(row)">
                {{ t('refundRecords') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('noData')" />
        </template>
      </el-table>

      <div class="pager-row">
        <el-pagination
          v-model:current-page="pager.page"
          v-model:page-size="pager.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="refundVisible" :title="t('refundTitle')" width="480px" destroy-on-close>
      <div v-if="refundRow" class="refund-summary">
        <div class="pay-plate-row">
          <el-tooltip :disabled="!refundRow.plateColor" :content="plateColorLabel(refundRow.plateColor)" placement="top">
            <span class="plate-badge" :style="plateBadgeStyle(refundRow.plateColor)">
              {{ refundRow.plateNumber }}
            </span>
          </el-tooltip>
          <span class="order-no">{{ refundRow.orderNo }}</span>
        </div>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item :label="t('amount')">
            {{ moneyText(refundRow.amountYuan) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('refunded')">
            {{ moneyText(refundRow.refundedYuan ?? 0) }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('remainingRefundable')">
            <span class="amount-cell">{{ moneyText(refundableOf(refundRow)) }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <el-form label-width="96px" class="refund-form">
        <el-form-item :label="t('refundMode')">
          <el-radio-group v-model="refundMode" @change="handleRefundModeChange">
            <el-radio value="full">{{ t('refundFull') }}</el-radio>
            <el-radio value="partial">{{ t('refundPartial') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('refundAmount')">
          <div class="number-row">
            <el-input-number
              v-model="refundAmount"
              :min="0.01"
              :max="refundRow ? refundableOf(refundRow) : undefined"
              :precision="2"
              :step="0.01"
              :disabled="refundMode === 'full'"
              controls-position="right"
              class="refund-amount-input"
            />
            <span class="unit-suffix">{{ moneyUnit }}</span>
          </div>
        </el-form-item>
        <el-form-item :label="t('refundReason')">
          <el-input
            v-model="refundReason"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            :placeholder="t('refundReasonPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">{{ t('cancelText') }}</el-button>
        <el-button type="danger" :loading="refundSaving" @click="submitRefund">{{ t('refundSubmit') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="recordsVisible" :title="t('refundRecords')" size="720px" destroy-on-close>
      <p v-if="recordsRow" class="records-head">
        <span class="order-no">{{ recordsRow.orderNo }}</span>
        <span>{{ recordsRow.plateNumber }}</span>
      </p>
      <el-table v-loading="recordsLoading" :data="records" stripe style="width: 100%">
        <el-table-column :label="t('refundNo')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="order-no">{{ row.refundNo }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('refundAmount')" min-width="110" align="right">
          <template #default="{ row }">
            <span class="amount-cell">{{ moneyText(row.amountYuan) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('remainingAfter')" min-width="110" align="right">
          <template #default="{ row }">
            {{ moneyText(row.remainingAfterYuan) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('refundType')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.refundType === 'FULL' ? 'info' : 'warning'" disable-transitions>
              {{ refundTypeLabel(row.refundType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('refundReason')" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.reason || '—' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('operator')" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">
            {{ operatorText(row) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('refundTime')" min-width="160">
          <template #default="{ row }">
            {{ fmtDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('noData')" />
        </template>
      </el-table>
    </el-drawer>

    <el-drawer v-model="sessionsVisible" :title="t('paymentSessions')" size="760px" destroy-on-close>
      <p v-if="sessionsRow" class="records-head">
        <span class="order-no">{{ sessionsRow.paymentNo || sessionsRow.orderNo }}</span>
        <span>{{ sessionsRow.plateNumber }}</span>
        <span v-if="sessionRows.length">{{ sessionRows.length }}</span>
      </p>
      <el-table v-loading="sessionsLoading" :data="sessionRows" stripe style="width: 100%">
        <el-table-column :label="t('sessionId')" width="100">
          <template #default="{ row }">
            <span class="order-no">{{ row.sessionId }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('amount')" min-width="110" align="right">
          <template #default="{ row }">
            <span class="amount-cell">{{ moneyText(row.amountYuan) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('orderStatus')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="orderTagType(row.status)" disable-transitions>
              {{ orderLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('sessionStatus')" width="100">
          <template #default="{ row }">
            <el-tag :type="sessionTagType(row.sessionStatus)" effect="plain" disable-transitions>
              {{ sessionLabel(row.sessionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lotName" :label="t('lot')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="t('entryTime')" min-width="165">
          <template #default="{ row }">
            {{ fmtDateTime(row.entryTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('orderNo')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('noData')" />
        </template>
      </el-table>
    </el-drawer>
  </section>
</template>

<style scoped>
.page-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-card {
  background: #fff;
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-soft);
  padding: 18px 20px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.order-no {
  font-family: Consolas, Monaco, 'Courier New', monospace;
  font-size: 13px;
}

.plate-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 4px;
  padding: 1px 7px 2px 8px;
  font-family: 'Segoe UI', 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.06em;
  line-height: 1.55;
  vertical-align: middle;
  box-shadow: 0 1px 3px rgb(0 0 0 / 0.18);
  white-space: nowrap;
}

.amount-cell {
  font-weight: 700;
  color: #d03050;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.actions-cell {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  column-gap: 12px;
  row-gap: 2px;
}

.actions-cell :deep(.el-button + .el-button) {
  margin-left: 0;
}

.pay-plate-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.refund-summary {
  margin-bottom: 8px;
}

.refund-form {
  margin-top: 16px;
}

.number-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.refund-amount-input {
  width: 180px;
}

.unit-suffix {
  color: #64748b;
  font-size: 13px;
}

.records-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 16px;
  color: #475569;
}
</style>
