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

/** 每次入场免费时长上限（分钟）：7 天 */
const MAX_FREE_MINUTES = 10080

const d: BiDict = {
  lotLabel: { 'zh-CN': '停车场', en: 'Parking Lot' },
  lotPlaceholder: { 'zh-CN': '请选择停车场', en: 'Select a parking lot' },
  noLots: { 'zh-CN': '暂无停车场数据，请先在「车场管理」中创建', en: 'No parking lots yet. Create one in Lot Management first' },
  searchPlaceholder: { 'zh-CN': '输入车牌号检索', en: 'Search by plate number' },
  query: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  add: { 'zh-CN': '新增优惠车辆', en: 'Add Discount Vehicle' },
  import: { 'zh-CN': '导入', en: 'Import' },
  export: { 'zh-CN': '导出', en: 'Export' },
  hint: {
    'zh-CN': '名单内的车辆在该车场每次入场时，自入场时刻起免费停「免费时长」分钟，超出部分按该车场计费规则正常计费。',
    en: 'Vehicles in this list get the free minutes on every entry to this lot, starting from the entry moment; the remaining duration is billed by the lot\'s billing rules.'
  },
  thPlate: { 'zh-CN': '车牌', en: 'Plate' },
  thFreeMinutes: { 'zh-CN': '免费时长', en: 'Free Minutes' },
  thRemark: { 'zh-CN': '备注', en: 'Remark' },
  thStatus: { 'zh-CN': '状态', en: 'Status' },
  thActions: { 'zh-CN': '操作', en: 'Actions' },
  minuteUnit: { 'zh-CN': '分钟', en: 'min' },
  enabled: { 'zh-CN': '启用', en: 'Enabled' },
  disabled: { 'zh-CN': '停用', en: 'Disabled' },
  empty: { 'zh-CN': '暂无数据', en: 'No data' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  deleteTitle: { 'zh-CN': '删除确认', en: 'Confirm Delete' },
  deleteConfirm: {
    'zh-CN': '确定删除该优惠车辆吗？删除后该车不再享受入场免费时长。',
    en: 'Delete this discount vehicle? It will no longer get free minutes on entry.'
  },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  addTitle: { 'zh-CN': '新增优惠车辆', en: 'New Discount Vehicle' },
  editTitle: { 'zh-CN': '编辑优惠车辆', en: 'Edit Discount Vehicle' },
  lblPlate: { 'zh-CN': '车牌号', en: 'Plate Number' },
  platePlaceholder: { 'zh-CN': '请输入车牌号，如 京A12345', en: 'Enter plate number, e.g. BJ·A12345' },
  lblColor: { 'zh-CN': '车牌颜色', en: 'Plate Color' },
  lblFreeMinutes: { 'zh-CN': '每次入场免费时长', en: 'Free minutes per entry' },
  freeMinutesPlaceholder: { 'zh-CN': '请输入免费分钟数', en: 'Enter free minutes' },
  freeMinutesTip: { 'zh-CN': '自入场时刻起免费时长', en: 'Free from the entry moment' },
  lblRemark: { 'zh-CN': '备注', en: 'Remark' },
  remarkPlaceholder: { 'zh-CN': '选填', en: 'Optional' },
  lblEnabled: { 'zh-CN': '启用该优惠', en: 'Enable this discount' },
  save: { 'zh-CN': '保存', en: 'Save' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  msgPlateRequired: { 'zh-CN': '请输入车牌号', en: 'Plate number is required' },
  msgFreeRequired: { 'zh-CN': '请输入 1 ~ 10080 之间的免费分钟数', en: 'Free minutes must be between 1 and 10080' },
  msgSaveOk: { 'zh-CN': '保存成功', en: 'Saved successfully' },
  msgDeleteOk: { 'zh-CN': '删除成功', en: 'Deleted successfully' },
  msgExportOk: { 'zh-CN': '导出成功', en: 'Exported successfully' },
  errLoad: { 'zh-CN': '加载失败，请稍后重试', en: 'Failed to load, please retry later' },
  errSave: { 'zh-CN': '保存失败', en: 'Failed to save' },
  errDelete: { 'zh-CN': '删除失败', en: 'Failed to delete' },
  errExport: { 'zh-CN': '导出失败', en: 'Failed to export' },
  errTemplate: { 'zh-CN': '模板下载失败', en: 'Failed to download template' },
  errImport: { 'zh-CN': '导入失败', en: 'Import failed' },
  copySuccess: { 'zh-CN': '已复制车牌：{plate}', en: 'Plate copied: {plate}' },
  copyFailed: { 'zh-CN': '复制失败，请重试', en: 'Copy failed, try again' },
  importTitle: { 'zh-CN': '批量导入优惠车辆', en: 'Batch Import Discount Vehicles' },
  importStepHint: {
    'zh-CN': '支持 .xlsx 文件，请先下载模板填写后上传；车牌缺失、时长非法或与已有记录重复的数据行将被跳过。',
    en: '.xlsx files are supported. Download the template first, then upload the filled file; rows with a missing plate, an invalid free-time value, or a duplicated plate will be skipped.'
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

interface DiscountItem {
  id: number
  plateNumber: string
  plateColor: string
  freeMinutes: number
  enabled: boolean
  remark?: string | null
  createdAt: string
  updatedAt: string
}

/* ---------------- 车牌底色（与停车流水一致） ---------------- */
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

function colorLabel(color: string): string {
  return t(`color.${color}`)
}

async function handleCopyPlate(row: DiscountItem) {
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

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function importDoneText(imported: number, skipped: number): string {
  return locale.value === 'en'
    ? `Import finished: ${imported} imported, ${skipped} skipped`
    : `导入完成：成功 ${imported} 条，跳过 ${skipped} 条`
}

/* ---------------- 车场加载与切换 ---------------- */

const lots = ref<LotItem[]>([])
const selectedLotId = ref<number | null>(null)
const loading = ref(false)
const rows = ref<DiscountItem[]>([])
const total = ref(0)
const saving = ref(false)

async function loadLots() {
  const list = await request.get<never, LotItem[]>('/lots')
  lots.value = list
  const raw = route.query.lotId
  const target = Array.isArray(raw) ? raw[0] : raw
  const matched = lots.value.find((lot) => String(lot.id) === String(target))
  selectedLotId.value = matched ? matched.id : (lots.value[0]?.id ?? null)
}

/* ---------------- 列表 ---------------- */

const query = reactive({
  plate: '',
  page: 1,
  size: 10
})

async function loadList() {
  if (selectedLotId.value == null) {
    rows.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const data = await request.get<never, PageResult<DiscountItem>>(
      `/lots/${selectedLotId.value}/discount-vehicles`,
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

function handleLotChange() {
  query.plate = ''
  query.page = 1
  loadList()
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
  freeMinutes: 60,
  remark: '',
  enabled: true
})

const rules = computed<FormRules>(() => ({
  plateNumber: [{ required: true, message: t('msgPlateRequired'), trigger: 'blur' }],
  freeMinutes: [{ required: true, message: t('msgFreeRequired'), trigger: 'change' }]
}))

function resetForm() {
  editingId.value = null
  form.plateNumber = ''
  form.plateColor = 'BLUE'
  form.freeMinutes = 60
  form.remark = ''
  form.enabled = true
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: DiscountItem) {
  editingId.value = row.id
  form.plateNumber = row.plateNumber
  form.plateColor = row.plateColor as PlateColorValue
  form.freeMinutes = row.freeMinutes
  form.remark = row.remark ?? ''
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
  if (form.freeMinutes < 1 || form.freeMinutes > MAX_FREE_MINUTES) {
    ElMessage.warning(t('msgFreeRequired'))
    return
  }
  saving.value = true
  const payload = {
    plateNumber: form.plateNumber.trim(),
    plateColor: form.plateColor,
    freeMinutes: form.freeMinutes,
    remark: form.remark.trim() || undefined,
    enabled: form.enabled
  }
  try {
    if (isEditing.value && editingId.value != null) {
      await request.put(`/lots/${selectedLotId.value}/discount-vehicles/${editingId.value}`, payload)
    } else {
      await request.post(`/lots/${selectedLotId.value}/discount-vehicles`, payload)
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

async function handleDelete(row: DiscountItem) {
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
    await request.delete(`/lots/${selectedLotId.value}/discount-vehicles/${row.id}`)
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
      `/lots/${selectedLotId.value}/discount-vehicles/import-template`,
      'discount-template.xlsx'
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
      `/lots/${selectedLotId.value}/discount-vehicles/export?plate=${encodeURIComponent(plate)}`,
      'discount-vehicles.xlsx'
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
      `/lots/${selectedLotId.value}/discount-vehicles/import`,
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
  <section class="dv-page">
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
      <el-alert class="lot-hint" type="info" :closable="false" :title="t('hint')" />
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
        <el-table-column :label="t('thPlate')" min-width="150">
          <template #default="{ row }">
            <el-tooltip :disabled="!row.plateColor" :content="colorLabel(row.plateColor)" placement="top">
              <span class="plate-badge" :style="plateBadgeStyle(row.plateColor)" @click="handleCopyPlate(row)">
                {{ row.plateNumber }}
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column :label="t('thFreeMinutes')" min-width="130">
          <template #default="{ row }">
            <span class="free-minutes">{{ row.freeMinutes }}</span>
            <span class="free-unit">{{ t('minuteUnit') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('thRemark')" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
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
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        class="pager"
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? t('editTitle') : t('addTitle')"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-form-item :label="t('lblPlate')" prop="plateNumber">
          <el-input v-model="form.plateNumber" :placeholder="t('platePlaceholder')" maxlength="20" clearable />
        </el-form-item>
        <el-form-item :label="t('lblColor')">
          <el-select v-model="form.plateColor" class="full-width">
            <el-option v-for="color in PLATE_COLORS" :key="color" :label="colorLabel(color)" :value="color" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('lblFreeMinutes')" prop="freeMinutes">
          <el-input-number
            v-model="form.freeMinutes"
            :min="1"
            :max="MAX_FREE_MINUTES"
            :placeholder="t('freeMinutesPlaceholder')"
            class="free-input"
          />
          <span class="free-unit">{{ t('minuteUnit') }}</span>
        </el-form-item>
        <el-form-item>
          <span class="form-tip">{{ t('freeMinutesTip') }}</span>
        </el-form-item>
        <el-form-item :label="t('lblRemark')">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            maxlength="255"
            :placeholder="t('remarkPlaceholder')"
            show-word-limit
          />
        </el-form-item>
        <el-form-item :label="t('lblEnabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ t('save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog
      v-model="importVisible"
      :title="t('importTitle')"
      width="480px"
      :close-on-click-modal="false"
      :close-on-press-escape="!importing"
      :show-close="!importing"
    >
      <div class="import-body">
        <p class="import-hint">{{ t('importStepHint') }}</p>
        <el-button link type="primary" :loading="downloadingTemplate" @click="handleDownloadTemplate">
          {{ t('downloadTemplate') }}
        </el-button>
        <el-upload
          ref="uploadRef"
          class="upload-box"
          drag
          accept=".xlsx"
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
        >
          <div class="el-upload__text">{{ t('chooseFile') }}</div>
        </el-upload>
      </div>
      <template #footer>
        <el-button :disabled="importing" @click="closeImportDialog">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">{{ t('startImport') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.dv-page .lot-bar :deep(.lot-line) {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.dv-page .field-label {
  color: var(--el-text-color-regular);
  font-size: 14px;
  white-space: nowrap;
}

.dv-page .lot-select {
  width: 280px;
}

.dv-page .lot-hint {
  margin-top: 12px;
}

.dv-page .toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.dv-page .toolbar .search-input {
  width: 240px;
}

.dv-page .toolbar .spacer {
  flex: 1;
}

.dv-page .plate-badge {
  display: inline-block;
  min-width: 92px;
  padding: 4px 10px;
  border-radius: 5px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1px;
  text-align: center;
  cursor: pointer;
  user-select: none;
}

.dv-page .free-minutes {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.dv-page .free-unit {
  margin-left: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.dv-page .pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.dv-page .full-width {
  width: 100%;
}

.dv-page .free-input {
  width: 200px;
}

.dv-page .form-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.dv-page .import-body .import-hint {
  margin: 0 0 8px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.dv-page .import-body .upload-box {
  margin-top: 12px;
}
</style>
