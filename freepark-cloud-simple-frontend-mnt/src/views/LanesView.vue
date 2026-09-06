<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

type LaneTypeOption = 'ENTRANCE' | 'EXIT' | 'BIDIRECTIONAL'

interface LotOption {
  id: number
  name: string
}

interface LaneView {
  id: number
  lotId: number
  lotName: string
  lotCode: string
  linkedLotId: number | null
  linkedLotName: string | null
  linkedLotCode: string | null
  name: string
  code: string
  laneType: LaneTypeOption
  enabled: boolean
  createdAt: string
  updatedAt: string
}

const LANE_TYPE_OPTIONS: LaneTypeOption[] = ['ENTRANCE', 'EXIT', 'BIDIRECTIONAL']

const d: BiDict = {
  title: { 'zh-CN': '通道管理', en: 'Parking Lanes' },
  lotLabel: { 'zh-CN': '车场', en: 'Lot' },
  allLots: { 'zh-CN': '全部车场', en: 'All lots' },
  noLot: { 'zh-CN': '暂无可管理的车场', en: 'No lots available yet' },
  noLotHint: { 'zh-CN': '请先在「停车场管理」中创建车场', en: 'Create a lot under Parking Lots first' },
  searchPlaceholder: { 'zh-CN': '搜索名称 / 编码 / 所属车场', en: 'Search name / code / lot' },
  search: { 'zh-CN': '搜索', en: 'Search' },
  reset: { 'zh-CN': '重置', en: 'Reset' },
  add: { 'zh-CN': '新增通道', en: 'Add Lane' },
  addDisabledHint: { 'zh-CN': '请先选择一个车场', en: 'Select a lot first' },
  colName: { 'zh-CN': '通道名称', en: 'Lane Name' },
  colCode: { 'zh-CN': '通道编码', en: 'Lane Code' },
  colLaneType: { 'zh-CN': '方向 / 类型', en: 'Direction / Type' },
  colLot: { 'zh-CN': '所属车场', en: 'Lot' },
  colLinkedLot: { 'zh-CN': '关联车场', en: 'Linked Lot' },
  colStatus: { 'zh-CN': '状态', en: 'Status' },
  colUpdatedAt: { 'zh-CN': '更新时间', en: 'Updated At' },
  colActions: { 'zh-CN': '操作', en: 'Actions' },
  laneTypeEntrance: { 'zh-CN': '入口', en: 'Entrance' },
  laneTypeExit: { 'zh-CN': '出口', en: 'Exit' },
  laneTypeBidirectional: { 'zh-CN': '双向', en: 'Bidirectional' },
  statusEnabled: { 'zh-CN': '启用', en: 'Enabled' },
  statusDisabled: { 'zh-CN': '停用', en: 'Disabled' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  emptyList: { 'zh-CN': '暂无通道', en: 'No lanes yet' },
  emptyHint: { 'zh-CN': '选择车场后可新增入口 / 出口 / 双向通道', en: 'Pick a lot to add entrance / exit / bidirectional lanes' },
  noMatch: { 'zh-CN': '没有符合条件的结果', en: 'No matching results' },
  createTitle: { 'zh-CN': '新增通道', en: 'Create Lane' },
  editTitle: { 'zh-CN': '编辑通道', en: 'Edit Lane' },
  name: { 'zh-CN': '通道名称', en: 'Name' },
  nameRequired: { 'zh-CN': '请输入通道名称', en: 'Please enter the lane name' },
  code: { 'zh-CN': '通道编码', en: 'Code' },
  codeRequired: { 'zh-CN': '请输入通道编码', en: 'Please enter the lane code' },
  codeLocked: { 'zh-CN': '编码创建后不可修改', en: 'Code cannot be changed after creation' },
  laneType: { 'zh-CN': '方向 / 类型', en: 'Direction / Type' },
  lot: { 'zh-CN': '所属车场', en: 'Lot' },
  lotRequired: { 'zh-CN': '请选择所属车场', en: 'Please select a lot' },
  linkedLot: { 'zh-CN': '关联车场', en: 'Linked Lot' },
  linkedLotNone: { 'zh-CN': '无（不关联）', en: 'None' },
  linkedLotHint: { 'zh-CN': '双向通道可关联对向车场，其它类型无需填写', en: 'Only bidirectional lanes may link to another lot' },
  enabled: { 'zh-CN': '启用', en: 'Enabled' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  save: { 'zh-CN': '保存', en: 'Save' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  createSuccess: { 'zh-CN': '新增成功', en: 'Created successfully' },
  updateSuccess: { 'zh-CN': '保存成功', en: 'Saved successfully' },
  requestFailed: { 'zh-CN': '请求失败，请稍后重试', en: 'Request failed, please try again later' }
}

const { t } = useBiText(d)
const route = useRoute()

const loading = ref(false)
const lots = ref<LotOption[]>([])
const lanes = ref<LaneView[]>([])
const selectedLotId = ref('')

const query = reactive({ keyword: '', page: 1, size: 10 })

const filteredLanes = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  if (!keyword) {
    return lanes.value
  }
  return lanes.value.filter((lane) => {
    const lotText = `${lane.lotName} ${lane.lotCode} ${lane.linkedLotName ?? ''}`.toLowerCase()
    return (
      lane.name.toLowerCase().includes(keyword) ||
      lane.code.toLowerCase().includes(keyword) ||
      lotText.includes(keyword)
    )
  })
})

const total = computed(() => filteredLanes.value.length)
const pagedLanes = computed(() => {
  const start = (query.page - 1) * query.size
  return filteredLanes.value.slice(start, start + query.size)
})

const linkedLotOptions = computed(() =>
  lots.value.filter((lot) => String(lot.id) !== String(form.lotId))
)

function laneTypeLabel(type: LaneTypeOption): string {
  if (type === 'EXIT') {
    return t('laneTypeExit')
  }
  if (type === 'BIDIRECTIONAL') {
    return t('laneTypeBidirectional')
  }
  return t('laneTypeEntrance')
}

function connectedLotsLabel(lane: LaneView): string {
  if (lane.linkedLotName) {
    return `${lane.lotName} · ${lane.linkedLotName}`
  }
  return lane.lotName
}

function formatTime(value?: string): string {
  return value || '-'
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

function applyQueryLotId(): void {
  const queryLotId = typeof route.query.lotId === 'string' ? route.query.lotId : ''
  if (!queryLotId) {
    return
  }
  if (lots.value.some((lot) => String(lot.id) === queryLotId)) {
    selectedLotId.value = queryLotId
  }
}

async function loadLots(): Promise<void> {
  lots.value = await request.get<never, LotOption[]>('/lots')
}

async function loadLanes(): Promise<void> {
  const params = selectedLotId.value ? { lotId: selectedLotId.value } : undefined
  lanes.value = await request.get<never, LaneView[]>('/lanes', { params })
}

async function reload(): Promise<void> {
  loading.value = true
  try {
    await loadLots()
    applyQueryLotId()
    await loadLanes()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('requestFailed'))
  } finally {
    loading.value = false
  }
}

async function onLotChange(): Promise<void> {
  query.keyword = ''
  query.page = 1
  loading.value = true
  try {
    await loadLanes()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('requestFailed'))
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.lotId,
  () => {
    // 从停车场列表带 lotId 跳转而来时预选车场
    if (selectedLotId.value === '' || typeof route.query.lotId === 'string') {
      applyQueryLotId()
      void loadLanes()
    }
  }
)

/* ---------------- 新增 / 编辑弹窗 ---------------- */

const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: null as number | null,
  name: '',
  code: '',
  laneType: 'ENTRANCE' as LaneTypeOption,
  lotId: '' as string,
  linkedLotId: '' as string,
  enabled: true
})

