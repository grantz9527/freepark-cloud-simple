<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

type LotTypeOption = 'INTERNAL' | 'PUBLIC'
type RuleTypeValue = 'DAILY' | 'GENERAL'

interface LotView {
  id: number
  name: string
  code: string
  lotType: LotTypeOption
  address: string | null
  totalSpaces: number
  enabled: boolean
  mapData: string | null
  createdAt: string
  updatedAt: string
}

interface LotPayload {
  name: string
  lotType: LotTypeOption
  address?: string
  totalSpaces?: number
  enabled?: boolean
}

/** 车场计费配置（规则模板 × 车场 × 车牌颜色 × 生效区间） */
interface BindingRow {
  id: number
  lotId: number
  ruleType: RuleTypeValue
  ruleId: number
  ruleTitle: string | null
  plateColor: string | null
  effectiveFrom: string | null
  effectiveTo: string | null
  createdAt: string
  updatedAt: string
}

interface RuleOption {
  id: number
  title: string
}

interface SettingsView {
  allowedPlateColors: string[]
  supportedPlateColors: string[]
}

const LOT_TYPE_OPTIONS: LotTypeOption[] = ['INTERNAL', 'PUBLIC']

const d: BiDict = {
  title: { 'zh-CN': '停车场管理', en: 'Parking Lots' },
  searchPlaceholder: { 'zh-CN': '搜索车场名称 / 编码', en: 'Search name / code' },
  search: { 'zh-CN': '搜索', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  add: { 'zh-CN': '新增车场', en: 'Add Lot' },
  colName: { 'zh-CN': '车场名称', en: 'Lot Name' },
  colCode: { 'zh-CN': '车场编码', en: 'Lot Code' },
  colLotType: { 'zh-CN': '车场类型', en: 'Lot Type' },
  colAddress: { 'zh-CN': '地址', en: 'Address' },
  colTotalSpaces: { 'zh-CN': '车位总数', en: 'Total Spaces' },
  colStatus: { 'zh-CN': '状态', en: 'Status' },
  colUpdatedAt: { 'zh-CN': '更新时间', en: 'Updated At' },
  colActions: { 'zh-CN': '操作', en: 'Actions' },
  lotTypeInternal: { 'zh-CN': '内部车场', en: 'Internal' },
  lotTypePublic: { 'zh-CN': '公共车场', en: 'Public' },
  statusEnabled: { 'zh-CN': '启用', en: 'Enabled' },
  statusDisabled: { 'zh-CN': '停用', en: 'Disabled' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  map: { 'zh-CN': '车场地图', en: 'Map' },
  linkSpaces: { 'zh-CN': '车位管理', en: 'Spaces' },
  linkBooths: { 'zh-CN': '岗亭管理', en: 'Booths' },
  linkInternalVehicles: { 'zh-CN': '内部车辆', en: 'Internal Vehicles' },
  linkWhitelist: { 'zh-CN': '白名单', en: 'Whitelist' },
  linkBlacklist: { 'zh-CN': '黑名单', en: 'Blacklist' },
  linkPatternAllowlist: { 'zh-CN': '放行规则', en: 'Allowlist Patterns' },
  cfgLink: { 'zh-CN': '计费配置', en: 'Billing Config' },
  cfgTip: {
    'zh-CN': '为车场选用全局计费模板（每日制 / 24 小时制），每条配置指定适用车牌颜色与生效起止日期。同一车场同一颜色（不选颜色视为“默认”）的生效区间不可重叠，跨每日制 / 24 小时制同样拦截；不同颜色可同时生效，专属颜色优先于默认。',
    en: 'Pick a global billing template (Daily / 24-hour) for this lot. Each entry sets a plate color and an effective date range. A lot cannot have overlapping ranges for the same color (no color = “default”), even across Daily and 24-hour rules; different colors may coexist, and color-specific rules take priority over the default.'
  },
  cfgAdd: { 'zh-CN': '新增配置', en: 'Add Config' },
  cfgEmpty: { 'zh-CN': '暂无计费配置', en: 'No billing config yet' },
  cfgAddTitle: { 'zh-CN': '新增计费配置', en: 'New Billing Config' },
  cfgEditTitle: { 'zh-CN': '编辑计费配置', en: 'Edit Billing Config' },
  cfgColType: { 'zh-CN': '计费方式', en: 'Rule Type' },
  cfgColTemplate: { 'zh-CN': '规则模板', en: 'Rule Template' },
  cfgColColor: { 'zh-CN': '车牌颜色', en: 'Plate Color' },
  cfgColRange: { 'zh-CN': '生效区间', en: 'Effective Range' },
  ruleTypeDaily: { 'zh-CN': '每日制', en: 'Daily' },
  ruleTypeGeneral: { 'zh-CN': '24小时制', en: '24-hour' },
  cfgTemplateRequired: { 'zh-CN': '请选择规则模板', en: 'Please select a rule template' },
  cfgTemplatePlaceholder: { 'zh-CN': '选择规则模板', en: 'Select a rule template' },
  cfgNoTemplates: {
    'zh-CN': '该类型暂无计费模板，请先到「计费规则」页面创建模板。',
    en: 'No templates of this type yet. Create one under Billing Rules first.'
  },
  cfgColorLabel: { 'zh-CN': '车牌颜色', en: 'Plate Color' },
  cfgColorHint: {
    'zh-CN': '不选表示“默认”（适用于未配置专属颜色的其余颜色）；选了颜色则仅该颜色的车按此配置计费。',
    en: 'Leave empty for the default (covers colors without a dedicated entry); pick a color to apply to that color only.'
  },
  cfgColorPlaceholder: { 'zh-CN': '选择颜色（可选）', en: 'Pick a color (optional)' },
  cfgColorDefault: { 'zh-CN': '默认（不限颜色）', en: 'Default (all colors)' },
  cfgColorDefaultTag: { 'zh-CN': '默认', en: 'Default' },
  cfgFromLabel: { 'zh-CN': '生效开始', en: 'Effective From' },
  cfgToLabel: { 'zh-CN': '生效结束', en: 'Effective To' },
  cfgFromPlaceholder: { 'zh-CN': '生效开始日期', en: 'Start date' },
  cfgToPlaceholder: { 'zh-CN': '生效结束日期', en: 'End date' },
  cfgDateHint: {
    'zh-CN': '起止日期均可留空：开始留空表示不限过去，结束留空表示长期生效。',
    en: 'Either bound is optional: an empty start means no past limit; an empty end means valid indefinitely.'
  },
  cfgRangeInvalid: { 'zh-CN': '生效结束日期不能早于开始日期', en: 'End date cannot be before the start date' },
  cfgRangeLong: { 'zh-CN': '长期有效', en: 'Always' },
  cfgSaveOk: { 'zh-CN': '保存成功', en: 'Saved' },
  cfgSaveFailed: { 'zh-CN': '保存失败，请重试', en: 'Save failed, try again' },
  cfgDeleteOk: { 'zh-CN': '已删除', en: 'Deleted' },
  cfgDeleteFailed: { 'zh-CN': '删除失败，请重试', en: 'Delete failed, try again' },
  cfgLoadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load, try again' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  cfgDeleteTitle: { 'zh-CN': '删除计费配置', en: 'Delete Billing Config' },
  cfgDeleteMsg: { 'zh-CN': '确定删除该计费配置吗？删除后该车场对应颜色将按其余配置计费。', en: 'Delete this billing config? The lot’s charging for that color will fall back to remaining entries.' },
  emptyList: { 'zh-CN': '暂无停车场', en: 'No parking lots yet' },
  noMatch: { 'zh-CN': '没有符合条件的结果', en: 'No matching results' },
  createTitle: { 'zh-CN': '新增停车场', en: 'Create Parking Lot' },
  editTitle: { 'zh-CN': '编辑停车场', en: 'Edit Parking Lot' },
  name: { 'zh-CN': '车场名称', en: 'Name' },
  nameRequired: { 'zh-CN': '请输入车场名称', en: 'Please enter the lot name' },
  code: { 'zh-CN': '车场编码', en: 'Code' },
  codeRequired: { 'zh-CN': '请输入车场编码', en: 'Please enter the lot code' },
  codeLocked: { 'zh-CN': '编码创建后不可修改', en: 'Code cannot be changed after creation' },
  lotType: { 'zh-CN': '车场类型', en: 'Lot Type' },
  address: { 'zh-CN': '地址', en: 'Address' },
  totalSpaces: { 'zh-CN': '车位总数', en: 'Total Spaces' },
  enabled: { 'zh-CN': '启用', en: 'Enabled' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  save: { 'zh-CN': '保存', en: 'Save' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  createSuccess: { 'zh-CN': '新增成功', en: 'Created successfully' },
  updateSuccess: { 'zh-CN': '保存成功', en: 'Saved successfully' },
  invalidTotalSpaces: { 'zh-CN': '车位总数必须是非负整数', en: 'Total spaces must be a non-negative integer' },
  requestFailed: { 'zh-CN': '请求失败，请稍后重试', en: 'Request failed, please try again later' },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  'color.BLUE': { 'zh-CN': '蓝色', en: 'Blue' },
  'color.YELLOW': { 'zh-CN': '黄色', en: 'Yellow' },
  'color.GREEN': { 'zh-CN': '绿色', en: 'Green' },
  'color.YELLOW_GREEN': { 'zh-CN': '黄绿色', en: 'Yellow-green' },
  'color.BLACK': { 'zh-CN': '黑色', en: 'Black' },
  'color.WHITE': { 'zh-CN': '白色', en: 'White' },
  'color.RED': { 'zh-CN': '红色', en: 'Red' },
  'color.ORANGE': { 'zh-CN': '橙色', en: 'Orange' },
  'color.BROWN': { 'zh-CN': '棕色', en: 'Brown' },
  'color.PURPLE': { 'zh-CN': '紫色', en: 'Purple' },
  'color.PINK': { 'zh-CN': '粉色', en: 'Pink' },
  'color.GRAY': { 'zh-CN': '灰色', en: 'Gray' },
  'color.SILVER': { 'zh-CN': '银色', en: 'Silver' },
  'color.GOLD': { 'zh-CN': '金色', en: 'Gold' },
  'color.CREAM': { 'zh-CN': '米色', en: 'Cream' },
  'color.BEIGE': { 'zh-CN': '浅褐色', en: 'Beige' },
  'color.NAVY': { 'zh-CN': '藏青色', en: 'Navy' },
  'color.MAROON': { 'zh-CN': '栗色', en: 'Maroon' },
  'color.OLIVE': { 'zh-CN': '橄榄色', en: 'Olive' },
  'color.TEAL': { 'zh-CN': '青绿色', en: 'Teal' },
  'color.CYAN': { 'zh-CN': '青色', en: 'Cyan' },
  'color.MAGENTA': { 'zh-CN': '品红色', en: 'Magenta' },
  'color.LIME': { 'zh-CN': '柠檬绿', en: 'Lime' },
  'color.LAVENDER': { 'zh-CN': '淡紫色', en: 'Lavender' },
  'color.TURQUOISE': { 'zh-CN': '绿松石色', en: 'Turquoise' },
  'color.INDIGO': { 'zh-CN': '靛蓝色', en: 'Indigo' },
  'color.CORAL': { 'zh-CN': '珊瑚色', en: 'Coral' },
  'color.AMBER': { 'zh-CN': '琥珀色', en: 'Amber' },
  'color.VIOLET': { 'zh-CN': '紫罗兰色', en: 'Violet' },
  'color.CHARCOAL': { 'zh-CN': '炭灰色', en: 'Charcoal' },
  'color.LIGHT_BLUE': { 'zh-CN': '浅蓝色', en: 'Light blue' },
  'color.LIGHT_GREEN': { 'zh-CN': '浅绿色', en: 'Light green' },
  'color.DARK_BLUE': { 'zh-CN': '深蓝色', en: 'Dark blue' },
  'color.DARK_GREEN': { 'zh-CN': '深绿色', en: 'Dark green' },
  'color.RUST': { 'zh-CN': '铁锈色', en: 'Rust' },
  'color.BRONZE': { 'zh-CN': '青铜色', en: 'Bronze' },
  'color.PEACH': { 'zh-CN': '桃色', en: 'Peach' },
  'color.MINT': { 'zh-CN': '薄荷绿', en: 'Mint' },
  'color.ROSE': { 'zh-CN': '玫瑰色', en: 'Rose' },
  'color.SALMON': { 'zh-CN': '鲑鱼色', en: 'Salmon' },
  'color.COPPER': { 'zh-CN': '铜色', en: 'Copper' },
  'color.PLUM': { 'zh-CN': '李子紫', en: 'Plum' },
  'color.CRIMSON': { 'zh-CN': '深红色', en: 'Crimson' },
  'color.SCARLET': { 'zh-CN': '猩红色', en: 'Scarlet' },
  'color.EMERALD': { 'zh-CN': '翠绿色', en: 'Emerald' },
  'color.SAPPHIRE': { 'zh-CN': '蓝宝石色', en: 'Sapphire' },
  'color.RUBY': { 'zh-CN': '红宝石色', en: 'Ruby' },
  'color.OTHER': { 'zh-CN': '其他', en: 'Other' }
}

const { t, locale } = useBiText(d)
const router = useRouter()

const loading = ref(false)
const rows = ref<LotView[]>([])
const query = reactive({ keyword: '', page: 1, size: 10 })

const filteredRows = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  if (!keyword) {
    return rows.value
  }
  return rows.value.filter(
    (row) => row.name.toLowerCase().includes(keyword) || row.code.toLowerCase().includes(keyword)
  )
})

const total = computed(() => filteredRows.value.length)
const pagedRows = computed(() => {
  const start = (query.page - 1) * query.size
  return filteredRows.value.slice(start, start + query.size)
})

function formatTime(value?: string): string {
  if (!value) {
    return '-'
  }
  const match = /^(\d{4})-(\d{1,2})-(\d{1,2})[T ](\d{1,2}):(\d{2})/.exec(value)
  if (!match) {
    return value
  }
  const pad = (n: string): string => n.padStart(2, '0')
  return `${match[1]}-${pad(match[2])}-${pad(match[3])} ${pad(match[4])}:${match[5]}`
}

function lotTypeLabel(type: LotTypeOption): string {
  return type === 'PUBLIC' ? t('lotTypePublic') : t('lotTypeInternal')
}

function statusLabel(enabled: boolean): string {
  return enabled ? t('statusEnabled') : t('statusDisabled')
}

function handleSearch(): void {
  query.page = 1
}

function handleReset(): void {
  query.keyword = ''
  query.page = 1
}

function handlePageChange(page: number): void {
  query.page = page
}

function handleSizeChange(size: number): void {
  query.size = size
  query.page = 1
}

async function loadList(): Promise<void> {
  loading.value = true
  try {
    rows.value = await request.get<never, LotView[]>('/lots')
    query.page = 1
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('requestFailed'))
  } finally {
    loading.value = false
  }
}

function openEdit(row: LotView): void {
  form.id = row.id
  form.name = row.name
  form.code = row.code
  form.lotType = row.lotType
  form.address = row.address ?? ''
  form.totalSpaces = row.totalSpaces
  form.enabled = row.enabled
  dialogVisible.value = true
}

function openCreate(): void {
  form.id = null
  form.name = ''
  form.code = ''
  form.lotType = 'INTERNAL'
  form.address = ''
  form.totalSpaces = 0
  form.enabled = true
  dialogVisible.value = true
}

function openMap(row: LotView): void {
  void router.push(`/lots/${row.id}/map`)
}

function jump(path: string, row: LotView): void {
  void router.push({ path, query: { lotId: String(row.id) } })
}

/* ---------------- 新增 / 编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: null as number | null,
  name: '',
  code: '',
  lotType: 'INTERNAL' as LotTypeOption,
  address: '',
  totalSpaces: 0,
  enabled: true
})

const formRules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('nameRequired'), trigger: 'blur' }]
}))

function buildPayload(): LotPayload | null {
  if (!form.name.trim()) {
    ElMessage.warning(t('nameRequired'))
    return null
  }
  if (form.id === null && !form.code.trim()) {
    ElMessage.warning(t('codeRequired'))
    return null
  }
  if (!Number.isInteger(form.totalSpaces) || form.totalSpaces < 0) {
    ElMessage.warning(t('invalidTotalSpaces'))
    return null
  }
  return {
    name: form.name.trim(),
    lotType: form.lotType,
    address: form.address.trim() || undefined,
    totalSpaces: form.totalSpaces,
    enabled: form.enabled
  }
}

async function handleSave(): Promise<void> {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const payload = buildPayload()
  if (!payload) {
    return
  }
  saving.value = true
  try {
    if (form.id === null) {
      await request.post<never, LotView>('/lots', { ...payload, code: form.code.trim() })
      ElMessage.success(t('createSuccess'))
    } else {
      // mapData 不参与本次编辑；后端仅在该字段非 null 时才覆盖，因此可安全省略以保留原值
      await request.put<never, LotView>(`/lots/${form.id}`, payload)
      ElMessage.success(t('updateSuccess'))
    }
    dialogVisible.value = false
    await loadList()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('requestFailed'))
  } finally {
    saving.value = false
  }
}

/* ---------------- 车场计费配置弹窗 ---------------- */

const bindingVisible = ref(false)
const currentLot = ref<LotView | null>(null)
const bindings = ref<BindingRow[]>([])
const bindingsLoading = ref(false)
const dailyTemplates = ref<RuleOption[]>([])
const generalTemplates = ref<RuleOption[]>([])
const plateColors = ref<string[]>([])

const bindingTitle = computed(() => {
  const name = currentLot.value?.name ?? ''
  return locale.value === 'en' ? `Billing Config · ${name}` : `计费配置 · ${name}`
})

function ruleTypeLabel(type: RuleTypeValue): string {
  return type === 'DAILY' ? t('ruleTypeDaily') : t('ruleTypeGeneral')
}

function colorLabel(color: string): string {
  const key = `color.${color}`
  return key in d ? t(key) : color
}

function colorCellText(binding: BindingRow): string {
  return binding.plateColor ? colorLabel(binding.plateColor) : t('cfgColorDefaultTag')
}

function errorTextOf(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function rangeText(binding: BindingRow): string {
  const from = binding.effectiveFrom
  const to = binding.effectiveTo
  if (from && to) {
    return locale.value === 'en' ? `${from} ~ ${to}` : `${from} 至 ${to}`
  }
  if (from) {
    return locale.value === 'en' ? `From ${from}` : `${from} 起`
  }
  if (to) {
    return locale.value === 'en' ? `Until ${to}` : `${to} 止`
  }
  return t('cfgRangeLong')
}

async function loadPlateColors(): Promise<void> {
  try {
    const settings = await request.get<never, SettingsView>('/system/settings')
    if (settings?.allowedPlateColors?.length) {
      plateColors.value = [...settings.allowedPlateColors]
    } else if (settings?.supportedPlateColors?.length) {
      plateColors.value = [...settings.supportedPlateColors]
    } else {
      plateColors.value = []
    }
  } catch {
    // 读取失败不阻塞主流程，颜色下拉可能为空（仍可选“默认”）
    plateColors.value = []
  }
}

async function loadRuleOptions(): Promise<void> {
  try {
    const daily = await request.get<never, RuleOption[]>('/billing/daily-rules')
    dailyTemplates.value = (daily ?? []).map((item) => ({ id: item.id, title: item.title }))
  } catch {
    dailyTemplates.value = []
  }
  try {
    const general = await request.get<never, RuleOption[]>('/billing/general-rules')
    generalTemplates.value = (general ?? []).map((item) => ({ id: item.id, title: item.title }))
  } catch {
    generalTemplates.value = []
  }
}

async function loadBindings(): Promise<void> {
  const lot = currentLot.value
  if (!lot) {
    bindings.value = []
    return
  }
  bindingsLoading.value = true
  try {
    const list = await request.get<never, BindingRow[]>('/billing/lot-bindings', {
      params: { lotId: lot.id }
    })
    bindings.value = list ?? []
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('cfgLoadFailed')))
  } finally {
    bindingsLoading.value = false
  }
}

