<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { useDefaultCurrency } from '../utils/currency'

/**
 * 模拟算费弹窗（每日制 / 24 小时制规则页共用）：
 * 让用户选一段「入场 → 出场」时间区间，调用后端 simulate-charge 试算该模板应收金额。
 * 请求里的时间按系统约定提交站点本地时刻（yyyy-MM-ddTHH:mm:ss），由后端换算处理。
 */
const d: BiDict = {
  title: { 'zh-CN': '模拟算费', en: 'Simulate charge' },
  ruleLabel: { 'zh-CN': '计费模板', en: 'Rule' },
  startLabel: { 'zh-CN': '入场时间', en: 'Entry time' },
  startTip: {
    'zh-CN': '车辆入场时刻，作为计费起点（24 小时制同时作为 24h 窗口锚点）。',
    en: 'When the vehicle enters — the billing anchor (for 24-hour mode it anchors each 24h window).'
  },
  endLabel: { 'zh-CN': '出场时间', en: 'Exit time' },
  endTip: {
    'zh-CN': '车辆出场时刻，须晚于入场时间。',
    en: 'When the vehicle leaves — must be later than the entry time.'
  },
  rangeTip: {
    'zh-CN': '选择一段停车区间后点击“开始试算”，将按该模板的免费时长、周期费率与封顶规则估算应收金额（仅供测算，不代表实际账单）。',
    en: 'Pick a parking interval and run the simulation. The fee is estimated with this rule’s free time, cycle rates and caps (for reference only, not an actual bill).'
  },
  compute: { 'zh-CN': '开始试算', en: 'Run simulation' },
  computing: { 'zh-CN': '试算中…', en: 'Running…' },
  resultLabel: { 'zh-CN': '模拟应收金额', en: 'Simulated fee' },
  close: { 'zh-CN': '关闭', en: 'Close' },
  needTimes: { 'zh-CN': '请选择入场与出场时间', en: 'Pick both entry and exit time' },
  needOrder: {
    'zh-CN': '出场时间必须晚于入场时间',
    en: 'Exit time must be later than entry time'
  },
  needRange: {
    'zh-CN': '模拟区间不能超过 366 天',
    en: 'The simulated interval cannot exceed 366 days'
  },
  reqFailed: { 'zh-CN': '试算失败，请检查输入后重试', en: 'Simulation failed, check the input and retry' },
  emptyResult: { 'zh-CN': '——', en: '—' }
}

const props = defineProps<{
  modelValue: boolean
  /** 后端计费规则资源前缀，如 /billing/daily-rules */
  basePath: string
  /** 被试算的规则 id */
  ruleId: number | null
  /** 被试算的规则标题（用于展示） */
  ruleTitle: string
}>()

const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const { t } = useBiText(d)

/** 站点「收费金额单位」：默认 CNY，随系统配置联动 */
const currency = useDefaultCurrency()
const moneyUnit = computed(() => currency.view.value.unit)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

/** 当前正在试算的模板（prop 快照），避免展示与请求数据源不一致 */
const workingRule = ref<{ id: number; title: string } | null>(null)
const startTime = ref('')
const endTime = ref('')
const running = ref(false)
const totalYuan = ref<number | null>(null)

function pad2(value: number): string {
  return String(value).padStart(2, '0')
}

/** Date → "yyyy-MM-ddTHH:mm"（站点本地钟面） */
function toLocalMinuteText(date: Date): string {
  const y = date.getFullYear()
  const mo = pad2(date.getMonth() + 1)
  const dd = pad2(date.getDate())
  const hh = pad2(date.getHours())
  const mi = pad2(date.getMinutes())
  return `${y}-${mo}-${dd}T${hh}:${mi}`
}

/** 弹窗打开时重置为默认区间：入场=当前时刻，出场=2 小时后 */
function resetInterval(): void {
  totalYuan.value = null
  const start = new Date()
  start.setSeconds(0, 0)
  const end = new Date(start.getTime() + 2 * 60 * 60 * 1000)
  startTime.value = toLocalMinuteText(start)
  endTime.value = toLocalMinuteText(end)
}

watch(
  () => props.modelValue,
  (opened) => {
    if (opened) {
      workingRule.value = props.ruleId ? { id: props.ruleId, title: props.ruleTitle } : null
      resetInterval()
      currency.load()
    }
  }
)

