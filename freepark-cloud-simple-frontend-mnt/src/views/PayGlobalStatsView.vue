<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getToken } from '../utils/auth'
import { useBiText, type BiDict } from '../utils/biText'
import { useDefaultCurrency } from '../utils/currency'
import { paymentMethodNameOf } from '../utils/payment'

const d: BiDict = {
  platform: { 'zh-CN': '支付平台', en: 'Platform' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  export: { 'zh-CN': '导出', en: 'Export' },
  exportSuccess: { 'zh-CN': '已开始下载', en: 'Download started' },
  exportFailed: { 'zh-CN': '导出失败，请重试', en: 'Export failed, try again' },
  tip: {
    'zh-CN': '按站点时区自然日统计全部车场的成功收款与退款。仅计入支付记录中状态为成功的请求；实收 = 收款 − 退款。合计不受分页影响。',
    en: 'Daily totals across all lots in the site timezone, counting successful payment and refund records only. Net = paid − refunded. Totals ignore pagination.'
  },
  statDate: { 'zh-CN': '日期', en: 'Date' },
  pay: { 'zh-CN': '收款', en: 'Paid' },
  wechat: { 'zh-CN': '微信支付', en: 'WeChat Pay' },
  alipay: { 'zh-CN': '支付宝', en: 'Alipay' },
  cash: { 'zh-CN': '现金收款', en: 'Cash' },
  refund: { 'zh-CN': '退款', en: 'Refunded' },
  net: { 'zh-CN': '实收', en: 'Net' },
  totalPay: { 'zh-CN': '收款合计', en: 'Paid total' },
  totalRefund: { 'zh-CN': '退款合计', en: 'Refunded total' },
  totalNet: { 'zh-CN': '实收合计', en: 'Net total' },
  startDate: { 'zh-CN': '开始日期', en: 'Start date' },
  endDate: { 'zh-CN': '结束日期', en: 'End date' },
  rangeMaxYear: { 'zh-CN': '查询区间不能超过 1 年', en: 'Query range cannot exceed 1 year' },
  noData: { 'zh-CN': '暂无数据', en: 'No data' },
  loadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load, try again' }
}

const { t, locale } = useBiText(d)

interface StatRow {
  statDate: string
  payYuan: number | null
  wechatYuan: number | null
  alipayYuan: number | null
  cashYuan: number | null
  refundYuan: number | null
  netYuan: number | null
}

interface StatsResult {
  list: StatRow[]
  total: number
  page: number
  size: number
  payYuan: number | null
  refundYuan: number | null
  netYuan: number | null
}

const rows = ref<StatRow[]>([])
const total = ref(0)
const loading = ref(false)
const summary = reactive({
  payYuan: 0,
  refundYuan: 0,
  netYuan: 0
})

const filters = reactive({
  platform: '' as '' | 'WECHAT_PAY' | 'ALIPAY_PAY' | 'CASH'
})

const pager = reactive({ page: 1, size: 10 })
const exporting = ref(false)

let systemTimezone = 'Asia/Shanghai'
const timeRange = ref<[string, string] | null>(defaultRange())
let lastValidRange: [string, string] = defaultRange()

const platformOptions = computed(() => [
  { value: 'WECHAT_PAY' as const, label: paymentMethodNameOf('WECHAT_PAY', locale.value) },
  { value: 'ALIPAY_PAY' as const, label: paymentMethodNameOf('ALIPAY_PAY', locale.value) },
  { value: 'CASH' as const, label: paymentMethodNameOf('CASH', locale.value) }
])

const currency = useDefaultCurrency()
const moneyUnit = computed(() => currency.view.value.unit)

function moneyText(value: number | null | undefined): string {
  if (value == null) {
    return `0 ${moneyUnit.value}`
  }
  const fixed = Number(value).toFixed(2)
  const amount = fixed.replace(/\.?0+$/, '').replace(/\.$/, '') || '0'
  return `${amount} ${moneyUnit.value}`
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

async function loadRows() {
  loading.value = true
  try {
    const range = timeRange.value ?? defaultRange()
    const data = await request.get<never, StatsResult>('/pay-records/global-daily-stats', {
      params: {
        platform: filters.platform || undefined,
        startDate: range[0],
        endDate: range[1],
        page: pager.page,
        size: pager.size
      }
    })
    rows.value = data.list
    total.value = data.total
    summary.payYuan = Number(data.payYuan ?? 0)
    summary.refundYuan = Number(data.refundYuan ?? 0)
    summary.netYuan = Number(data.netYuan ?? 0)
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
  filters.platform = ''
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

function parseFilename(header: string | null, fallback: string): string {
  if (!header) {
    return fallback
  }
  const utf = header.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf?.[1]) {
    try {
      return decodeURIComponent(utf[1])
    } catch {
      return utf[1]
    }
  }
  const plain = header.match(/filename="?([^";]+)"?/i)
  return plain?.[1] ?? fallback
}

async function handleExport() {
  const range = timeRange.value ?? defaultRange()
  exporting.value = true
  try {
    const params = new URLSearchParams({
      startDate: range[0],
      endDate: range[1]
    })
    if (filters.platform) {
      params.set('platform', filters.platform)
    }
    const response = await fetch(`/api/pay-records/global-daily-stats/export?${params.toString()}`, {
      headers: {
        Authorization: `Bearer ${getToken()}`,
        'Accept-Language': locale.value
      }
    })
    if (!response.ok) {
      let message = t('exportFailed')
      try {
        const body = await response.json()
        if (body?.message) {
          message = body.message
        }
      } catch {
        // ignore
      }
      ElMessage.error(message)
      return
    }
    const blob = await response.blob()
    const fallback = `global-stats_${range[0]}_${range[1]}.csv`
    const filename = parseFilename(response.headers.get('Content-Disposition'), fallback)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    ElMessage.success(t('exportSuccess'))
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('exportFailed'))
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  currency.load()
  await loadSiteTimezone()
  runSearch()
})
</script>

<template>
  <section class="page-stack">
    <div class="page-card toolbar">
      <el-select
        v-model="filters.platform"
        :placeholder="t('platform')"
        clearable
        style="width: 150px"
        @change="runSearch"
      >
        <el-option v-for="item in platformOptions" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-button :loading="exporting" @click="handleExport">{{ t('export') }}</el-button>
    </div>

    <el-alert :title="t('tip')" type="info" :closable="false" show-icon />

    <div class="summary-row">
      <div class="summary-card">
        <span class="summary-label">{{ t('totalPay') }}</span>
        <span class="summary-value pay">{{ moneyText(summary.payYuan) }}</span>
      </div>
      <div class="summary-card">
        <span class="summary-label">{{ t('totalRefund') }}</span>
        <span class="summary-value refund">{{ moneyText(summary.refundYuan) }}</span>
      </div>
      <div class="summary-card">
        <span class="summary-label">{{ t('totalNet') }}</span>
        <span class="summary-value net">{{ moneyText(summary.netYuan) }}</span>
      </div>
    </div>

    <div class="page-card">
      <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
        <el-table-column prop="statDate" :label="t('statDate')" min-width="140" />
        <el-table-column :label="t('pay')" min-width="130" align="right">
          <template #default="{ row }">
            <span class="amount-pay">{{ moneyText(row.payYuan) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('wechat')" min-width="130" align="right">
          <template #default="{ row }">
            {{ moneyText(row.wechatYuan) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('alipay')" min-width="130" align="right">
          <template #default="{ row }">
            {{ moneyText(row.alipayYuan) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('cash')" min-width="130" align="right">
          <template #default="{ row }">
            {{ moneyText(row.cashYuan) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('refund')" min-width="130" align="right">
          <template #default="{ row }">
            <span class="amount-refund">{{ moneyText(row.refundYuan) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('net')" min-width="130" align="right">
          <template #default="{ row }">
            <span class="amount-net">{{ moneyText(row.netYuan) }}</span>
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

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  background: #fff;
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-soft);
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  color: #909399;
  font-size: 13px;
}

.summary-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.summary-value.pay,
.amount-pay {
  color: #0d9a58;
}

.summary-value.refund,
.amount-refund {
  color: #d03050;
}

.summary-value.net,
.amount-net {
  color: #1f6feb;
  font-weight: 700;
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 900px) {
  .summary-row {
    grid-template-columns: 1fr;
  }
}
</style>