function openBillingDialog(row: LotView): void {
  currentLot.value = row
  bindings.value = []
  loadRuleOptions()
  loadPlateColors()
  loadBindings()
  bindingVisible.value = true
}

function resetBindingForm(): void {
  editingBindingId.value = null
  bform.ruleType = 'DAILY'
  bform.ruleId = null
  bform.plateColor = ''
  bform.effectiveFrom = ''
  bform.effectiveTo = ''
}

function openBindingCreate(): void {
  resetBindingForm()
  editorVisible.value = true
}

function openBindingEdit(binding: BindingRow): void {
  editingBindingId.value = binding.id
  bform.ruleType = binding.ruleType
  bform.ruleId = binding.ruleId
  bform.plateColor = binding.plateColor ?? ''
  bform.effectiveFrom = binding.effectiveFrom ?? ''
  bform.effectiveTo = binding.effectiveTo ?? ''
  editorVisible.value = true
}

/** 编辑中可选模板按当前规则类型过滤 */
const templateOptions = computed<RuleOption[]>(() =>
  bform.ruleType === 'DAILY' ? dailyTemplates.value : generalTemplates.value
)

const editorVisible = ref(false)
const editorSaving = ref(false)
const editingBindingId = ref<number | null>(null)
const bform = reactive({
  ruleType: 'DAILY' as RuleTypeValue,
  ruleId: null as number | null,
  plateColor: '',
  effectiveFrom: '',
  effectiveTo: ''
})

