<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
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

interface InternalVehicleItem {
  id: number
  lotId: number
  lotName: string
  plateNumber: string
  plateColor: string
  ownerName: string
  type: string
  phone: string
  department: string
  remark: string
  batchId: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

interface VehicleImportResult {
  batchId: string
  imported: number
  skipped: number
}

const VEHICLE_TYPES = ['TEMPORARY', 'RESERVED', 'VIP', 'OWNER', 'MONTHLY', 'OTHER'] as const
const PLATE_COLORS = ['BLUE', 'YELLOW', 'GREEN', 'YELLOW_GREEN', 'BLACK', 'WHITE', 'OTHER'] as const

const PLATE_COLOR_TEXT: Record<string, { zh: string; en: string }> = {
  BLUE: { zh: '蓝牌', en: 'Blue' },
  YELLOW: { zh: '黄牌', en: 'Yellow' },
  GREEN: { zh: '绿牌', en: 'Green' },
  YELLOW_GREEN: { zh: '黄绿牌', en: 'Yellow-green' },
  BLACK: { zh: '黑牌', en: 'Black' },
  WHITE: { zh: '白牌', en: 'White' },
  OTHER: { zh: '其他', en: 'Other' }
}

/* ---------------- 双语文案 ---------------- */

const d: BiDict = {
  lot: { 'zh-CN': '车场', en: 'Parking lot' },
  lotPlaceholder: { 'zh-CN': '请选择车场', en: 'Select a parking lot' },
  noLot: { 'zh-CN': '暂无车场数据', en: 'No parking lots' },
  noLotHint: {
    'zh-CN': '请先在车场管理中创建车场，再来管理内部车辆。',
    en: 'Please create a parking lot first.'
  },
  plate: { 'zh-CN': '车牌号', en: 'Plate number' },
  platePlaceholder: { 'zh-CN': '输入车牌号搜索', en: 'Search by plate number' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  add: { 'zh-CN': '新增车辆', en: 'Add vehicle' },
  import: { 'zh-CN': '批量导入', en: 'Import' },
  export: { 'zh-CN': '导出', en: 'Export' },
  exporting: { 'zh-CN': '导出中…', en: 'Exporting…' },
  empty: { 'zh-CN': '暂无内部车辆数据', en: 'No vehicles yet' },
  plateColor: { 'zh-CN': '车牌颜色', en: 'Plate color' },
  owner: { 'zh-CN': '车主', en: 'Owner' },
  type: { 'zh-CN': '车辆类型', en: 'Vehicle type' },
  phone: { 'zh-CN': '联系电话', en: 'Phone' },
  department: { 'zh-CN': '部门', en: 'Department' },
  remark: { 'zh-CN': '备注', en: 'Remark' },
  batch: { 'zh-CN': '导入批次', en: 'Import batch' },
  enabled: { 'zh-CN': '启用', en: 'Enabled' },
  disabled: { 'zh-CN': '停用', en: 'Disabled' },
  updateTime: { 'zh-CN': '更新时间', en: 'Updated at' },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  deleteBatch: { 'zh-CN': '删除批次', en: 'Delete batch' },
  createTitle: { 'zh-CN': '新增内部车辆', en: 'Add vehicle' },
  editTitle: { 'zh-CN': '编辑内部车辆', en: 'Edit vehicle' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  save: { 'zh-CN': '保存', en: 'Save' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  close: { 'zh-CN': '关闭', en: 'Close' },
  plateRequired: { 'zh-CN': '请输入车牌号', en: 'Please enter the plate number' },
  ownerRequired: { 'zh-CN': '请输入车主姓名', en: 'Please enter the owner name' },
  createSuccess: { 'zh-CN': '车辆创建成功', en: 'Vehicle created' },
  updateSuccess: { 'zh-CN': '车辆更新成功', en: 'Vehicle updated' },
  deleteTitle: { 'zh-CN': '删除确认', en: 'Confirm deletion' },
  deleteConfirm: { 'zh-CN': '确定删除车辆「{plate}」吗？', en: 'Delete vehicle "{plate}"?' },
  deleteSuccess: { 'zh-CN': '车辆已删除', en: 'Vehicle deleted' },
  batchDeleteTitle: { 'zh-CN': '删除批次确认', en: 'Confirm batch deletion' },
  batchDeleteConfirm: {
    'zh-CN': '确定删除该导入批次的所有车辆吗？此操作不可恢复。',
    en: 'Delete all vehicles of this import batch? This cannot be undone.'
  },
  batchDeleteSuccess: { 'zh-CN': '已删除 {deleted} 辆车', en: '{deleted} vehicles deleted' },
  lotLoadFailed: { 'zh-CN': '车场列表加载失败', en: 'Failed to load lots' },
  loadFailed: { 'zh-CN': '内部车辆列表加载失败', en: 'Failed to load vehicles' },
  submitFailed: { 'zh-CN': '操作失败，请重试', en: 'Operation failed. Please retry.' },
  importTitle: { 'zh-CN': '批量导入内部车辆', en: 'Import vehicles' },
  importHint: {
    'zh-CN': '请先下载模板并按格式填写，导入成功后可在弹窗内一键删除本次批次。',
    en: 'Download the template and fill it in. After a successful import you can delete the batch here.'
  },
  downloadTemplate: { 'zh-CN': '下载模板', en: 'Download template' },
  chooseFile: { 'zh-CN': '选择文件', en: 'Choose file' },
  changeFile: { 'zh-CN': '重新选择', en: 'Choose again' },
  fileRequired: { 'zh-CN': '请选择要导入的 Excel 文件', en: 'Please choose an Excel file' },
  startImport: { 'zh-CN': '开始导入', en: 'Import' },
  importing: { 'zh-CN': '导入中…', en: 'Importing…' },
  importSuccess: {
    'zh-CN': '导入完成：成功 {imported} 辆，跳过 {skipped} 辆',
    en: 'Done: {imported} imported, {skipped} skipped'
  },
  importFailed: { 'zh-CN': '导入失败', en: 'Import failed' },
  downloadFailed: { 'zh-CN': '模板下载失败', en: 'Failed to download template' },
  exportFailed: { 'zh-CN': '导出失败', en: 'Failed to export' },
  exportSuccess: { 'zh-CN': '导出成功', en: 'Export started' },
  deletingBatch: { 'zh-CN': '删除中…', en: 'Deleting…' },
  typeTEMPORARY: { 'zh-CN': '临时车', en: 'Temporary' },
  typeRESERVED: { 'zh-CN': '预约车', en: 'Reserved' },
  typeVIP: { 'zh-CN': 'VIP 车辆', en: 'VIP' },
  typeOWNER: { 'zh-CN': '业主车辆', en: 'Owner' },
  typeMONTHLY: { 'zh-CN': '月租车辆', en: 'Monthly' },
  typeOTHER: { 'zh-CN': '其他', en: 'Other' }
}

const { t, locale } = useBiText(d)
const route = useRoute()

/** 替换文案中的 {placeholder} 占位符 */
function fill(template: string, values: Record<string, string | number>): string {
  return template.replace(/\{(\w+)\}/g, (_m: string, key: string) => String(values[key] ?? ''))
}

function errorTextOf(e: unknown, fallback: string): string {
  return e instanceof Error && e.message ? e.message : fallback
}

function vehicleTypeLabel(type: string): string {
  return t(`type${type}`)
}

function plateColorLabel(color: string): string {
  const entry = PLATE_COLOR_TEXT[color]
  if (!entry) {
    return color
  }
  return locale.value === 'en' ? entry.en : entry.zh
}

function shortBatchId(batchId: string): string {
  return batchId.length > 8 ? batchId.slice(0, 8) : batchId
}

/* ---------------- 列表状态 ---------------- */

const loading = ref(false)
const rows = ref<InternalVehicleItem[]>([])
const total = ref(0)
const lots = ref<LotItem[]>([])
const lotId = ref<number>()
const query = reactive({
  plate: '',
  page: 1,
  size: 10
})
const appliedPlate = ref('')

async function loadLots(): Promise<void> {
  try {
    const data = await request.get<never, LotItem[]>('/lots')
    lots.value = data
    if (lots.value.length === 0) {
      lotId.value = undefined
      return
    }
    const queryLotId = Number(route.query.lotId)
    const target =
      lots.value.find((lot) => queryLotId > 0 && lot.id === queryLotId) ?? lots.value[0]
    if (target) {
      lotId.value = target.id
      await loadList()
    }
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('lotLoadFailed')))
  }
}

async function loadList(): Promise<void> {
  if (!lotId.value) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const params: Record<string, string | number> = { page: query.page, size: query.size }
    if (appliedPlate.value) {
      params.plate = appliedPlate.value
    }
    const data = await request.get<never, PageResult<InternalVehicleItem>>(
      `/lots/${lotId.value}/internal-vehicles`,
      { params }
    )
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('loadFailed')))
  } finally {
    loading.value = false
  }
}

