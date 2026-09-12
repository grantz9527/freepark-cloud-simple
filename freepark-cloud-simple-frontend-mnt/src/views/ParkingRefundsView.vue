<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { useDefaultCurrency } from '../utils/currency'

const d: BiDict = {
  lot: { 'zh-CN': '车场', en: 'Lot' },
  keywordPlaceholder: { 'zh-CN': '退款单号 / 订单号 / 车牌 / 车场', en: 'Refund no / order no / plate / lot' },
  type: { 'zh-CN': '退款类型', en: 'Refund type' },
  allType: { 'zh-CN': '全部类型', en: 'All types' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  tip: {
    'zh-CN': '每次全部退款或部分退款都会生成一笔退款记录，金额从关联停车流水的累计已支付中回冲。可按车场、车牌、订单号和退款时间查询。',
    en: 'Each full or partial refund creates a refund record and reverses the credited session paid amount. Filter by lot, plate, order no, and refund time.'
  },
  refundNo: { 'zh-CN': '退款单号', en: 'Refund No' },
  orderNo: { 'zh-CN': '订单号', en: 'Order No' },
  plate: { 'zh-CN': '车牌', en: 'Plate' },
  amount: { 'zh-CN': '本次退款', en: 'This refund' },
  refundedAfter: { 'zh-CN': '累计已退', en: 'Refunded after' },
  remainingAfter: { 'zh-CN': '剩余可退', en: 'Remaining' },
  refundType: { 'zh-CN': '类型', en: 'Type' },
  typePartial: { 'zh-CN': '部分退款', en: 'Partial' },
  typeFull: { 'zh-CN': '全部退款', en: 'Full' },
  reason: { 'zh-CN': '退款原因', en: 'Reason' },
  operator: { 'zh-CN': '操作人', en: 'Operator' },
  createdAt: { 'zh-CN': '退款时间', en: 'Refunded at' },
  startDate: { 'zh-CN': '开始日期', en: 'Start date' },
  endDate: { 'zh-CN': '结束日期', en: 'End date' },
  rangeMaxYear: { 'zh-CN': '查询区间不能超过 1 年', en: 'Query range cannot exceed 1 year' },
  noData: { 'zh-CN': '暂无数据', en: 'No data' },
  loadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load, try again' },
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

interface RefundRow {
  id: number
  refundNo: string
  orderId: number
  orderNo: string
  sessionId: number
  lotId: number | null
  lotName: string | null
  plateNumber: string
  plateColor: string | null
  amountYuan: number | null
  refundedAfterYuan: number | null
  remainingAfterYuan: number | null
  refundType: 'PARTIAL' | 'FULL'
  reason: string | null
  operatorUsername: string | null
  operatorNickname: string | null
  createdAt: string
}

const lots = ref<LotOption[]>([])
const rows = ref<RefundRow[]>([])
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  lotId: undefined as number | undefined,
  keyword: '',
  refundType: '' as '' | 'PARTIAL' | 'FULL'
})

const pager = reactive({ page: 1, size: 10 })

let systemTimezone = 'Asia/Shanghai'
const timeRange = ref<[string, string] | null>(defaultRange())
let lastValidRange: [string, string] = defaultRange()

const typeOptions = computed(() => [
  { value: 'PARTIAL' as const, label: t('typePartial') },
  { value: 'FULL' as const, label: t('typeFull') }
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

function plateBadgeStyle(color: string | null | undefined) {
  return (color && PLATE_STYLES[color]) || PLATE_STYLES.BLUE
}

function typeTagType(value: RefundRow['refundType']): 'warning' | 'info' {
  return value === 'PARTIAL' ? 'warning' : 'info'
}

function typeLabel(value: RefundRow['refundType']): string {
  return value === 'FULL' ? t('typeFull') : t('typePartial')
}

function operatorText(row: RefundRow): string {
  return row.operatorNickname || row.operatorUsername || '—'
}

const currency = useDefaultCurrency()
const moneyUnit = computed(() => currency.view.value.unit)

function moneyText(value: number | null): string {
  if (value == null) {
    return '—'
  }
  const fixed = Number(value).toFixed(2)
  const amount = fixed.replace(/\.?0+$/, '').replace(/\.$/, '') || '0'
  return `${amount} ${moneyUnit.value}`
}

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
    // ignore
  }
}

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
    const data = await request.get<never, PageResult<RefundRow>>('/parking-order-refunds', {
      params: {
        lotId: filters.lotId,
        keyword: filters.keyword.trim() || undefined,
        refundType: filters.refundType || undefined,
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
  filters.refundType = ''
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

onMounted(async () => {
  currency.load()
  await loadSiteTimezone()
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
        style="width: 260px"
        @keyup.enter="runSearch"
      />
      <el-select
        v-model="filters.refundType"
        :placeholder="t('type')"
        clearable
        style="width: 150px"
        @change="runSearch"
      >
        <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
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
        <el-table-column prop="refundNo" :label="t('refundNo')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.refundNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" :label="t('orderNo')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.orderNo }}</span>
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
        <el-table-column :label="t('amount')" min-width="115" align="right">
          <template #default="{ row }">
            <span class="amount-cell">{{ moneyText(row.amountYuan) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('refundedAfter')" min-width="115" align="right">
          <template #default="{ row }">
            {{ moneyText(row.refundedAfterYuan) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('remainingAfter')" min-width="115" align="right">
          <template #default="{ row }">
            {{ moneyText(row.remainingAfterYuan) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('refundType')" min-width="110">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.refundType)" disable-transitions>
              {{ typeLabel(row.refundType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" :label="t('reason')" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.reason || '—' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('operator')" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ operatorText(row) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('createdAt')" min-width="165">
          <template #default="{ row }">
            {{ fmtDateTime(row.createdAt) }}
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

.mono {
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
</style>
