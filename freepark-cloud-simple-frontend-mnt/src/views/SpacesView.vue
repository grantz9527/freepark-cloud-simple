<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { downloadFile } from '../utils/download'
import { useBiText, type BiDict } from '../utils/biText'

/* ---------------- 类型 ---------------- */

interface LotItem {
  id: number
  name: string
}

interface LocationItem {
  id: number
  name: string
}

interface AreaItem {
  id: number
  name: string
}

interface SpaceItem {
  id: number
  code: string
  enabled: boolean
  areaId: number
}

interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/* ---------------- 双语文案 ---------------- */

const d: BiDict = {
  lot: { 'zh-CN': '车场', en: 'Parking lot' },
  lotPlaceholder: { 'zh-CN': '请选择车场', en: 'Select a parking lot' },
  noLot: { 'zh-CN': '暂无车场数据', en: 'No parking lots' },
  noLotHint: { 'zh-CN': '请先在车场管理中创建车场，再来管理车位。', en: 'Please create a parking lot first.' },
  locationInfo: { 'zh-CN': '位置信息', en: 'Locations' },
  locationInfoHint: { 'zh-CN': '如「B1 层」「A 座」等，先建位置，再在位置下建区域。', en: 'e.g. "Floor B1", "Block A". Create a location first, then areas under it.' },
  areaInfo: { 'zh-CN': '区域信息', en: 'Areas' },
  areaInfoHint: { 'zh-CN': '如「A 区」「B 区」，车位最终归属于区域。', en: 'e.g. "Zone A". Spaces belong to an area.' },
  areaNeedLocation: { 'zh-CN': '请先选择位置，再管理该位置下的区域', en: 'Select a location to manage its areas' },
  add: { 'zh-CN': '新增', en: 'Add' },
  locationEmpty: { 'zh-CN': '该车场暂无位置', en: 'No location in this lot' },
  locationEmptyHint: { 'zh-CN': '请先新增位置，再在位置下新增区域与车位。', en: 'Add a location first, then create areas and spaces.' },
  areaEmpty: { 'zh-CN': '该位置暂无区域', en: 'No area under this location' },
  areaEmptyHint: { 'zh-CN': '请先新增区域，再添加车位。', en: 'Add an area first, then create spaces.' },
  currentContext: { 'zh-CN': '当前车位目录', en: 'Current directory' },
  selectLocationFirst: { 'zh-CN': '请先选择一个位置', en: 'Select a location first' },
  selectAreaFirst: { 'zh-CN': '请选择该位置下的区域', en: 'Select an area of this location' },
  needLocation: { 'zh-CN': '请先选择一个位置', en: 'Please select a location first' },
  needArea: { 'zh-CN': '请先在左侧选择一个区域', en: 'Please select an area on the left first' },
  colIndex: { 'zh-CN': '序号', en: 'No.' },
  colCode: { 'zh-CN': '车位编号', en: 'Space code' },
  codePlaceholder: { 'zh-CN': '输入车位编号搜索', en: 'Search by space code' },
  colStatus: { 'zh-CN': '状态', en: 'Status' },
  enabled: { 'zh-CN': '启用', en: 'Enabled' },
  disabled: { 'zh-CN': '停用', en: 'Disabled' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  addSpace: { 'zh-CN': '新增车位', en: 'Add space' },
  import: { 'zh-CN': '批量导入', en: 'Import' },
  empty: { 'zh-CN': '暂无车位数据', en: 'No spaces yet' },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  createTitle: { 'zh-CN': '新增车位', en: 'Add space' },
  editTitle: { 'zh-CN': '编辑车位', en: 'Edit space' },
  locationName: { 'zh-CN': '位置名称', en: 'Location name' },
  locationNamePh: { 'zh-CN': '如：B1 层 / A 座', en: 'e.g. Floor B1 / Block A' },
  areaName: { 'zh-CN': '区域名称', en: 'Area name' },
  areaNamePh: { 'zh-CN': '如：A 区 / B 区', en: 'e.g. Zone A / Zone B' },
  createLocationTitle: { 'zh-CN': '新增位置', en: 'Add location' },
  createAreaTitle: { 'zh-CN': '新增区域', en: 'Add area' },
  createAreaHint: { 'zh-CN': '将区域新增到「{location}」下', en: 'Adding an area under "{location}"' },
  saveAndContinue: { 'zh-CN': '保存并继续添加', en: 'Save & add another' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  save: { 'zh-CN': '保存', en: 'Save' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  nameRequired: { 'zh-CN': '请输入名称', en: 'Please enter a name' },
  codeRequired: { 'zh-CN': '请输入车位编号', en: 'Please enter the space code' },
  createSuccess: { 'zh-CN': '车位创建成功', en: 'Space created' },
  updateSuccess: { 'zh-CN': '车位更新成功', en: 'Space updated' },
  deleteTitle: { 'zh-CN': '删除确认', en: 'Confirm deletion' },
  deleteConfirm: { 'zh-CN': '确定删除车位「{code}」吗？', en: 'Delete space "{code}"?' },
  deleteSuccess: { 'zh-CN': '车位已删除', en: 'Space deleted' },
  locationCreated: { 'zh-CN': '位置创建成功', en: 'Location created' },
  areaCreated: { 'zh-CN': '区域创建成功', en: 'Area created' },
  lotLoadFailed: { 'zh-CN': '车场列表加载失败', en: 'Failed to load lots' },
  loadFailed: { 'zh-CN': '车位列表加载失败', en: 'Failed to load spaces' },
  submitFailed: { 'zh-CN': '操作失败，请重试', en: 'Operation failed. Please retry.' },
  importTitle: { 'zh-CN': '批量导入车位', en: 'Import spaces' },
  importHint: {
    'zh-CN': '请先下载模板并按模板格式填写，导入后车位将归属到左侧当前选中的区域。',
    en: 'Download the template and fill it in. Imported spaces will belong to the area selected on the left.'
  },
  importToArea: { 'zh-CN': '导入区域', en: 'Target area' },
  importNeedContext: { 'zh-CN': '请先在左侧选择位置与区域', en: 'Select a location and area on the left first' },
  downloadTemplate: { 'zh-CN': '下载模板', en: 'Download template' },
  chooseFile: { 'zh-CN': '选择文件', en: 'Choose file' },
  changeFile: { 'zh-CN': '重新选择', en: 'Choose again' },
  fileRequired: { 'zh-CN': '请选择要导入的 Excel 文件', en: 'Please choose an Excel file' },
  startImport: { 'zh-CN': '开始导入', en: 'Import' },
  importing: { 'zh-CN': '导入中…', en: 'Importing…' },
  importSuccess: { 'zh-CN': '成功导入 {count} 个车位', en: '{count} spaces imported' },
  importFailed: { 'zh-CN': '导入失败', en: 'Import failed' },
  downloadFailed: { 'zh-CN': '模板下载失败', en: 'Failed to download template' }
}

const { t } = useBiText(d)
const route = useRoute()

/** 替换文案中的 {placeholder} 占位符 */
function fill(template: string, values: Record<string, string | number>): string {
  return template.replace(/\{(\w+)\}/g, (_m: string, key: string) => String(values[key] ?? ''))
}

function errorTextOf(e: unknown, fallback: string): string {
  return e instanceof Error && e.message ? e.message : fallback
}

/* ---------------- 目录层级状态 ---------------- */

const LOT_KEY = 'fp.spaces.lot'
const locationKey = (lotId: number) => `fp.spaces.lot.${lotId}.location`
const areaKey = (lotId: number) => `fp.spaces.lot.${lotId}.area`

const loading = ref(false)
const lots = ref<LotItem[]>([])
const lotId = ref<number>()
const selectedLot = computed(() => lots.value.find((item) => item.id === lotId.value))

const locations = ref<LocationItem[]>([])
const areas = ref<AreaItem[]>([])
const selectedLocationId = ref<number>()
const selectedAreaId = ref<number>()
const selectedLocation = computed(() =>
  locations.value.find((item) => item.id === selectedLocationId.value)
)
const selectedArea = computed(() => areas.value.find((item) => item.id === selectedAreaId.value))
const canLoadSpaces = computed(
  () => lotId.value !== undefined && selectedLocationId.value !== undefined && selectedAreaId.value !== undefined
)

const spaces = ref<SpaceItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const searchInput = ref('')
const appliedCode = ref('')
const pageStart = computed(() => (total.value === 0 ? 0 : (page.value - 1) * pageSize.value + 1))

/* ---------------- 目录层级加载 ---------------- */

async function loadLots(): Promise<void> {
  try {
    const data = await request.get<never, LotItem[]>('/lots')
    lots.value = data
    if (lots.value.length === 0) {
      lotId.value = undefined
      return
    }
    const queryLotId = Number(route.query.lotId)
    const stored = Number(sessionStorage.getItem(LOT_KEY))
    const target =
      lots.value.find((lot) => queryLotId > 0 && lot.id === queryLotId) ??
      lots.value.find((lot) => stored > 0 && lot.id === stored) ??
      lots.value[0]
    lotId.value = target.id
    persistLot(target.id)
    await loadContext()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('lotLoadFailed')))
  }
}

function persistLot(id: number): void {
  sessionStorage.setItem(LOT_KEY, String(id))
}

function persistLocation(id: number): void {
  if (lotId.value !== undefined) {
    sessionStorage.setItem(locationKey(lotId.value), String(id))
  }
}

function persistArea(id: number): void {
  if (lotId.value !== undefined) {
    sessionStorage.setItem(areaKey(lotId.value), String(id))
  }
}

async function loadLocations(): Promise<void> {
  if (lotId.value === undefined) {
    locations.value = []
    return
  }
  locations.value = await request.get<never, LocationItem[]>(`/lots/${lotId.value}/locations`)
}

async function loadAreas(): Promise<void> {
  if (lotId.value === undefined || selectedLocationId.value === undefined) {
    areas.value = []
    return
  }
  areas.value = await request.get<never, AreaItem[]>(`/lots/${lotId.value}/areas`, {
    params: { locationId: selectedLocationId.value }
  })
}

async function loadSpaces(): Promise<void> {
  if (!canLoadSpaces.value || lotId.value === undefined) {
    spaces.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const params: Record<string, string | number> = { page: page.value, size: pageSize.value }
    params.locationId = selectedLocationId.value as number
    params.areaId = selectedAreaId.value as number
    if (appliedCode.value.trim()) {
      params.code = appliedCode.value.trim()
    }
    const data = await request.get<never, PageResult<SpaceItem>>(
      `/lots/${lotId.value}/spaces`,
      { params }
    )
    spaces.value = data.list
    total.value = data.total
    page.value = data.page || page.value
    pageSize.value = data.size || pageSize.value
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('loadFailed')))
  } finally {
    loading.value = false
  }
}

