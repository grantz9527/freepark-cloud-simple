<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { useDefaultCurrency } from '../utils/currency'

const d: BiDict = {
  title: { 'zh-CN': '计费周期', en: 'Billing Cycles' },
  hintTitle: { 'zh-CN': '使用说明', en: 'How it works' },
  hintBody: {
    'zh-CN': '计费周期方案是全局共享的分段费率模板，供各车场的「24小时制计费规则」选用，替代其固定的“周期时长 + 单价”。方案按顺序由若干分段行组成，如：第1个60分钟收5 → 第2个60分钟收1 → 第3个60分钟收2（金额单位以系统配置的币种为准）。每一行还可以填写「重复次数」：计费时该行会连续展开成 N 个相同档位，例如第 1 行 60 分钟收 5、第 2 行 60 分钟收 1 重复 9 次，即第 1 档收 5、第 2~10 档各收 1，无需一行行手动添加。计费按展开后的档位逐档累计；档位全部消耗后，超出时间不再收费。被计费规则引用中的方案无法删除；修改方案会同步影响引用它的规则。',
    en: 'Billing cycle profiles are global segment-rate templates that 24-hour rules can select to replace a fixed cycle length + unit price. Each profile is an ordered list of segment rows, e.g. 1st 60 min rate 5 → 2nd 60 min rate 1 → 3rd 60 min rate 2 (amounts are in the currency configured in System Settings). Every row also has a "repeat count": it expands into N consecutive identical tiers at billing time, e.g. row 1 = 60 min rate 5 and row 2 = 60 min rate 1 ×9 gives tier 1 rate 5 and tiers 2-10 rate 1 each — no need to type them one by one. Fees accumulate tier by tier; once the tiers are fully consumed, no further charge accrues. A profile referenced by rules cannot be deleted; editing it affects every referencing rule.'
  },
  addProfile: { 'zh-CN': '新增方案', en: 'Add profile' },
  addTitle: { 'zh-CN': '新增计费周期方案', en: 'Add billing cycle profile' },
  editTitle: { 'zh-CN': '编辑计费周期方案', en: 'Edit billing cycle profile' },
  colName: { 'zh-CN': '方案名称', en: 'Profile' },
  colSegments: { 'zh-CN': '分段费率', en: 'Segments' },
  colTail: { 'zh-CN': '尾数计费', en: 'Partial segment' },
  actions: { 'zh-CN': '操作', en: 'Actions' },
  edit: { 'zh-CN': '编辑', en: 'Edit' },
  delete: { 'zh-CN': '删除', en: 'Delete' },
  formName: { 'zh-CN': '方案名称', en: 'Profile name' },
  namePlaceholder: { 'zh-CN': '如：首小时较高、逐段递减的梯度方案', en: 'e.g. Tiered profile: higher first hour, then cheaper' },
  formDesc: { 'zh-CN': '方案描述', en: 'Description' },
  descPlaceholder: { 'zh-CN': '补充说明（可选，最多 255 字）', en: 'Notes (optional, max 255 chars)' },
  formTail: { 'zh-CN': '尾数计费方式', en: 'Partial-segment mode' },
  formTailTip: {
    'zh-CN': '停车时长不足当前分段时，该段如何计费。',
    en: 'How to charge when parking ends mid-segment.'
  },
  formSegments: { 'zh-CN': '分段费率', en: 'Rate segments' },
  formSegmentsTip: {
    'zh-CN': '按顺序逐段消耗：第 1 行完成后进入第 2 行，依此类推。「重复次数」表示该行在计费时连续展开成 N 个相同档位，例如第 1 行 60 分钟收 5（×1）、第 2 行 60 分钟收 1（×9），即第 1 档收 5、第 2~10 档各收 1（金额单位以系统配置的币种为准）。重复次数最多可填 99999 次，展开后的总时长不受限制：计费满 24 小时会按本方案自动重新开始。',
    en: 'Rows are consumed in order: after row 1 finishes, row 2 starts, etc. The "repeat count" expands a row into N consecutive identical tiers — e.g. 60 min rate 5 (×1) then 60 min rate 1 (×9) means tier 1 rate 5 and tiers 2-10 rate 1 each (amounts are in the currency configured in System Settings). Repeat count may go up to 99,999 and total expanded minutes are not capped: a full 24-hour cycle automatically restarts this profile.'
  },
  formSegRepeat: { 'zh-CN': '重复次数', en: 'Repeat' },
  repeatTimesUnit: { 'zh-CN': '次', en: 'x' },
  addSegment: { 'zh-CN': '添加分段', en: 'Add segment' },
  removeSegment: { 'zh-CN': '删除', en: 'Remove' },
  segMinuteUnit: { 'zh-CN': '分钟', en: 'min' },
  chainSeparator: { 'zh-CN': ' ＋ ', en: ' + ' },
  countPattern: { 'zh-CN': '共 {n} 段', en: '{n} tiers' },
  coverPattern: { 'zh-CN': '覆盖 {n} 分钟', en: 'covers {n} min' },
  totalNow: { 'zh-CN': '展开合计', en: 'Current total' },
  tailWHOLE_SEGMENT: { 'zh-CN': '不足一段按整段收', en: 'Round up to a full segment' },
  tailPROPORTIONAL: { 'zh-CN': '按分钟比例折算', en: 'Charge by minute proportion' },
  tailNO_CHARGE: { 'zh-CN': '不足一段不计费', en: 'No charge for a partial segment' },
  noData: { 'zh-CN': '暂无计费周期方案', en: 'No billing cycle profile yet' },
  noDataHint: {
    'zh-CN': '点击右上角「新增方案」创建分段费率模板，再到「24小时制计费规则」中选用。',
    en: 'Click “Add profile” above to create a segment template, then pick it in “24-Hour Billing Rules”.'
  },
  cancel: { 'zh-CN': '取消', en: 'Cancel' },
  confirm: { 'zh-CN': '确定', en: 'Confirm' },
  needName: { 'zh-CN': '请填写方案名称', en: 'Please enter a profile name' },
  needSegments: { 'zh-CN': '至少需要配置一个分段', en: 'Configure at least one segment' },
  needNumbers: { 'zh-CN': '请检查分段时长与金额的取值', en: 'Please check segment durations and amounts' },
  deleteTitle: { 'zh-CN': '删除计费周期方案', en: 'Delete billing cycle profile' },
  deleteMsg: {
    'zh-CN': '确定删除该方案吗？被计费规则引用的方案将无法删除。',
    en: 'Delete this profile? Profiles referenced by rules cannot be deleted.'
  },
  createSuccess: { 'zh-CN': '已添加', en: 'Added' },
  updateSuccess: { 'zh-CN': '已更新', en: 'Updated' },
  deleteSuccess: { 'zh-CN': '已删除', en: 'Deleted' },
  loadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load, try again' },
  reqFailed: { 'zh-CN': '操作失败，请重试', en: 'Operation failed, try again' }
}

