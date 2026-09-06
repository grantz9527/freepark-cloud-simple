<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

type LotTypeOption = 'INTERNAL' | 'PUBLIC'

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
  requestFailed: { 'zh-CN': '请求失败，请稍后重试', en: 'Request failed, please try again later' }
}

const { t } = useBiText(d)
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
  return value || '-'
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
        <el-table-column :label="t('colActions')" min-width="320" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button link type="primary" @click="openEdit(row)">{{ t('edit') }}</el-button>
              <el-button link type="primary" @click="openMap(row)">{{ t('map') }}</el-button>
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

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