/** 选车场后重新加载位置/区域/车位 */
async function loadContext(): Promise<void> {
  loading.value = true
  try {
    selectedLocationId.value = undefined
    selectedAreaId.value = undefined
    await loadLocations()
    if (locations.value.length > 0) {
      const stored = Number(sessionStorage.getItem(locationKey(lotId.value as number)))
      selectedLocationId.value =
        locations.value.find((item) => item.id === stored)?.id ?? locations.value[0].id
      persistLocation(selectedLocationId.value)
      await selectLocation(selectedLocationId.value, true)
    } else {
      await loadAreas()
      await loadSpaces()
    }
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('loadFailed')))
  } finally {
    loading.value = false
  }
}

async function selectLocation(locationId: number, skipReset = false): Promise<void> {
  if (selectedLocationId.value === locationId && skipReset) {
    // 首次自动选择时仍要加载其区域
  } else if (selectedLocationId.value === locationId) {
    return
  }
  selectedLocationId.value = locationId
  selectedAreaId.value = undefined
  persistLocation(locationId)
  await loadAreas()
  if (areas.value.length > 0) {
    const stored = Number(sessionStorage.getItem(areaKey(lotId.value as number)))
    selectedAreaId.value = areas.value.find((item) => item.id === stored)?.id ?? areas.value[0].id
    persistArea(selectedAreaId.value)
  }
  page.value = 1
  appliedCode.value = ''
  searchInput.value = ''
  await loadSpaces()
}