const TAIL_MODES = ['WHOLE_SEGMENT', 'PROPORTIONAL', 'NO_CHARGE'] as const
type TailMode = (typeof TAIL_MODES)[number]

interface SegView {
  id: number
  seq: number
  minutes: number
  unitPriceYuan: number
  repeatCount: number
}

interface ProfileRow {
  id: number
  name: string
  description: string | null
  tailMode: TailMode
  segments: SegView[]
  createdAt: string
  updatedAt: string
}

interface SegEdit {
  minutes: number
  unitPriceYuan: number
  repeatCount: number
}

const { t } = useBiText(d)

/** 站点「收费金额单位」：默认 CNY，随系统配置联动 */
const currency = useDefaultCurrency()
const moneyUnit = computed(() => currency.view.value.unit)

const rows = ref<ProfileRow[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref<ProfileRow | null>(null)
const saving = ref(false)

const form = reactive({
  name: '',
  description: '',
  tailMode: 'WHOLE_SEGMENT' as TailMode,
  segments: [] as SegEdit[]
})

function tailLabel(mode: string): string {
  return t(`tail${mode}`)
}

function fmtMoney(value: number): string {
  const fixed = Number(value ?? 0).toFixed(2)
  return fixed.replace(/\.?0+$/, '').replace(/\.$/, '') || '0'
}

function repeatOf(seg: { repeatCount?: number }): number {
  const repeat = Number(seg.repeatCount ?? 1)
  return Number.isFinite(repeat) && repeat >= 1 ? repeat : 1
}

function segChain(row: ProfileRow): string {
  return row.segments
    .map((seg) => {
      const base = `${seg.minutes}${t('segMinuteUnit')} ${fmtMoney(seg.unitPriceYuan)}${moneyUnit.value}`
      const repeat = repeatOf(seg)
      return repeat > 1 ? `${base} ×${repeat}` : base
    })
    .join(t('chainSeparator'))
}

/** 展开后的档位数（每行按重复次数展开） */
function expandedCount(row: ProfileRow): number {
  return row.segments.reduce((sum, seg) => sum + repeatOf(seg), 0)
}

/** 展开后的总时长（分钟） */
function totalMinutes(row: ProfileRow): number {
  return row.segments.reduce((sum, seg) => sum + seg.minutes * repeatOf(seg), 0)
}

function dialogTotal(): number {
  return form.segments.reduce((sum, seg) => sum + Number(seg.minutes || 0) * repeatOf(seg), 0)
}

function errorTextOf(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

async function loadProfiles(): Promise<void> {
  loading.value = true
  try {
    const list = await request.get<never, ProfileRow[]>('/billing/cycle-profiles')
    rows.value = list ?? []
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('loadFailed')))
  } finally {
    loading.value = false
  }
}

