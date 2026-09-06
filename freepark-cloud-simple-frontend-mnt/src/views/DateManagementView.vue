<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

const d: BiDict = {
  holiday: { 'zh-CN': '节假日', en: 'Holidays' },
  makeupWorkday: { 'zh-CN': '补班', en: 'Make-up Workdays' },
  hintTitle: {
    'zh-CN': '计费日期口径',
    en: 'Billing day baseline'
  },
  hintBody: {
    'zh-CN': '默认工作日（周一至周五）收费，周六、周日免费。节假日时间段内一律免费；补班时间段（由周末调为上班）按工作日计费。',
    en: 'By default, weekdays (Mon-Fri) are charged and weekends are free. Dates inside a holiday period are always free; make-up workday periods (weekend turned into a workday) are charged as weekdays.'
  },
  name: { 'zh-CN': '名称', en: 'Name' },
  startTime: { 'zh-CN': '开始时间', en: 'Start time' },
  endTime: { 'zh-CN': '结束时间', en: 'End time' },
  duration: { 'zh-CN': '时长', en: 'Duration' },
  createdAt: { 'zh-CN': '创建时间', en: 'Created at' },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  addHoliday: { 'zh-CN': '新增节假日', en: 'Add holiday' },
  addMakeup: { 'zh-CN': '新增补班', en: 'Add make-up workday' },
  addTitle: { 'zh-CN': '新增时段配置', en: 'Add period entry' },
  editTitle: { 'zh-CN': '编辑时段配置', en: 'Edit period entry' },
  namePlaceholder: { 'zh-CN': '如：国庆假期（可选）', en: 'e.g. National Day (optional)' },
  startHint: { 'zh-CN': '选择开始时间', en: 'Pick start time' },
  endHint: { 'zh-CN': '选择结束时间', en: 'Pick end time' },
  periodTip: {
    'zh-CN': '时间精确到分钟，结束时间须晚于开始时间。整天可设开始 00:00、结束为次日 00:00。',
    en: 'Time is precise to the minute; end must be after start. For a whole day, set start 00:00 and end 00:00 of the next day.'
  },
  tzNote: {
    'zh-CN': '时间段均按系统配置时区 {tz} 判定（保存与展示一致）。',
    en: 'All periods are interpreted in the configured system timezone {tz} (save and display stay consistent).'
  },
  tzBadge: { 'zh-CN': '系统时区', en: 'System timezone' },
  deleteTitle: { 'zh-CN': '删除时段配置', en: 'Delete period entry' },
  deleteMsg: {
    'zh-CN': '确定删除该记录吗？删除后将不再按此时段判断计费。',
    en: 'Delete this entry? It will no longer affect billing after deletion.'
  },
  createSuccess: { 'zh-CN': '已添加', en: 'Added' },
  updateSuccess: { 'zh-CN': '已更新', en: 'Updated' },
  deleteSuccess: { 'zh-CN': '已删除', en: 'Deleted' },
  loadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load, try again' },
  reqFailed: { 'zh-CN': '操作失败，请重试', en: 'Operation failed, try again' },
  needTime: { 'zh-CN': '请选择完整的开始/结束时间', en: 'Please select both start and end time' },
  nameTooLong: { 'zh-CN': '名称过长（最多 80 字）', en: 'Name too long (max 80 chars)' },
  emptyHoliday: { 'zh-CN': '暂无节假日配置，点击右上角新增', en: 'No holidays yet. Add one from the top right.' },
  emptyMakeup: { 'zh-CN': '暂无补班配置，点击右上角新增', en: 'No make-up workdays yet. Add one from the top right.' },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  confirm: { 'zh-CN': '确定', en: 'Confirm' }
}

const { t, locale } = useBiText(d)

/** 系统配置的默认时区（区域与语言-系统配置），日期管理以它为口径 */
const systemTimezone = ref('Asia/Shanghai')
const timezoneLoaded = ref(false)

function timezoneLabel(zone: string): string {
  try {
    const formatter = new Intl.DateTimeFormat(locale.value === 'en' ? 'en' : 'zh-CN', {
      timeZone: zone,
      timeZoneName: 'longOffset'
    })
    const offset =
      formatter.formatToParts(new Date()).find((part) => part.type === 'timeZoneName')?.value ?? ''
    return offset ? `${zone} (${offset})` : zone
  } catch {
    return zone
  }
}

function timezoneHintText(): string {
  return t('tzNote').replace('{tz}', timezoneLabel(systemTimezone.value))
}

type DateRuleType = 'HOLIDAY' | 'MAKEUP_WORKDAY'