async function handleLotChange(): Promise<void> {
  query.plate = ''
  appliedPlate.value = ''
  query.page = 1
  await loadList()
}

function handleSearch(): void {
  appliedPlate.value = query.plate.trim()
  query.page = 1
  loadList()
}

function handleReset(): void {
  query.plate = ''
  appliedPlate.value = ''
  query.page = 1
  loadList()
}

function handlePageChange(page: number): void {
  query.page = page
  loadList()
}

/* ---------------- 新增 / 编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const editing = ref(false)
const editingId = ref<number>()
const saving = ref(false)
const form = reactive({
  plateNumber: '',
  plateColor: 'BLUE',
  type: 'OTHER',
  ownerName: '',
  phone: '',
  department: '',
  remark: '',
  enabled: true
})

function openCreateDialog(): void {
  if (!lotId.value) {
    return
  }
  editing.value = false
  editingId.value = undefined
  form.plateNumber = ''
  form.plateColor = 'BLUE'
  form.type = 'OTHER'
  form.ownerName = ''
  form.phone = ''
  form.department = ''
  form.remark = ''
  form.enabled = true
  dialogVisible.value = true
}

function openEditDialog(row: InternalVehicleItem): void {
  editing.value = true
  editingId.value = row.id
  form.plateNumber = row.plateNumber
  form.plateColor = row.plateColor
  form.type = row.type
  form.ownerName = row.ownerName
  form.phone = row.phone ?? ''
  form.department = row.department ?? ''
  form.remark = row.remark ?? ''
  form.enabled = row.enabled
  dialogVisible.value = true
}

async function handleSubmit(): Promise<void> {
  if (!lotId.value) {
    return
  }
  const plateNumber = form.plateNumber.trim()
  if (!plateNumber) {
    ElMessage.warning(t('plateRequired'))
    return
  }
  const ownerName = form.ownerName.trim()
  if (!ownerName) {
    ElMessage.warning(t('ownerRequired'))
    return
  }
  saving.value = true
  const payload = {
    plateNumber,
    plateColor: form.plateColor,
    ownerName,
    type: form.type,
    phone: form.phone.trim() || undefined,
    department: form.department.trim() || undefined,
    remark: form.remark.trim() || undefined,
    enabled: form.enabled
  }
  try {
    if (editing.value && editingId.value) {
      await request.put<never, InternalVehicleItem>(
        `/lots/${lotId.value}/internal-vehicles/${editingId.value}`,
        payload
      )
      ElMessage.success(t('updateSuccess'))
    } else {
      await request.post<never, InternalVehicleItem>(
        `/lots/${lotId.value}/internal-vehicles`,
        payload
      )
      ElMessage.success(t('createSuccess'))
    }
    dialogVisible.value = false
    handleSearch()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('submitFailed')))
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: InternalVehicleItem): Promise<void> {
  if (!lotId.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      fill(t('deleteConfirm'), { plate: row.plateNumber }),
      t('deleteTitle'),
      {
        type: 'warning',
        confirmButtonText: t('confirm'),
        cancelButtonText: t('cancel')
      }
    )
  } catch {
    return
  }
  try {
    await request.delete<never, void>(`/lots/${lotId.value}/internal-vehicles/${row.id}`)
    ElMessage.success(t('deleteSuccess'))
    loadList()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('submitFailed')))
  }
}

async function handleDeleteBatch(batchId: string): Promise<void> {
  if (!lotId.value || !batchId) {
    return
  }
  try {
    await ElMessageBox.confirm(t('batchDeleteConfirm'), t('batchDeleteTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  try {
    const deleted = await request.delete<never, number>(
      `/lots/${lotId.value}/internal-vehicles/batch/${encodeURIComponent(batchId)}`
    )
    ElMessage.success(fill(t('batchDeleteSuccess'), { deleted }))
    loadList()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('submitFailed')))
  }
}

/* ---------------- 导出 ---------------- */

