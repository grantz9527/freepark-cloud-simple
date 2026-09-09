<script setup lang="ts">
/**
 * 车牌分段点选输入组件。
 *
 * CN（中国大陆，默认）：首个字符为汉字省份简称（31 省级点选），后续字母 + 数字，最多 8 位；
 * 其它区域（HK/MO/TW/EU/GB/US/JP/KR/SG…）：按该地区常见版式做分段提示
 * （如英国 AB12 CDE），字符与版式不匹配时自动退回自由输入，最多 12 位。
 *
 * - 单元格点选可回改，省份可重选；值通过 v-model 输出全车牌字符串。
 * - tone：车牌外观颜色（BLUE/GREEN/YELLOW/BLACK/WHITE…），缺省按第 2 位自动识别蓝/绿（仅 CN）。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { detectPlateColor } from '../plateColor'
import { matchFormat, regionPresetOf } from '../plateRegion'
import { activeLocale, t, tpl, tRegion } from '../i18n'

/** 当前界面语言：英文界面下非 CN 区域提示改用通用文案 */
const isEn = computed(() => activeLocale.value === 'en')

const props = defineProps<{
  modelValue: string
  disabled?: boolean
  autofocus?: boolean
  /** 车牌颜色枚举名，决定车牌显示配色；缺省自动蓝/绿 */
  tone?: string
  /** 车牌版式区域（云端站点配置 plateRegion），缺省 CN */
  region?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const CN_MAX = 8
const GENERIC_MAX = 12
/** 全国省级行政区简称（与交管车牌首字一致，不含港澳台/使领） */
const PROVINCES = [
  '京', '津', '冀', '晋', '蒙', '辽', '吉', '黑',
  '沪', '苏', '浙', '皖', '闽', '赣', '鲁', '豫',
  '鄂', '湘', '粤', '桂', '琼', '渝', '川', '贵',
  '云', '藏', '陕', '甘', '青', '宁', '新'
]
/** 车牌字母：去除交管规定不使用的 I / O */
const LETTERS = 'ABCDEFGHJKLMNPQRSTUVWXYZ'.split('')
const DIGITS = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0']

const chars = ref<string[]>([])
const open = ref(false)
/** 光标所在单元格：0..max-1；追加场景统一落在已有长度处 */
const activeIndex = ref(0)

/** CN 模式还是“按地区版式”的通用模式 */
const isCn = computed(() => !props.region || props.region === 'CN')
const regionMeta = computed(() => regionPresetOf(props.region))
const maxLen = computed(() => (isCn.value ? CN_MAX : GENERIC_MAX))

/** 通用模式下当前字符序列命中的版式（前缀匹配，按注册表顺序取第一条） */
const chosenFormat = computed(() =>
  isCn.value ? null : matchFormat(chars.value, regionMeta.value.formats)
)
/** 通用模式展示的“期望字符类型”提示（随语言） */
const kindLabel = computed(() => {
  if (!chosenFormat.value) return t('pi.type.free')
  const kind = chosenFormat.value.kinds[activeIndex.value]
  if (!kind) return t('pi.type.any')
  if (kind === 'L') return t('pi.type.L')
  if (kind === 'D') return t('pi.type.D')
  return t('pi.type.any')
})

/** 第 2 位（下标 1）只允许字母（仅 CN 的规则） */
const isRegion = computed(() => isCn.value && activeIndex.value === 1)
const modeProvince = computed(() => isCn.value && activeIndex.value === 0)
const toneKey = computed(() => props.tone || detectPlateColor(chars.value.join('')))

watch(
  () => props.modelValue,
  (value) => {
    const next = syncChars(value ?? '')
    if (next.join('') !== chars.value.join('')) {
      chars.value = next
    }
  }
)
watch(
  () => props.disabled,
  (disabled) => {
    if (disabled) open.value = false
  }
)

function allowedMax(): number {
  return maxLen.value
}

function syncChars(value: string): string[] {
  const list: string[] = []
  const max = allowedMax()
  for (const raw of value.trim().toUpperCase()) {
    if (list.length >= max) break
    if (/^[\u4e00-\u9fa5]$/.test(raw)) {
      if (list.length === 0 && PROVINCES.includes(raw)) list.push(raw)
      continue
    }
    if (/^[A-Z0-9]$/.test(raw)) list.push(raw)
  }
  return list
}

function commit(ch: string) {
  if (props.disabled) return
  if (modeProvince.value) {
    // 省份：替换首字（保留后续），或空时插入
    if (chars.value.length === 0) chars.value = [ch]
    else chars.value.splice(0, 1, ch)
    emitValue()
    activeIndex.value = Math.min(chars.value.length, CN_MAX - 1)
    if (chars.value.length >= CN_MAX) open.value = false
    return
  }
  const max = allowedMax()
  const pos = activeIndex.value
  if (pos > max - 1) return
  const appending = pos >= chars.value.length
  if (pos < chars.value.length) {
    // 点选已填位：从该位起重写，清空其后内容
    chars.value.splice(pos, chars.value.length - pos, ch)
  } else {
    if (chars.value.length >= max) return
    chars.value.push(ch)
  }
  emitValue()
  activeIndex.value = Math.min(pos + 1, max - 1)
  // CN 填满即收起；通用模式可继续输入（超出版式位会转为自由输入）
  if (appending && isCn.value && chars.value.length >= CN_MAX) open.value = false
}

function backspace() {
  if (props.disabled) return
  if (chars.value.length === 0) return
  const pos = activeIndex.value
  if (pos === 0) return
  const target = Math.min(pos, chars.value.length) - 1
  chars.value.splice(target, 1)
  activeIndex.value = Math.max(0, target)
  emitValue()
}

function setActive(index: number) {
  if (props.disabled) return
  const max = allowedMax()
  if (index < 0 || index > max - 1) return
  // 已满时最多停到末位（用于回改）；未满时只允许点到下一个空位
  const maxCursor = chars.value.length >= max ? max - 1 : chars.value.length
  activeIndex.value = Math.min(index, maxCursor)
  open.value = true
}

function close() {
  open.value = false
}

function cellText(index: number): string {
  return chars.value[index] ?? ''
}

/** 通用模式下按命中的版式解析分隔符位置 */
function gapAt(index: number): string {
  return chosenFormat.value?.gapAt[index] ?? ''
}

/**
 * 展示单元格总数：
 * - CN：固定 8 位；- 通用：命中版式时按版式位数铺满（未填位为占位）；
 * - 未命中（自由输入）时随已输入长度 + 1 追加一格。
 */
const renderTotal = computed(() => {
  if (isCn.value) return CN_MAX
  if (chosenFormat.value) return chosenFormat.value.kinds.length
  const slots = Math.max(chars.value.length + (chars.value.length >= GENERIC_MAX ? 0 : 1), 1)
  return Math.min(slots, GENERIC_MAX)
})

function cellLabel(index: number): string {
  if (isCn.value) return index === 0 ? (cellText(0) || '省') : cellText(index)
  return cellText(index)
}

function emitValue() {
  emit('update:modelValue', chars.value.join(''))
}

onMounted(() => {
  chars.value = syncChars(props.modelValue ?? '')
  if (props.autofocus) {
    activeIndex.value = chars.value.length === 0 ? 0 : Math.min(chars.value.length, allowedMax() - 1)
    open.value = true
  }
})
</script>

<template>
  <div class="plate-input">
    <!-- 分段车牌展示：每一格都可点选回改，配色随车牌颜色 -->
    <div
      class="plate-view"
      :class="[`tone-${toneKey.toLowerCase()}`, { 'is-active': open, 'is-disabled': disabled }]"
      role="group"
      :aria-label="t('field.plate')"
    >
      <template v-for="i in renderTotal" :key="i">
        <span v-if="!isCn && gapAt(i - 1)" class="plate-gap">{{ gapAt(i - 1) }}</span>
        <button
          :class="[
            isCn && i === 1 ? 'plate-prov' : 'plate-cell',
            {
              'is-filled': !!cellText(i - 1),
              'is-cursor': open && activeIndex === i - 1
            }
          ]"
          type="button"
          :disabled="disabled"
          :aria-label="
            isCn && i === 1
              ? t('pi.pos.provAria')
              : tpl('pi.pos.aria', {
                  n: i,
                  act: cellText(i - 1) ? t('pi.pos.modify') : t('pi.pos.type')
                })
          "
          @click="setActive(i - 1)"
        >
          {{ cellLabel(i - 1) }}
        </button>
      </template>
      <span class="plate-flag">
        {{ toneKey === 'GREEN' && isCn ? t('pi.flag.new') : isCn ? t('pi.flag.cn') : regionMeta.code }}
      </span>
    </div>

    <p v-if="disabled" class="kb-tip">{{ t('pi.busy') }}</p>
    <p v-else-if="!isCn && chars.length > 0" class="kb-tip">
      {{ isEn ? tpl('pi.tip.free', { n: GENERIC_MAX }) : regionMeta.hintZh }}
    </p>
  </div>

  <!-- 键盘弹层（底部滑出） -->
  <Teleport to="body">
    <div v-if="open && !disabled" class="kb-root">
      <div class="kb-mask" @click="close" />
      <section class="kb" role="dialog" aria-modal="true" :aria-label="t('pi.kbd.aria')">
        <div class="kb-head">
          <span class="kb-title">
            <template v-if="modeProvince">{{ t('pi.kbd.province') }}</template>
            <template v-else-if="isRegion">{{ t('pi.kbd.region2') }}</template>
            <template v-else-if="!isCn">
              {{ tpl('pi.kbd.generic', { region: tRegion(regionMeta.code), kind: kindLabel, code: regionMeta.code }) }}
            </template>
            <template v-else>{{ tpl('pi.kbd.pos', { n: activeIndex + 1 }) }}</template>
          </span>
          <span class="kb-cur">{{ tpl('pi.kbd.counter', { a: chars.length, b: maxLen }) }}</span>
        </div>

        <!-- 省份面板 -->
        <div v-if="modeProvince" class="kb-grid kb-prov-grid">
          <button
            v-for="p in PROVINCES"
            :key="p"
            class="key key-prov"
            :class="{ 'key-selected': chars[0] === p }"
            type="button"
            @click="commit(p)"
          >
            {{ p }}
          </button>
        </div>

        <!-- 字母 / 数字键盘 -->
        <template v-else>
          <div class="kb-grid kb-letter-grid">
            <button
              v-for="l in LETTERS"
              :key="l"
              class="key key-letter"
              type="button"
              @click="commit(l)"
            >
              {{ l }}
            </button>
          </div>
          <div v-if="!isRegion" class="kb-grid kb-digit-grid">
            <button
              v-for="d in DIGITS"
              :key="d"
              class="key key-digit"
              type="button"
              @click="commit(d)"
            >
              {{ d }}
            </button>
          </div>
        </template>

        <!-- 底栏：删除 / 完成 -->
        <div class="kb-bar">
          <button
            class="key key-del"
            type="button"
            :disabled="chars.length === 0"
            :aria-label="t('pi.kbd.del')"
            @click="backspace"
          >
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
              <path
                d="M7 6h11a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H7l-4-6 4-6zM10 9l6 6M16 9l-6 6"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
          <button class="key key-done" type="button" @click="close">{{ t('pi.kbd.done') }}</button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.plate-input {
  width: 100%;
}