interface DateRuleRow {
  id: number
  type: DateRuleType
  name: string | null
  startTime: string
  endTime: string
  createdAt: string
  updatedAt: string
}

interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

const activeType = ref<string>('HOLIDAY')
const rows = ref<DateRuleRow[]>([])
const total = ref(0)
const loading = ref(false)
const pager = reactive({ page: 1, size: 10 })

const dialogVisible = ref(false)
const editing = ref<DateRuleRow | null>(null)
const saving = ref(false)
const form = reactive<{ name: string; startTime: string; endTime: string }>({
  name: '',
  startTime: '',
  endTime: ''
})

const addLabel = computed(() =>
  activeType.value === 'HOLIDAY' ? t('addHoliday') : t('addMakeup')
)
const emptyText = computed(() =>
  activeType.value === 'HOLIDAY' ? t('emptyHoliday') : t('emptyMakeup')
)
const dialogTitle = computed(() => (editing.value ? t('editTitle') : t('addTitle')))

function currentType(): DateRuleType {
  return activeType.value === 'HOLIDAY' ? 'HOLIDAY' : 'MAKEUP_WORKDAY'
}

function errorTextOf(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

/**
 * 解析后端返回的本地挂钟时间文本（如 2026-10-01T08:00:00）。
 * 该时间在系统配置时区下解释，因此此处按纯文本处理，不经过 new Date 的本地时区换算。
 */
function splitWall(value: string): { y: number; mo: number; d: number; h: number; mi: number } | null {
  const match = /^(\d{4})-(\d{1,2})-(\d{1,2})[T ](\d{1,2}):(\d{2})/.exec(value)
  if (!match) {
    return null
  }
  return { y: +match[1], mo: +match[2], d: +match[3], h: +match[4], mi: +match[5] }
}

function formatTime(value?: string): string {
  if (!value) {
    return '-'
  }
  const p = splitWall(value)
  if (!p) {
    return value
  }
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${p.y}-${pad(p.mo)}-${pad(p.d)} ${pad(p.h)}:${pad(p.mi)}`
}

function displayTime(value: string): string {
  return value ? formatTime(value) : '—'
}

function durationText(row: DateRuleRow): string {
  const start = splitWall(row.startTime)
  const end = splitWall(row.endTime)
  if (!start || !end) {
    return '—'
  }
  // 同一口径的挂钟时间，用 UTC 构造只做日历差，避免浏览器本地时区（含夏令时）干扰
  const startMs = Date.UTC(start.y, start.mo - 1, start.d, start.h, start.mi)
  const endMs = Date.UTC(end.y, end.mo - 1, end.d, end.h, end.mi)
  if (endMs <= startMs) {
    return '—'
  }
  const totalMinutes = Math.round((endMs - startMs) / 60_000)
  const days = Math.floor(totalMinutes / 1440)
  const hours = Math.floor((totalMinutes % 1440) / 60)
  const minutes = totalMinutes % 60
  if (locale.value === 'en') {
    if (hours === 0 && minutes === 0) {
      return `${days} day${days === 1 ? '' : 's'}`
    }
    if (days === 0) {
      return `${hours > 0 ? `${hours}h ` : ''}${minutes > 0 ? `${minutes}m` : ''}`.trim()
    }
    return `${days}d ${hours > 0 ? `${hours}h` : ''}${minutes > 0 ? ` ${minutes}m` : ''}`.trim()
  }
  if (hours === 0 && minutes === 0) {
    return `${days} 天`
  }
  if (days === 0) {
    return `${hours > 0 ? `${hours} 小时` : ''}${hours > 0 && minutes > 0 ? ' ' : ''}${minutes > 0 ? `${minutes} 分钟` : ''}`
  }
  return `${days} 天 ${hours} 小时${minutes > 0 ? ` ${minutes} 分钟` : ''}`
}

async function loadList() {
  loading.value = true
  try {
    const data = await request.get<never, PageResult<DateRuleRow>>('/billing/date-rules', {
      params: { type: currentType(), page: pager.page, size: pager.size }
    })
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('loadFailed')))
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pager.page = page
  loadList()
}

function handleTabChange() {
  pager.page = 1
  loadList()
}

function openCreateDialog() {
  editing.value = null
  form.name = ''
  form.startTime = ''
  form.endTime = ''
  dialogVisible.value = true
}

function openEditDialog(row: DateRuleRow) {
  editing.value = row
  form.name = row.name ?? ''
  form.startTime = row.startTime
  form.endTime = row.endTime
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.startTime || !form.endTime) {
    ElMessage.warning(t('needTime'))
    return
  }
  if (form.name.length > 80) {
    ElMessage.warning(t('nameTooLong'))
    return
  }
  const payload = {
    type: currentType(),
    name: form.name.trim() || null,
    startTime: form.startTime,
    endTime: form.endTime
  }
  saving.value = true
  try {
    if (editing.value) {
      await request.put<never, DateRuleRow>(`/billing/date-rules/${editing.value.id}`, payload)
      ElMessage.success(t('updateSuccess'))
    } else {
      await request.post<never, DateRuleRow>('/billing/date-rules', payload)
      ElMessage.success(t('createSuccess'))
    }
    dialogVisible.value = false
    loadList()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('reqFailed')))
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: DateRuleRow) {
  const label = row.name || `${displayTime(row.startTime)} ~ ${displayTime(row.endTime)}`
  try {
    await ElMessageBox.confirm(`${t('deleteMsg')}\n${label}`, t('deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  try {
    await request.delete<never, void>(`/billing/date-rules/${row.id}`)
    ElMessage.success(t('deleteSuccess'))
    if (rows.value.length === 1 && pager.page > 1) {
      pager.page -= 1
    }
    loadList()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('reqFailed')))
  }
}

async function loadTimezone(): Promise<void> {
  try {
    const view = await request.get<never, { timezone: string }>('/system/settings')
    if (view?.timezone) {
      systemTimezone.value = view.timezone
    }
  } catch {
    // 读取失败时保留默认时区，不阻塞日期列表加载
  } finally {
    timezoneLoaded.value = true
  }
}

onMounted(() => {
  loadList()
  loadTimezone()
})
</script>

<template>
  <section class="date-page">
    <el-card class="panel" shadow="never">
      <el-alert type="info" :closable="false" class="hint-alert" show-icon>
        <template #title>
          <strong>{{ t('hintTitle') }}</strong>
        </template>
        <p class="hint-body">{{ t('hintBody') }}</p>
        <p class="hint-tz">{{ timezoneHintText() }}</p>
      </el-alert>

      <el-tabs v-model="activeType" @tab-change="handleTabChange">
        <el-tab-pane :label="t('holiday')" name="HOLIDAY" />
        <el-tab-pane :label="t('makeupWorkday')" name="MAKEUP_WORKDAY" />
      </el-tabs>

      <div class="toolbar">
        <div class="spacer" />
        <el-button type="primary" @click="openCreateDialog">{{ addLabel }}</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="name" :label="t('name')" min-width="160">
          <template #default="{ row }">{{ (row as DateRuleRow).name || '—' }}</template>
        </el-table-column>
        <el-table-column :label="t('startTime')" min-width="150">
          <template #default="{ row }">{{ displayTime((row as DateRuleRow).startTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('endTime')" min-width="150">
          <template #default="{ row }">{{ displayTime((row as DateRuleRow).endTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('duration')" width="130">
          <template #default="{ row }">{{ durationText(row as DateRuleRow) }}</template>
        </el-table-column>
        <el-table-column :label="t('createdAt')" min-width="150">
          <template #default="{ row }">{{ displayTime((row as DateRuleRow).createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('actions')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row as DateRuleRow)">
              {{ t('edit') }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row as DateRuleRow)">
              {{ t('delete') }}
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="emptyText" :image-size="80" />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pager.page"
          :total="total"
          :page-size="pager.size"
          layout="total, prev, pager, next"
          background
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="min(480px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item :label="t('name')">
          <el-input v-model="form.name" :placeholder="t('namePlaceholder')" maxlength="80" clearable />
        </el-form-item>
        <el-form-item :label="t('startTime')">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :placeholder="t('startHint')"
            class="time-picker"
          />
        </el-form-item>
        <el-form-item :label="t('endTime')">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :placeholder="t('endHint')"
            class="time-picker"
          />
        </el-form-item>
        <p class="period-tip">{{ t('periodTip') }}</p>
        <p class="period-tip tz-tip">{{ timezoneHintText() }}</p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">{{ t('confirm') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.date-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  animation: fade-up 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.panel {
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-soft);
}

.hint-alert {
  margin-bottom: 4px;
}

.hint-body {
  margin: 4px 0 0;
  line-height: 1.6;
}

.hint-tz {
  margin: 6px 0 0;
  line-height: 1.5;
  font-weight: 600;
  color: var(--fp-primary, #2563eb);
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.spacer {
  flex: 1;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.time-picker {
  width: 100%;
}

.period-tip {
  margin: 0 0 0 90px;
  font-size: 0.78rem;
  color: var(--fp-muted);
  line-height: 1.5;
}

.period-tip + .tz-tip {
  margin-top: 2px;
}

.tz-tip {
  color: var(--fp-primary, #2563eb);
  font-weight: 600;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
