<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { useDefaultCurrency } from '../utils/currency'

const d: BiDict = {
  lot: { 'zh-CN': '车场', en: 'Lot' },
  allLots: { 'zh-CN': '全部车场', en: 'All lots' },
  plate: { 'zh-CN': '车牌号', en: 'Plate number' },
  platePlaceholder: { 'zh-CN': '请输入要查询的车牌', en: 'Enter plate to query' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  tip: {
    'zh-CN': '查询单辆车的欠费停车数据：在场（已产生估算费用）与已出场的流水只要应收金额大于 0 且未付清都会列出；免缴费与已支付的不计入。车场留空表示在所有车场中查询。',
    en: 'Query unpaid parking records of a vehicle: parking (with estimated fee) and closed sessions with fee due are listed; free (0 yuan) and fully paid ones are excluded. Leave the lot empty to search across all lots.'
  },
  plateRequired: { 'zh-CN': '请先输入车牌号', en: 'Please enter a plate number first' },
  summaryRecords: { 'zh-CN': '欠费停车 {count} 次', en: '{count} unpaid session(s)' },
  summaryAmount: { 'zh-CN': '欠费总额', en: 'Total unpaid' },
  noArrears: { 'zh-CN': '该车牌暂无欠费停车记录', en: 'No unpaid parking records for this plate' },
  noData: { 'zh-CN': '暂无数据', en: 'No data' },
  status: { 'zh-CN': '状态', en: 'Status' },
  statusOpen: { 'zh-CN': '在场（估算）', en: 'Parking (est.)' },
  statusClosed: { 'zh-CN': '已出场', en: 'Closed' },
  lotName: { 'zh-CN': '车场', en: 'Lot' },
  color: { 'zh-CN': '车牌颜色', en: 'Plate color' },
  entryTime: { 'zh-CN': '入场时间', en: 'Entry time' },
  exitTime: { 'zh-CN': '出场时间', en: 'Exit time' },
  duration: { 'zh-CN': '停车时长', en: 'Duration' },
  fee: { 'zh-CN': '应收金额', en: 'Fee due' },
  payStatus: { 'zh-CN': '支付状态', en: 'Payment status' },
  payUnpaid: { 'zh-CN': '未支付', en: 'Unpaid' },
  payPartial: { 'zh-CN': '部分支付', en: 'Partially paid' },
  clickCopy: { 'zh-CN': '点击复制车牌', en: 'Click to copy plate' },
  copied: { 'zh-CN': '已复制车牌 {plate}', en: 'Plate {plate} copied' },
  colorBlue: { 'zh-CN': '蓝', en: 'Blue' },
  colorYellow: { 'zh-CN': '黄', en: 'Yellow' },
  colorGreen: { 'zh-CN': '绿', en: 'Green' },
  colorYellowGreen: { 'zh-CN': '黄绿', en: 'Yellow-green' },
  colorBlack: { 'zh-CN': '黑', en: 'Black' },
  colorWhite: { 'zh-CN': '白', en: 'White' },
  colorOther: { 'zh-CN': '其他', en: 'Other' },
  recalcDone: {
    'zh-CN': '已按当前计费配置刷新车牌 {plate} 最近一笔停车费用',
    en: 'Latest session fee for {plate} refreshed with the current config'
  },
  recalcOpenHint: {
    'zh-CN': '车牌 {plate} 正在停车，已按「入场 ~ 当前时刻」刷新最新估算费用',
    en: 'Plate {plate} is parking; latest fee re-estimated up to now'
  },
  loadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load, try again' },
  operation: { 'zh-CN': '操作', en: 'Actions' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  editTitle: { 'zh-CN': '编辑流水', en: 'Edit session' },
  saveSuccess: { 'zh-CN': '保存成功，费用已按当前配置重算', en: 'Saved; fee recalculated with current config' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  confirm: { 'zh-CN': '确定', en: 'OK' },
  reqFailed: { 'zh-CN': '操作失败，请重试', en: 'Operation failed, try again' },
  entryRequired: { 'zh-CN': '请填写入场时间', en: 'Please set entry time' },
  exitRequired: { 'zh-CN': '请填写出场时间', en: 'Please set exit time' },
  closeNow: { 'zh-CN': '立即出场并关场', en: 'Exit now and close session' },
  recalc: { 'zh-CN': '重新算费', en: 'Recalculate' },
  recalcPreviewTitle: { 'zh-CN': '重新算费 - 确认结果', en: 'Recalculate - confirm' },
  recalcPreviewMsg: {
    'zh-CN': '车牌 {plate}：当前应收 {old}，按当前配置重算为 {new}。确认后生效。',
    en: 'Plate {plate}: current fee {old}; recalculated as {new}. Confirm to apply.'
  },
  recalcPreviewOpenMsg: {
    'zh-CN': '车牌 {plate} 正在停车：当前估算 {old}，按当前配置重算为 {new}（入场 ~ 当前时刻）。出场时会按真实出场时间重新结算。',
    en: 'Plate {plate} is parking: current estimate {old}; recalculated as {new} (entry ~ now). It will be settled with the real exit time.'
  },
  recalcSuccess: { 'zh-CN': '已按当前计费配置重新算费', en: 'Fee recalculated with current config' },
  pay: { 'zh-CN': '支付登记', en: 'Record payment' },
  payTitle: { 'zh-CN': '支付登记', en: 'Record payment' },
  payMsg: {
    'zh-CN': '车牌 {plate} 应收 {fee}。请选择实际支付状态。',
    en: 'Plate {plate} fee is {fee}. Select the actual payment status.'
  },
  payPaid: { 'zh-CN': '已支付', en: 'Paid' },
  payUpdated: { 'zh-CN': '支付状态已更新', en: 'Payment status updated' },
  voidTitle: { 'zh-CN': '作废流水', en: 'Void session' },
  voidMsg: {
    'zh-CN': '确定作废车牌 {plate} 的这条流水吗？作废后不可恢复。',
    en: 'Void the session of plate {plate}? This cannot be undone.'
  },
  voidSuccess: { 'zh-CN': '流水已作废', en: 'Session voided' },
  void: { 'zh-CN': '作废', en: 'Void' }
}

const { t, locale } = useBiText(d)

interface LotOption {
  id: number
  name: string
}

interface ArrearsRow {
  id: number
  lotId: number
  lotName: string
  plateNumber: string
  plateColor: string | null
  status: 'OPEN' | 'CLOSED' | 'VOIDED'
  entryTime: string
  exitTime: string | null
  parkedMinutes: number | null
  feeYuan: number | null
  payStatus: 'UNPAID' | 'PARTIAL' | 'PAID' | 'FREE' | null
}

interface ArrearsPage {
  list: ArrearsRow[]
  total: number
  page: number
  size: number
  totalAmount: number
}

const lots = ref<LotOption[]>([])
const rows = ref<ArrearsRow[]>([])
const total = ref(0)
const totalAmount = ref(0)
const loading = ref(false)
const queried = ref(false)
const filters = reactive<{ lotId: number | undefined; plateNumber: string }>({
  lotId: undefined,
  plateNumber: ''
})
const pager = reactive({ page: 1, size: 10 })

const currency = useDefaultCurrency()
const moneyUnit = computed(() => currency.view.value.unit)

/** 编辑流水弹窗状态。 */
const editingRow = ref<ArrearsRow | null>(null)
const editDialogVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive({
  plateNumber: '',
  plateColor: 'BLUE',
  entryTime: '',
  exitTime: '',
  enableExit: false
})

/** 支付登记弹窗状态。 */
const payRow = ref<ArrearsRow | null>(null)
const payDialogVisible = ref(false)
const paySaving = ref(false)
const payStatus = ref<'UNPAID' | 'PARTIAL' | 'PAID'>('UNPAID')

/** 车牌颜色双语下拉（展示用），与全局停车流水页口径一致。 */
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

function colorTagType(color: string | null | undefined): 'success' | 'warning' | 'info' {
  if (color === 'YELLOW' || color === 'YELLOW_GREEN') return 'warning'
  if (color === 'GREEN') return 'success'
  return 'info'
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

function durationText(minutes: number | null): string {
  if (minutes == null || minutes < 0) {
    return '—'
  }
  const days = Math.floor(minutes / 1440)
  const hours = Math.floor((minutes % 1440) / 60)
  const mins = minutes % 60
  if (locale.value === 'en') {
    const parts: string[] = []
    if (days > 0) parts.push(`${days}d`)
    if (hours > 0) parts.push(`${hours}h`)
    if (mins > 0 || parts.length === 0) parts.push(`${mins}m`)
    return parts.join(' ')
  }
  const parts: string[] = []
  if (days > 0) parts.push(`${days}天`)
  if (hours > 0) parts.push(`${hours}小时`)
  if (mins > 0 || parts.length === 0) parts.push(`${mins}分`)
  return parts.join('')
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

function feeText(value: number | null): string {
  if (value == null) {
    return '—'
  }
  const fixed = Number(value).toFixed(2)
  const amount = fixed.replace(/\.?0+$/, '').replace(/\.$/, '') || '0'
  return `${amount} ${moneyUnit.value}`
}

function payTagType(value: ArrearsRow['payStatus']): 'info' | 'warning' | 'success' {
  if (value === 'PARTIAL') return 'warning'
  if (value === 'PAID') return 'success'
  return 'info'
}

function payLabel(value: ArrearsRow['payStatus']): string {
  if (value === 'UNPAID' || value == null) return t('payUnpaid')
  if (value === 'PARTIAL') return t('payPartial')
  return '—'
}

function statusLabel(value: ArrearsRow['status']): string {
  if (value === 'OPEN') return t('statusOpen')
  if (value === 'CLOSED') return t('statusClosed')
  return '—'
}

function statusTagType(value: ArrearsRow['status']): 'success' | 'info' | 'danger' {
  if (value === 'OPEN') return 'success'
  if (value === 'CLOSED') return 'info'
  return 'danger'
}

async function handleCopyPlate(plate: string) {
  try {
    await navigator.clipboard.writeText(plate)
    ElMessage.success(t('copied').replace('{plate}', plate))
  } catch {
    // 剪贴板不可用时静默
  }
}

async function loadLots() {
  try {
    lots.value = await request.get<never, LotOption[]>('/lots')
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('loadFailed'))
  }
}

async function runSearch() {
  const plate = filters.plateNumber.trim()
  if (!plate) {
    ElMessage.warning(t('plateRequired'))
    return
  }
  pager.page = 1
  await refreshLatestFee(plate)
  await loadRows(plate)
}

/** 查询前自动刷新该车牌最近一笔流水费用（在场按当前估算 / 已出场按真实时间重算）；失败不阻断列表查询。 */
async function refreshLatestFee(plate: string) {
  try {
    const recalc = await request.post<never, { status: string } | null>(
      '/parking-sessions/vehicle-query/recalc-latest',
      null,
      { params: { lotId: filters.lotId, plateNumber: plate } }
    )
    if (recalc) {
      const hint = recalc.status === 'OPEN' ? t('recalcOpenHint') : t('recalcDone')
      ElMessage.info(hint.replace('{plate}', plate))
    }
  } catch {
    // 无权限 / 无流水等不阻断列表查询
  }
}

async function loadRows(plate = filters.plateNumber.trim()) {
  if (!plate) {
    return
  }
  loading.value = true
  try {
    const data = await request.get<never, ArrearsPage>('/parking-sessions/vehicle-query', {
      params: {
        lotId: filters.lotId,
        plateNumber: plate,
        page: pager.page,
        size: pager.size
      }
    })
    rows.value = data.list
    total.value = data.total
    totalAmount.value = data.totalAmount ?? 0
    queried.value = true
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('loadFailed'))
  } finally {
    loading.value = false
  }
}

function handleReset() {
  filters.lotId = undefined
  filters.plateNumber = ''
  rows.value = []
  total.value = 0
  totalAmount.value = 0
  queried.value = false
  pager.page = 1
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

function payStatusOptions() {
  return [
    { value: 'UNPAID' as const, label: t('payUnpaid') },
    { value: 'PARTIAL' as const, label: t('payPartial') },
    { value: 'PAID' as const, label: t('payPaid') }
  ]
}

/** 本地时区 yyyy-MM-ddTHH:mm:ss，作为编辑弹窗出场时间的初始默认值。 */
function currentLocalIso(): string {
  const now = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${p(now.getMonth() + 1)}-${p(now.getDate())}T${p(now.getHours())}:${p(now.getMinutes())}:${p(now.getSeconds())}`
}

/** 打开编辑流水弹窗（客服修正车牌/颜色/时间；改完保存后端自动按当前配置重算）。 */
function openEdit(row: ArrearsRow) {
  editingRow.value = row
  editForm.plateNumber = row.plateNumber
  editForm.plateColor = row.plateColor ?? 'BLUE'
  editForm.entryTime = row.entryTime ?? currentLocalIso()
  editForm.enableExit = row.status === 'CLOSED'
  editForm.exitTime = row.exitTime ?? currentLocalIso()
  editDialogVisible.value = true
}

async function submitEdit() {
  const row = editingRow.value
  if (!row) {
    return
  }
  const plate = editForm.plateNumber.trim()
  if (!plate) {
    ElMessage.warning(t('plateRequired'))
    return
  }
  if (!editForm.entryTime) {
    ElMessage.warning(t('entryRequired'))
    return
  }
  if (editForm.enableExit && !editForm.exitTime) {
    ElMessage.warning(t('exitRequired'))
    return
  }
  editSaving.value = true
  try {
    const payload: Record<string, unknown> = {
      plateNumber: plate,
      plateColor: editForm.plateColor,
      entryTime: editForm.entryTime
    }
    if (editForm.enableExit && editForm.exitTime) {
      payload.exitTime = editForm.exitTime
    }
    await request.put(`/parking-sessions/${row.id}`, payload)
    editDialogVisible.value = false
    ElMessage.success(t('saveSuccess'))
    await loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  } finally {
    editSaving.value = false
  }
}

/** 重新算费：先预览金额弹窗确认（当前值 → 新值），确认后才落库。 */
async function handleRecalc(row: ArrearsRow) {
  let newFee: number | null = null
  try {
    newFee = await request.post<never, number | null>(`/parking-sessions/${row.id}/fee-preview`)
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
    return
  }
  const template = row.status === 'OPEN' ? t('recalcPreviewOpenMsg') : t('recalcPreviewMsg')
  const message = template
    .replace('{plate}', row.plateNumber)
    .replace('{old}', feeText(row.feeYuan))
    .replace('{new}', feeText(newFee))
  try {
    await ElMessageBox.confirm(message, t('recalcPreviewTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  try {
    await request.post(`/parking-sessions/${row.id}/recalculate`)
    ElMessage.success(t('recalcSuccess'))
    await loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  }
}

/** 打开支付登记弹窗（仅已出场欠费流水；在场不可登记）。 */
function openPay(row: ArrearsRow) {
  payRow.value = row
  const current = row.payStatus
  payStatus.value = current === 'PAID' || current === 'PARTIAL' ? current : 'UNPAID'
  payDialogVisible.value = true
}

async function submitPay() {
  const row = payRow.value
  if (!row) {
    return
  }
  paySaving.value = true
  try {
    await request.post(`/parking-sessions/${row.id}/pay-status`, { status: payStatus.value })
    payDialogVisible.value = false
    ElMessage.success(t('payUpdated'))
    await loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  } finally {
    paySaving.value = false
  }
}

/** 作废流水（客服确认误录/无需收费的流水）。 */
async function handleVoid(row: ArrearsRow) {
  try {
    await ElMessageBox.confirm(
      t('voidMsg').replace('{plate}', row.plateNumber),
      t('voidTitle'),
      {
        type: 'warning',
        confirmButtonText: t('void'),
        cancelButtonText: t('cancel')
      }
    )
  } catch {
    return
  }
  try {
    await request.post(`/parking-sessions/${row.id}/void`)
    ElMessage.success(t('voidSuccess'))
    await loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  }
}

onMounted(async () => {
  currency.load()
  await loadLots()
})
</script>

<template>
  <section class="page-stack">
    <div class="page-card toolbar">
      <el-select
        v-model="filters.lotId"
        :placeholder="t('allLots')"
        clearable
        style="width: 200px"
      >
        <el-option v-for="item in lots" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-input
        v-model="filters.plateNumber"
        :placeholder="t('platePlaceholder')"
        clearable
        style="width: 260px"
        @keyup.enter="runSearch"
      />
      <el-button type="primary" @click="runSearch">{{ t('search') }}</el-button>
      <el-button @click="handleReset">{{ t('reset') }}</el-button>
    </div>

    <el-alert :title="t('tip')" type="info" :closable="false" show-icon />

    <div class="page-card">
      <template v-if="queried">
        <div v-if="total > 0" class="arrears-summary">
          <el-tooltip
            :content="`${plateColorLabel(rows[0]?.plateColor)} · ${t('clickCopy')}`"
            placement="top"
          >
            <span
              class="plate-badge plate-badge-lg"
              :style="plateBadgeStyle(rows[0]?.plateColor)"
              @click="handleCopyPlate(filters.plateNumber.trim())"
            >
              {{ filters.plateNumber.trim() }}
            </span>
          </el-tooltip>
          <div class="summary-meta">
            <div class="summary-item">
              <span class="summary-label">{{ t('summaryRecords').replace('{count}', String(total)) }}</span>
            </div>
            <div class="summary-item amount">
              <span class="summary-label">{{ t('summaryAmount') }}</span>
              <span class="summary-value">{{ feeText(totalAmount) }}</span>
            </div>
          </div>
        </div>

        <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
          <el-table-column :label="t('status')" width="130">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" disable-transitions>
                {{ statusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('color')" width="110">
            <template #default="{ row }">
              <el-tag :type="colorTagType(row.plateColor)" effect="plain" disable-transitions>
                {{ plateColorLabel(row.plateColor) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lotName" :label="t('lotName')" min-width="150" show-overflow-tooltip />
          <el-table-column :label="t('entryTime')" min-width="170">
            <template #default="{ row }">
              {{ fmtDateTime(row.entryTime) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('exitTime')" min-width="170">
            <template #default="{ row }">
              {{ fmtDateTime(row.exitTime) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('duration')" min-width="130">
            <template #default="{ row }">
              {{ durationText(row.parkedMinutes) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('fee')" min-width="130" align="right">
            <template #default="{ row }">
              <span class="fee-amount">{{ feeText(row.feeYuan) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('payStatus')" min-width="110">
            <template #default="{ row }">
              <el-tag :type="payTagType(row.payStatus)" disable-transitions>
                {{ payLabel(row.payStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('operation')" width="200" fixed="right">
            <template #default="{ row }">
              <div class="actions-cell">
                <el-button link type="primary" @click="openEdit(row)">{{ t('edit') }}</el-button>
                <el-button link type="primary" @click="handleRecalc(row)">{{ t('recalc') }}</el-button>
                <el-button v-if="row.status === 'CLOSED'" link type="primary" @click="openPay(row)">
                  {{ t('pay') }}
                </el-button>
                <el-button link type="danger" @click="handleVoid(row)">{{ t('void') }}</el-button>
              </div>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :description="total === 0 ? t('noArrears') : t('noData')" />
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
      </template>

      <el-empty v-else :description="t('platePlaceholder')" />
    </div>

    <el-dialog v-model="editDialogVisible" :title="t('editTitle')" width="480px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item :label="t('plate')">
          <el-input v-model="editForm.plateNumber" :placeholder="t('plate')" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('color')">
          <el-select v-model="editForm.plateColor" style="width: 100%">
            <el-option v-for="item in colorOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('entryTime')">
          <el-date-picker
            v-model="editForm.entryTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="editingRow?.status === 'OPEN'">
          <el-checkbox v-model="editForm.enableExit">{{ t('closeNow') }}</el-checkbox>
        </el-form-item>
        <template v-if="editingRow && (editingRow.status === 'CLOSED' || editForm.enableExit)">
          <el-form-item :label="t('exitTime')">
            <el-date-picker
              v-model="editForm.exitTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="editSaving" @click="submitEdit">{{ t('confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payDialogVisible" :title="t('payTitle')" width="440px" destroy-on-close>
      <p v-if="payRow" class="pay-dialog-tip">
        {{ t('payMsg').replace('{plate}', payRow.plateNumber).replace('{fee}', feeText(payRow.feeYuan)) }}
      </p>
      <el-radio-group v-model="payStatus">
        <el-radio v-for="item in payStatusOptions()" :key="item.value" :value="item.value">
          {{ item.label }}
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="payDialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="paySaving" @click="submitPay">{{ t('confirm') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.arrears-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 16px 20px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: linear-gradient(135deg, #fff1f0, #fff7f6);
  border: 1px solid #ffccc7;
}

.summary-meta {
  display: flex;
  align-items: flex-end;
  gap: 36px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.summary-label {
  font-size: 12px;
  color: #8c8c8c;
}

.summary-item.amount .summary-value {
  font-size: 26px;
  font-weight: 700;
  color: #cf1322;
  line-height: 1.2;
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
  cursor: pointer;
  transition: transform 0.12s ease, filter 0.12s ease;
}

.plate-badge-lg {
  font-size: 22px;
  letter-spacing: 0.14em;
  padding: 4px 14px 5px 16px;
  border-radius: 6px;
}

.plate-badge:hover {
  filter: brightness(1.08);
  transform: translateY(-1px);
}

.fee-amount {
  font-weight: 600;
  color: #cf1322;
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

.pay-dialog-tip {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}
</style>