const exporting = ref(false)

async function handleExport(): Promise<void> {
  if (!lotId.value) {
    return
  }
  exporting.value = true
  try {
    const plate = appliedPlate.value
    const path = plate
      ? `/lots/${lotId.value}/internal-vehicles/export?plate=${encodeURIComponent(plate)}`
      : `/lots/${lotId.value}/internal-vehicles/export`
    await downloadFile(path, 'internal-vehicles.xlsx')
    ElMessage.success(t('exportSuccess'))
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('exportFailed')))
  } finally {
    exporting.value = false
  }
}

/* ---------------- 批量导入 ---------------- */

const importVisible = ref(false)
const importing = ref(false)
const importFile = ref<File>()
const importError = ref('')
const importResult = ref<VehicleImportResult>()
const deletingResultBatch = ref(false)
const downloadingTemplate = ref(false)
const fileInput = ref<HTMLInputElement>()

function resetImport(): void {
  importFile.value = undefined
  importError.value = ''
  importResult.value = undefined
  nextTick(() => {
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  })
}

function openImportDialog(): void {
  if (!lotId.value) {
    return
  }
  resetImport()
  importVisible.value = true
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
  if (!lotId.value) {
    return
  }
  downloadingTemplate.value = true
  try {
    await downloadFile(
      `/lots/${lotId.value}/internal-vehicles/import-template`,
      'internal-vehicles-template.xlsx'
    )
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('downloadFailed')))
  } finally {
    downloadingTemplate.value = false
  }
}

