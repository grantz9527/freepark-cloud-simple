<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadFile,
  type UploadInstance
} from 'element-plus'
import request from '../utils/request'
import { downloadFile } from '../utils/download'
import { useBiText, type BiDict } from '../utils/biText'

const PLATE_COLORS = ['BLUE', 'YELLOW', 'GREEN', 'YELLOW_GREEN', 'BLACK', 'WHITE', 'OTHER'] as const
type PlateColorValue = (typeof PLATE_COLORS)[number]

const d: BiDict = {
  lotLabel: { 'zh-CN': '停车场', en: 'Parking Lot' },
  lotPlaceholder: { 'zh-CN': '请选择停车场', en: 'Select a parking lot' },
  noLots: { 'zh-CN': '暂无停车场数据，请先在「车场管理」中创建', en: 'No parking lots yet. Create one in Lot Management first' },
  searchPlaceholder: { 'zh-CN': '输入车牌号检索', en: 'Search by plate number' },
  query: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  add: { 'zh-CN': '新增黑名单', en: 'Add Blacklist' },
  import: { 'zh-CN': '导入', en: 'Import' },
  export: { 'zh-CN': '导出', en: 'Export' },
  thPlate: { 'zh-CN': '车牌', en: 'Plate' },
  thOwner: { 'zh-CN': '车主', en: 'Owner' },
  thPhone: { 'zh-CN': '联系电话', en: 'Phone' },
  thDepartment: { 'zh-CN': '部门', en: 'Department' },
  thStartTime: { 'zh-CN': '生效时间', en: 'Start Time' },
  thEndTime: { 'zh-CN': '失效时间', en: 'End Time' },
  thStatus: { 'zh-CN': '状态', en: 'Status' },
  thActions: { 'zh-CN': '操作', en: 'Actions' },
  enabled: { 'zh-CN': '启用', en: 'Enabled' },
  disabled: { 'zh-CN': '停用', en: 'Disabled' },
  empty: { 'zh-CN': '暂无数据', en: 'No data' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  deleteTitle: { 'zh-CN': '删除确认', en: 'Confirm Delete' },
  deleteConfirm: {
    'zh-CN': '确定删除该黑名单车辆吗？删除后不可恢复。',
    en: 'Delete this blacklist vehicle? This cannot be undone.'
  },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  addTitle: { 'zh-CN': '新增黑名单车辆', en: 'New Blacklist Vehicle' },
  editTitle: { 'zh-CN': '编辑黑名单车辆', en: 'Edit Blacklist Vehicle' },
  lblPlate: { 'zh-CN': '车牌号', en: 'Plate Number' },
  platePlaceholder: { 'zh-CN': '请输入车牌号，如 京A12345', en: 'Enter plate number, e.g. BJ·A12345' },
  lblColor: { 'zh-CN': '车牌颜色', en: 'Plate Color' },
  lblOwner: { 'zh-CN': '车主名称', en: 'Owner Name' },
  ownerPlaceholder: { 'zh-CN': '请输入车主名称', en: 'Enter owner name' },
  lblPhone: { 'zh-CN': '联系电话', en: 'Phone' },
  phonePlaceholder: { 'zh-CN': '请输入联系电话', en: 'Enter phone number' },
  lblDepartment: { 'zh-CN': '部门', en: 'Department' },
  departmentPlaceholder: { 'zh-CN': '请输入部门', en: 'Enter department' },
  lblRemark: { 'zh-CN': '备注', en: 'Remark' },
  remarkPlaceholder: { 'zh-CN': '选填', en: 'Optional' },
  lblStartTime: { 'zh-CN': '生效时间', en: 'Start Time' },
  lblEndTime: { 'zh-CN': '失效时间', en: 'End Time' },
  lblEnabled: { 'zh-CN': '启用该黑名单', en: 'Enable this blacklist entry' },
  save: { 'zh-CN': '保存', en: 'Save' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  msgPlateRequired: { 'zh-CN': '请输入车牌号', en: 'Plate number is required' },
  msgOwnerRequired: { 'zh-CN': '请输入车主名称', en: 'Owner name is required' },
  msgTimeRequired: { 'zh-CN': '请填写完整的生效与失效时间', en: 'Start and end time are required' },
  msgTimeInvalid: { 'zh-CN': '失效时间必须晚于生效时间', en: 'End time must be after start time' },
  msgSaveOk: { 'zh-CN': '保存成功', en: 'Saved successfully' },
  msgDeleteOk: { 'zh-CN': '删除成功', en: 'Deleted successfully' },
  msgExportOk: { 'zh-CN': '导出成功', en: 'Exported successfully' },
  errLoad: { 'zh-CN': '加载失败，请稍后重试', en: 'Failed to load, please retry later' },
  errSave: { 'zh-CN': '保存失败', en: 'Failed to save' },
  errDelete: { 'zh-CN': '删除失败', en: 'Failed to delete' },
  errExport: { 'zh-CN': '导出失败', en: 'Failed to export' },
  errTemplate: { 'zh-CN': '模板下载失败', en: 'Failed to download template' },
  errImport: { 'zh-CN': '导入失败', en: 'Import failed' },
  importTitle: { 'zh-CN': '批量导入黑名单', en: 'Batch Import Blacklist' },
  importStepHint: {
    'zh-CN': '支持 .xlsx 文件，请先下载模板填写后上传；时间非法等数据行将被跳过。',
    en: '.xlsx files are supported. Download the template first, then upload the filled file; invalid rows (e.g. bad time) will be skipped.'
  },
  downloadTemplate: { 'zh-CN': '下载导入模板', en: 'Download Template' },
  chooseFile: { 'zh-CN': '点击选择 .xlsx 文件', en: 'Click to choose a .xlsx file' },
  uploadFileRequired: { 'zh-CN': '请先选择要导入的文件', en: 'Please choose a file to import' },
  startImport: { 'zh-CN': '开始导入', en: 'Import' },
  importing: { 'zh-CN': '导入中…', en: 'Importing…' },
  'color.BLUE': { 'zh-CN': '蓝牌', en: 'Blue' },
  'color.YELLOW': { 'zh-CN': '黄牌', en: 'Yellow' },
  'color.GREEN': { 'zh-CN': '绿牌（新能源）', en: 'Green (New Energy)' },
  'color.YELLOW_GREEN': { 'zh-CN': '黄绿牌', en: 'Yellow-Green' },
  'color.BLACK': { 'zh-CN': '黑牌', en: 'Black' },
  'color.WHITE': { 'zh-CN': '白牌', en: 'White' },
  'color.OTHER': { 'zh-CN': '其他', en: 'Other' }
}
const { t, locale } = useBiText(d)

const route = useRoute()

interface LotItem {
  id: number
  name: string
  code: string
}

interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

interface BlacklistItem {
  id: number
  plateNumber: string
  plateColor: string
  ownerName: string
  phone?: string | null
  department?: string | null
  remark?: string | null
  startTime: string
  endTime: string
  enabled: boolean
  createdAt: string
  updatedAt: string
}

const loading = ref(false)
const saving = ref(false)
const rows = ref<BlacklistItem[]>([])
const total = ref(0)

const lots = ref<LotItem[]>([])
const selectedLotId = ref<number | null>(null)

const query = reactive({
  plate: '',
  page: 1,
  size: 10
})

/* ---------------- 基础方法 ---------------- */

function fmtTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 16)
}