// 编辑时切换计费方式后，原模板 id 若不属于新类型则清空，避免带错类型提交
watch(
  () => bform.ruleType,
  () => {
    if (bform.ruleId != null && !templateOptions.value.some((option) => option.id === bform.ruleId)) {
      bform.ruleId = null
    }
  }
)

async function handleBindingSave(): Promise<void> {
  const lot = currentLot.value
  if (!lot) {
    return
  }
  if (bform.ruleId == null) {
    ElMessage.warning(t('cfgTemplateRequired'))
    return
  }
  if (bform.effectiveFrom && bform.effectiveTo && bform.effectiveTo < bform.effectiveFrom) {
    ElMessage.warning(t('cfgRangeInvalid'))
    return
  }
  const payload = {
    lotId: lot.id,
    ruleType: bform.ruleType,
    ruleId: bform.ruleId,
    plateColor: bform.plateColor.trim() || null,
    effectiveFrom: bform.effectiveFrom || null,
    effectiveTo: bform.effectiveTo || null
  }
  editorSaving.value = true
  try {
    if (editingBindingId.value != null) {
      await request.put<never, BindingRow>(`/billing/lot-bindings/${editingBindingId.value}`, payload)
    } else {
      await request.post<never, BindingRow>('/billing/lot-bindings', payload)
    }
    ElMessage.success(t('cfgSaveOk'))
    editorVisible.value = false
    loadBindings()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('cfgSaveFailed')))
  } finally {
    editorSaving.value = false
  }
}