function resetForm(): void {
  form.name = ''
  form.description = ''
  form.tailMode = 'WHOLE_SEGMENT'
  form.segments = [{ minutes: 60, unitPriceYuan: 0, repeatCount: 1 }]
}

function openCreateDialog(): void {
  editing.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: ProfileRow): void {
  editing.value = row
  form.name = row.name
  form.description = row.description ?? ''
  form.tailMode = row.tailMode
  form.segments = row.segments.map((seg) => ({
    minutes: seg.minutes,
    unitPriceYuan: Number(seg.unitPriceYuan),
    repeatCount: repeatOf(seg)
  }))
  dialogVisible.value = true
}

function addSegmentRow(): void {
  form.segments.push({ minutes: 60, unitPriceYuan: 0, repeatCount: 1 })
}

function removeSegmentRow(index: number): void {
  form.segments.splice(index, 1)
}

async function handleSubmit(): Promise<void> {
  if (!form.name.trim()) {
    ElMessage.warning(t('needName'))
    return
  }
  if (form.segments.length === 0) {
    ElMessage.warning(t('needSegments'))
    return
  }
  for (const seg of form.segments) {
    const minutes = Number(seg.minutes)
    const price = Number(seg.unitPriceYuan ?? 0)
    const repeat = Number(seg.repeatCount ?? 1)
    if (
      !Number.isFinite(minutes) || minutes < 1 || minutes > 1440 ||
      !Number.isFinite(price) || price < 0 || price > 99999999.99 ||
      !Number.isInteger(repeat) || repeat < 1 || repeat > 99999
    ) {
      ElMessage.warning(t('needNumbers'))
      return
    }
  }

  const payload = {
    name: form.name.trim(),
    description: form.description.trim() || null,
    tailMode: form.tailMode,
    segments: form.segments.map((seg) => ({
      minutes: Number(seg.minutes),
      unitPriceYuan: Number(seg.unitPriceYuan ?? 0),
      repeatCount: Number(seg.repeatCount ?? 1)
    }))
  }
  saving.value = true
  try {
    if (editing.value) {
      await request.put<never, ProfileRow>(`/billing/cycle-profiles/${editing.value.id}`, payload)
      ElMessage.success(t('updateSuccess'))
    } else {
      await request.post<never, ProfileRow>('/billing/cycle-profiles', payload)
      ElMessage.success(t('createSuccess'))
    }
    dialogVisible.value = false
    loadProfiles()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('reqFailed')))
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: ProfileRow): Promise<void> {
  try {
    await ElMessageBox.confirm(t('deleteMsg'), t('deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('confirm'),
      cancelButtonText: t('cancel')
    })
  } catch {
    return
  }
  try {
    await request.delete<never, void>(`/billing/cycle-profiles/${row.id}`)
    ElMessage.success(t('deleteSuccess'))
    loadProfiles()
  } catch (error) {
    ElMessage.error(errorTextOf(error, t('reqFailed')))
  }
}

onMounted(() => {
  currency.load()
  loadProfiles()
})
</script>