async function handleImport(): Promise<void> {
  if (!lotId.value) {
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
    const result = await request.post<never, VehicleImportResult>(
      `/lots/${lotId.value}/internal-vehicles/import`,
      fd
    )
    importResult.value = result
    ElMessage.success(fill(t('importSuccess'), { imported: result.imported, skipped: result.skipped }))
    await loadList()
  } catch (error) {
    importError.value = errorTextOf(error, t('importFailed'))
  } finally {
    importing.value = false
  }
}

async function handleDeleteResultBatch(): Promise<void> {
  if (!lotId.value) {
    return
  }
  const batchId = importResult.value?.batchId
  if (!batchId) {
    return
  }
  try {
    await ElMessageBox.confirm(t('batchDeleteConfirm'), t('batchDeleteTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  deletingResultBatch.value = true
  try {
    const deleted = await request.delete<never, number>(
      `/lots/${lotId.value}/internal-vehicles/batch/${encodeURIComponent(batchId)}`
    )
    importVisible.value = false
    ElMessage.success(fill(t('batchDeleteSuccess'), { deleted }))
    await loadList()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('submitFailed')))
  } finally {
    deletingResultBatch.value = false
  }
}

onMounted(() => {
  loadLots()
})
</script>

<template>
  <section class="vehicles-page">
    <el-card class="panel" shadow="never">
      <div v-if="lots.length === 0" class="empty-panel">
        <el-empty :description="t('noLot')" :image-size="80">
          <p class="empty-hint">{{ t('noLotHint') }}</p>
        </el-empty>
      </div>

      <template v-else>
        <div class="toolbar">
          <div class="field">
            <span class="field-label">{{ t('lot') }}</span>
            <el-select
              v-model="lotId"
              :placeholder="t('lotPlaceholder')"
              class="lot-select"
              @change="handleLotChange"
            >
              <el-option v-for="lot in lots" :key="lot.id" :label="lot.name" :value="lot.id" />
            </el-select>
          </div>

          <div class="field">
            <el-input
              v-model="query.plate"
              class="search-input"
              :placeholder="t('platePlaceholder')"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </div>

          <el-button type="primary" @click="handleSearch">{{ t('search') }}</el-button>
          <el-button @click="handleReset">{{ t('reset') }}</el-button>

          <div class="spacer" />
          <el-button type="primary" plain :loading="exporting" @click="handleExport">
            {{ exporting ? t('exporting') : t('export') }}
          </el-button>
          <el-button type="primary" plain @click="openImportDialog">{{ t('import') }}</el-button>
          <el-button type="primary" @click="openCreateDialog">{{ t('add') }}</el-button>
        </div>

        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column :label="t('plate')" min-width="180">
            <template #default="{ row }">
              <span class="plate-text">{{ row.plateNumber }}</span>
              <el-tag size="small" effect="plain" class="color-tag">
                {{ plateColorLabel(row.plateColor) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ownerName" :label="t('owner')" min-width="130" />
          <el-table-column :label="t('type')" min-width="110">
            <template #default="{ row }">{{ vehicleTypeLabel(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="phone" :label="t('phone')" min-width="130">
            <template #default="{ row }">{{ row.phone || '—' }}</template>
          </el-table-column>
          <el-table-column prop="department" :label="t('department')" min-width="120">
            <template #default="{ row }">{{ row.department || '—' }}</template>
          </el-table-column>
          <el-table-column prop="remark" :label="t('remark')" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.remark || '—' }}</template>
          </el-table-column>
          <el-table-column :label="t('batch')" min-width="130">
            <template #default="{ row }">
              <el-tag v-if="row.batchId" size="small" effect="plain" :title="row.batchId" class="batch-tag">
                {{ shortBatchId(row.batchId) }}
              </el-tag>
              <span v-else class="muted-text">—</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('enabled')" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
                {{ row.enabled ? t('enabled') : t('disabled') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" :label="t('updateTime')" min-width="170" />
          <el-table-column :label="t('actions')" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEditDialog(row)">{{ t('edit') }}</el-button>
              <el-button
                v-if="row.batchId"
                link
                type="warning"
                @click="handleDeleteBatch(row.batchId)"
              >
                {{ t('deleteBatch') }}
              </el-button>
              <el-button link type="danger" @click="handleDelete(row)">{{ t('delete') }}</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty :description="t('empty')" :image-size="80" />
          </template>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="query.page"
            :total="total"
            :page-size="query.size"
            layout="total, prev, pager, next"
            background
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </el-card>

    <!-- 新增 / 编辑车辆 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? t('editTitle') : t('createTitle')"
      width="520px"
      destroy-on-close
      append-to-body
    >
      <el-form :model="form" label-width="100px">
        <el-form-item :label="t('plate')">
          <el-input v-model="form.plateNumber" maxlength="16" />
        </el-form-item>
        <el-form-item :label="t('plateColor')">
          <el-select v-model="form.plateColor" style="width: 100%">
            <el-option
              v-for="color in PLATE_COLORS"
              :key="color"
              :label="plateColorLabel(color)"
              :value="color"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('owner')">
          <el-input v-model="form.ownerName" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('type')">
          <el-select v-model="form.type" style="width: 100%">
            <el-option
              v-for="type in VEHICLE_TYPES"
              :key="type"
              :label="vehicleTypeLabel(type)"
              :value="type"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('phone')">
          <el-input v-model="form.phone" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('department')">
          <el-input v-model="form.department" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('remark')">
          <el-input v-model="form.remark" maxlength="256" />
        </el-form-item>
        <el-form-item :label="t('enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ saving ? t('saving') : t('save') }}
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
      <div v-if="!importResult" class="import-area">
        <p class="import-hint">{{ t('importHint') }}</p>
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

      <div v-else class="import-area">
        <el-alert
          :title="fill(t('importSuccess'), { imported: importResult.imported, skipped: importResult.skipped })"
          type="success"
          :closable="false"
          show-icon
        />
        <p v-if="importResult.batchId" class="batch-line">
          <span class="batch-label">{{ t('batch') }}：</span>
          <span class="batch-value">{{ shortBatchId(importResult.batchId) }}</span>
        </p>
      </div>

      <template #footer>
        <template v-if="!importResult">
          <el-button :disabled="importing" @click="importVisible = false">{{ t('cancel') }}</el-button>
          <el-button type="primary" :loading="importing" :disabled="!importFile" @click="handleImport">
            {{ importing ? t('importing') : t('startImport') }}
          </el-button>
        </template>
        <template v-else>
          <el-button
            v-if="importResult.batchId"
            type="danger"
            :loading="deletingResultBatch"
            @click="handleDeleteResultBatch"
          >
            {{ deletingResultBatch ? t('deletingBatch') : t('deleteBatch') }}
          </el-button>
          <el-button type="primary" @click="importVisible = false">{{ t('close') }}</el-button>
        </template>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.vehicles-page {
  animation: fade-in 0.35s ease;
}

.panel {
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-label {
  color: var(--fp-muted);
  font-size: 0.88rem;
  white-space: nowrap;
}

.lot-select {
  width: 230px;
}

.search-input {
  width: 240px;
}

.spacer {
  flex: 1;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.plate-text {
  font-weight: 600;
  margin-right: 8px;
}

.color-tag {
  margin-right: 2px;
}

.batch-tag {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
}

.muted-text {
  color: var(--fp-muted);
  font-size: 0.88rem;
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

.batch-line {
  margin: 0;
  color: var(--fp-ink-soft);
  font-size: 0.9rem;
}

.batch-label {
  color: var(--fp-muted);
}

.batch-value {
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-weight: 600;
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