async function removeBinding(binding: BindingRow): Promise<void> {
  try {
    await ElMessageBox.confirm(t('cfgDeleteMsg'), t('cfgDeleteTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  try {
    await request.delete<never, void>(`/billing/lot-bindings/${binding.id}`)
    ElMessage.success(t('cfgDeleteOk'))
    loadBindings()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('cfgDeleteFailed')))
  }
}

onMounted(loadList)
</script>

<template>
  <section class="lots-view">
    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          class="search-input"
          :placeholder="t('searchPlaceholder')"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">{{ t('search') }}</el-button>
        <el-button @click="handleReset">{{ t('reset') }}</el-button>
        <div class="spacer" />
        <el-button type="primary" plain @click="openCreate">{{ t('add') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="pagedRows" stripe>
        <el-table-column prop="name" :label="t('colName')" min-width="150" />
        <el-table-column prop="code" :label="t('colCode')" min-width="110" />
        <el-table-column :label="t('colLotType')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.lotType === 'PUBLIC' ? 'warning' : 'primary'" effect="light" round>
              {{ lotTypeLabel(row.lotType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('colAddress')" min-width="150">
          <template #default="{ row }">{{ row.address || '-' }}</template>
        </el-table-column>
        <el-table-column prop="totalSpaces" :label="t('colTotalSpaces')" width="100" />
        <el-table-column :label="t('colStatus')" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light" round>
              {{ statusLabel(row.enabled) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('colUpdatedAt')" min-width="160">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('colActions')" min-width="360" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button link type="primary" @click="openEdit(row)">{{ t('edit') }}</el-button>
              <el-button link type="primary" @click="openMap(row)">{{ t('map') }}</el-button>
              <el-button link type="primary" @click="openBillingDialog(row)">
                {{ t('cfgLink') }}
              </el-button>
              <el-button link type="primary" @click="jump('/spaces', row)">{{ t('linkSpaces') }}</el-button>
              <el-button link type="primary" @click="jump('/booths', row)">{{ t('linkBooths') }}</el-button>
              <el-button link type="primary" @click="jump('/internal-vehicles', row)">
                {{ t('linkInternalVehicles') }}
              </el-button>
              <el-button link type="primary" @click="jump('/access/whitelist', row)">
                {{ t('linkWhitelist') }}
              </el-button>
              <el-button link type="primary" @click="jump('/access/blacklist', row)">
                {{ t('linkBlacklist') }}
              </el-button>
              <el-button link type="primary" @click="jump('/access/pattern-allowlist', row)">
                {{ t('linkPatternAllowlist') }}
              </el-button>
            </div>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty
            :description="query.keyword ? t('noMatch') : t('emptyList')"
            :image-size="80"
          />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id === null ? t('createTitle') : t('editTitle')"
      width="min(520px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item :label="t('name')" prop="name">
          <el-input v-model="form.name" maxlength="120" />
        </el-form-item>
        <el-form-item :label="t('code')" prop="code">
          <el-input
            v-model="form.code"
            :disabled="form.id !== null"
            maxlength="64"
          />
          <div v-if="form.id !== null" class="form-hint">{{ t('codeLocked') }}</div>
        </el-form-item>
        <el-form-item :label="t('lotType')" prop="lotType">
          <el-select v-model="form.lotType" style="width: 100%">
            <el-option
              v-for="option in LOT_TYPE_OPTIONS"
              :key="option"
              :label="lotTypeLabel(option)"
              :value="option"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('address')" prop="address">
          <el-input v-model="form.address" maxlength="255" />
        </el-form-item>
        <el-form-item :label="t('totalSpaces')" prop="totalSpaces">
          <el-input-number v-model="form.totalSpaces" :min="0" :precision="0" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('enabled')" prop="enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ saving ? t('saving') : t('save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 车场计费配置弹窗 -->
    <el-dialog
      v-model="bindingVisible"
      :title="bindingTitle"
      width="min(860px, calc(100vw - 32px))"
      destroy-on-close
      append-to-body
    >
      <el-alert type="info" :closable="false" class="cfg-alert" show-icon>
        <p class="cfg-tip">{{ t('cfgTip') }}</p>
      </el-alert>

      <div class="cfg-toolbar">
        <el-button type="primary" plain @click="openBindingCreate">
          {{ t('cfgAdd') }}
        </el-button>
      </div>

      <el-table v-loading="bindingsLoading" :data="bindings" stripe class="cfg-table">
        <el-table-column :label="t('cfgColType')" width="110">
          <template #default="{ row }">
            <el-tag
              :type="(row as BindingRow).ruleType === 'DAILY' ? 'primary' : 'success'"
              effect="light"
            >
              {{ ruleTypeLabel((row as BindingRow).ruleType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('cfgColTemplate')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="cfg-title">{{ (row as BindingRow).ruleTitle || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('cfgColColor')" width="150">
          <template #default="{ row }">
            <el-tag
              v-if="!(row as BindingRow).plateColor"
              type="info"
              effect="plain"
              size="small"
            >
              {{ colorCellText(row as BindingRow) }}
            </el-tag>
            <span v-else class="color-label">{{ colorCellText(row as BindingRow) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('cfgColRange')" min-width="170">
          <template #default="{ row }">
            <span class="cfg-range">{{ rangeText(row as BindingRow) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('colActions')" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openBindingEdit(row as BindingRow)">
              {{ t('edit') }}
            </el-button>
            <el-button link type="danger" @click="removeBinding(row as BindingRow)">
              {{ t('delete') }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('cfgEmpty')" :image-size="80" />
        </template>
      </el-table>

      <template #footer>
        <el-button @click="bindingVisible = false">{{ t('cancel') }}</el-button>
      </template>
    </el-dialog>

    <!-- 新增 / 编辑计费配置弹窗 -->
    <el-dialog
      v-model="editorVisible"
      :title="editingBindingId == null ? t('cfgAddTitle') : t('cfgEditTitle')"
      width="min(560px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <el-form label-width="110px">
        <el-form-item :label="t('cfgColType')">
          <el-select v-model="bform.ruleType" style="width: 100%">
            <el-option value="DAILY" :label="t('ruleTypeDaily')" />
            <el-option value="GENERAL" :label="t('ruleTypeGeneral')" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('cfgColTemplate')">
          <el-select
            v-model="bform.ruleId"
            class="full-field"
            :placeholder="t('cfgTemplatePlaceholder')"
          >
            <el-option
              v-for="option in templateOptions"
              :key="option.id"
              :value="option.id"
              :label="option.title"
            />
          </el-select>
          <p v-if="templateOptions.length === 0" class="form-tip warn-tip">
            {{ t('cfgNoTemplates') }}
          </p>
        </el-form-item>
        <el-form-item :label="t('cfgColorLabel')">
          <el-select
            v-model="bform.plateColor"
            class="full-field"
            clearable
            :placeholder="t('cfgColorPlaceholder')"
          >
            <el-option :value="''" :label="t('cfgColorDefault')" />
            <el-option
              v-for="color in plateColors"
              :key="color"
              :value="color"
              :label="colorLabel(color)"
            />
          </el-select>
          <p class="form-tip">{{ t('cfgColorHint') }}</p>
        </el-form-item>
        <el-form-item :label="t('cfgFromLabel')">
          <el-date-picker
            v-model="bform.effectiveFrom"
            class="full-field"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('cfgFromPlaceholder')"
            :clearable="true"
          />
        </el-form-item>
        <el-form-item :label="t('cfgToLabel')">
          <el-date-picker
            v-model="bform.effectiveTo"
            class="full-field"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('cfgToPlaceholder')"
            :clearable="true"
          />
        </el-form-item>
        <p class="date-hint">{{ t('cfgDateHint') }}</p>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="editorSaving" @click="handleBindingSave">
          {{ editorSaving ? t('saving') : t('save') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.lots-view {
  animation: fade-in 0.35s ease;
}

.card {
  background: var(--fp-surface-elevated);
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  padding: 20px;
  box-shadow: var(--fp-shadow-soft);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 280px;
}

.spacer {
  flex: 1;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-hint {
  width: 100%;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--fp-muted);
}

.cfg-alert {
  margin-bottom: 12px;
}

.cfg-tip {
  margin: 2px 0 0;
  line-height: 1.6;
}

.cfg-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.cfg-table {
  width: 100%;
}

.cfg-title {
  font-weight: 500;
  color: var(--fp-ink);
}

.color-label {
  font-size: 0.85rem;
  color: var(--fp-ink-soft);
  white-space: nowrap;
}

.cfg-range {
  font-size: 0.85rem;
  color: var(--fp-ink-soft);
  white-space: nowrap;
}

.full-field {
  width: 100%;
}

.form-tip {
  width: 100%;
  margin: 4px 0 0;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--fp-muted);
}

.warn-tip {
  color: #d03050;
}

.date-hint {
  margin: -6px 0 0 110px;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--fp-muted);
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
