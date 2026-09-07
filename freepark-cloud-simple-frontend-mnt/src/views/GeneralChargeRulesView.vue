<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { useDefaultCurrency } from '../utils/currency'
import SimulateChargeDialog from '../components/SimulateChargeDialog.vue'

const d: BiDict = {
  title: { 'zh-CN': '24小时制计费规则', en: '24-Hour Billing Rules' },
  hintTitle: {
    'zh-CN': '规则口径',
    en: 'How rules apply'
  },
  hintBody: {
    'zh-CN': '以 24 小时为一个计费周期。计价支持两种方式：「固定周期单价」（周期时长 × 周期单价），或选用全局「计费周期」方案按分段费率逐段累计（分段用尽后不再收费）。费用达到每 24 小时封顶金额后不再上浮（0 = 不封顶）。可通过「周计划」逐天（周一~周日）控制当天是否计费：未配置的天默认全天收费，配置收费时段的天仅时段内计费，标记「当天免费」则整天不收费；开启「周六日/节假日免费」时对应日期整天免费、优先于周计划。本页维护的是全局计费模板，不直接归属车场或车牌颜色；在「车场管理」每行的“计费配置”中按车场选用模板，并指定适用车牌颜色与生效起止日期（同车场同颜色的生效区间不可重叠）。',
    en: 'Billing runs in 24-hour cycles. Two pricing modes: a fixed cycle (length × unit price), or a global Billing Cycle profile charged segment by segment (no charge once the segments are fully consumed). Total fee never exceeds the per-24h ceiling (0 = no cap). A weekly plan (Mon–Sun) controls which days charge: days with no settings charge all day; days with windows charge only inside them; days marked “free” are not charged at all. When Weekend/Holiday free is on, those calendar days are free and take priority over the weekly plan. This page keeps global templates that are not bound to any lot or color; lots pick them in “Billing Config” under Lot Management, choosing the plate color and an effective date range (a lot cannot have overlapping ranges for the same color).'
  },
  addRule: { 'zh-CN': '新增模板', en: 'Add template' },
  addTitle: { 'zh-CN': '新增计费模板', en: 'Add billing template' },
  editTitle: { 'zh-CN': '编辑计费模板', en: 'Edit billing template' },
  colTitle: { 'zh-CN': '模板标题', en: 'Title' },
  colFee: { 'zh-CN': '计费周期配置', en: 'Billing cycle & fee' },
  colFree: { 'zh-CN': '免费时长', en: 'Free time' },
  colGrace: { 'zh-CN': '再次计费宽限', en: 'Re-billing grace' },
  colDays: { 'zh-CN': '免费日判定', en: 'Free days' },
  weekendFree: { 'zh-CN': '周末免费', en: 'Weekend free' },
  weekendCharge: { 'zh-CN': '周末收费', en: 'Weekend charge' },
  holidayFree: { 'zh-CN': '假期免费', en: 'Holiday free' },
  holidayCharge: { 'zh-CN': '假期收费', en: 'Holiday charge' },
  colPlan: { 'zh-CN': '周计划（收费时段）', en: 'Weekly charge plan' },
  allDayDefault: { 'zh-CN': '周一~周日 全天收费', en: 'Mon–Sun all-day charging' },
  restAllDay: { 'zh-CN': '其余 全天收费', en: 'others all-day' },
  freeSuffix: { 'zh-CN': '免费', en: ' free' },
  planLabel: { 'zh-CN': '周计划收费', en: 'Weekly plan' },
  planTip: {
    'zh-CN': '为每周每一天选择当天口径：默认「全天收费」；需要限制收费时间选「指定收费时段」（可添加多个不重叠的时段）；某天想整天不收费选「当天免费」。',
    en: 'Choose a mode for each weekday: default all-day charging; pick specific charge windows (add multiple non-overlapping ones); or mark the day as free.'
  },
  planPriorityTip: {
    'zh-CN': '已开启「周六日/节假日免费」：命中免费开关的整天将按免费处理，优先于上方周计划。',
    en: 'Weekend/Holiday free is on: those calendar days are free as a whole and take priority over the plan above.'
  },
  modeAll: { 'zh-CN': '全天收费', en: 'All day' },
  modeWindows: { 'zh-CN': '指定收费时段', en: 'Charge windows' },
  modeFree: { 'zh-CN': '当天免费', en: 'Free day' },
  addWindow: { 'zh-CN': '添加时段', en: 'Add window' },
  removeWindow: { 'zh-CN': '移除', en: 'Remove' },
  startAt: { 'zh-CN': '开始', en: 'Start' },
  endAt: { 'zh-CN': '结束', en: 'End' },
  planInvalid: {
    'zh-CN': '请完善周计划：标记「指定收费时段」的天至少需要一个有效时段',
    en: 'Fix the weekly plan: a day set to “charge windows” needs at least one valid window'
  },
  day1: { 'zh-CN': '周一', en: 'Mon' },
  day2: { 'zh-CN': '周二', en: 'Tue' },
  day3: { 'zh-CN': '周三', en: 'Wed' },
  day4: { 'zh-CN': '周四', en: 'Thu' },
  day5: { 'zh-CN': '周五', en: 'Fri' },
  day6: { 'zh-CN': '周六', en: 'Sat' },
  day7: { 'zh-CN': '周日', en: 'Sun' },
  capText: { 'zh-CN': '24h 封顶', en: '24h cap' },
  noCap: { 'zh-CN': '不封顶', en: 'No cap' },
  unitMinute: { 'zh-CN': '分钟', en: 'min' },
  none: { 'zh-CN': '无', en: 'None' },
  perCyclePattern: {
    'zh-CN': '{money} {unit} / 每 {cycle} 分钟',
    en: '{money} {unit} / every {cycle} min'
  },
  feeMode: { 'zh-CN': '计费方式', en: 'Billing mode' },
  feeModeFixed: { 'zh-CN': '固定周期单价', en: 'Fixed cycle & rate' },
  feeModeProfile: { 'zh-CN': '选用计费周期方案', en: 'Use a billing cycle profile' },
  formProfile: { 'zh-CN': '计费周期方案', en: 'Billing cycle profile' },
  formProfileTip: {
    'zh-CN': '计费时按所选方案的分段费率逐段累计（方案在「计费周期」页维护，全局共享）。',
    en: 'Fees accumulate per the selected profile’s segment rates (profiles are maintained under Billing Cycles).'
  },
  profilePlaceholder: { 'zh-CN': '选择计费周期方案', en: 'Select a billing cycle profile' },
  needProfile: { 'zh-CN': '请选择计费周期方案', en: 'Please select a billing cycle profile' },
  noProfileTip: {
    'zh-CN': '暂无计费周期方案，请先到「计费周期」页面创建。',
    en: 'No billing cycle profile yet. Create one under “Billing Cycles” first.'
  },
  profileSegTag: { 'zh-CN': '分段费率', en: 'Segment rates' },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  simulate: { 'zh-CN': '模拟算费', en: 'Simulate' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  formTitle: { 'zh-CN': '模板标题', en: 'Title' },
  formDesc: { 'zh-CN': '模板描述', en: 'Description' },
  descPlaceholder: { 'zh-CN': '计费说明（可选，最多 255 字）', en: 'Notes about this template (optional, max 255 chars)' },
  formFree: { 'zh-CN': '免费时长', en: 'Free duration' },
  formFreeTip: {
    'zh-CN': '计费开始前不收费的时长（分钟）。0 = 无免费。',
    en: 'Free minutes before charging starts. 0 = no free time.'
  },
  formGrace: { 'zh-CN': '再次计费时长', en: 'Re-billing duration' },
  formGraceTip: {
    'zh-CN': '场内缴费后离场宽限（分钟）：宽限时间内不再产生费用。0 = 无宽限。',
    en: 'Exit grace after in-lot payment (minutes): no extra charge within it. 0 = none.'
  },
  formWeekend: { 'zh-CN': '周六/周日免费', en: 'Weekends free' },
  formHoliday: { 'zh-CN': '假期免费', en: 'Holidays free' },
  formCycle: { 'zh-CN': '计费周期时长', en: 'Cycle length' },
  formCycleTip: {
    'zh-CN': '一个计费档的时长（分钟），如 60 表示每小时一档。',
    en: 'Length of one billing unit in minutes, e.g. 60 = per hour.'
  },
  formPrice: { 'zh-CN': '周期单价', en: 'Unit price' },
  formPriceTip: {
    'zh-CN': '每个计费周期（上一项时长）对应的金额，单位见「收费金额单位」配置。',
    en: 'Fee charged per billing cycle (duration above); unit per the fee-currency setting.'
  },
  formCap: { 'zh-CN': '每24小时封顶', en: 'Per-24h cap' },
  formCapTip: {
    'zh-CN': '任意连续 24 小时累计不超过该金额，单位见「收费金额单位」配置。0 = 不封顶。',
    en: 'Total fee never exceeds this amount within any 24 hours; unit per the fee-currency setting. 0 = no cap.'
  },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  needTitle: { 'zh-CN': '请填写模板标题', en: 'Please enter a template title' },
  needNumbers: { 'zh-CN': '请检查时长与金额的取值', en: 'Please check durations and amounts' },
  createSuccess: { 'zh-CN': '已添加', en: 'Added' },
  updateSuccess: { 'zh-CN': '已更新', en: 'Updated' },
  deleteSuccess: { 'zh-CN': '已删除', en: 'Deleted' },
  loadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load, try again' },
  reqFailed: { 'zh-CN': '操作失败，请重试', en: 'Operation failed, try again' },
  deleteTitle: { 'zh-CN': '删除计费模板', en: 'Delete billing template' },
  deleteMsg: {
    'zh-CN': '确定删除该模板吗？若该模板仍被车场“计费配置”引用将无法删除。',
    en: 'Delete this template? A template still referenced by any lot’s Billing Config cannot be deleted.'
  },
  noRules: { 'zh-CN': '暂无计费模板', en: 'No billing template yet' },
  noRulesHint: {
    'zh-CN': '点击右上角“新增模板”创建全局模板；再到「车场管理」每行的“计费配置”中为车场选用。',
    en: 'Click “Add template” on the top right to create a global template; then pick it per lot under “Billing Config” in Lot Management.'
  }
}

const { t } = useBiText(d)

/** 站点「收费金额单位」：默认 CNY，随系统配置联动 */
const currency = useDefaultCurrency()
const moneyUnit = computed(() => currency.view.value.unit)

interface GeneralSlot {
  id: number
  weekday: number
  allDayFree: boolean
  startMinute: number | null
  endMinute: number | null
}

interface RuleRow {
  id: number
  title: string
  description: string | null
  freeMinutes: number
  graceMinutes: number
  weekendFree: boolean
  holidayFree: boolean
  cycleMinutes: number
  unitPriceYuan: number
  capPer24hYuan: number
  cycleProfileId: number | null
  slots: GeneralSlot[]
  createdAt: string
  updatedAt: string
}

interface ProfileOption {
  id: number
  name: string
  description: string | null
}

interface WindowUI {
  start: string
  end: string
}

interface PlanDay {
  weekday: number
  mode: 'all' | 'windows' | 'free'
  windows: WindowUI[]
}

function pad2(value: number): string {
  return String(value).padStart(2, '0')
}

/** 分钟（0..1440）转 "HH:mm"，1440 显示为 "24:00" */
function fmtMinute(minute: number): string {
  const m = Math.max(0, Math.min(1440, Math.round(minute)))
  return `${pad2(Math.floor(m / 60))}:${pad2(m % 60)}`
}

/** "HH:mm" 转分钟（"24:00" = 1440） */
function parseMinute(text: string): number {
  if (text === '24:00') {
    return 1440
  }
  const parts = text.split(':')
  return Number(parts[0]) * 60 + Number(parts[1] ?? 0)
}

/** 起点可选时刻：00:00 ~ 23:45（每 15 分钟） */
const START_TIMES: string[] = (() => {
  const times: string[] = []
  for (let m = 0; m < 1440; m += 15) {
    times.push(fmtMinute(m))
  }
  return times
})()

/** 给定开始时间后的可选取结束时刻：start+15 ~ 24:00 */
function endTimesAfter(startText: string): string[] {
  const start = parseMinute(startText)
  const times: string[] = []
  for (let m = start + 15; m <= 1440; m += 15) {
    times.push(fmtMinute(m))
  }
  return times.length > 0 ? times : ['24:00']
}

const profiles = ref<ProfileOption[]>([])
const rows = ref<RuleRow[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<RuleRow | null>(null)
const saving = ref(false)
const simulateVisible = ref(false)
const simulateRule = ref<RuleRow | null>(null)

const form = reactive({
  title: '',
  description: '',
  freeMinutes: 0,
  graceMinutes: 0,
  weekendFree: true,
  holidayFree: true,
  cycleMinutes: 60,
  unitPriceYuan: 5,
  capPer24hYuan: 0,
  chargeMode: 'fixed' as 'fixed' | 'profile',
  cycleProfileId: null as number | null
})
const plan = ref<PlanDay[]>([])

function defaultPlan(): PlanDay[] {
  return Array.from({ length: 7 }, (_, index) => ({
    weekday: index + 1,
    mode: 'all' as const,
    windows: []
  }))
}

/** 由后端 slot 行还原出每天的三态计划 */
function planFromSlots(slots: GeneralSlot[]): PlanDay[] {
  return defaultPlan().map((day) => {
    const entries = slots.filter((slot) => slot.weekday === day.weekday)
    if (entries.length === 0) {
      return day
    }
    if (entries.some((entry) => entry.allDayFree)) {
      return { ...day, mode: 'free', windows: [] }
    }
    return {
      ...day,
      mode: 'windows',
      windows: entries.map((entry) => ({
        start: fmtMinute(entry.startMinute ?? 0),
        end: fmtMinute(entry.endMinute ?? 0)
      }))
    }
  })
}

function addWindow(day: PlanDay): void {
  day.windows.push({ start: '08:00', end: '20:00' })
}

function removeWindow(day: PlanDay, index: number): void {
  day.windows.splice(index, 1)
}

/** 起点被改小后保证终点仍晚于起点 */
function handleStartChange(window: WindowUI): void {
  if (parseMinute(window.end) <= parseMinute(window.start)) {
    window.end = fmtMinute(Math.min(parseMinute(window.start) + 15, 1440))
  }
}

function onModeChange(day: PlanDay): void {
  if (day.mode !== 'windows') {
    day.windows = []
  }
}

const dialogTitle = computed(() => (editing.value ? t('editTitle') : t('addTitle')))

function errorTextOf(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function fmtMoney(value: number): string {
  const fixed = Number(value ?? 0).toFixed(2)
  return fixed.replace(/\.?0+$/, '').replace(/\.$/, '') || '0'
}

function minuteText(value: number): string {
  return value > 0 ? `${value} ${t('unitMinute')}` : t('none')
}

function perCycleText(row: RuleRow): string {
  return t('perCyclePattern')
    .replace('{money}', fmtMoney(row.unitPriceYuan))
    .replace('{cycle}', String(row.cycleMinutes))
    .replace('{unit}', moneyUnit.value)
}

function capText(row: RuleRow): string {
  return row.capPer24hYuan > 0
    ? `${t('capText')} ${fmtMoney(row.capPer24hYuan)} ${moneyUnit.value}`
    : t('noCap')
}

function profileById(id: number | null): ProfileOption | undefined {
  if (id == null) {
    return undefined
  }
  return profiles.value.find((profile) => profile.id === id)
}

/** 计价主文案：选用方案时显示方案名，否则显示固定周期单价 */
function feeMain(row: RuleRow): string {
  const profile = profileById(row.cycleProfileId)
  return profile ? profile.name : perCycleText(row)
}

/** 计价辅助文案：选用方案时标注「分段费率」，固定周期时为封顶说明 */
function feeCap(row: RuleRow): string {
  const profile = profileById(row.cycleProfileId)
  const base = capText(row)
  return profile ? `${t('profileSegTag')} · ${base}` : base
}

function weekdayLabel(row: RuleRow): string {
  return row.weekendFree ? t('weekendFree') : t('weekendCharge')
}

function holidayLabel(row: RuleRow): string {
  return row.holidayFree ? t('holidayFree') : t('holidayCharge')
}

/** 周计划摘要：只列出非默认的天，其余补充「全天收费」 */
function planSummary(row: RuleRow): string {
  const segments: string[] = []
  const coveredDays = new Set<number>()
  for (let weekday = 1; weekday <= 7; weekday += 1) {
    const entries = (row.slots ?? []).filter((slot) => slot.weekday === weekday)
    if (entries.length === 0) {
      continue
    }
    coveredDays.add(weekday)
    if (entries.some((entry) => entry.allDayFree)) {
      segments.push(`${t(`day${weekday}`)}${t('freeSuffix')}`)
    } else {
      const windows = entries
        .map((entry) => `${fmtMinute(entry.startMinute ?? 0)}-${fmtMinute(entry.endMinute ?? 0)}`)
        .join('/')
      segments.push(`${t(`day${weekday}`)} ${windows}`)
    }
  }
  if (segments.length === 0) {
    return t('allDayDefault')
  }
  return coveredDays.size === 7
    ? segments.join('；')
    : `${segments.join('；')}；${t('restAllDay')}`
}

async function loadProfiles(): Promise<void> {
  try {
    const list = await request.get<never, ProfileOption[]>('/billing/cycle-profiles')
    profiles.value = list ?? []
  } catch {
    // 读取失败不阻塞主流程，方案下拉可能为空
  }
}

async function loadRules(): Promise<void> {
  loading.value = true
  try {
    const list = await request.get<never, RuleRow[]>('/billing/general-rules')
    rows.value = list ?? []
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('loadFailed')))
  } finally {
    loading.value = false
  }
}

function openCreateDialog(): void {
  loadProfiles()
  editing.value = null
  form.title = ''
  form.description = ''
  form.freeMinutes = 0
  form.graceMinutes = 0
  form.weekendFree = true
  form.holidayFree = true
  form.cycleMinutes = 60
  form.unitPriceYuan = 5
  form.capPer24hYuan = 0
  form.chargeMode = 'fixed'
  form.cycleProfileId = null
  plan.value = defaultPlan()
  dialogVisible.value = true
}

function openEditDialog(row: RuleRow): void {
  loadProfiles()
  editing.value = row
  form.title = row.title
  form.description = row.description ?? ''
  form.freeMinutes = row.freeMinutes
  form.graceMinutes = row.graceMinutes
  form.weekendFree = row.weekendFree
  form.holidayFree = row.holidayFree
  form.cycleMinutes = row.cycleMinutes
  form.unitPriceYuan = Number(row.unitPriceYuan)
  form.capPer24hYuan = Number(row.capPer24hYuan)
  form.chargeMode = row.cycleProfileId ? 'profile' : 'fixed'
  form.cycleProfileId = row.cycleProfileId
  plan.value = planFromSlots(row.slots ?? [])
  dialogVisible.value = true
}

function openSimulateDialog(row: RuleRow): void {
  simulateRule.value = row
  simulateVisible.value = true
}

async function handleSubmit(): Promise<void> {
  if (!form.title.trim()) {
    ElMessage.warning(t('needTitle'))
    return
  }
  if (form.description.length > 255) {
    ElMessage.warning(t('reqFailed'))
    return
  }
  const cycle = Number(form.cycleMinutes)
  const free = Number(form.freeMinutes)
  const grace = Number(form.graceMinutes)
  const price = Number(form.unitPriceYuan ?? 0)
  const cap = Number(form.capPer24hYuan ?? 0)
  if (
    !Number.isFinite(cycle) || cycle < 1 || cycle > 1440 ||
    !Number.isFinite(free) || free < 0 || free > 1440 ||
    !Number.isFinite(grace) || grace < 0 || grace > 1440 ||
    !Number.isFinite(price) || price < 0 ||
    !Number.isFinite(cap) || cap < 0
  ) {
    ElMessage.warning(t('needNumbers'))
    return
  }
  if (form.chargeMode === 'profile' && form.cycleProfileId == null) {
    ElMessage.warning(t('needProfile'))
    return
  }
  // 周计划客户端校验：标记「指定收费时段」的天至少需要一个有效时段
  const slots: Array<{
    weekday: number
    allDayFree: boolean
    startMinute: number | null
    endMinute: number | null
  }> = []
  for (const day of plan.value) {
    if (day.mode === 'free') {
      slots.push({ weekday: day.weekday, allDayFree: true, startMinute: null, endMinute: null })
    } else if (day.mode === 'windows') {
      if (day.windows.length === 0) {
        ElMessage.warning(t('planInvalid'))
        return
      }
      for (const window of day.windows) {
        const start = parseMinute(window.start)
        const end = parseMinute(window.end)
        if (!Number.isInteger(start) || !Number.isInteger(end) || end <= start) {
          ElMessage.warning(t('planInvalid'))
          return
        }
        slots.push({ weekday: day.weekday, allDayFree: false, startMinute: start, endMinute: end })
      }
    }
  }

  const payload = {
    title: form.title.trim(),
    description: form.description.trim() || null,
    freeMinutes: free,
    graceMinutes: grace,
    weekendFree: form.weekendFree,
    holidayFree: form.holidayFree,
    cycleMinutes: cycle,
    unitPriceYuan: price,
    capPer24hYuan: cap,
    cycleProfileId: form.chargeMode === 'profile' ? form.cycleProfileId : null,
    slots
  }
  saving.value = true
  try {
    if (editing.value) {
      await request.put<never, RuleRow>(`/billing/general-rules/${editing.value.id}`, payload)
      ElMessage.success(t('updateSuccess'))
    } else {
      await request.post<never, RuleRow>('/billing/general-rules', payload)
      ElMessage.success(t('createSuccess'))
    }
    dialogVisible.value = false
    loadRules()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('reqFailed')))
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: RuleRow): Promise<void> {
  try {
    await ElMessageBox.confirm(`${t('deleteMsg')}\n${row.title}`, t('deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  try {
    await request.delete<never, void>(`/billing/general-rules/${row.id}`)
    ElMessage.success(t('deleteSuccess'))
    loadRules()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('reqFailed')))
  }
}

