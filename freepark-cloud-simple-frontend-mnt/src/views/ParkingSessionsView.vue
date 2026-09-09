<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { useDefaultCurrency } from '../utils/currency'

const d: BiDict = {
  lot: { 'zh-CN': '车场', en: 'Lot' },
  allLots: { 'zh-CN': '全部车场', en: 'All lots' },
  keyword: { 'zh-CN': '关键词', en: 'Keyword' },
  keywordPlaceholder: { 'zh-CN': '车牌 / 车场 / 通道', en: 'Plate / lot / lane' },
  status: { 'zh-CN': '状态', en: 'Status' },
  allStatus: { 'zh-CN': '全部状态', en: 'All status' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  manualEntry: { 'zh-CN': '手动新增', en: 'Manual add' },
  plate: { 'zh-CN': '车牌', en: 'Plate' },
  plateColor: { 'zh-CN': '车牌颜色', en: 'Plate color' },
  entryTime: { 'zh-CN': '入场时间', en: 'Entry time' },
  entryLane: { 'zh-CN': '入场通道', en: 'Entry lane' },
  noLane: { 'zh-CN': '不限通道', en: 'Any lane' },
  exitTime: { 'zh-CN': '出场时间', en: 'Exit time' },
  exitLane: { 'zh-CN': '出场通道', en: 'Exit lane' },
  closeNow: { 'zh-CN': '同时补录出场信息', en: 'Include exit info' },
  duration: { 'zh-CN': '停车时长', en: 'Duration' },
  fee: { 'zh-CN': '应收金额', en: 'Fee due' },
  payStatus: { 'zh-CN': '支付状态', en: 'Payment status' },
  payTime: { 'zh-CN': '支付时间', en: 'Paid at' },
  payUnpaid: { 'zh-CN': '未支付', en: 'Unpaid' },
  payPartial: { 'zh-CN': '部分支付', en: 'Partially paid' },
  payPaid: { 'zh-CN': '已支付', en: 'Paid' },
  payFree: { 'zh-CN': '免缴费', en: 'Free' },
  payReg: { 'zh-CN': '登记支付', en: 'Record pay' },
  payTitle: { 'zh-CN': '登记支付', en: 'Record payment' },
  payMsg: {
    'zh-CN': '为车牌 {plate} 的已出场流水登记支付结果（应收 {fee}，已支付将记录当前时间为支付时间）：',
    en: 'Record the payment result for plate {plate} (fee due {fee}; PAID stores the current time as paid-at):'
  },
  payUpdated: { 'zh-CN': '支付状态已更新', en: 'Payment status updated' },
  recalc: { 'zh-CN': '重新算费', en: 'Recalculate' },
  recalcSuccess: { 'zh-CN': '已按当前计费配置重新算费', en: 'Fee recalculated with current config' },
  recalcPreviewTitle: { 'zh-CN': '重新算费 - 确认结果', en: 'Recalculate - confirm result' },
  recalcPreviewMsg: {
    'zh-CN': '车牌 {plate}：当前应收 {old}，按当前计费配置重算为 {new}。确认后才会更新该流水。',
    en: 'Plate {plate}: current fee {old}; recalculated as {new} with the current config. Confirm to update.'
  },
  recalcPreviewOpenMsg: {
    'zh-CN': '车牌 {plate}：当前应收 {old}，在停按「入场 ~ 当前时刻」估算为 {new}。确认后写入该流水，出场结算时会按真实出场时间重算。',
    en: 'Plate {plate}: current fee {old}; estimated as {new} for ongoing parking (entry ~ now). Confirm to write; it will be recalculated with the real exit time.'
  },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  void: { 'zh-CN': '作废', en: 'Void' },
  createTitle: { 'zh-CN': '手动新增流水', en: 'Manual add session' },
  editTitle: { 'zh-CN': '编辑流水', en: 'Edit session' },
  createSuccess: { 'zh-CN': '流水已新增', en: 'Session added' },
  timeOrderExit: { 'zh-CN': '出场时间必须晚于入场时间', en: 'Exit time must be later than entry time' },
  exitTimeRequired: { 'zh-CN': '请先填写出场时间', en: 'Please set an exit time first' },
  updateSuccess: { 'zh-CN': '流水已更新', en: 'Session updated' },
  voidTitle: { 'zh-CN': '作废流水', en: 'Void session' },
  voidMsg: { 'zh-CN': '确定作废车牌 {plate} 的这条流水吗？作废后不可恢复。', en: 'Void the session of plate {plate}? This cannot be undone.' },
  voidSuccess: { 'zh-CN': '流水已作废', en: 'Session voided' },
  loadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load, try again' },
  reqFailed: { 'zh-CN': '操作失败，请重试', en: 'Operation failed, try again' },
  noData: { 'zh-CN': '暂无数据', en: 'No data' },
  statusOpen: { 'zh-CN': '在场', en: 'Open' },
  statusClosed: { 'zh-CN': '已出场', en: 'Closed' },
  statusVoided: { 'zh-CN': '已作废', en: 'Voided' },
  colorBlue: { 'zh-CN': '蓝', en: 'Blue' },
  colorYellow: { 'zh-CN': '黄', en: 'Yellow' },
  colorGreen: { 'zh-CN': '绿', en: 'Green' },
  colorYellowGreen: { 'zh-CN': '黄绿', en: 'Yellow-green' },
  colorBlack: { 'zh-CN': '黑', en: 'Black' },
  colorWhite: { 'zh-CN': '白', en: 'White' },
  colorOther: { 'zh-CN': '其他', en: 'Other' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  startDate: { 'zh-CN': '开始日期', en: 'Start date' },
  endDate: { 'zh-CN': '结束日期', en: 'End date' },
  rangeMaxYear: { 'zh-CN': '查询区间不能超过 1 年', en: 'Query range cannot exceed 1 year' },
  copySuccess: { 'zh-CN': '已复制车牌：{plate}', en: 'Plate copied: {plate}' },
  copyFailed: { 'zh-CN': '复制失败，请重试', en: 'Copy failed, try again' }
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

interface LaneOption {
  id: number
  name: string
}

interface SessionRow {
  id: number
  lotId: number
  lotName: string
  plateNumber: string
  plateColor: string | null
  status: 'OPEN' | 'CLOSED' | 'VOIDED'
  entryTime: string
  entryLaneId: number | null
  entryLaneName: string | null
  exitTime: string | null
  exitLaneId: number | null
  exitLaneName: string | null
  feeYuan: number | null
  payStatus: 'UNPAID' | 'PARTIAL' | 'PAID' | 'FREE' | null
  payTime: string | null
  parkedMinutes: number | null
}

const route = useRoute()

const lots = ref<LotOption[]>([])
const lanes = ref<LaneOption[]>([])
const rows = ref<SessionRow[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<SessionRow | null>(null)
const saving = ref(false)

/** 登记支付小弹窗状态：仅对已出场流水开放三态登记（未支付/部分支付/已支付）。 */
const payDialogVisible = ref(false)
const payRow = ref<SessionRow | null>(null)
const payStatus = ref<'UNPAID' | 'PARTIAL' | 'PAID'>('UNPAID')
const paySaving = ref(false)

interface Filters {
  lotId: number | undefined
  keyword: string
  status: '' | 'OPEN' | 'CLOSED' | 'VOIDED'
}

const filters = reactive<Filters>({
  lotId: undefined,
  keyword: '',
  status: ''
})

const pager = reactive({ page: 1, size: 10 })

/** 系统配置时区（缓存），默认 Asia/Shanghai；手动进出场默认时间按其口径生成。须在 timeRange 初始化前声明。 */
let systemTimezone = 'Asia/Shanghai'

/** 查询时间区间（默认最近1个月；清空自动回退默认，最长可查 1 年）。null 仅出现在清空瞬间，随即回退默认。 */
const timeRange = ref<[string, string] | null>(defaultRange())
let lastValidRange: [string, string] = defaultRange()

const queryLotId = computed(() => {
  const raw = route.query.lotId
  const value = typeof raw === 'string' ? Number(raw) : Number.NaN
  return Number.isFinite(value) && value > 0 ? value : undefined
})

const statusOptions = computed(() => [
  { value: 'OPEN' as const, label: t('statusOpen') },
  { value: 'CLOSED' as const, label: t('statusClosed') },
  { value: 'VOIDED' as const, label: t('statusVoided') }
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

function statusLabel(value: string): string {
  if (value === 'OPEN') return t('statusOpen')
  if (value === 'CLOSED') return t('statusClosed')
  return t('statusVoided')
}

function statusTagType(value: string): 'success' | 'info' | 'danger' {
  if (value === 'OPEN') return 'success'
  if (value === 'CLOSED') return 'info'
  return 'danger'
}

type PayStatusValue = 'UNPAID' | 'PARTIAL' | 'PAID'

/** 弹窗三态可登记选项；「免缴费(FREE)」为展示派生态，不作为可登记状态。 */
const payStatusOptions = computed<{ value: PayStatusValue; label: string }[]>(() => [
  { value: 'UNPAID', label: t('payUnpaid') },
  { value: 'PARTIAL', label: t('payPartial') },
  { value: 'PAID', label: t('payPaid') }
])

function payStatusLabel(value: SessionRow['payStatus']): string {
  if (value === 'FREE') return t('payFree')
  return payStatusOptions.value.find((item) => item.value === value)?.label ?? '—'
}

function payTagType(value: SessionRow['payStatus']): 'info' | 'warning' | 'success' | 'primary' {
  if (value === 'PAID') return 'success'
  if (value === 'PARTIAL') return 'warning'
  if (value === 'FREE') return 'primary'
  return 'info'
}

/** 站点「收费金额单位」：中文=元…，英文=币种代码，随系统配置联动 */
const currency = useDefaultCurrency()
const moneyUnit = computed(() => currency.view.value.unit)

/** 停车时长展示：如 2小时10分 / 1天2小时；已作废等无时长显示 — */
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

/** 时间展示：与白名单生效时间口径一致，去掉 ISO 的 T 并统一精确到秒（yyyy-MM-dd HH:mm:ss）。 */
function fmtDateTime(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const text = value.includes('T') ? value.replace('T', ' ') : value
  const clean = text.split('.')[0]
  return clean.length === 16 ? `${clean}:00` : clean
}

/** 应收金额展示：未计费（null）显示 —，已结算显示“金额 + 单位” */
function feeText(value: number | null): string {
  if (value == null) {
    return '—'
  }
  const fixed = Number(value).toFixed(2)
  const amount = fixed.replace(/\.?0+$/, '').replace(/\.$/, '') || '0'
  return `${amount} ${moneyUnit.value}`
}

/** 读取系统配置时区：成功后同步到 systemTimezone，用于时间口径计算 */
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

/** 生成系统配置时区下的“当前时间”文本（YYYY-MM-DDTHH:mm:ss），避免用浏览器本地时间 */
function nowFormatted(): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: systemTimezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).formatToParts(new Date())
  const map = new Map(parts.map((part) => [part.type, part.value]))
  const hour = map.get('hour') === '24' ? '00' : (map.get('hour') ?? '00')
  return `${map.get('year')}-${map.get('month')}-${map.get('day')}T${hour}:${map.get('minute')}:${map.get('second')}`
}

/** 时间区间相关工具：默认最近 1 个月（月末按目标月天数收敛），最长跨度校验为自然年 1 年。 */
function pad2(value: number): string {
  return String(value).padStart(2, '0')
}

function parseDate(text: string): Date {
  const [y, m, d] = text.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** 站点时区下“今天”的年/月/日：与页面时间口径、后端换算保持一致，避免浏览器时区偏差。 */
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

/** 默认区间：今天与回退 1 个自然月（YYYY-MM-DD），按站点时区计算。 */
function defaultRange(): [string, string] {
  const { y, m, d } = siteTodayParts()
  const index = y * 12 + (m - 1) - 1
  const py = Math.floor(index / 12)
  const pm = (index % 12) + 1
  return [`${py}-${pad2(pm)}-${pad2(Math.min(d, lastDayOfMonth(py, pm)))}`, `${y}-${pad2(m)}-${pad2(d)}`]
}

/** 是否超过最长可查区间（起点早于终点往回 1 年）。 */
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

/** 时间区间变更：非法跨度（>1年）拦截并回退；清空自动回退默认最近 1 个月；合法则立即查询。 */
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

/** 点击车牌复制车牌号到剪贴板。 */
async function handleCopyPlate(row: SessionRow) {
  const text = (row.plateNumber ?? '').trim()
  if (!text) {
    return
  }
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const area = document.createElement('textarea')
      area.value = text
      area.style.position = 'fixed'
      area.style.opacity = '0'
      document.body.appendChild(area)
      area.select()
      document.execCommand('copy')
      document.body.removeChild(area)
    }
    ElMessage.success(t('copySuccess').replace('{plate}', text))
  } catch {
    ElMessage.error(t('copyFailed'))
  }
}

async function loadLots() {
  try {
    lots.value = await request.get<never, LotOption[]>('/lots')
    if (filters.lotId === undefined && queryLotId.value !== undefined) {
      filters.lotId = queryLotId.value
    }
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('loadFailed'))
  }
}

async function loadLanes(lotId: number | undefined) {
  lanes.value = []
  if (!lotId) {
    return
  }
  try {
    lanes.value = await request.get<never, LaneOption[]>('/lanes', {
      params: { lotId }
    })
  } catch {
    // 通道加载失败不阻塞流水页面
  }
}

async function loadRows() {
  loading.value = true
  try {
    const range = timeRange.value ?? defaultRange()
    const data = await request.get<never, PageResult<SessionRow>>('/parking-sessions', {
      params: {
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
  filters.lotId = queryLotId.value
  filters.keyword = ''
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

interface SessionForm {
  lotId: number | undefined
  plateNumber: string
  plateColor: string
  entryTime: string
  entryLaneId: number | undefined
  entryLaneName: string
  enableExit: boolean
  exitTime: string
  exitLaneId: number | undefined
  exitLaneName: string
}

const formRef = ref<FormInstance>()
const form = reactive<SessionForm>({
  lotId: undefined,
  plateNumber: '',
  plateColor: 'BLUE',
  entryTime: nowFormatted(),
  entryLaneId: undefined,
  entryLaneName: '',
  enableExit: false,
  exitTime: nowFormatted(),
  exitLaneId: undefined,
  exitLaneName: ''
})

const formTitle = computed(() => (editing.value ? t('editTitle') : t('createTitle')))
const isOpenSession = computed(() => editing.value?.status === 'OPEN')

const rules: FormRules<SessionForm> = {
  lotId: [{ required: true, message: d.lot['zh-CN'], trigger: 'change' }],
  plateNumber: [{ required: true, message: d.plate['zh-CN'], trigger: 'blur' }]
}

function laneNameOf(id: number | undefined): string {
  const found = lanes.value.find((item) => item.id === id)
  return found ? found.name : ''
}

function openCreate() {
  editing.value = null
  form.lotId = filters.lotId ?? undefined
  form.plateNumber = ''
  form.plateColor = 'BLUE'
  form.entryTime = nowFormatted()
  form.entryLaneId = undefined
  form.entryLaneName = ''
  form.enableExit = false
  form.exitTime = nowFormatted()
  form.exitLaneId = undefined
  form.exitLaneName = ''
  loadLanes(form.lotId)
  dialogVisible.value = true
}

function openEdit(row: SessionRow) {
  editing.value = row
  form.lotId = row.lotId
  form.plateNumber = row.plateNumber
  form.plateColor = row.plateColor ?? 'BLUE'
  form.entryTime = row.entryTime ?? nowFormatted()
  form.entryLaneId = row.entryLaneId ?? undefined
  form.entryLaneName = row.entryLaneName ?? ''
  form.enableExit = row.status === 'OPEN' ? false : true
  form.exitTime = row.exitTime ?? nowFormatted()
  form.exitLaneId = row.exitLaneId ?? undefined
  form.exitLaneName = row.exitLaneName ?? ''
  loadLanes(row.lotId)
  dialogVisible.value = true
}

function handleLotChange(lotId: number | undefined) {
  form.entryLaneId = undefined
  form.entryLaneName = ''
  form.exitLaneId = undefined
  form.exitLaneName = ''
  loadLanes(lotId)
}

async function submitForm() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!editing.value && form.enableExit) {
    if (!form.exitTime) {
      ElMessage.warning(t('exitTimeRequired'))
      return
    }
    if (form.exitTime <= form.entryTime) {
      ElMessage.warning(t('timeOrderExit'))
      return
    }
  }
  saving.value = true
  try {
    if (editing.value) {
      const payload: Record<string, unknown> = {
        plateNumber: form.plateNumber,
        plateColor: form.plateColor,
        entryTime: form.entryTime,
        entryLaneId: form.entryLaneId ?? null,
        entryLaneName: form.entryLaneName || laneNameOf(form.entryLaneId) || null,
        entryImage: null
      }
      if (form.enableExit && form.exitTime) {
        payload.exitTime = form.exitTime
        payload.exitLaneId = form.exitLaneId ?? null
        payload.exitLaneName = form.exitLaneName || laneNameOf(form.exitLaneId) || null
        payload.exitImage = null
      }
      await request.put(`/parking-sessions/${editing.value.id}`, payload)
    } else {
      const payload: Record<string, unknown> = {
        lotId: form.lotId,
        plateNumber: form.plateNumber,
        plateColor: form.plateColor,
        entryTime: form.entryTime,
        entryLaneId: form.entryLaneId ?? null,
        entryLaneName: form.entryLaneName || laneNameOf(form.entryLaneId) || null,
        entryImage: null
      }
      if (form.enableExit && form.exitTime) {
        payload.exitTime = form.exitTime
        payload.exitLaneId = form.exitLaneId ?? null
        payload.exitLaneName = form.exitLaneName || laneNameOf(form.exitLaneId) || null
        payload.exitImage = null
      }
      await request.post('/parking-sessions', payload)
    }
    dialogVisible.value = false
    ElMessage.success(editing.value ? t('updateSuccess') : t('createSuccess'))
    loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  } finally {
    saving.value = false
  }
}

async function handleVoid(row: SessionRow) {
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
    loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  }
}

/**
 * 重新算费：先调只读预览接口算出金额并弹窗展示（当前值 → 新值）。
 * 已出场按真实出场时间；在场按「入场 ~ 当前时刻」估算。
 * 用户确认后才调用 /recalculate 真正落库，取消则不做任何修改。
 */
async function handleRecalc(row: SessionRow) {
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
    loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  }
}

/** 打开支付登记弹窗：回显当前登记状态；仅已出场且非「免缴费」流水可登记。 */
function openPay(row: SessionRow) {
  payRow.value = row
  const current = row.payStatus
  payStatus.value = current === 'PAID' || current === 'PARTIAL' ? current : 'UNPAID'
  payDialogVisible.value = true
}

/** 提交支付登记：三态由人工选择；费用重算/编辑不会自动改变支付状态。 */
async function submitPay() {
  if (!payRow.value) {
    return
  }
  paySaving.value = true
  try {
    await request.post(`/parking-sessions/${payRow.value.id}/pay-status`, { status: payStatus.value })
    ElMessage.success(t('payUpdated'))
    payDialogVisible.value = false
    loadRows()
  } catch (error) {
    ElMessage.error((error as Error)?.message ?? t('reqFailed'))
  } finally {
    paySaving.value = false
  }
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
        style="width: 220px"
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
        style="width: 160px"
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
      <div class="toolbar-spacer" />
      <el-button type="primary" plain @click="openCreate">{{ t('manualEntry') }}</el-button>
    </div>

    <div class="page-card">
      <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
        <el-table-column :label="t('plate')" min-width="150">
          <template #default="{ row }">
            <el-tooltip :disabled="!row.plateColor" :content="plateColorLabel(row.plateColor)" placement="top">
              <span class="plate-badge" :style="plateBadgeStyle(row.plateColor)" @click="handleCopyPlate(row)">
                {{ row.plateNumber }}
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column :label="t('status')" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" disable-transitions>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lotName" :label="t('lot')" min-width="150" show-overflow-tooltip />
        <el-table-column :label="t('entryTime')" min-width="170">
          <template #default="{ row }">
            {{ fmtDateTime(row.entryTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="entryLaneName" :label="t('entryLane')" min-width="130" show-overflow-tooltip />
        <el-table-column :label="t('exitTime')" min-width="170">
          <template #default="{ row }">
            {{ fmtDateTime(row.exitTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="exitLaneName" :label="t('exitLane')" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.exitLaneName ?? '—' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('duration')" min-width="120">
          <template #default="{ row }">
            {{ durationText(row.parkedMinutes) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('fee')" min-width="120">
          <template #default="{ row }">
            <span :class="{ 'fee-cell': row.feeYuan != null }">{{ feeText(row.feeYuan) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('payStatus')" min-width="110">
          <template #default="{ row }">
            <el-tag v-if="row.payStatus" :type="payTagType(row.payStatus)" disable-transitions>
              {{ payStatusLabel(row.payStatus) }}
            </el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('payTime')" min-width="165">
          <template #default="{ row }">
            {{ row.payTime ?? '—' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('actions')" min-width="230" fixed="right">
          <template #default="{ row }">
            <div class="actions-cell">
              <el-button v-if="row.status !== 'VOIDED'" link type="primary" @click="openEdit(row)">
                {{ t('edit') }}
              </el-button>
              <el-button v-if="row.status !== 'VOIDED'" link type="primary" @click="handleRecalc(row)">
                {{ t('recalc') }}
              </el-button>
              <el-button v-if="row.status === 'CLOSED' && row.payStatus !== 'FREE'" link type="primary" @click="openPay(row)">
                {{ t('payReg') }}
              </el-button>
              <el-button v-if="row.status !== 'VOIDED'" link type="danger" @click="handleVoid(row)">
                {{ t('void') }}
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

    <el-dialog v-model="dialogVisible" :title="formTitle" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item :label="t('lot')" prop="lotId">
          <el-select v-model="form.lotId" :disabled="!!editing" placeholder="..." style="width: 100%" @change="handleLotChange">
            <el-option v-for="item in lots" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('plate')" prop="plateNumber">
          <el-input v-model="form.plateNumber" :placeholder="t('plate')" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('plateColor')">
          <el-select v-model="form.plateColor" style="width: 100%">
            <el-option v-for="item in colorOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('entryTime')" prop="entryTime">
          <el-date-picker
            v-model="form.entryTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item :label="t('entryLane')">
          <el-select v-model="form.entryLaneId" clearable :placeholder="t('noLane')" style="width: 100%">
            <el-option v-for="item in lanes" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="!editing || isOpenSession">
          <el-checkbox v-model="form.enableExit">{{ t('closeNow') }}</el-checkbox>
        </el-form-item>
        <template v-if="form.enableExit || (editing && !isOpenSession)">
          <el-form-item :label="t('exitTime')">
            <el-date-picker
              v-model="form.exitTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item :label="t('exitLane')">
            <el-select v-model="form.exitLaneId" clearable :placeholder="t('noLane')" style="width: 100%">
              <el-option v-for="item in lanes" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">{{ t('confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payDialogVisible" :title="t('payTitle')" width="440px" destroy-on-close>
      <p v-if="payRow" class="pay-dialog-tip">
        {{ t('payMsg').replace('{plate}', payRow.plateNumber).replace('{fee}', feeText(payRow.feeYuan)) }}
      </p>
      <el-radio-group v-model="payStatus">
        <el-radio v-for="item in payStatusOptions" :key="item.value" :value="item.value">
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

.toolbar-spacer {
  flex: 1;
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

.plate-badge:hover {
  filter: brightness(1.08);
  transform: translateY(-1px);
}

.fee-cell {
  font-weight: 600;
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

.pay-dialog-tip {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}
</style>
