<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

type JudgmentRule = 'BLACKLIST' | 'WHITELIST' | 'PATTERN_ALLOWLIST'

const DEFAULT_JUDGE_ORDER: JudgmentRule[] = ['BLACKLIST', 'WHITELIST', 'PATTERN_ALLOWLIST']

type InterceptRule = 'ARREARS' | 'BLACKLIST'

const INTERCEPT_RULES: InterceptRule[] = ['ARREARS', 'BLACKLIST']

const d: BiDict = {
  lotLabel: { 'zh-CN': '停车场', en: 'Parking Lot' },
  lotPlaceholder: { 'zh-CN': '请选择停车场', en: 'Select a parking lot' },
  noLots: { 'zh-CN': '暂无停车场数据，请先在「车场管理」中创建', en: 'No parking lots yet. Create one in Lot Management first' },
  judgeTitle: { 'zh-CN': '通行判定顺序', en: 'Access Judgment Order' },
  judgeHint: {
    'zh-CN': '按从上到下的顺序逐条判定，先命中的规则立即生效，后续规则不再执行。',
    en: 'Rules are evaluated from top to bottom; the first matched rule takes effect and the rest are skipped.'
  },
  defaultOrderHint: {
    'zh-CN': '默认顺序（未自定义时）：黑名单 → 白名单 → 正则名单。',
    en: 'Default order (when not customized): Blacklist → Whitelist → Pattern Allowlist.'
  },
  moveUp: { 'zh-CN': '上移', en: 'Up' },
  moveDown: { 'zh-CN': '下移', en: 'Down' },
  saveOrder: { 'zh-CN': '保存判定顺序', en: 'Save Order' },
  savingOrder: { 'zh-CN': '保存中…', en: 'Saving…' },
  orderNoChange: { 'zh-CN': '顺序尚未调整', en: 'No changes yet' },
  interceptTitle: { 'zh-CN': '出入口拦截规则', en: 'Entry / Exit Intercepts' },
  interceptHint: {
    'zh-CN': '勾选车辆进出车场时需要拦截的情形；拦截后车辆无法通行，需人工处理。',
    en: 'Check the situations that should be intercepted when vehicles enter or leave; intercepted vehicles cannot pass and require manual handling.'
  },
  groupEntry: { 'zh-CN': '入口拦截', en: 'Entry Intercept' },
  groupEntryHint: {
    'zh-CN': '车辆进入车场时，满足以下任一情形即拦截放行失败。',
    en: 'When a vehicle enters, any checked situation below blocks entry.'
  },
  groupExit: { 'zh-CN': '出口拦截', en: 'Exit Intercept' },
  groupExitHint: {
    'zh-CN': '车辆离开车场时，满足以下任一情形即拦截放行失败。',
    en: 'When a vehicle leaves, any checked situation below blocks exit.'
  },
  saveIntercept: { 'zh-CN': '保存拦截规则', en: 'Save Intercepts' },
  savingIntercept: { 'zh-CN': '保存中…', en: 'Saving…' },
  interceptNoChange: { 'zh-CN': '拦截规则尚未调整', en: 'No changes yet' },
  'intercept.ARREARS': { 'zh-CN': '欠费拦截', en: 'Arrears Intercept' },
  'intercept.BLACKLIST': { 'zh-CN': '黑名单拦截', en: 'Blacklist Intercept' },
  'judgeRule.BLACKLIST': { 'zh-CN': '黑名单', en: 'Blacklist' },
  'judgeRule.WHITELIST': { 'zh-CN': '白名单', en: 'Whitelist' },
  'judgeRule.PATTERN_ALLOWLIST': { 'zh-CN': '正则名单', en: 'Pattern Allowlist' },
  'judgeDesc.BLACKLIST': { 'zh-CN': '命中黑名单 → 禁止通行', en: 'On the blacklist → deny entry' },
  'judgeDesc.WHITELIST': { 'zh-CN': '命中白名单 → 直接放行', en: 'On the whitelist → allow pass' },
  'judgeDesc.PATTERN_ALLOWLIST': { 'zh-CN': '命中号段正则 → 直接放行', en: 'Matches the pattern → allow pass' },
  msgSaveOk: { 'zh-CN': '保存成功', en: 'Saved successfully' },
  errLoad: { 'zh-CN': '加载失败，请稍后重试', en: 'Failed to load, please retry later' },
  errSaveOrder: { 'zh-CN': '保存判定顺序失败', en: 'Failed to save the order' },
  errSaveIntercept: { 'zh-CN': '保存拦截规则失败', en: 'Failed to save the intercepts' }
}
const { t } = useBiText(d)

const route = useRoute()

interface LotItem {
  id: number
  name: string
  code: string
}

const loading = ref(false)
const orderSaving = ref(false)
const interceptSaving = ref(false)

const lots = ref<LotItem[]>([])
const selectedLotId = ref<number | null>(null)

const ruleOrder = ref<JudgmentRule[]>([])
const orderDirty = ref(false)