/* ===== 车牌展示（仿实体车牌） ===== */
.plate-view {
  position: relative;
  display: flex;
  width: 100%;
  height: 60px;
  border-radius: 14px;
  box-shadow:
    0 6px 16px rgba(15, 28, 36, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.35);
  overflow: hidden;
  transition: box-shadow 0.2s;
}
.plate-view.is-active {
  box-shadow:
    0 0 0 3px var(--plate-ring, rgba(13, 122, 111, 0.25)),
    0 8px 20px rgba(15, 28, 36, 0.16);
}
.plate-view.is-disabled {
  opacity: 0.7;
}
.plate-prov,
.plate-cell {
  flex: 1 1 0;
  width: 0;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  padding: 0;
  font-weight: 800;
  font-size: 21px;
  letter-spacing: 1px;
  line-height: 1;
  background: linear-gradient(160deg, var(--plate-bg1), var(--plate-bg2));
  color: var(--plate-ink);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}
.plate-prov:disabled,
.plate-cell:disabled {
  cursor: not-allowed;
}
.plate-prov {
  color: var(--plate-ink);
  border-right: 1px solid var(--plate-line);
}
.plate-cell {
  border-right: 1px solid var(--plate-line);
}
.plate-cell:last-of-type {
  border-right: none;
}
/* 分组分隔符：紧随其后的格不再画竖线，避免双重分隔 */
.plate-cell:has(+ .plate-gap) {
  border-right: none;
}
.plate-gap {
  display: inline-flex;
  align-items: center;
  padding: 0 4px;
  font-size: 10px;
  font-weight: 800;
  color: var(--plate-ink);
  opacity: 0.7;
}
.plate-prov:not(.is-filled),
.plate-cell:not(.is-filled) {
  font-weight: 600;
  font-size: 12px;
  opacity: 0.62;
  letter-spacing: 2px;
}
.plate-prov.is-filled,
.plate-cell.is-filled {
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.18);
}
.plate-cell.is-cursor,
.plate-prov.is-cursor {
  box-shadow: inset 0 -4px 0 var(--plate-cursor, #ffd76a);
}

/* 各颜色车牌 */
.tone-blue {
  --plate-bg1: #2f7fe0;
  --plate-bg2: #1453ae;
  --plate-ink: #ffffff;
  --plate-line: rgba(255, 255, 255, 0.28);
  --plate-ring: rgba(47, 127, 224, 0.3);
}
.tone-green {
  --plate-bg1: #22b573;
  --plate-bg2: #0c8a50;
  --plate-ink: #ffffff;
  --plate-line: rgba(255, 255, 255, 0.28);
  --plate-ring: rgba(34, 181, 115, 0.3);
}
.tone-yellow,
.tone-yellow_green {
  --plate-bg1: #ffd257;
  --plate-bg2: #f5a623;
  --plate-ink: #2a2002;
  --plate-line: rgba(60, 42, 2, 0.16);
  --plate-ring: rgba(245, 166, 35, 0.35);
}
.tone-black {
  --plate-bg1: #3a3f4a;
  --plate-bg2: #15181d;
  --plate-ink: #ffffff;
  --plate-line: rgba(255, 255, 255, 0.2);
  --plate-ring: rgba(60, 63, 74, 0.35);
}
.tone-white {
  --plate-bg1: #ffffff;
  --plate-bg2: #dbe3ea;
  --plate-ink: #10151c;
  --plate-line: rgba(16, 21, 28, 0.1);
  --plate-ring: rgba(16, 21, 28, 0.12);
}
.tone-other {
  --plate-bg1: #7c8aa0;
  --plate-bg2: #525e70;
  --plate-ink: #ffffff;
  --plate-line: rgba(255, 255, 255, 0.22);
  --plate-ring: rgba(124, 138, 160, 0.35);
}

.plate-flag {
  position: absolute;
  right: 6px;
  bottom: 3px;
  font-size: 8px;
  line-height: 1;
  font-weight: 500;
  letter-spacing: 0.5px;
  opacity: 0.8;
  color: var(--plate-ink);
}
.kb-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--fp-muted);
  text-align: center;
}