async function selectArea(areaId: number): Promise<void> {
  if (selectedAreaId.value === areaId) {
    return
  }
  selectedAreaId.value = areaId
  persistArea(areaId)
  page.value = 1
  appliedCode.value = ''
  searchInput.value = ''
  await loadSpaces()
}

async function handleLotChange(): Promise<void> {
  persistLot(lotId.value as number)
  page.value = 1
  appliedCode.value = ''
  searchInput.value = ''
  await loadContext()
}

function handleSearch(): void {
  appliedCode.value = searchInput.value.trim()
  page.value = 1
  loadSpaces()
}

function handleReset(): void {
  searchInput.value = ''
  appliedCode.value = ''
  page.value = 1
  loadSpaces()
}

function handlePageChange(target: number): void {
  page.value = target
  loadSpaces()
}

function handlePageSizeChange(): void {
  page.value = 1
  loadSpaces()
}

/* ---------------- 新增 / 编辑车位 ---------------- */

const spaceDialog = ref(false)
const editing = ref(false)
const editingId = ref<number>()
const saving = ref(false)
const spaceForm = reactive({ code: '', enabled: true })

function openCreateDialog(): void {
  if (!canLoadSpaces.value) {
    ElMessage.warning(t('needArea'))
    return
  }
  editing.value = false
  editingId.value = undefined
  spaceForm.code = ''
  spaceForm.enabled = true
  spaceDialog.value = true
}

