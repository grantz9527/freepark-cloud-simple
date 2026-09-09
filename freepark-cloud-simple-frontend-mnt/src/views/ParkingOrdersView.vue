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
  keywordPlaceholder: { 'zh-CN': '订单号 / 车牌 / 车场', en: 'Order no / plate / lot' },
  status: { 'zh-CN': '订单状态', en: 'Order status' },
  allStatus: { 'zh-CN': '全部状态', en: 'All status' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  tip: {
    'zh-CN': '每次收费/缴费请求都会为该停车流水生成一笔订单并记录支付金额。订单金额 = 当前应收 − 流水累计已支付 − 待付订单合计；再次缴费只会收取新产生的金额，不会重复计费。',
    en: 'Each payment/charge request creates an order for the parking session and records the amount paid. Order amount = current receivable − session paid − pending orders; later payments only collect the newly accrued amount.'
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
  orderCancelled: { 'zh-CN': '已取消', en: 'Cancelled' },
  payTime: { 'zh-CN': '支付时间', en: 'Paid at' },
  createdAt: { 'zh-CN': '下单时间', en: 'Created at' },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  registerPay: { 'zh-CN': '登记收款', en: 'Register payment' },
  cancel: { 'zh-CN': '取消订单', en: 'Cancel' },
  cancelText: { 'zh-CN': '取消', en: 'Cancel' },
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

interface OrderRow {
  id: number
  orderNo: string
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
  status: 'PENDING' | 'PAID' | 'CANCELLED'
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
  status: '' | 'PENDING' | 'PAID' | 'CANCELLED'
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

function orderTagType(value: OrderRow['status']): 'warning' | 'success' | 'info' {
  if (value === 'PENDING') return 'warning'
  if (value === 'PAID') return 'success'
  return 'info'
}

function orderLabel(value: OrderRow['status']): string {
  if (value === 'PENDING') return t('orderPending')
  if (value === 'PAID') return t('orderPaid')
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
        <el-table-column :label="t('sessionStatus')" width="100">
          <template #default="{ row }">
            <el-tag :type="sessionTagType(row.sessionStatus)" effect="plain" disable-transitions>
              {{ sessionLabel(row.sessionStatus) }}
            </el-tag>
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
        <el-table-column :label="t('actions')" width="150" fixed="right">
          <template #default="{ row }">
            <div class="actions-cell">
              <el-button v-if="row.status === 'PENDING'" link type="primary" @click="handlePay(row)">
                {{ t('registerPay') }}
              </el-button>
              <el-button v-if="row.status === 'PENDING'" link type="warning" @click="handleCancel(row)">
                {{ t('cancelText') }}
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
</style>
