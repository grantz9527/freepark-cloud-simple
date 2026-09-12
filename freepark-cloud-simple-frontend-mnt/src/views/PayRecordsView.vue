<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { useDefaultCurrency } from '../utils/currency'
import { paymentMethodNameOf } from '../utils/payment'
import type { AppLocale } from '../i18n'

const d: BiDict = {
  lot: { 'zh-CN': '车场', en: 'Lot' },
  keywordPlaceholder: {
    'zh-CN': '记录号 / 缴款单号 / 车牌 / 渠道单号 / 车场',
    en: 'Record / pay no / plate / channel no / lot'
  },
  kind: { 'zh-CN': '类型', en: 'Type' },
  platform: { 'zh-CN': '支付平台', en: 'Platform' },
  status: { 'zh-CN': '状态', en: 'Status' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  tip: {
    'zh-CN': '每个支付请求、每个退款请求各记一笔，按微信支付 / 支付宝 / 现金收款区分。一次缴款覆盖多个车场时，会记下各车场金额，供后续按车场拆分统计。',
    en: 'Each payment or refund request is one row, by WeChat Pay, Alipay, or cash. Amounts are split by lot so lot-level revenue can be reported later.'
  },
  recordNo: { 'zh-CN': '记录号', en: 'Record No' },
  kindPay: { 'zh-CN': '支付', en: 'Payment' },
  kindRefund: { 'zh-CN': '退款', en: 'Refund' },
  statusPending: { 'zh-CN': '待确认', en: 'Pending' },
  statusSuccess: { 'zh-CN': '成功', en: 'Success' },
  statusClosed: { 'zh-CN': '已关闭', en: 'Closed' },
  amount: { 'zh-CN': '金额', en: 'Amount' },
  plate: { 'zh-CN': '车牌', en: 'Plate' },
  lots: { 'zh-CN': '车场金额', en: 'Lot amounts' },
  relatedPayNo: { 'zh-CN': '缴款单号', en: 'Payment no' },
  transactionId: { 'zh-CN': '渠道单号', en: 'Channel no' },
  operator: { 'zh-CN': '操作人', en: 'Operator' },
  createdAt: { 'zh-CN': '请求时间', en: 'Requested at' },
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

const { t, locale } = useBiText(d)

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

interface LotAmount {
  lotId: number | null
  lotName: string | null
  amountYuan: number | null
}

interface RecordRow {
  id: number
  recordNo: string
  kind: 'PAY' | 'REFUND'
  platform: 'WECHAT_PAY' | 'ALIPAY_PAY' | 'CASH'
  status: 'PENDING' | 'SUCCESS' | 'CLOSED'
  amountYuan: number | null
  plateNumber: string
  plateColor: string | null
  relatedPayNo: string | null
  relatedOrderNo: string | null
  relatedRefundNo: string | null
  transactionId: string | null
  mock: boolean
  operatorUsername: string | null
  operatorNickname: string | null
  successTime: string | null
  createdAt: string
  lots: LotAmount[]
}

const lots = ref<LotOption[]>([])
const rows = ref<RecordRow[]>([])
const total = ref(0)
const loading = ref(false)

const filters = reactive({
  lotId: undefined as number | undefined,
  keyword: '',
  kind: '' as '' | 'PAY' | 'REFUND',
  platform: '' as '' | 'WECHAT_PAY' | 'ALIPAY_PAY' | 'CASH',
  status: '' as '' | 'PENDING' | 'SUCCESS' | 'CLOSED'
})

const pager = reactive({ page: 1, size: 10 })

let systemTimezone = 'Asia/Shanghai'
const timeRange = ref<[string, string] | null>(defaultRange())
let lastValidRange: [string, string] = defaultRange()

function currentLocale(): AppLocale {
  return locale.value
}

const kindOptions = computed(() => [
  { value: 'PAY' as const, label: t('kindPay') },
  { value: 'REFUND' as const, label: t('kindRefund') }
])

const platformOptions = computed(() => {
  const locale = currentLocale()
  return [
    { value: 'WECHAT_PAY' as const, label: paymentMethodNameOf('WECHAT_PAY', locale) },
    { value: 'ALIPAY_PAY' as const, label: paymentMethodNameOf('ALIPAY_PAY', locale) },
    { value: 'CASH' as const, label: paymentMethodNameOf('CASH', locale) }
  ]
})

const statusOptions = computed(() => [
  { value: 'PENDING' as const, label: t('statusPending') },
  { value: 'SUCCESS' as const, label: t('statusSuccess') },
  { value: 'CLOSED' as const, label: t('statusClosed') }
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

function kindTagType(value: RecordRow['kind']): 'success' | 'warning' {
  return value === 'REFUND' ? 'warning' : 'success'
}

function kindLabel(value: RecordRow['kind']): string {
  return value === 'REFUND' ? t('kindRefund') : t('kindPay')
}

function statusTagType(value: RecordRow['status']): 'warning' | 'success' | 'info' {
  if (value === 'PENDING') return 'warning'
  if (value === 'SUCCESS') return 'success'
  return 'info'
}

function statusLabel(value: RecordRow['status']): string {
  if (value === 'PENDING') return t('statusPending')
  if (value === 'SUCCESS') return t('statusSuccess')
  return t('statusClosed')
}

function platformLabel(value: RecordRow['platform']): string {
  return paymentMethodNameOf(value, currentLocale())
}

function operatorText(row: RecordRow): string {
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
    const data = await request.get<never, PageResult<RecordRow>>('/pay-records', {
      params: {
        lotId: filters.lotId,
        keyword: filters.keyword.trim() || undefined,
        kind: filters.kind || undefined,
        platform: filters.platform || undefined,
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
  filters.kind = ''
  filters.platform = ''
  filters.status = ''
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
        style="width: 280px"
        @keyup.enter="runSearch"
      />
      <el-select
        v-model="filters.kind"
        :placeholder="t('kind')"
        clearable
        style="width: 120px"
        @change="runSearch"
      >
        <el-option v-for="item in kindOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select
        v-model="filters.platform"
        :placeholder="t('platform')"
        clearable
        style="width: 150px"
        @change="runSearch"
      >
        <el-option v-for="item in platformOptions" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select
        v-model="filters.status"
        :placeholder="t('status')"
        clearable
        style="width: 130px"
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
        <el-table-column prop="recordNo" :label="t('recordNo')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.recordNo }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('kind')" width="90">
          <template #default="{ row }">
            <el-tag :type="kindTagType(row.kind)" disable-transitions>
              {{ kindLabel(row.kind) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('platform')" min-width="120">
          <template #default="{ row }">
            {{ platformLabel(row.platform) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('status')" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain" disable-transitions>
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('amount')" min-width="120" align="right">
          <template #default="{ row }">
            <span :class="row.kind === 'REFUND' ? 'amount-refund' : 'amount-cell'">
              {{ row.kind === 'REFUND' ? '-' : '' }}{{ moneyText(row.amountYuan) }}
            </span>
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
        <el-table-column :label="t('lots')" min-width="180">
          <template #default="{ row }">
            <div v-if="row.lots?.length" class="lot-amounts">
              <div v-for="(item, index) in row.lots" :key="`${item.lotId}-${index}`">
                {{ item.lotName || '—' }}
                <span class="lot-amount">{{ moneyText(item.amountYuan) }}</span>
              </div>
            </div>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('relatedPayNo')" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.relatedPayNo" class="mono">{{ row.relatedPayNo }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('transactionId')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.transactionId" class="mono">{{ row.transactionId }}</span>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('operator')" min-width="110" show-overflow-tooltip>
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
  color: #0d9a58;
}

.amount-refund {
  font-weight: 700;
  color: #d03050;
}

.lot-amounts {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
  line-height: 1.45;
}

.lot-amount {
  margin-left: 6px;
  color: #606266;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