function openEditDialog(row: SpaceItem): void {
  editing.value = true
  editingId.value = row.id
  spaceForm.code = row.code
  spaceForm.enabled = row.enabled
  spaceDialog.value = true
}

async function handleSubmitSpace(): Promise<void> {
  if (lotId.value === undefined || selectedAreaId.value === undefined) {
    return
  }
  const code = spaceForm.code.trim()
  if (!code) {
    ElMessage.warning(t('codeRequired'))
    return
  }
  saving.value = true
  const payload = { areaId: selectedAreaId.value, code, enabled: spaceForm.enabled }
  try {
    if (editing.value && editingId.value) {
      await request.put<never, SpaceItem>(`/lots/${lotId.value}/spaces/${editingId.value}`, payload)
      ElMessage.success(t('updateSuccess'))
    } else {
      await request.post<never, SpaceItem>(`/lots/${lotId.value}/spaces`, payload)
      ElMessage.success(t('createSuccess'))
    }
    spaceDialog.value = false
    handleSearch()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('submitFailed')))
  } finally {
    saving.value = false
  }
}

async function handleDeleteSpace(row: SpaceItem): Promise<void> {
  if (lotId.value === undefined) {
    return
  }
  try {
    await ElMessageBox.confirm(fill(t('deleteConfirm'), { code: row.code }), t('deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  try {
    await request.delete<never, void>(`/lots/${lotId.value}/spaces/${row.id}`)
    ElMessage.success(t('deleteSuccess'))
    loadSpaces()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('submitFailed')))
  }
}

/* ---------------- 新增位置 / 新增区域 ---------------- */

const locationDialog = ref(false)
const areaDialog = ref(false)
const locationName = ref('')
const areaName = ref('')
const savingExtra = ref(false)

function openLocationDialog(): void {
  if (lotId.value === undefined) {
    return
  }
  locationName.value = ''
  locationDialog.value = true
}

function openAreaDialog(): void {
  if (lotId.value === undefined) {
    return
  }
  if (selectedLocationId.value === undefined) {
    ElMessage.warning(t('needLocation'))
    return
  }
  areaName.value = ''
  areaDialog.value = true
}

async function submitLocation(continueAdding: boolean): Promise<void> {
  if (lotId.value === undefined) {
    return
  }
  const name = locationName.value.trim()
  if (!name) {
    ElMessage.warning(t('nameRequired'))
    return
  }
  savingExtra.value = true
  try {
    const created = await request.post<never, LocationItem>(`/lots/${lotId.value}/locations`, { name })
    ElMessage.success(t('locationCreated'))
    await loadLocations()
    await selectLocation(created.id)
    if (continueAdding) {
      locationName.value = ''
    } else {
      locationDialog.value = false
    }
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('submitFailed')))
  } finally {
    savingExtra.value = false
  }
}