const formRules = computed<FormRules>(() => ({
  name: [{ required: true, message: t('nameRequired'), trigger: 'blur' }]
}))

function openCreate(): void {
  form.id = null
  form.name = ''
  form.code = ''
  form.laneType = 'ENTRANCE'
  form.lotId = selectedLotId.value
  form.linkedLotId = ''
  form.enabled = true
  dialogVisible.value = true
}

function openEdit(lane: LaneView): void {
  form.id = lane.id
  form.name = lane.name
  form.code = lane.code
  form.laneType = lane.laneType
  form.lotId = String(lane.lotId)
  form.linkedLotId = lane.linkedLotId === null ? '' : String(lane.linkedLotId)
  form.enabled = lane.enabled
  dialogVisible.value = true
}

function onFormLotChange(): void {
  if (form.linkedLotId && form.linkedLotId === form.lotId) {
    form.linkedLotId = ''
  }
}

async function handleSave(): Promise<void> {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  if (!form.name.trim()) {
    ElMessage.warning(t('nameRequired'))
    return
  }
  if (!form.lotId) {
    ElMessage.warning(t('lotRequired'))
    return
  }
  if (form.id === null && !form.code.trim()) {
    ElMessage.warning(t('codeRequired'))
    return
  }
  saving.value = true
  try {
    const base = {
      name: form.name.trim(),
      laneType: form.laneType,
      lotId: Number(form.lotId),
      linkedLotId: form.linkedLotId ? Number(form.linkedLotId) : null,
      enabled: form.enabled
    }
    if (form.id === null) {
      await request.post<never, LaneView>('/lanes', { ...base, code: form.code.trim() })
      ElMessage.success(t('createSuccess'))
    } else {
      await request.put<never, LaneView>(`/lanes/${form.id}`, base)
      ElMessage.success(t('updateSuccess'))
    }
    dialogVisible.value = false
    await loadLanes()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('requestFailed'))
  } finally {
    saving.value = false
  }
}