<template>
  <section class="profiles-page">
    <el-card class="panel" shadow="never">
      <el-alert type="info" :closable="false" class="hint-alert" show-icon>
        <template #title>
          <strong>{{ t('hintTitle') }}</strong>
        </template>
        <p class="hint-body">{{ t('hintBody') }}</p>
      </el-alert>

      <div class="toolbar">
        <h2 class="page-title">{{ t('title') }}</h2>
        <div class="spacer" />
        <el-button type="primary" @click="openCreateDialog">
          {{ t('addProfile') }}
        </el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe class="profiles-table">
        <el-table-column :label="t('colName')" min-width="190">
          <template #default="{ row }">
            <div class="title-cell">
              <span class="profile-name">{{ (row as ProfileRow).name }}</span>
              <span v-if="(row as ProfileRow).description" class="profile-desc">
                {{ (row as ProfileRow).description }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('colSegments')" min-width="260">
          <template #default="{ row }">
            <div class="seg-cell">
              <span class="seg-chain">{{ segChain(row as ProfileRow) }}</span>
              <span class="seg-meta">
                {{ t('countPattern').replace('{n}', String(expandedCount(row as ProfileRow))) }}
                ·
                {{ t('coverPattern').replace('{n}', String(totalMinutes(row as ProfileRow))) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('colTail')" width="180">
          <template #default="{ row }">
            <el-tag effect="light" type="info">{{ tailLabel((row as ProfileRow).tailMode) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column :label="t('actions')" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row as ProfileRow)">
              {{ t('edit') }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row as ProfileRow)">
              {{ t('delete') }}
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="t('noData')" :image-size="80">
            <p class="empty-hint">{{ t('noDataHint') }}</p>
          </el-empty>
        </template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? t('editTitle') : t('addTitle')"
      width="min(680px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
    >
      <el-form label-width="120px" label-position="left">
        <el-form-item :label="t('formName')">
          <el-input v-model="form.name" maxlength="80" clearable :placeholder="t('namePlaceholder')" />
        </el-form-item>

        <el-form-item :label="t('formDesc')">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            maxlength="255"
            :placeholder="t('descPlaceholder')"
          />
        </el-form-item>

        <el-form-item :label="t('formSegments')">
          <div class="segments-editor">
            <p class="field-tip">{{ t('formSegmentsTip') }}</p>
            <div v-for="(seg, index) in form.segments" :key="index" class="seg-row">
              <span class="seg-badge">{{ index + 1 }}</span>
              <el-input-number
                v-model="seg.minutes"
                :min="1"
                :max="1440"
                :step="5"
                class="num-input minutes"
              />
              <span class="unit-label">{{ t('segMinuteUnit') }}</span>
              <el-input-number
                v-model="seg.unitPriceYuan"
                :min="0"
                :max="99999999.99"
                :precision="2"
                :step="0.5"
                class="num-input price"
              />
              <span class="unit-label">{{ moneyUnit }}</span>
              <el-input-number
                v-model="seg.repeatCount"
                :min="1"
                :max="99999"
                :precision="0"
                :step="1"
                class="num-input repeat"
                :title="t('formSegRepeat')"
              />
              <span class="unit-label">{{ t('repeatTimesUnit') }}</span>
              <el-button
                link
                type="danger"
                :disabled="form.segments.length <= 1"
                @click="removeSegmentRow(index)"
              >
                {{ t('removeSegment') }}
              </el-button>
            </div>
            <el-button size="small" plain type="primary" class="add-seg" @click="addSegmentRow">
              ＋ {{ t('addSegment') }}
            </el-button>
            <p class="field-tip total-line">
              {{ t('totalNow') }}：{{ dialogTotal() }} {{ t('segMinuteUnit') }}
            </p>
          </div>
        </el-form-item>

        <el-form-item :label="t('formTail')">
          <el-select v-model="form.tailMode" class="tail-select">
            <el-option
              v-for="mode in TAIL_MODES"
              :key="mode"
              :value="mode"
              :label="tailLabel(mode)"
            />
          </el-select>
          <p class="field-tip">{{ t('formTailTip') }}</p>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">{{ t('confirm') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.profiles-page {
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0 14px;
}

.page-title {
  margin: 0;
  font-family: var(--fp-font-display);
  font-size: 1.05rem;
  color: var(--fp-ink);
}

.spacer {
  flex: 1;
}

.profiles-table {
  width: 100%;
}

.empty-hint {
  margin: 4px 0 0;
  max-width: 52ch;
  line-height: 1.6;
  color: var(--fp-muted);
}

.title-cell,
.seg-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.4;
}

.profile-name {
  color: var(--fp-ink);
  font-weight: 600;
}

.profile-desc {
  font-size: 0.78rem;
  color: var(--fp-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 34ch;
}

.seg-chain {
  color: var(--fp-ink);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.seg-meta {
  font-size: 0.78rem;
  color: var(--fp-muted);
  white-space: nowrap;
}

.segments-editor {
  width: 100%;
}

.field-tip {
  margin: 0 0 6px;
  max-width: 420px;
  font-size: 0.75rem;
  line-height: 1.5;
  color: var(--fp-muted);
}

.total-line {
  margin-top: 8px;
  font-weight: 600;
  color: var(--fp-ink);
}

.seg-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.seg-badge {
  width: 22px;
  flex: none;
  font-size: 0.8rem;
  color: var(--fp-muted);
  text-align: right;
}

.num-input {
  width: 132px;
}

.num-input.minutes {
  width: 118px;
}

.num-input.price {
  width: 128px;
}

.num-input.repeat {
  width: 128px;
}

.unit-label {
  color: var(--fp-muted);
  font-size: 0.8rem;
  white-space: nowrap;
}

.add-seg {
  margin-top: 2px;
}

.tail-select {
  width: 320px;
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