/* ===== 键盘弹层 ===== */
.kb-root {
  position: fixed;
  inset: 0;
  z-index: 90;
}
.kb-mask {
  position: absolute;
  inset: 0;
  background: rgba(10, 14, 20, 0.3);
}
.kb {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: 100%;
  max-width: 560px;
  box-sizing: border-box;
  padding: 10px 12px calc(12px + env(safe-area-inset-bottom));
  background: var(--fp-surface-elevated);
  border-radius: 20px 20px 0 0;
  border-top: 1px solid var(--fp-line);
  box-shadow: 0 -10px 30px rgba(15, 28, 36, 0.14);
  animation: kb-up 0.22s ease-out;
}
@keyframes kb-up {
  from {
    transform: translate(-50%, 60px);
    opacity: 0;
  }
  to {
    transform: translate(-50%, 0);
    opacity: 1;
  }
}
.kb-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 4px 10px;
}
.kb-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--fp-ink);
}
.kb-cur {
  font-size: 11px;
  color: var(--fp-muted);
  font-variant-numeric: tabular-nums;
}

.kb-grid {
  display: grid;
  gap: 6px;
}
.kb-prov-grid {
  grid-template-columns: repeat(8, 1fr);
}
.kb-letter-grid {
  grid-template-columns: repeat(8, 1fr);
}
.kb-digit-grid {
  margin-top: 6px;
  grid-template-columns: repeat(10, 1fr);
}

.key {
  height: 44px;
  border: none;
  border-radius: 10px;
  background: var(--fp-field);
  color: var(--fp-ink);
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
  -webkit-tap-highlight-color: transparent;
}
.key:active {
  transform: scale(0.95);
}
.key-prov.key-selected {
  background: var(--fp-gradient-soft);
  color: var(--fp-ink);
  box-shadow: inset 0 0 0 1.5px var(--fp-accent);
}
.key-letter {
  font-size: 18px;
}
.key-digit {
  font-size: 18px;
}

.kb-bar {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.key-del {
  flex: 1.4;
  background: var(--fp-field);
  color: var(--fp-ink-soft);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.key-del:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.key-done {
  flex: 1;
  background: var(--fp-gradient);
  color: #fff;
  font-size: 16px;
  box-shadow: 0 6px 14px var(--fp-glow);
}
</style>