function toLocalInput(value?: string | null): string {
  return value ? value.slice(0, 16) : ''
}

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function importDoneText(imported: number, skipped: number): string {
  return locale.value === 'en'
    ? `Import finished: ${imported} imported, ${skipped} skipped`
    : `导入完成：成功 ${imported} 条，跳过 ${skipped} 条`
}

function colorLabel(color: string): string {
  return t(`color.${color}`)
}

/* ---------------- 车场加载与切换 ---------------- */

async function loadLots() {
  const list = await request.get<never, LotItem[]>('/lots')
  lots.value = list
  const raw = route.query.lotId
  const target = Array.isArray(raw) ? raw[0] : raw
  const matched = lots.value.find((lot) => String(lot.id) === String(target))
  selectedLotId.value = matched ? matched.id : (lots.value[0]?.id ?? null)
}

function handleLotChange() {
  query.plate = ''
  query.page = 1
  loadList()
}

/* ---------------- 列表 ---------------- */

async function loadList() {
  if (selectedLotId.value == null) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const data = await request.get<never, PageResult<BlacklistItem>>(
      `/lots/${selectedLotId.value}/blacklist-vehicles`,
      {
        params: {
          plate: query.plate.trim() || undefined,
          page: query.page,
          size: query.size
        }
      }
    )
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(errorText(error, t('errLoad')))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function handleReset() {
  query.plate = ''
  query.page = 1
  loadList()
}

function handlePageChange(page: number) {
  query.page = page
  loadList()
}

function handleSizeChange(size: number) {
  query.size = size
  query.page = 1
  loadList()
}

/* ---------------- 新增 / 编辑 ---------------- */

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const isEditing = computed(() => editingId.value !== null)
const formRef = ref<FormInstance>()

const form = reactive({
  plateNumber: '',
  plateColor: 'BLUE' as PlateColorValue,
  ownerName: '',
  phone: '',
  department: '',
  remark: '',
  startTime: '',
  endTime: '',
  enabled: true
})

const rules = computed<FormRules>(() => ({
  plateNumber: [{ required: true, message: t('msgPlateRequired'), trigger: 'blur' }],
  ownerName: [{ required: true, message: t('msgOwnerRequired'), trigger: 'blur' }],
  startTime: [{ required: true, message: t('msgTimeRequired'), trigger: 'change' }],
  endTime: [{ validator: validateEndTime, trigger: 'change' }]
}))

function validateEndTime(
  _rule: unknown,
  _value: string,
  callback: (error?: Error) => void
): void {
  if (!form.startTime || !form.endTime) {
    callback(new Error(t('msgTimeRequired')))
    return
  }
  if (form.endTime <= form.startTime) {
    callback(new Error(t('msgTimeInvalid')))
    return
  }
  callback()
}

function resetForm() {
  editingId.value = null
  form.plateNumber = ''
  form.plateColor = 'BLUE'
  form.ownerName = ''
  form.phone = ''
  form.department = ''
  form.remark = ''
  form.startTime = ''
  form.endTime = ''
  form.enabled = true
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: BlacklistItem) {
  editingId.value = row.id
  form.plateNumber = row.plateNumber
  form.plateColor = row.plateColor as PlateColorValue
  form.ownerName = row.ownerName
  form.phone = row.phone ?? ''
  form.department = row.department ?? ''
  form.remark = row.remark ?? ''
  form.startTime = toLocalInput(row.startTime)
  form.endTime = toLocalInput(row.endTime)
  form.enabled = row.enabled
  dialogVisible.value = true
}

function closeDialog() {
  dialogVisible.value = false
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  const payload = {
    plateNumber: form.plateNumber.trim(),
    plateColor: form.plateColor,
    ownerName: form.ownerName.trim(),
    phone: form.phone.trim() || undefined,
    department: form.department.trim() || undefined,
    remark: form.remark.trim() || undefined,
    startTime: form.startTime,
    endTime: form.endTime,
    enabled: form.enabled
  }
  try {
    if (isEditing.value && editingId.value != null) {
      await request.put(`/lots/${selectedLotId.value}/blacklist-vehicles/${editingId.value}`, payload)
    } else {
      await request.post(`/lots/${selectedLotId.value}/blacklist-vehicles`, payload)
    }
    ElMessage.success(t('msgSaveOk'))
    dialogVisible.value = false
    handleSearch()
  } catch (error) {
    ElMessage.error(errorText(error, t('errSave')))
  } finally {
    saving.value = false
  }
}

/* ---------------- 删除 ---------------- */

async function handleDelete(row: BlacklistItem) {
  try {
    await ElMessageBox.confirm(t('deleteConfirm'), t('deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  try {
    await request.delete(`/lots/${selectedLotId.value}/blacklist-vehicles/${row.id}`)
    ElMessage.success(t('msgDeleteOk'))
    loadList()
  } catch (error) {
    ElMessage.error(errorText(error, t('errDelete')))
  }
}

/* ---------------- 导入 / 导出 ---------------- */

const exportLoading = ref(false)
const importVisible = ref(false)
const importing = ref(false)
const downloadingTemplate = ref(false)
const uploadRef = ref<UploadInstance>()
const chosenFile = ref<File | null>(null)

function openImportDialog() {
  chosenFile.value = null
  uploadRef.value?.clearFiles()
  importVisible.value = true
}

function closeImportDialog() {
  if (importing.value) {
    return
  }
  importVisible.value = false
  chosenFile.value = null
  uploadRef.value?.clearFiles()
}

function handleFileChange(file: UploadFile) {
  chosenFile.value = file.raw ?? null
}

function handleFileRemove() {
  chosenFile.value = null
}

async function handleDownloadTemplate() {
  downloadingTemplate.value = true
  try {
    await downloadFile(
      `/lots/${selectedLotId.value}/blacklist-vehicles/import-template`,
      'blacklist-template.xlsx'
    )
  } catch (error) {
    ElMessage.error(errorText(error, t('errTemplate')))
  } finally {
    downloadingTemplate.value = false
  }
}

async function handleExport() {
  exportLoading.value = true
  try {
    const plate = query.plate.trim()
    await downloadFile(
      `/lots/${selectedLotId.value}/blacklist-vehicles/export?plate=${encodeURIComponent(plate)}`,
      'blacklist-vehicles.xlsx'
    )
    ElMessage.success(t('msgExportOk'))
  } catch (error) {
    ElMessage.error(errorText(error, t('errExport')))
  } finally {
    exportLoading.value = false
  }
}

async function handleImport() {
  if (!chosenFile.value) {
    ElMessage.warning(t('uploadFileRequired'))
    return
  }
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', chosenFile.value)
    const data = await request.post<never, { imported: number; skipped: number }>(
      `/lots/${selectedLotId.value}/blacklist-vehicles/import`,
      fd
    )
    ElMessage.success(importDoneText(data.imported, data.skipped))
    importVisible.value = false
    chosenFile.value = null
    uploadRef.value?.clearFiles()
    loadList()
  } catch (error) {
    ElMessage.error(errorText(error, t('errImport')))
  } finally {
    importing.value = false
  }
}

onMounted(async () => {
  try {
    await loadLots()
    if (selectedLotId.value != null) {
      await loadList()
    }
  } catch (error) {
    ElMessage.error(errorText(error, t('errLoad')))
  }
})
</script>

<template>
  <section class="bl-page">
    <el-card class="panel lot-bar" shadow="never">
      <div class="lot-line">
        <span class="field-label">{{ t('lotLabel') }}</span>
        <el-select
          v-model="selectedLotId"
          class="lot-select"
          :placeholder="t('lotPlaceholder')"
          @change="handleLotChange"
        >
          <el-option
            v-for="lot in lots"
            :key="lot.id"
            :label="lot.code ? `${lot.name} (${lot.code})` : lot.name"
            :value="lot.id"
          />
        </el-select>
      </div>
    </el-card>

    <el-empty v-if="lots.length === 0" :description="t('noLots')" />

    <el-card v-else class="panel" shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.plate"
          class="search-input"
          :placeholder="t('searchPlaceholder')"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">{{ t('query') }}</el-button>
        <el-button @click="handleReset">{{ t('reset') }}</el-button>
        <div class="spacer" />
        <el-button type="primary" plain @click="openImportDialog">{{ t('import') }}</el-button>
        <el-button type="primary" plain :loading="exportLoading" @click="handleExport">
          {{ t('export') }}
        </el-button>
        <el-button type="primary" @click="openCreateDialog">{{ t('add') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column :label="t('thPlate')" min-width="170">
          <template #default="{ row }">
            <div class="plate-cell">
              <span class="plate-num">{{ row.plateNumber }}</span>
              <span class="plate-color">{{ colorLabel(row.plateColor) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" :label="t('thOwner')" min-width="130" show-overflow-tooltip />
        <el-table-column :label="t('thPhone')" min-width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('thDepartment')" min-width="130">
          <template #default="{ row }">{{ row.department || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('thStartTime')" width="150">
          <template #default="{ row }">{{ fmtTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('thEndTime')" width="150">
          <template #default="{ row }">{{ fmtTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('thStatus')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
              {{ row.enabled ? t('enabled') : t('disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('thActions')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">{{ t('edit') }}</el-button>
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
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? t('editTitle') : t('addTitle')"
      width="min(560px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="t('lblPlate')" prop="plateNumber">
          <el-input
            v-model="form.plateNumber"
            :placeholder="t('platePlaceholder')"
            maxlength="20"
          />
        </el-form-item>
        <el-form-item :label="t('lblColor')" prop="plateColor">
          <el-select v-model="form.plateColor" style="width: 100%">
            <el-option
              v-for="color in PLATE_COLORS"
              :key="color"
              :label="t(`color.${color}`)"
              :value="color"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('lblOwner')" prop="ownerName">
          <el-input
            v-model="form.ownerName"
            :placeholder="t('ownerPlaceholder')"
            maxlength="80"
          />
        </el-form-item>
        <el-form-item :label="t('lblPhone')" prop="phone">
          <el-input v-model="form.phone" :placeholder="t('phonePlaceholder')" maxlength="30" />
        </el-form-item>
        <el-form-item :label="t('lblDepartment')" prop="department">
          <el-input
            v-model="form.department"
            :placeholder="t('departmentPlaceholder')"
            maxlength="80"
          />
        </el-form-item>
        <el-form-item :label="t('lblRemark')" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            :placeholder="t('remarkPlaceholder')"
            maxlength="200"
          />
        </el-form-item>
        <el-form-item :label="t('lblStartTime')" prop="startTime">
          <el-input v-model="form.startTime" type="datetime-local" />
        </el-form-item>
        <el-form-item :label="t('lblEndTime')" prop="endTime">
          <el-input v-model="form.endTime" type="datetime-local" />
        </el-form-item>
        <el-form-item :label="t('lblEnabled')" prop="enabled">
          <el-switch
            v-model="form.enabled"
            inline-prompt
            :active-text="t('enabled')"
            :inactive-text="t('disabled')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ saving ? t('saving') : t('save') }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog
      v-model="importVisible"
      :title="t('importTitle')"
      width="min(560px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <p class="import-hint">{{ t('importStepHint') }}</p>
      <div class="import-actions">
        <el-button :loading="downloadingTemplate" @click="handleDownloadTemplate">
          {{ t('downloadTemplate') }}
        </el-button>
      </div>
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx"
        drag
        style="width: 100%"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
      >
        <div class="upload-area">
          <span>{{ t('chooseFile') }}</span>
        </div>
      </el-upload>
      <template #footer>
        <el-button @click="closeImportDialog">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">
          {{ importing ? t('importing') : t('startImport') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.bl-page {
  animation: fade-in 0.35s ease;
}

.panel {
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
}

.lot-bar {
  margin-bottom: 16px;
}

.lot-bar :deep(.el-card__body) {
  padding: 14px 20px;
}

.lot-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.field-label {
  flex: 0 0 auto;
  font-weight: 600;
  color: var(--fp-ink-soft);
}

.lot-select {
  width: 320px;
  max-width: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-input {
  width: 260px;
}

.spacer {
  flex: 1;
}

.plate-cell {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.plate-num {
  font-weight: 600;
  font-family: var(--fp-font-display);
}

.plate-color {
  font-size: 0.75rem;
  color: var(--fp-muted);
  white-space: nowrap;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.import-hint {
  margin: 0 0 12px;
  line-height: 1.6;
  color: var(--fp-muted);
  font-size: 0.9rem;
}

.import-actions {
  margin-bottom: 14px;
}

.upload-area {
  padding: 18px 12px;
  text-align: center;
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