onMounted(reload)
</script>

<template>
  <section class="lanes-view">
    <div class="card filter-card">
      <div class="lot-filter">
        <span class="filter-label">{{ t('lotLabel') }}</span>
        <el-select
          v-model="selectedLotId"
          :placeholder="t('allLots')"
          clearable
          style="width: 260px"
          @change="onLotChange"
          @clear="onLotChange"
        >
          <el-option :label="t('allLots')" value="" />
          <el-option v-for="lot in lots" :key="lot.id" :label="lot.name" :value="String(lot.id)" />
        </el-select>
        <span v-if="!selectedLotId" class="filter-hint">{{ t('addDisabledHint') }}</span>
      </div>
    </div>

    <div v-if="lots.length === 0" class="card">
      <el-empty :description="t('noLot')" :image-size="80">
        <p class="empty-hint">{{ t('noLotHint') }}</p>
      </el-empty>
    </div>

    <template v-else>
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
          <el-tooltip :disabled="!!selectedLotId" :content="t('addDisabledHint')" placement="top">
            <span>
              <el-button type="primary" plain :disabled="!selectedLotId" @click="openCreate">
                {{ t('add') }}
              </el-button>
            </span>
          </el-tooltip>
        </div>

        <el-table v-loading="loading" :data="pagedLanes" stripe>
          <el-table-column prop="name" :label="t('colName')" min-width="150" />
          <el-table-column prop="code" :label="t('colCode')" min-width="110" />
          <el-table-column :label="t('colLaneType')" width="120">
            <template #default="{ row }">
              <el-tag
                :type="row.laneType === 'EXIT' ? 'warning' : row.laneType === 'BIDIRECTIONAL' ? 'info' : 'success'"
                effect="light"
                round
              >
                {{ laneTypeLabel(row.laneType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('colLot')" min-width="160">
            <template #default="{ row }">{{ connectedLotsLabel(row) }}</template>
          </el-table-column>
          <el-table-column :label="t('colLinkedLot')" min-width="130">
            <template #default="{ row }">{{ row.linkedLotName || '-' }}</template>
          </el-table-column>
          <el-table-column :label="t('colStatus')" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'" effect="light" round>
                {{ row.enabled ? t('statusEnabled') : t('statusDisabled') }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('colUpdatedAt')" min-width="160">
            <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column :label="t('colActions')" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">{{ t('edit') }}</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty
              :description="lanes.length === 0 ? t('emptyList') : t('noMatch')"
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
    </template>

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
          <el-input v-model="form.code" :disabled="form.id !== null" maxlength="64" />
          <div v-if="form.id !== null" class="form-hint">{{ t('codeLocked') }}</div>
        </el-form-item>
        <el-form-item :label="t('laneType')" prop="laneType">
          <el-select v-model="form.laneType" style="width: 100%">
            <el-option
              v-for="option in LANE_TYPE_OPTIONS"
              :key="option"
              :label="laneTypeLabel(option)"
              :value="option"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('lot')" prop="lotId">
          <el-select v-model="form.lotId" style="width: 100%" @change="onFormLotChange">
            <el-option
              v-for="lot in lots"
              :key="lot.id"
              :label="lot.name"
              :value="String(lot.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('linkedLot')" prop="linkedLotId">
          <el-select v-model="form.linkedLotId" clearable style="width: 100%">
            <el-option :label="t('linkedLotNone')" value="" />
            <el-option
              v-for="lot in linkedLotOptions"
              :key="lot.id"
              :label="lot.name"
              :value="String(lot.id)"
            />
          </el-select>
          <div class="form-hint">{{ t('linkedLotHint') }}</div>
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
  </section>
</template>

<style scoped>
.lanes-view {
  display: grid;
  gap: 14px;
  animation: fade-in 0.35s ease;
}

.card {
  background: var(--fp-surface-elevated);
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  padding: 20px;
  box-shadow: var(--fp-shadow-soft);
}

.filter-card {
  padding: 14px 20px;
}

.lot-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-label {
  font-weight: 600;
  color: var(--fp-ink-soft);
}

.filter-hint {
  font-size: 0.8rem;
  color: var(--fp-muted);
}

.empty-hint {
  margin: 0;
  color: var(--fp-muted);
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

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