async function submitArea(continueAdding: boolean): Promise<void> {
  if (lotId.value === undefined || selectedLocationId.value === undefined) {
    return
  }
  const name = areaName.value.trim()
  if (!name) {
    ElMessage.warning(t('nameRequired'))
    return
  }
  savingExtra.value = true
  try {
    const created = await request.post<never, AreaItem>(`/lots/${lotId.value}/areas`, {
      locationId: selectedLocationId.value,
      name
    })
    ElMessage.success(t('areaCreated'))
    await loadAreas()
    selectedAreaId.value = created.id
    persistArea(created.id)
    page.value = 1
    await loadSpaces()
    if (continueAdding) {
      areaName.value = ''
    } else {
      areaDialog.value = false
    }
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('submitFailed')))
  } finally {
    savingExtra.value = false
  }
}

/* ---------------- 批量导入 ---------------- */

const importVisible = ref(false)
const importing = ref(false)
const importFile = ref<File>()
const importError = ref('')
const downloadingTemplate = ref(false)
const fileInput = ref<HTMLInputElement>()

function openImportDialog(): void {
  if (!canLoadSpaces.value || lotId.value === undefined) {
    ElMessage.warning(t('importNeedContext'))
    return
  }
  importFile.value = undefined
  importError.value = ''
  importVisible.value = true
  nextTick(() => {
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  })
}

function pickFile(): void {
  fileInput.value?.click()
}

function onFileChange(event: Event): void {
  const input = event.target as HTMLInputElement
  importFile.value = input.files?.[0]
  importError.value = ''
}

async function handleDownloadTemplate(): Promise<void> {
  if (lotId.value === undefined) {
    return
  }
  downloadingTemplate.value = true
  try {
    await downloadFile(`/lots/${lotId.value}/spaces/import-template`, 'spaces-template.xlsx')
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('downloadFailed')))
  } finally {
    downloadingTemplate.value = false
  }
}

async function handleImport(): Promise<void> {
  if (lotId.value === undefined || selectedAreaId.value === undefined) {
    return
  }
  const file = importFile.value
  if (!file) {
    importError.value = t('fileRequired')
    return
  }
  importing.value = true
  importError.value = ''
  try {
    const fd = new FormData()
    fd.append('file', file)
    const count = await request.post<never, number>(
      `/lots/${lotId.value}/spaces/import`,
      fd,
      { params: { areaId: selectedAreaId.value } }
    )
    importVisible.value = false
    ElMessage.success(fill(t('importSuccess'), { count }))
    handleSearch()
  } catch (error) {
    importError.value = errorTextOf(error, t('importFailed'))
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadLots()
})
</script>