const entryRules = ref<InterceptRule[]>([])
const exitRules = ref<InterceptRule[]>([])
const interceptDirty = ref(false)

/* ---------------- 基础方法 ---------------- */

function errorText(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function judgeLabel(rule: JudgmentRule): string {
  return t(`judgeRule.${rule}`)
}

function judgeDesc(rule: JudgmentRule): string {
  return t(`judgeDesc.${rule}`)
}

function judgeTag(rule: JudgmentRule): 'success' | 'warning' | 'danger' {
  if (rule === 'BLACKLIST') {
    return 'danger'
  }
  if (rule === 'WHITELIST') {
    return 'success'
  }
  return 'warning'
}

function interceptLabel(rule: InterceptRule): string {
  return t(`intercept.${rule}`)
}

function normalizeOrder(order?: JudgmentRule[] | null): JudgmentRule[] {
  if (
    order &&
    order.length === DEFAULT_JUDGE_ORDER.length &&
    DEFAULT_JUDGE_ORDER.every((rule) => order.includes(rule))
  ) {
    return [...order]
  }
  return [...DEFAULT_JUDGE_ORDER]
}

function normalizeRules(value?: InterceptRule[] | null): InterceptRule[] {
  if (!value) {
    return []
  }
  return value.filter((rule) => (INTERCEPT_RULES as string[]).includes(rule))
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
  loadConfigs()
}

/* ---------------- 配置加载 ---------------- */

async function loadConfigs() {
  if (selectedLotId.value == null) {
    ruleOrder.value = [...DEFAULT_JUDGE_ORDER]
    entryRules.value = []
    exitRules.value = []
    return
  }
  loading.value = true
  try {
    const [judgment, intercept] = await Promise.all([
      request.get<never, { ruleOrder: JudgmentRule[] | null }>(
        `/lots/${selectedLotId.value}/access-judgment`
      ),
      request.get<never, { entryRules: InterceptRule[] | null; exitRules: InterceptRule[] | null }>(
        `/lots/${selectedLotId.value}/intercept`
      )
    ])
    ruleOrder.value = normalizeOrder(judgment.ruleOrder)
    entryRules.value = normalizeRules(intercept.entryRules)
    exitRules.value = normalizeRules(intercept.exitRules)
    orderDirty.value = false
    interceptDirty.value = false
  } catch (error) {
    ElMessage.error(errorText(error, t('errLoad')))
  } finally {
    loading.value = false
  }
}

/* ---------------- 判序调整与保存 ---------------- */

function moveRule(index: number, direction: -1 | 1) {
  const target = index + direction
  if (target < 0 || target >= ruleOrder.value.length) {
    return
  }
  const next = [...ruleOrder.value]
  const [moved] = next.splice(index, 1)
  if (moved) {
    next.splice(target, 0, moved)
    ruleOrder.value = next
    orderDirty.value = true
  }
}

function moveUp(index: number) {
  moveRule(index, -1)
}

function moveDown(index: number) {
  moveRule(index, 1)
}

async function saveOrder() {
  if (selectedLotId.value == null) {
    return
  }
  orderSaving.value = true
  try {
    const data = await request.put<never, { ruleOrder: JudgmentRule[] | null }>(
      `/lots/${selectedLotId.value}/access-judgment`,
      { ruleOrder: [...ruleOrder.value] }
    )
    ruleOrder.value = normalizeOrder(data.ruleOrder)
    orderDirty.value = false
    ElMessage.success(t('msgSaveOk'))
  } catch (error) {
    ElMessage.error(errorText(error, t('errSaveOrder')))
  } finally {
    orderSaving.value = false
  }
}

/* ---------------- 出入口拦截 ---------------- */

function onEntryChange(value: unknown) {
  entryRules.value = Array.isArray(value) ? normalizeRules(value as InterceptRule[]) : []
  interceptDirty.value = true
}

function onExitChange(value: unknown) {
  exitRules.value = Array.isArray(value) ? normalizeRules(value as InterceptRule[]) : []
  interceptDirty.value = true
}

async function saveIntercept() {
  if (selectedLotId.value == null) {
    return
  }
  interceptSaving.value = true
  try {
    const data = await request.put<
      never,
      { entryRules: InterceptRule[] | null; exitRules: InterceptRule[] | null }
    >(`/lots/${selectedLotId.value}/intercept`, {
      entryRules: [...entryRules.value],
      exitRules: [...exitRules.value]
    })
    entryRules.value = normalizeRules(data.entryRules)
    exitRules.value = normalizeRules(data.exitRules)
    interceptDirty.value = false
    ElMessage.success(t('msgSaveOk'))
  } catch (error) {
    ElMessage.error(errorText(error, t('errSaveIntercept')))
  } finally {
    interceptSaving.value = false
  }
}

onMounted(async () => {
  try {
    await loadLots()
    await loadConfigs()
  } catch (error) {
    ElMessage.error(errorText(error, t('errLoad')))
  }
})
</script>

<template>
  <section class="aj-page">
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

    <template v-else>
      <!-- 通行判定顺序 -->
      <el-card v-loading="loading" class="panel cfg-panel" shadow="never">
        <div class="panel-head">
          <h3 class="panel-title">{{ t('judgeTitle') }}</h3>
          <p class="panel-hint">{{ t('judgeHint') }}</p>
          <p class="panel-hint default-hint">{{ t('defaultOrderHint') }}</p>
        </div>

        <div class="order-list">
          <div v-for="(rule, index) in ruleOrder" :key="rule" class="order-row">
            <span class="order-index">{{ index + 1 }}</span>
            <div class="order-info">
              <div class="order-name">
                <el-tag :type="judgeTag(rule)" effect="light" size="small">
                  {{ judgeLabel(rule) }}
                </el-tag>
              </div>
              <div class="order-desc">{{ judgeDesc(rule) }}</div>
            </div>
            <div class="order-actions">
              <el-button link type="primary" :disabled="index === 0" @click="moveUp(index)">
                {{ t('moveUp') }}
              </el-button>
              <el-button
                link
                type="primary"
                :disabled="index === ruleOrder.length - 1"
                @click="moveDown(index)"
              >
                {{ t('moveDown') }}
              </el-button>
            </div>
          </div>
        </div>

        <div class="panel-foot">
          <span v-if="!orderDirty" class="foot-tip">{{ t('orderNoChange') }}</span>
          <el-button
            type="primary"
            :loading="orderSaving"
            :disabled="!orderDirty"
            @click="saveOrder"
          >
            {{ orderSaving ? t('savingOrder') : t('saveOrder') }}
          </el-button>
        </div>
      </el-card>

      <!-- 出入口拦截 -->
      <el-card v-loading="loading" class="panel cfg-panel" shadow="never">
        <div class="panel-head">
          <h3 class="panel-title">{{ t('interceptTitle') }}</h3>
          <p class="panel-hint">{{ t('interceptHint') }}</p>
        </div>

        <div class="intercept-grid">
          <div class="intercept-box">
            <div class="box-title">{{ t('groupEntry') }}</div>
            <p class="box-hint">{{ t('groupEntryHint') }}</p>
            <el-checkbox-group :model-value="entryRules" @update:model-value="onEntryChange">
              <el-checkbox
                v-for="rule in INTERCEPT_RULES"
                :key="rule"
                :value="rule"
                border
                class="intercept-check"
              >
                {{ interceptLabel(rule) }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
          <div class="intercept-box">
            <div class="box-title">{{ t('groupExit') }}</div>
            <p class="box-hint">{{ t('groupExitHint') }}</p>
            <el-checkbox-group :model-value="exitRules" @update:model-value="onExitChange">
              <el-checkbox
                v-for="rule in INTERCEPT_RULES"
                :key="rule"
                :value="rule"
                border
                class="intercept-check"
              >
                {{ interceptLabel(rule) }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>

        <div class="panel-foot">
          <span v-if="!interceptDirty" class="foot-tip">{{ t('interceptNoChange') }}</span>
          <el-button
            type="primary"
            :loading="interceptSaving"
            :disabled="!interceptDirty"
            @click="saveIntercept"
          >
            {{ interceptSaving ? t('savingIntercept') : t('saveIntercept') }}
          </el-button>
        </div>
      </el-card>
    </template>
  </section>
</template>

<style scoped>
.aj-page {
  display: grid;
  gap: 16px;
  animation: fade-in 0.35s ease;
}

.panel {
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-elevated);
}

.lot-bar :deep(.el-card__body) {
  padding: 14px 20px;
}

.cfg-panel :deep(.el-card__body) {
  padding: 18px 20px;
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

.panel-head {
  margin-bottom: 14px;
}

.panel-title {
  margin: 0;
  font-size: 1.02rem;
  font-family: var(--fp-font-display);
}

.panel-hint {
  margin: 6px 0 0;
  line-height: 1.6;
  color: var(--fp-muted);
  font-size: 0.88rem;
}

.default-hint {
  font-size: 0.84rem;
}

.order-list {
  display: grid;
  gap: 8px;
}

.order-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-subtle, rgba(0, 0, 0, 0.02));
}

.order-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-weight: 700;
  font-size: 0.85rem;
  color: var(--fp-teal, #0f766e);
  background: var(--fp-teal-soft, #ccfbf1);
}

.order-info {
  flex: 1;
  min-width: 0;
}

.order-name {
  margin-bottom: 2px;
}

.order-desc {
  font-size: 0.82rem;
  color: var(--fp-muted);
}

.order-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 4px;
}

.panel-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.foot-tip {
  color: var(--fp-muted);
  font-size: 0.84rem;
}

.intercept-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
}

.intercept-box {
  padding: 14px 16px;
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-subtle, rgba(0, 0, 0, 0.02));
}

.box-title {
  font-weight: 600;
}

.box-hint {
  margin: 4px 0 12px;
  line-height: 1.5;
  color: var(--fp-muted);
  font-size: 0.84rem;
}

.intercept-check {
  margin-right: 12px;
  margin-bottom: 8px;
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