onMounted(() => {
  currency.load()
  loadRules()
})
</script>

<template>
  <section class="rules-page">
    <el-card class="panel" shadow="never">
      <el-alert type="info" :closable="false" class="hint-alert" show-icon>
        <template #title>
          <strong>{{ t('hintTitle') }}</strong>
        </template>
        <p class="hint-body">{{ t('hintBody') }}</p>
      </el-alert>

      <div class="toolbar">
        <span class="page-title">{{ t('title') }}</span>
        <div class="spacer" />
        <el-button type="primary" @click="openCreateDialog">
          {{ t('addRule') }}
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        class="rules-table"
      >
        <el-table-column :label="t('colTitle')" min-width="200">
          <template #default="{ row }">
            <div class="title-cell">
              <span class="rule-title">{{ (row as RuleRow).title }}</span>
              <span v-if="(row as RuleRow).description" class="rule-desc">
                {{ (row as RuleRow).description }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('colFee')" min-width="200">
          <template #default="{ row }">
            <div class="fee-cell">
              <span class="fee-main">{{ feeMain(row as RuleRow) }}</span>
              <span class="fee-cap">{{ feeCap(row as RuleRow) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('colFree')" width="110" align="center">
          <template #default="{ row }">{{ minuteText((row as RuleRow).freeMinutes) }}</template>
        </el-table-column>

        <el-table-column :label="t('colGrace')" width="120" align="center">
          <template #default="{ row }">{{ minuteText((row as RuleRow).graceMinutes) }}</template>
        </el-table-column>

        <el-table-column :label="t('colDays')" min-width="130">
          <template #default="{ row }">
            <div class="day-tags">
              <el-tag
                size="small"
                :type="(row as RuleRow).weekendFree ? 'success' : 'info'"
                effect="light"
              >
                {{ weekdayLabel(row as RuleRow) }}
              </el-tag>
              <el-tag
                size="small"
                :type="(row as RuleRow).holidayFree ? 'success' : 'info'"
                effect="light"
              >
                {{ holidayLabel(row as RuleRow) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('colPlan')" min-width="230">
          <template #default="{ row }">
            <span class="plan-text">{{ planSummary(row as RuleRow) }}</span>
          </template>
        </el-table-column>

        <el-table-column :label="t('actions')" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openSimulateDialog(row as RuleRow)">
              {{ t('simulate') }}
            </el-button>
            <el-button link type="primary" @click="openEditDialog(row as RuleRow)">
              {{ t('edit') }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row as RuleRow)">
              {{ t('delete') }}
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="t('noRules')" :image-size="80">
            <p class="empty-hint">{{ t('noRulesHint') }}</p>
          </el-empty>
        </template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="min(760px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="150px" label-position="left">
        <el-form-item :label="t('formTitle')">
          <el-input v-model="form.title" maxlength="80" class="dialog-field" clearable />
        </el-form-item>

        <el-form-item :label="t('formDesc')">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="255"
            :placeholder="t('descPlaceholder')"
            class="dialog-field"
          />
        </el-form-item>

        <el-form-item :label="t('planLabel')">
          <div class="plan-editor">
            <div v-for="day in plan" :key="day.weekday" class="plan-day">
              <span class="day-label">{{ t(`day${day.weekday}`) }}</span>
              <el-select v-model="day.mode" class="mode-select" @change="onModeChange(day)">
                <el-option :value="'all'" :label="t('modeAll')" />
                <el-option :value="'windows'" :label="t('modeWindows')" />
                <el-option :value="'free'" :label="t('modeFree')" />
              </el-select>
              <template v-if="day.mode === 'windows'">
                <div v-for="(window, index) in day.windows" :key="index" class="window-row">
                  <span class="window-label">{{ t('startAt') }}</span>
                  <el-select v-model="window.start" class="time-select" @change="handleStartChange(window)">
                    <el-option v-for="time in START_TIMES" :key="time" :value="time" :label="time" />
                  </el-select>
                  <span class="window-label">{{ t('endAt') }}</span>
                  <el-select v-model="window.end" class="time-select">
                    <el-option v-for="time in endTimesAfter(window.start)" :key="time" :value="time" :label="time" />
                  </el-select>
                  <el-button link type="danger" class="remove-btn" @click="removeWindow(day, index)">
                    {{ t('removeWindow') }}
                  </el-button>
                </div>
                <el-button link type="primary" class="add-btn" @click="addWindow(day)">
                  + {{ t('addWindow') }}
                </el-button>
              </template>
            </div>
            <p class="field-tip">{{ t('planTip') }}</p>
            <p v-if="form.weekendFree || form.holidayFree" class="field-tip warn-tip">
              {{ t('planPriorityTip') }}
            </p>
          </div>
        </el-form-item>

        <el-form-item :label="t('formFree')">
          <div class="number-row">
            <el-input-number
              v-model="form.freeMinutes"
              :min="0"
              :max="1440"
              :step="5"
              class="number-input"
            />
            <span class="unit-suffix">{{ t('unitMinute') }}</span>
          </div>
          <p class="field-tip">{{ t('formFreeTip') }}</p>
        </el-form-item>

        <el-form-item :label="t('formGrace')">
          <div class="number-row">
            <el-input-number
              v-model="form.graceMinutes"
              :min="0"
              :max="1440"
              :step="5"
              class="number-input"
            />
            <span class="unit-suffix">{{ t('unitMinute') }}</span>
          </div>
          <p class="field-tip">{{ t('formGraceTip') }}</p>
        </el-form-item>

        <el-form-item :label="t('feeMode')">
          <el-radio-group v-model="form.chargeMode">
            <el-radio value="fixed">{{ t('feeModeFixed') }}</el-radio>
            <el-radio value="profile">{{ t('feeModeProfile') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.chargeMode === 'fixed'">
          <el-form-item :label="t('formCycle')">
            <div class="number-row">
              <el-input-number
                v-model="form.cycleMinutes"
                :min="1"
                :max="1440"
                :step="5"
                class="number-input"
              />
              <span class="unit-suffix">{{ t('unitMinute') }}</span>
            </div>
            <p class="field-tip">{{ t('formCycleTip') }}</p>
          </el-form-item>

          <el-form-item :label="t('formPrice')">
            <div class="number-row">
              <el-input-number
                v-model="form.unitPriceYuan"
                :min="0"
                :max="99999999.99"
                :precision="2"
                :step="0.5"
                class="number-input"
              />
              <span class="unit-suffix">{{ moneyUnit }}</span>
            </div>
            <p class="field-tip">{{ t('formPriceTip') }}</p>
          </el-form-item>
        </template>

        <el-form-item v-else :label="t('formProfile')">
          <el-select
            v-model="form.cycleProfileId"
            class="dialog-field"
            :placeholder="t('profilePlaceholder')"
          >
            <el-option
              v-for="profile in profiles"
              :key="profile.id"
              :value="profile.id"
              :label="profile.name"
            />
          </el-select>
          <p v-if="profiles.length === 0" class="field-tip warn-tip">{{ t('noProfileTip') }}</p>
          <p v-else class="field-tip">{{ t('formProfileTip') }}</p>
        </el-form-item>

        <el-form-item :label="t('formCap')">
          <div class="number-row">
            <el-input-number
              v-model="form.capPer24hYuan"
              :min="0"
              :max="99999999.99"
              :precision="2"
              :step="10"
              class="number-input"
            />
            <span class="unit-suffix">{{ moneyUnit }}</span>
          </div>
          <p class="field-tip">{{ t('formCapTip') }}</p>
        </el-form-item>

        <el-form-item :label="t('formWeekend')">
          <el-switch v-model="form.weekendFree" />
        </el-form-item>
        <el-form-item :label="t('formHoliday')">
          <el-switch v-model="form.holidayFree" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">{{ t('confirm') }}</el-button>
      </template>
    </el-dialog>

    <SimulateChargeDialog
      v-model="simulateVisible"
      :base-path="'/billing/general-rules'"
      :rule-id="simulateRule?.id ?? null"
      :rule-title="simulateRule?.title ?? ''"
    />
  </section>
</template>

<style scoped>
.rules-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: fade-up 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.panel {
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-soft);
}

.hint-alert {
  margin-bottom: 4px;
}

.hint-body {
  margin: 4px 0 0;
  line-height: 1.6;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 8px 0 14px;
}

.page-title {
  font-size: 1.05rem;
  font-weight: 600;
  color: var(--fp-ink);
}

.spacer {
  flex: 1;
}

.rules-table {
  width: 100%;
}

.empty-hint {
  margin: 4px 0 0;
  max-width: 46ch;
  line-height: 1.6;
  color: var(--fp-muted);
}

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.4;
}

.rule-title {
  color: var(--fp-ink);
  font-weight: 600;
}

.rule-desc {
  font-size: 0.78rem;
  color: var(--fp-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 34ch;
}

.fee-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.4;
}

.fee-main {
  font-weight: 600;
  color: var(--fp-ink);
  white-space: nowrap;
}

.fee-cap {
  font-size: 0.78rem;
  color: var(--fp-muted);
  white-space: nowrap;
}

.day-tags {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.plan-text {
  font-size: 0.8rem;
  line-height: 1.6;
  color: var(--fp-ink);
}

.dialog-field {
  width: 100%;
}

.number-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.number-input {
  width: 220px;
}

.unit-suffix {
  color: var(--fp-muted);
  white-space: nowrap;
}

.field-tip {
  margin: 4px 0 0;
  max-width: 380px;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--fp-muted);
}

.warn-tip {
  color: #d03050;
}

.plan-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.plan-day {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.day-label {
  width: 44px;
  flex: none;
  font-weight: 600;
  color: var(--fp-ink);
}

.mode-select {
  width: 150px;
}

.window-row {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 8px;
}

.window-label {
  font-size: 0.78rem;
  color: var(--fp-muted);
  white-space: nowrap;
}

.time-select {
  width: 96px;
}

.remove-btn {
  padding: 4px 6px;
}

.add-btn {
  margin-left: 8px;
  padding: 4px 6px;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
