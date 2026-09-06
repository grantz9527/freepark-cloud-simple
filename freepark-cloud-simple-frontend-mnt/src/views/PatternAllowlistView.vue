<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules
} from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

const d: BiDict = {
  lotLabel: { 'zh-CN': '停车场', en: 'Parking Lot' },
  lotPlaceholder: { 'zh-CN': '请选择停车场', en: 'Select a parking lot' },
  noLots: { 'zh-CN': '暂无停车场数据，请先在「车场管理」中创建', en: 'No parking lots yet. Create one in Lot Management first' },
  searchPlaceholder: { 'zh-CN': '按规则名称或正则模糊搜索', en: 'Search by rule name or pattern' },
  query: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  add: { 'zh-CN': '新增放行规则', en: 'Add Rule' },
  thName: { 'zh-CN': '规则名称', en: 'Rule Name' },
  thPattern: { 'zh-CN': '匹配正则', en: 'Pattern' },
  thRemark: { 'zh-CN': '备注', en: 'Remark' },
  thStatus: { 'zh-CN': '状态', en: 'Status' },
  thUpdatedAt: { 'zh-CN': '更新时间', en: 'Updated At' },
  thActions: { 'zh-CN': '操作', en: 'Actions' },
  enabled: { 'zh-CN': '启用', en: 'Enabled' },
  disabled: { 'zh-CN': '停用', en: 'Disabled' },
  empty: { 'zh-CN': '暂无数据', en: 'No data' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  deleteTitle: { 'zh-CN': '删除确认', en: 'Confirm Delete' },
  deleteConfirm: {
    'zh-CN': '确定删除该放行规则吗？删除后不可恢复。',
    en: 'Delete this pattern allowlist rule? This cannot be undone.'
  },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  addTitle: { 'zh-CN': '新增放行规则', en: 'New Pattern Allowlist Rule' },
  editTitle: { 'zh-CN': '编辑放行规则', en: 'Edit Pattern Allowlist Rule' },
  lblName: { 'zh-CN': '规则名称', en: 'Rule Name' },
  namePlaceholder: { 'zh-CN': '如：京牌号段放行', en: 'e.g. BJ-plate passage' },
  lblPattern: { 'zh-CN': '匹配正则', en: 'Pattern' },
  patternPlaceholder: { 'zh-CN': '如 ^京A\\d{5}$ 或 ^京A.*', en: 'e.g. ^京A\\d{5}$ or ^京A.*' },
  patternHint: {
    'zh-CN': '按 Java 正则语法编写，匹配采用「包含命中」（find）方式，命中即放行；空白的正则无法保存。',
    en: 'Write a Java-style regular expression; matching uses find() semantics, and a matched plate passes. An empty pattern cannot be saved.'
  },
  patternInvalid: { 'zh-CN': '正则表达式无效，请检查后重试', en: 'Invalid regular expression. Please check and retry' },
  lblRemark: { 'zh-CN': '备注', en: 'Remark' },
  remarkPlaceholder: { 'zh-CN': '选填', en: 'Optional' },
  lblEnabled: { 'zh-CN': '启用该规则', en: 'Enable this rule' },
  testTitle: { 'zh-CN': '正则预览', en: 'Pattern Preview' },
  testPlateLabel: { 'zh-CN': '测试车牌', en: 'Test Plate' },
  testPlatePlaceholder: { 'zh-CN': '输入车牌号验证是否命中', en: 'Enter a plate number to test' },
  testMatch: { 'zh-CN': '匹配：该车牌将被放行', en: 'Matched: this plate will pass' },
  testNoMatch: { 'zh-CN': '不匹配：该车牌不会被放行', en: 'Not matched: this plate will not pass' },
  testNeedBoth: {
    'zh-CN': '填写有效的正则与测试车牌后可预览匹配结果',
    en: 'Enter a valid pattern and a test plate to preview the result'
  },
  save: { 'zh-CN': '保存', en: 'Save' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  msgNameRequired: { 'zh-CN': '请输入规则名称', en: 'Rule name is required' },
  msgPatternRequired: { 'zh-CN': '请输入匹配正则', en: 'Pattern is required' },
  msgSaveOk: { 'zh-CN': '保存成功', en: 'Saved successfully' },
  msgDeleteOk: { 'zh-CN': '删除成功', en: 'Deleted successfully' },
  errLoad: { 'zh-CN': '加载失败，请稍后重试', en: 'Failed to load, please retry later' },
  errSave: { 'zh-CN': '保存失败', en: 'Failed to save' },
  errDelete: { 'zh-CN': '删除失败', en: 'Failed to delete' }
}
const { t } = useBiText(d)

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

interface PatternEntry {
  id: number
  lotId: number
  lotName: string
  name: string
  pattern: string
  remark?: string | null
  enabled: boolean
  createdAt: string
  updatedAt: string
}

const loading = ref(false)
const saving = ref(false)
const rows = ref<PatternEntry[]>([])
const total = ref(0)

const lots = ref<LotItem[]>([])
const selectedLotId = ref<number | null>(null)

const query = reactive({
  keyword: '',
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

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function isValidPattern(value: string): boolean {
  try {
    new RegExp(value)
    return true
  } catch {
    return false
  }
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
  query.keyword = ''
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
    const data = await request.get<never, PageResult<PatternEntry>>(
      `/lots/${selectedLotId.value}/pattern-allowlist`,
      {
        params: {
          keyword: query.keyword.trim() || undefined,
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
  query.keyword = ''
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
  name: '',
  pattern: '',
  remark: '',
  enabled: true,
  testPlate: ''
})

const patternInvalidMessage = computed<string | null>(() => {
  const pattern = form.pattern.trim()
  if (!pattern) {
    return null
  }
  return isValidPattern(pattern) ? null : t('patternInvalid')
})

const testResult = computed<boolean | null>(() => {
  const pattern = form.pattern.trim()
  const plate = form.testPlate.trim()
  if (!pattern || !plate || !isValidPattern(pattern)) {
    return null
  }
  return new RegExp(pattern).test(plate)
})

const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('msgNameRequired'), trigger: 'blur' }],
  pattern: [
    { required: true, message: t('msgPatternRequired'), trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (value && !isValidPattern(value)) {
          callback(new Error(t('patternInvalid')))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}))

function resetForm() {
  editingId.value = null
  form.name = ''
  form.pattern = ''
  form.remark = ''
  form.enabled = true
  form.testPlate = ''
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: PatternEntry) {
  editingId.value = row.id
  form.name = row.name
  form.pattern = row.pattern
  form.remark = row.remark ?? ''
  form.enabled = row.enabled
  form.testPlate = ''
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
    name: form.name.trim(),
    pattern: form.pattern.trim(),
    remark: form.remark.trim() || undefined,
    enabled: form.enabled
  }
  try {
    if (isEditing.value && editingId.value != null) {
      await request.put(`/lots/${selectedLotId.value}/pattern-allowlist/${editingId.value}`, payload)
    } else {
      await request.post(`/lots/${selectedLotId.value}/pattern-allowlist`, payload)
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

async function handleDelete(row: PatternEntry) {
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
    await request.delete(`/lots/${selectedLotId.value}/pattern-allowlist/${row.id}`)
    ElMessage.success(t('msgDeleteOk'))
    loadList()
  } catch (error) {
    ElMessage.error(errorText(error, t('errDelete')))
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
  <section class="pa-page">
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
          v-model="query.keyword"
          class="search-input"
          :placeholder="t('searchPlaceholder')"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">{{ t('query') }}</el-button>
        <el-button @click="handleReset">{{ t('reset') }}</el-button>
        <div class="spacer" />
        <el-button type="primary" @click="openCreateDialog">{{ t('add') }}</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="name" :label="t('thName')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('thPattern')" min-width="220">
          <template #default="{ row }">
            <code class="pattern-text">{{ row.pattern }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('thRemark')" min-width="160">
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('thStatus')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="light">
              {{ row.enabled ? t('enabled') : t('disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('thUpdatedAt')" width="150">
          <template #default="{ row }">{{ fmtTime(row.updatedAt) }}</template>
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
      width="min(600px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="t('lblName')" prop="name">
          <el-input
            v-model="form.name"
            :placeholder="t('namePlaceholder')"
            maxlength="80"
          />
        </el-form-item>
        <el-form-item :label="t('lblPattern')" prop="pattern">
          <el-input
            v-model="form.pattern"
            :placeholder="t('patternPlaceholder')"
            maxlength="255"
          />
        </el-form-item>

        <div class="test-box">
          <div class="test-head">
            <span class="test-title">{{ t('testTitle') }}</span>
            <span class="test-hint">{{ t('testNeedBoth') }}</span>
          </div>
          <el-form-item :label="t('testPlateLabel')" label-width="110px">
            <el-input
              v-model="form.testPlate"
              :placeholder="t('testPlatePlaceholder')"
              maxlength="20"
            />
          </el-form-item>
          <p v-if="patternInvalidMessage" class="test-result invalid">
            {{ patternInvalidMessage }}
          </p>
          <p v-else-if="testResult === true" class="test-result ok">{{ t('testMatch') }}</p>
          <p v-else-if="testResult === false" class="test-result no">{{ t('testNoMatch') }}</p>
          <p class="pattern-hint">{{ t('patternHint') }}</p>
        </div>

        <el-form-item :label="t('lblRemark')" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            :placeholder="t('remarkPlaceholder')"
            maxlength="255"
          />
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
  </section>
</template>

<style scoped>
.pa-page {
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
  width: 300px;
}

.spacer {
  flex: 1;
}

.pattern-text {
  font-family: var(--fp-font-mono, ui-monospace, SFMono-Regular, Consolas, monospace);
  font-size: 0.85rem;
  background: var(--fp-surface-subtle, #f2f4f3);
  padding: 2px 6px;
  border-radius: 4px;
  word-break: break-all;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.test-box {
  margin-bottom: 18px;
  padding: 12px 14px 2px;
  border: 1px dashed var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-subtle, rgba(0, 0, 0, 0.02));
}

.test-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.test-title {
  font-weight: 600;
}

.test-hint {
  font-size: 0.8rem;
  color: var(--fp-muted);
}

.test-result {
  margin: 0 0 10px;
  font-size: 0.9rem;
  font-weight: 600;
}

.test-result.ok {
  color: var(--fp-teal, #0f9d58);
}

.test-result.no {
  color: var(--fp-danger, #e5484d);
}

.test-result.invalid {
  color: var(--fp-danger, #e5484d);
}

.pattern-hint {
  margin: 0 0 10px;
  line-height: 1.6;
  color: var(--fp-muted);
  font-size: 0.85rem;
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