<template>
  <section class="spaces-page">
    <template v-if="lots.length > 0">
      <div class="lot-bar">
        <label class="lot-label">{{ t('lot') }}</label>
        <el-select
          v-model="lotId"
          :placeholder="t('lotPlaceholder')"
          class="lot-select"
          @change="handleLotChange"
        >
          <el-option v-for="lot in lots" :key="lot.id" :label="lot.name" :value="lot.id" />
        </el-select>
      </div>

      <div class="spaces-layout">
        <!-- 左侧：位置 / 区域 -->
        <aside class="sidebar" v-loading="loading">
          <section class="side-block">
            <header>
              <strong>
                {{ t('locationInfo') }}
                <span v-if="locations.length > 0" class="count">({{ locations.length }})</span>
              </strong>
              <button type="button" class="manage-link" @click="openLocationDialog">
                {{ t('add') }}
              </button>
            </header>
            <p class="side-hint">{{ t('locationInfoHint') }}</p>
            <div class="chip-list scrollable">
              <button
                v-for="item in locations"
                :key="item.id"
                type="button"
                class="chip location"
                :class="{ active: selectedLocationId === item.id }"
                @click="selectLocation(item.id)"
              >
                {{ item.name }}
              </button>
              <p v-if="locations.length === 0" class="side-empty">
                <strong>{{ t('locationEmpty') }}</strong>
                <span>{{ t('locationEmptyHint') }}</span>
              </p>
            </div>
          </section>

          <section class="side-block" :class="{ muted: selectedLocationId === undefined }">
            <header>
              <strong>
                {{ t('areaInfo') }}
                <span v-if="areas.length > 0" class="count">({{ areas.length }})</span>
              </strong>
              <button
                type="button"
                class="manage-link"
                :disabled="selectedLocationId === undefined"
                @click="openAreaDialog"
              >
                {{ t('add') }}
              </button>
            </header>
            <p v-if="selectedLocationId === undefined" class="side-hint">
              {{ t('areaNeedLocation') }}
            </p>
            <p v-else class="side-hint">{{ t('areaInfoHint') }}</p>
            <template v-if="selectedLocationId !== undefined">
              <div class="chip-list scrollable">
                <button
                  v-for="item in areas"
                  :key="item.id"
                  type="button"
                  class="chip area"
                  :class="{ active: selectedAreaId === item.id }"
                  @click="selectArea(item.id)"
                >
                  {{ item.name }}
                </button>
                <p v-if="areas.length === 0" class="side-empty">
                  <strong>{{ t('areaEmpty') }}</strong>
                  <span>{{ t('areaEmptyHint') }}</span>
                </p>
              </div>
            </template>
          </section>
        </aside>

        <!-- 右侧：车位列表 -->
        <div class="main-panel" v-loading="loading">
          <div v-if="selectedLocationId !== undefined" class="context-bar">
            <span class="context-label">{{ t('currentContext') }}</span>
            <span class="context-chip location">{{ selectedLocation?.name }}</span>
            <span class="context-sep">/</span>
            <span class="context-chip area">{{ selectedArea?.name ?? '—' }}</span>
          </div>
          <div v-else class="context-bar muted">
            <span>{{ t('selectLocationFirst') }}</span>
          </div>

          <template v-if="!canLoadSpaces">
            <div class="table-card">
              <div class="empty">
                <strong>{{ selectedLocationId !== undefined ? t('selectAreaFirst') : t('selectLocationFirst') }}</strong>
                <p>
                  {{
                    selectedLocationId !== undefined ? t('areaEmptyHint') : t('locationEmptyHint')
                  }}
                </p>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="filter-bar">
              <label class="filter-field">
                <span>{{ t('colCode') }}</span>
                <el-input
                  v-model="searchInput"
                  class="search-input"
                  :placeholder="t('codePlaceholder')"
                  clearable
                  @keyup.enter="handleSearch"
                  @clear="handleReset"
                />
              </label>
              <div class="filter-actions">
                <el-button @click="handleReset">{{ t('reset') }}</el-button>
                <el-button type="primary" @click="handleSearch">{{ t('search') }}</el-button>
              </div>
            </div>

            <div class="action-bar">
              <el-button type="primary" plain @click="openImportDialog">{{ t('import') }}</el-button>
              <el-button type="primary" @click="openCreateDialog">{{ t('addSpace') }}</el-button>
            </div>

            <div class="table-card">
              <el-table :data="spaces" stripe>
                <el-table-column :label="t('colIndex')" width="70">
                  <template #default="{ $index }">{{ pageStart + $index }}</template>
                </el-table-column>
                <el-table-column prop="code" :label="t('colCode')" min-width="150" />
                <el-table-column :label="t('colStatus')" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
                      {{ row.enabled ? t('enabled') : t('disabled') }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('actions')" width="130" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openEditDialog(row)">{{ t('edit') }}</el-button>
                    <el-button link type="danger" @click="handleDeleteSpace(row)">{{ t('delete') }}</el-button>
                  </template>
                </el-table-column>

                <template #empty>
                  <el-empty :description="t('empty')" :image-size="80" />
                </template>
              </el-table>

              <div v-if="total > 0" class="pagination">
                <el-pagination
                  v-model:current-page="page"
                  v-model:page-size="pageSize"
                  :total="total"
                  :page-sizes="[10, 20, 50]"
                  layout="total, sizes, prev, pager, next"
                  background
                  @current-change="handlePageChange"
                  @size-change="handlePageSizeChange"
                />
              </div>
            </div>
          </template>
        </div>
      </div>
    </template>

    <div v-else class="empty-panel">
      <el-empty :description="t('noLot')" :image-size="80">
        <p class="empty-hint">{{ t('noLotHint') }}</p>
      </el-empty>
    </div>

    <!-- 新增 / 编辑车位 -->
    <el-dialog
      v-model="spaceDialog"
      :title="editing ? t('editTitle') : t('createTitle')"
      width="420px"
      destroy-on-close
      append-to-body
    >
      <p v-if="selectedLot && selectedLocation && selectedArea" class="dialog-context">
        <span class="ctx-chip">{{ selectedLot.name }}</span>
        <span class="ctx-sep">/</span>
        <span class="ctx-chip location">{{ selectedLocation.name }}</span>
        <span class="ctx-sep">/</span>
        <span class="ctx-chip area">{{ selectedArea.name }}</span>
      </p>
      <el-form label-width="90px" @submit.prevent>
        <el-form-item :label="t('colCode')">
          <el-input v-model="spaceForm.code" maxlength="64" placeholder="A-001" />
        </el-form-item>
        <el-form-item :label="t('colStatus')">
          <el-switch v-model="spaceForm.enabled" :active-text="t('enabled')" :inactive-text="t('disabled')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="spaceDialog = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmitSpace">
          {{ saving ? t('saving') : t('save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增位置 -->
    <el-dialog
      v-model="locationDialog"
      :title="t('createLocationTitle')"
      width="420px"
      destroy-on-close
      append-to-body
    >
      <p class="dialog-hint">{{ t('locationInfoHint') }}</p>
      <el-form label-width="90px" @submit.prevent>
        <el-form-item :label="t('locationName')">
          <el-input v-model="locationName" maxlength="64" :placeholder="t('locationNamePh')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="savingExtra" @click="locationDialog = false">{{ t('cancel') }}</el-button>
        <el-button :disabled="savingExtra" @click="submitLocation(true)">{{ t('saveAndContinue') }}</el-button>
        <el-button type="primary" :loading="savingExtra" @click="submitLocation(false)">
          {{ savingExtra ? t('saving') : t('save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 新增区域 -->
    <el-dialog
      v-model="areaDialog"
      :title="t('createAreaTitle')"
      width="420px"
      destroy-on-close
      append-to-body
    >
      <p v-if="selectedLocation" class="dialog-context">
        <span class="ctx-chip location">{{ selectedLocation.name }}</span>
      </p>
      <p class="dialog-hint">{{ t('areaInfoHint') }}</p>
      <el-form label-width="90px" @submit.prevent>
        <el-form-item :label="t('areaName')">
          <el-input v-model="areaName" maxlength="64" :placeholder="t('areaNamePh')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="savingExtra" @click="areaDialog = false">{{ t('cancel') }}</el-button>
        <el-button :disabled="savingExtra" @click="submitArea(true)">{{ t('saveAndContinue') }}</el-button>
        <el-button type="primary" :loading="savingExtra" @click="submitArea(false)">
          {{ savingExtra ? t('saving') : t('save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog
      v-model="importVisible"
      :title="t('importTitle')"
      width="560px"
      :close-on-click-modal="false"
      append-to-body
    >
      <div class="import-area">
        <p class="import-hint">{{ t('importHint') }}</p>
        <p v-if="selectedLocation && selectedArea" class="import-context">
          {{ t('importToArea') }}：
          <span class="ctx-chip location">{{ selectedLocation.name }}</span>
          <span class="ctx-sep">/</span>
          <span class="ctx-chip area">{{ selectedArea.name }}</span>
        </p>
        <div class="import-toolbar">
          <el-button plain :loading="downloadingTemplate" @click="handleDownloadTemplate">
            {{ t('downloadTemplate') }}
          </el-button>
          <el-button plain @click="pickFile">
            {{ importFile ? t('changeFile') : t('chooseFile') }}
          </el-button>
          <input
            ref="fileInput"
            type="file"
            class="hidden-file"
            accept=".xlsx,.xls,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.ms-excel"
            @change="onFileChange"
          />
        </div>
        <p v-if="importFile" class="file-name">{{ importFile.name }}</p>
        <p v-if="importError" class="form-error">{{ importError }}</p>
      </div>
      <template #footer>
        <el-button :disabled="importing" @click="importVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="handleImport">
          {{ importing ? t('importing') : t('startImport') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.spaces-page {
  display: grid;
  gap: 14px;
  animation: fade-in 0.35s ease;
}

.empty-panel {
  display: flex;
  justify-content: center;
  padding: 32px 0;
}

.empty-hint {
  margin: 0;
  color: var(--fp-muted);
  font-size: 0.9rem;
}

.lot-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
  padding: 12px 16px;
}

.lot-label {
  color: var(--fp-muted);
  font-size: 0.9rem;
  font-weight: 600;
  white-space: nowrap;
}

.lot-select {
  width: 240px;
}

.spaces-layout {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.sidebar {
  display: grid;
  gap: 14px;
}

.side-block {
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
  padding: 12px;
}

.side-block.muted {
  opacity: 0.82;
}

.side-block header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.side-block header strong {
  font-size: 0.95rem;
}

.count {
  margin-left: 4px;
  color: var(--fp-muted);
  font-size: 0.82rem;
  font-weight: 600;
}

.manage-link {
  border: 0;
  background: none;
  padding: 0;
  color: var(--fp-accent, var(--fp-teal-deep));
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
}

.manage-link:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.side-hint {
  margin: 0 0 8px;
  color: var(--fp-muted);
  font-size: 0.82rem;
  line-height: 1.5;
}

.chip-list {
  display: grid;
  gap: 8px;
}

.chip-list.scrollable {
  max-height: 11rem;
  overflow-y: auto;
}

.chip {
  width: 100%;
  border: 1px solid var(--fp-line);
  border-radius: 8px;
  padding: 7px 10px;
  text-align: center;
  font-size: 0.9rem;
  font-weight: 600;
  background: var(--fp-surface);
  color: var(--fp-ink);
  cursor: pointer;
}

.chip.location.active {
  background: var(--fp-teal-deep);
  border-color: var(--fp-teal-deep);
  color: #fff;
}

.chip.area.active {
  background: #2f9e44;
  border-color: #2f9e44;
  color: #fff;
}

.side-empty {
  display: grid;
  gap: 4px;
  margin: 0;
  padding: 4px 2px;
  color: var(--fp-muted);
  font-size: 0.85rem;
  line-height: 1.5;
}

.main-panel {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.context-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
  padding: 10px 14px;
  font-size: 0.9rem;
}

.context-bar.muted {
  color: var(--fp-muted);
}

.context-label {
  color: var(--fp-muted);
  font-weight: 600;
}

.ctx-chip {
  display: inline-block;
  border-radius: 6px;
  padding: 1px 10px;
  font-weight: 600;
}

.ctx-chip.location {
  background: var(--fp-teal-deep);
  color: #fff;
}

.ctx-chip.area {
  background: #2f9e44;
  color: #fff;
}

.ctx-sep {
  color: var(--fp-muted);
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
  padding: 12px 14px;
}

.filter-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-field > span {
  color: var(--fp-muted);
  font-size: 0.88rem;
  white-space: nowrap;
}

.search-input {
  width: 220px;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.action-bar {
  display: flex;
  gap: 8px;
}

.table-card {
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
  overflow: hidden;
}

.empty {
  padding: 42px 16px;
  text-align: center;
}

.empty strong {
  display: block;
  margin-bottom: 6px;
}

.empty p {
  margin: 0 auto;
  max-width: 30rem;
  color: var(--fp-muted);
  line-height: 1.6;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px;
  border-top: 1px solid var(--fp-line);
}

.dialog-context {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0 0 10px;
}

.dialog-hint {
  margin: 0 0 10px;
  color: var(--fp-muted);
  font-size: 0.88rem;
  line-height: 1.5;
}

.import-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.import-hint {
  margin: 0;
  color: var(--fp-muted);
  font-size: 0.9rem;
  line-height: 1.6;
}

.import-context {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  color: var(--fp-ink-soft);
  font-size: 0.9rem;
}

.import-toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hidden-file {
  display: none;
}

.file-name {
  margin: 0;
  color: var(--fp-muted);
  font-size: 0.85rem;
  word-break: break-all;
}

.form-error {
  margin: 0;
  color: var(--fp-danger);
  font-size: 0.88rem;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@media (max-width: 900px) {
  .spaces-layout {
    grid-template-columns: 1fr;
  }
}
</style>