/** 金额展示：去掉无意义的尾零，保留与页面一致的简洁写法 */
function fmtAmount(value: number): string {
  if (value == null || Number.isNaN(value)) {
    return t('emptyResult')
  }
  const fixed = Number(value).toFixed(2)
  return fixed.replace(/\.?0+$/, '').replace(/\.$/, '') || '0'
}

async function runSimulation(): Promise<void> {
  const rule = workingRule.value
  const startText = startTime.value
  const endText = endTime.value
  if (!rule) {
    return
  }
  if (!startText || !endText) {
    ElMessage.warning(t('needTimes'))
    return
  }
  const startMs = new Date(startText).getTime()
  const endMs = new Date(endText).getTime()
  if (!Number.isFinite(startMs) || !Number.isFinite(endMs) || endMs <= startMs) {
    ElMessage.warning(t('needOrder'))
    return
  }
  if (Math.floor((endMs - startMs) / 86400000) >= 367) {
    ElMessage.warning(t('needRange'))
    return
  }

  running.value = true
  try {
    const result = await request.post<never, { totalYuan: number }>(
      `${props.basePath}/${rule.id}/simulate-charge`,
      { startTime: startText, endTime: endText }
    )
    totalYuan.value = result?.totalYuan ?? null
  } catch (error) {
    totalYuan.value = null
    const message = error instanceof Error && error.message ? error.message : t('reqFailed')
    ElMessage.error(message)
  } finally {
    running.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('title')"
    width="min(560px, calc(100vw - 32px))"
    append-to-body
  >
    <div class="simulate-body">
      <div class="rule-line">
        <span class="rule-label">{{ t('ruleLabel') }}</span>
        <span class="rule-title">{{ workingRule?.title ?? '' }}</span>
      </div>

      <el-form label-width="96px" label-position="left">
        <el-form-item :label="t('startLabel')">
          <div class="field-wrap">
            <el-date-picker
              v-model="startTime"
              type="datetime"
              :format="'YYYY-MM-DD HH:mm'"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :placeholder="'YYYY-MM-DD HH:mm'"
              class="time-picker"
              :clearable="false"
            />
            <p class="field-tip">{{ t('startTip') }}</p>
          </div>
        </el-form-item>

        <el-form-item :label="t('endLabel')">
          <div class="field-wrap">
            <el-date-picker
              v-model="endTime"
              type="datetime"
              :format="'YYYY-MM-DD HH:mm'"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :placeholder="'YYYY-MM-DD HH:mm'"
              class="time-picker"
              :clearable="false"
            />
            <p class="field-tip">{{ t('endTip') }}</p>
          </div>
        </el-form-item>
      </el-form>

      <p class="range-tip">{{ t('rangeTip') }}</p>

      <div class="result-box">
        <div class="result-label">{{ t('resultLabel') }}</div>
        <div class="result-amount" :class="{ muted: totalYuan === null }">
          <template v-if="totalYuan === null">{{ t('emptyResult') }}</template>
          <template v-else>{{ fmtAmount(totalYuan) }}</template>
          <span v-if="totalYuan !== null" class="result-unit">{{ moneyUnit }}</span>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">{{ t('close') }}</el-button>
      <el-button type="primary" :loading="running" @click="runSimulation">
        {{ running ? t('computing') : t('compute') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.simulate-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rule-line {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 10px;
}

.rule-label {
  flex: none;
  font-size: 0.8rem;
  color: var(--fp-muted);
}

.rule-title {
  font-weight: 600;
  color: var(--fp-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-wrap {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.time-picker {
  width: 100%;
  max-width: 320px;
}

.field-tip {
  margin: 2px 0 0;
  max-width: 360px;
  font-size: 0.72rem;
  line-height: 1.5;
  color: var(--fp-muted);
}

.range-tip {
  margin: 6px 0 12px;
  max-width: 52ch;
  font-size: 0.75rem;
  line-height: 1.6;
  color: var(--fp-muted);
}

.result-box {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--fp-radius);
  background: var(--fp-bg-soft, #f5f7fa);
}

.result-label {
  font-size: 0.85rem;
  color: var(--fp-ink);
}

.result-amount {
  font-size: 1.6rem;
  font-weight: 700;
  color: #d03050;
  line-height: 1.2;
}

.result-amount.muted {
  color: var(--fp-muted);
  font-weight: 500;
}

.result-unit {
  margin-left: 6px;
  font-size: 0.9rem;
  font-weight: 500;
}
</style>
