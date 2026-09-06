<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

const d: BiDict = {
  lot: { 'zh-CN': '车场', en: 'Lot' },
  allLots: { 'zh-CN': '全部车场', en: 'All lots' },
  keyword: { 'zh-CN': '关键词', en: 'Keyword' },
  keywordPlaceholder: { 'zh-CN': '车牌 / 车场 / 通道', en: 'Plate / lot / lane' },
  status: { 'zh-CN': '状态', en: 'Status' },
  allStatus: { 'zh-CN': '全部状态', en: 'All status' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  manualEntry: { 'zh-CN': '手动入场', en: 'Manual entry' },
  plate: { 'zh-CN': '车牌', en: 'Plate' },
  plateColor: { 'zh-CN': '车牌颜色', en: 'Plate color' },
  entryTime: { 'zh-CN': '入场时间', en: 'Entry time' },
  entryLane: { 'zh-CN': '入场通道', en: 'Entry lane' },
  noLane: { 'zh-CN': '不限通道', en: 'Any lane' },
  exitTime: { 'zh-CN': '出场时间', en: 'Exit time' },
  exitLane: { 'zh-CN': '出场通道', en: 'Exit lane' },
  closeNow: { 'zh-CN': '同时出场（关场）', en: 'Close session on exit' },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  void: { 'zh-CN': '作废', en: 'Void' },
  createTitle: { 'zh-CN': '手动入场（新增在场流水）', en: 'Manual entry (open a session)' },
  editTitle: { 'zh-CN': '编辑流水', en: 'Edit session' },
  createSuccess: { 'zh-CN': '已生成在场流水', en: 'Open session created' },
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
  confirm: { 'zh-CN': '确定', en: 'Confirm' }
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

/** 系统配置时区（缓存），默认 Asia/Shanghai；手动进出场默认时间按其口径生成 */
let systemTimezone = 'Asia/Shanghai'

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
    const data = await request.get<never, PageResult<SessionRow>>('/parking-sessions', {
      params: {
        lotId: filters.lotId,
        keyword: filters.keyword.trim() || undefined,
        status: filters.status || undefined,
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
      await request.post('/parking-sessions', {
        lotId: form.lotId,
        plateNumber: form.plateNumber,
        plateColor: form.plateColor,
        entryTime: form.entryTime,
        entryLaneId: form.entryLaneId ?? null,
        entryLaneName: form.entryLaneName || laneNameOf(form.entryLaneId) || null,
        entryImage: null
      })
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

onMounted(async () => {
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
      <el-button type="primary" @click="runSearch">{{ t('search') }}</el-button>
      <el-button @click="handleReset">{{ t('reset') }}</el-button>
      <div class="toolbar-spacer" />
      <el-button type="primary" plain @click="openCreate">{{ t('manualEntry') }}</el-button>
    </div>

    <div class="page-card">
      <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
        <el-table-column :label="t('plate')" min-width="130">
          <template #default="{ row }">
            <span class="plate-cell">{{ row.plateNumber }}</span>
            <span class="plate-color">{{ plateColorLabel(row.plateColor) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('status')" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" disable-transitions>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lotName" :label="t('lot')" min-width="150" show-overflow-tooltip />
        <el-table-column prop="entryTime" :label="t('entryTime')" min-width="165" />
        <el-table-column prop="entryLaneName" :label="t('entryLane')" min-width="130" show-overflow-tooltip />
        <el-table-column prop="exitTime" :label="t('exitTime')" min-width="165">
          <template #default="{ row }">
            {{ row.exitTime ?? '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="exitLaneName" :label="t('exitLane')" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.exitLaneName ?? '—' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('actions')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'VOIDED'" link type="primary" @click="openEdit(row)">
              {{ t('edit') }}
            </el-button>
            <el-button v-if="row.status !== 'VOIDED'" link type="danger" @click="handleVoid(row)">
              {{ t('void') }}
            </el-button>
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
          <el-select v-model="form.lotId" :disabled="!editing" placeholder="..." style="width: 100%" @change="handleLotChange">
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

        <template v-if="editing">
          <el-form-item v-if="isOpenSession">
            <el-checkbox v-model="form.enableExit">{{ t('closeNow') }}</el-checkbox>
          </el-form-item>
          <template v-if="!isOpenSession || form.enableExit">
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
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">{{ t('confirm') }}</el-button>
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

.plate-cell {
  font-weight: 600;
  letter-spacing: 0.04em;
}

.plate-color {
  margin-left: 6px;
  font-size: 0.78rem;
  color: var(--fp-muted);
}

.pager-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
