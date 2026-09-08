<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

/* ---------------- 类型 ---------------- */

interface LotItem {
  id: number
  name: string
}

interface LaneItem {
  id: number
  name: string
}

interface BoothLaneItem {
  id: number
  name: string
}

interface BoothItem {
  id: number
  lotId: number
  lotName: string
  name: string
  code: string
  location: string
  enabled: boolean
  lanes: BoothLaneItem[]
  createdAt: string
  updatedAt: string
}

interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

interface BoothPayload {
  name: string
  code?: string
  location?: string
  enabled: boolean
  laneIds: number[]
}

/* ---------------- 双语文案 ---------------- */

const d: BiDict = {
  lot: { 'zh-CN': '车场', en: 'Parking lot' },
  lotPlaceholder: { 'zh-CN': '请选择车场', en: 'Select a parking lot' },
  noLot: { 'zh-CN': '暂无车场数据', en: 'No parking lots' },
  noLotHint: { 'zh-CN': '请先在车场管理中创建车场，再来管理岗亭。', en: 'Please create a parking lot first.' },
  name: { 'zh-CN': '名称', en: 'Name' },
  namePlaceholder: { 'zh-CN': '输入岗亭名称搜索', en: 'Search by booth name' },
  code: { 'zh-CN': '编码', en: 'Code' },
  location: { 'zh-CN': '位置', en: 'Location' },
  search: { 'zh-CN': '查询', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  add: { 'zh-CN': '新增岗亭', en: 'Add booth' },
  empty: { 'zh-CN': '暂无岗亭数据', en: 'No booths yet' },
  enabled: { 'zh-CN': '启用', en: 'Enabled' },
  disabled: { 'zh-CN': '停用', en: 'Disabled' },
  lanes: { 'zh-CN': '绑定通道', en: 'Lanes' },
  updateTime: { 'zh-CN': '更新时间', en: 'Updated at' },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  createTitle: { 'zh-CN': '新增岗亭', en: 'Add booth' },
  editTitle: { 'zh-CN': '编辑岗亭', en: 'Edit booth' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  save: { 'zh-CN': '保存', en: 'Save' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  nameRequired: { 'zh-CN': '请输入岗亭名称', en: 'Please enter the booth name' },
  lanesPlaceholder: { 'zh-CN': '选择要绑定的通道', en: 'Select lanes to bind' },
  noLane: { 'zh-CN': '该车场暂无可用通道', en: 'No lanes available in this lot' },
  createSuccess: { 'zh-CN': '岗亭创建成功', en: 'Booth created' },
  updateSuccess: { 'zh-CN': '岗亭更新成功', en: 'Booth updated' },
  deleteTitle: { 'zh-CN': '删除确认', en: 'Confirm deletion' },
  deleteConfirm: { 'zh-CN': '确定删除岗亭「{name}」吗？', en: 'Delete booth "{name}"?' },
  deleteSuccess: { 'zh-CN': '岗亭已删除', en: 'Booth deleted' },
  lotLoadFailed: { 'zh-CN': '车场列表加载失败', en: 'Failed to load lots' },
  loadFailed: { 'zh-CN': '岗亭列表加载失败', en: 'Failed to load booths' },
  lanesLoadFailed: { 'zh-CN': '通道列表加载失败', en: 'Failed to load lanes' },
  submitFailed: { 'zh-CN': '操作失败，请重试', en: 'Operation failed. Please retry.' }
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

/* ---------------- 列表状态 ---------------- */

const loading = ref(false)
const rows = ref<BoothItem[]>([])
const total = ref(0)
const lots = ref<LotItem[]>([])
const lanes = ref<LaneItem[]>([])
const lotId = ref<number>()
const query = reactive({
  keyword: '',
  page: 1,
  size: 10
})

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
    if (query.keyword.trim()) {
      params.keyword = query.keyword.trim()
    }
    const data = await request.get<never, PageResult<BoothItem>>(
      `/lots/${lotId.value}/booths`,
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

async function loadLanes(): Promise<void> {
  if (!lotId.value) {
    lanes.value = []
    return
  }
  try {
    lanes.value = await request.get<never, LaneItem[]>('/lanes', {
      params: { lotId: lotId.value }
    })
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('lanesLoadFailed')))
  }
}

async function handleLotChange(): Promise<void> {
  query.keyword = ''
  query.page = 1
  lanes.value = []
  await loadList()
}

function handleSearch(): void {
  query.page = 1
  loadList()
}

function handleReset(): void {
  query.keyword = ''
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
  name: '',
  code: '',
  location: '',
  enabled: true,
  laneIds: [] as number[]
})

function openCreateDialog(): void {
  if (!lotId.value) {
    return
  }
  editing.value = false
  editingId.value = undefined
  form.name = ''
  form.code = ''
  form.location = ''
  form.enabled = true
  form.laneIds = []
  dialogVisible.value = true
  loadLanes()
}

function openEditDialog(row: BoothItem): void {
  editing.value = true
  editingId.value = row.id
  form.name = row.name
  form.code = row.code ?? ''
  form.location = row.location ?? ''
  form.enabled = row.enabled
  form.laneIds = (row.lanes ?? []).map((lane) => lane.id)
  dialogVisible.value = true
  loadLanes()
}

async function handleSubmit(): Promise<void> {
  if (!lotId.value) {
    return
  }
  const name = form.name.trim()
  if (!name) {
    ElMessage.warning(t('nameRequired'))
    return
  }
  saving.value = true
  const payload: BoothPayload = {
    name,
    enabled: form.enabled,
    laneIds: form.laneIds
  }
  const code = form.code.trim()
  if (code) {
    payload.code = code
  }
  const location = form.location.trim()
  if (location) {
    payload.location = location
  }
  try {
    if (editing.value && editingId.value) {
      await request.put<never, BoothItem>(
        `/lots/${lotId.value}/booths/${editingId.value}`,
        payload
      )
      ElMessage.success(t('updateSuccess'))
    } else {
      await request.post<never, BoothItem>(`/lots/${lotId.value}/booths`, payload)
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

async function handleDelete(row: BoothItem): Promise<void> {
  if (!lotId.value) {
    return
  }
  try {
    await ElMessageBox.confirm(fill(t('deleteConfirm'), { name: row.name }), t('deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  try {
    await request.delete<never, void>(`/lots/${lotId.value}/booths/${row.id}`)
    ElMessage.success(t('deleteSuccess'))
    loadList()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('submitFailed')))
  }
}

onMounted(() => {
  loadLots()
})
</script>

<template>
  <section class="booths-page">
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
              v-model="query.keyword"
              class="search-input"
              :placeholder="t('namePlaceholder')"
              clearable
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </div>

          <el-button type="primary" @click="handleSearch">{{ t('search') }}</el-button>
          <el-button @click="handleReset">{{ t('reset') }}</el-button>

          <div class="spacer" />
          <el-button type="primary" @click="openCreateDialog">{{ t('add') }}</el-button>
        </div>

        <el-table v-loading="loading" :data="rows" stripe>
          <el-table-column prop="name" :label="t('name')" min-width="160" />
          <el-table-column prop="code" :label="t('code')" min-width="120">
            <template #default="{ row }">{{ row.code || '—' }}</template>
          </el-table-column>
          <el-table-column prop="location" :label="t('location')" min-width="160">
            <template #default="{ row }">{{ row.location || '—' }}</template>
          </el-table-column>
          <el-table-column :label="t('lanes')" min-width="220">
            <template #default="{ row }">
              <template v-if="row.lanes && row.lanes.length > 0">
                <el-tag
                  v-for="lane in row.lanes"
                  :key="lane.id"
                  class="lane-tag"
                  size="small"
                  effect="plain"
                >
                  {{ lane.name }}
                </el-tag>
              </template>
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
          <el-table-column :label="t('updateTime')" min-width="170">
            <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column :label="t('actions')" width="130" fixed="right">
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
            :total="total"
            :page-size="query.size"
            layout="total, prev, pager, next"
            background
            @current-change="handlePageChange"
          />
        </div>
      </template>
    </el-card>

    <!-- 新增 / 编辑岗亭 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? t('editTitle') : t('createTitle')"
      width="500px"
      destroy-on-close
      append-to-body
    >
      <el-form :model="form" label-width="90px">
        <el-form-item :label="t('name')">
          <el-input v-model="form.name" maxlength="64" />
        </el-form-item>
        <el-form-item :label="t('code')">
          <el-input v-model="form.code" maxlength="32" />
        </el-form-item>
        <el-form-item :label="t('location')">
          <el-input v-model="form.location" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('enabled')">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item :label="t('lanes')">
          <template v-if="lanes.length > 0">
            <el-select v-model="form.laneIds" multiple :placeholder="t('lanesPlaceholder')" style="width: 100%">
              <el-option v-for="lane in lanes" :key="lane.id" :label="lane.name" :value="lane.id" />
            </el-select>
          </template>
          <span v-else class="muted-text">{{ t('noLane') }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">
          {{ saving ? t('saving') : t('save') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.booths-page {
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

.lane-tag {
  margin: 2px 6px 2px 0;
}

.muted-text {
  color: var(--fp-muted);
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
</style>
