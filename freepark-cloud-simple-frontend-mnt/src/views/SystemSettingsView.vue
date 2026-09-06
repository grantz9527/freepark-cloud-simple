<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

const d: BiDict = {
  loading: { 'zh-CN': '正在加载配置…', en: 'Loading settings…' },
  regional: { 'zh-CN': '区域与语言', en: 'Region & Language' },
  regionalHint: {
    'zh-CN': '站点默认使用的语言与时区，新接入的设备与页面将按此基准展示。',
    en: 'Default language and timezone of the site. Newly connected devices and pages follow this baseline.'
  },
  defaultLanguage: { 'zh-CN': '默认语言', en: 'Default language' },
  languageZh: { 'zh-CN': '简体中文', en: 'Simplified Chinese' },
  languageEn: { 'zh-CN': '英文', en: 'English' },
  timezone: { 'zh-CN': '时区', en: 'Timezone' },
  plateColors: { 'zh-CN': '车牌颜色', en: 'Plate Colors' },
  plateColorsHint: {
    'zh-CN': '勾选允许的车牌颜色（至少要保留一种），并指定默认颜色。默认颜色必须是允许颜色之一。',
    en: 'Check the allowed plate colors (keep at least one) and pick the default one. The default must be one of the allowed colors.'
  },
  allowedPlateColors: { 'zh-CN': '允许的车牌颜色', en: 'Allowed plate colors' },
  defaultPlateColor: { 'zh-CN': '默认车牌颜色', en: 'Default plate color' },
  colorBLUE: { 'zh-CN': '蓝色', en: 'Blue' },
  colorYELLOW: { 'zh-CN': '黄色', en: 'Yellow' },
  colorGREEN: { 'zh-CN': '绿色', en: 'Green' },
  colorYELLOW_GREEN: { 'zh-CN': '黄绿色', en: 'Yellow-green' },
  colorBLACK: { 'zh-CN': '黑色', en: 'Black' },
  colorWHITE: { 'zh-CN': '白色', en: 'White' },
  colorRED: { 'zh-CN': '红色', en: 'Red' },
  colorORANGE: { 'zh-CN': '橙色', en: 'Orange' },
  colorBROWN: { 'zh-CN': '棕色', en: 'Brown' },
  colorPURPLE: { 'zh-CN': '紫色', en: 'Purple' },
  colorPINK: { 'zh-CN': '粉色', en: 'Pink' },
  colorGRAY: { 'zh-CN': '灰色', en: 'Gray' },
  colorSILVER: { 'zh-CN': '银色', en: 'Silver' },
  colorGOLD: { 'zh-CN': '金色', en: 'Gold' },
  colorCREAM: { 'zh-CN': '米色', en: 'Cream' },
  colorBEIGE: { 'zh-CN': '浅褐色', en: 'Beige' },
  colorNAVY: { 'zh-CN': '藏青色', en: 'Navy' },
  colorMAROON: { 'zh-CN': '栗色', en: 'Maroon' },
  colorOLIVE: { 'zh-CN': '橄榄色', en: 'Olive' },
  colorTEAL: { 'zh-CN': '青绿色', en: 'Teal' },
  colorCYAN: { 'zh-CN': '青色', en: 'Cyan' },
  colorMAGENTA: { 'zh-CN': '品红色', en: 'Magenta' },
  colorLIME: { 'zh-CN': '柠檬绿', en: 'Lime' },
  colorLAVENDER: { 'zh-CN': '淡紫色', en: 'Lavender' },
  colorTURQUOISE: { 'zh-CN': '绿松石色', en: 'Turquoise' },
  colorINDIGO: { 'zh-CN': '靛蓝色', en: 'Indigo' },
  colorCORAL: { 'zh-CN': '珊瑚色', en: 'Coral' },
  colorAMBER: { 'zh-CN': '琥珀色', en: 'Amber' },
  colorVIOLET: { 'zh-CN': '紫罗兰色', en: 'Violet' },
  colorCHARCOAL: { 'zh-CN': '炭灰色', en: 'Charcoal' },
  colorLIGHT_BLUE: { 'zh-CN': '浅蓝色', en: 'Light blue' },
  colorLIGHT_GREEN: { 'zh-CN': '浅绿色', en: 'Light green' },
  colorDARK_BLUE: { 'zh-CN': '深蓝色', en: 'Dark blue' },
  colorDARK_GREEN: { 'zh-CN': '深绿色', en: 'Dark green' },
  colorRUST: { 'zh-CN': '铁锈色', en: 'Rust' },
  colorBRONZE: { 'zh-CN': '青铜色', en: 'Bronze' },
  colorPEACH: { 'zh-CN': '桃色', en: 'Peach' },
  colorMINT: { 'zh-CN': '薄荷绿', en: 'Mint' },
  colorROSE: { 'zh-CN': '玫瑰色', en: 'Rose' },
  colorSALMON: { 'zh-CN': '鲑鱼色', en: 'Salmon' },
  colorCOPPER: { 'zh-CN': '铜色', en: 'Copper' },
  colorPLUM: { 'zh-CN': '李子紫', en: 'Plum' },
  colorCRIMSON: { 'zh-CN': '深红色', en: 'Crimson' },
  colorSCARLET: { 'zh-CN': '猩红色', en: 'Scarlet' },
  colorEMERALD: { 'zh-CN': '翠绿色', en: 'Emerald' },
  colorSAPPHIRE: { 'zh-CN': '蓝宝石色', en: 'Sapphire' },
  colorRUBY: { 'zh-CN': '红宝石色', en: 'Ruby' },
  colorOTHER: { 'zh-CN': '其他', en: 'Other' },
  atLeastOne: { 'zh-CN': '至少保留一种允许的车牌颜色', en: 'Keep at least one allowed plate color' },
  saved: { 'zh-CN': '配置已保存', en: 'Settings saved' },
  save: { 'zh-CN': '保存配置', en: 'Save settings' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  loadFailed: { 'zh-CN': '加载配置失败，请重试', en: 'Failed to load settings. Try again.' },
  lastUpdated: { 'zh-CN': '最近更新', en: 'Last updated' },
  scope: { 'zh-CN': '仅超级管理员可修改站点配置。', en: 'Only super administrators can modify site settings.' }
}

const { t, locale } = useBiText(d)

interface SystemSettingsData {
  defaultLocale: string
  timezone: string
  defaultPlateColor: string
  allowedPlateColors: string[]
  supportedLocales: string[]
  supportedTimezones: string[]
  supportedPlateColors: string[]
  updatedAt: string
}

const loading = ref(true)
const saving = ref(false)
const data = ref<SystemSettingsData | null>(null)

const defaultLocale = ref('zh-CN')
const timezone = ref('Asia/Shanghai')
const defaultPlateColor = ref('BLUE')
const allowedPlateColors = ref<string[]>([])
const supportedLocales = ref<string[]>([])
const supportedTimezones = ref<string[]>([])
const supportedPlateColors = ref<string[]>([])
const updatedAt = ref('')

/** 颜色块配色：与参考实现 PlateColor 枚举对应的近似底色 */
const COLOR_HEX: Record<string, string> = {
  BLUE: '#1e64c8',
  YELLOW: '#f2c53d',
  GREEN: '#1e9e5a',
  YELLOW_GREEN: '#a6c80f',
  BLACK: '#24272b',
  WHITE: '#f6f7f9',
  RED: '#d93025',
  ORANGE: '#f57c25',
  BROWN: '#8a5a28',
  PURPLE: '#7e3ff2',
  PINK: '#f07fae',
  GRAY: '#9aa2ad',
  SILVER: '#c2c7cf',
  GOLD: '#d4af37',
  CREAM: '#fdf1d7',
  BEIGE: '#e7d6b2',
  NAVY: '#1c2f5a',
  MAROON: '#7d1f2e',
  OLIVE: '#6b8e23',
  TEAL: '#0f8b8d',
  CYAN: '#22b8d4',
  MAGENTA: '#d81b60',
  LIME: '#aeea00',
  LAVENDER: '#b78ae0',
  TURQUOISE: '#2dd4bf',
  INDIGO: '#3f51b5',
  CORAL: '#ff6f61',
  AMBER: '#ffb300',
  VIOLET: '#7e4de8',
  CHARCOAL: '#37474f',
  LIGHT_BLUE: '#69b1ff',
  LIGHT_GREEN: '#8fd460',
  DARK_BLUE: '#1b2f66',
  DARK_GREEN: '#1b5e20',
  RUST: '#b7410e',
  BRONZE: '#8c5e2f',
  PEACH: '#ffd9b3',
  MINT: '#9fe8cf',
  ROSE: '#f5426f',
  SALMON: '#fa8072',
  COPPER: '#b87333',
  PLUM: '#8e4585',
  CRIMSON: '#9b1b30',
  SCARLET: '#e32f1f',
  EMERALD: '#2fbf71',
  SAPPHIRE: '#0f52ba',
  RUBY: '#e0115f',
  OTHER: '#9aa0a6'
}

const localeOptions = computed(() =>
  supportedLocales.value.map((code) => ({
    value: code,
    label:
      code === 'en' ? t('languageEn') : code === 'zh-CN' ? t('languageZh') : code
  }))
)

/** 默认颜色下拉展示全部支持颜色，但只能选择“已允许”的颜色 */
const defaultColorOptions = computed(() =>
  supportedPlateColors.value.map((color) => ({
    value: color,
    label: colorLabel(color),
    disabled: !allowedPlateColors.value.includes(color)
  }))
)

watch(allowedPlateColors, (colors) => {
  if (colors.length > 0 && !colors.includes(defaultPlateColor.value)) {
    defaultPlateColor.value = colors[0]
  }
})

function colorLabel(color: string): string {
  return t(`color${color}`) || color
}

function chipStyle(color: string): Record<string, string> {
  const bg = COLOR_HEX[color] ?? COLOR_HEX.OTHER ?? '#9aa0a6'
  return { background: bg, color: readableTextColor(bg) }
}

/** 依据底色亮度自动选择深/浅文字色 */
function readableTextColor(hex: string): string {
  const value = hex.replace('#', '')
  const r = parseInt(value.slice(0, 2), 16)
  const g = parseInt(value.slice(2, 4), 16)
  const b = parseInt(value.slice(4, 6), 16)
  const luminance = (r * 299 + g * 587 + b * 114) / 1000
  return luminance > 150 ? '#2f3640' : '#ffffff'
}

function isAllowed(color: string): boolean {
  return allowedPlateColors.value.includes(color)
}

function toggleColor(color: string, checked: boolean): void {
  if (checked) {
    if (!allowedPlateColors.value.includes(color)) {
      allowedPlateColors.value = [...allowedPlateColors.value, color]
    }
    return
  }
  if (allowedPlateColors.value.length <= 1) {
    ElMessage.warning(t('atLeastOne'))
    return
  }
  allowedPlateColors.value = allowedPlateColors.value.filter((item) => item !== color)
}

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

/**
 * 后端返回的本地挂钟时间文本（如 2026-09-07T03:01:02），
 * 该时间在系统配置时区下解释，因此按纯文本格式化，不经过 new Date 的浏览器本地时区换算。
 */
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

async function loadSettings(): Promise<void> {
  loading.value = true
  try {
    data.value = await request.get<never, SystemSettingsData>('/system/settings')
    const view = data.value
    defaultLocale.value = view.defaultLocale
    timezone.value = view.timezone
    defaultPlateColor.value = view.defaultPlateColor
    allowedPlateColors.value = [...view.allowedPlateColors]
    supportedLocales.value = [...view.supportedLocales]
    supportedTimezones.value = [...view.supportedTimezones]
    supportedPlateColors.value = [...view.supportedPlateColors]
    updatedAt.value = view.updatedAt
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleSave(): Promise<void> {
  if (allowedPlateColors.value.length === 0) {
    ElMessage.warning(t('atLeastOne'))
    return
  }
  saving.value = true
  try {
    const view = await request.put<never, SystemSettingsData>('/system/settings', {
      defaultLocale: defaultLocale.value,
      timezone: timezone.value,
      defaultPlateColor: defaultPlateColor.value,
      allowedPlateColors: allowedPlateColors.value
    })
    defaultLocale.value = view.defaultLocale
    timezone.value = view.timezone
    defaultPlateColor.value = view.defaultPlateColor
    allowedPlateColors.value = [...view.allowedPlateColors]
    updatedAt.value = view.updatedAt
    ElMessage.success(t('saved'))
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('loadFailed'))
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<template>
  <section class="settings-page">
    <el-card class="panel" shadow="never" v-loading="loading">
      <el-form label-position="top" class="settings-form" @submit.prevent="handleSave">
        <el-alert type="info" :closable="false" class="scope-alert" show-icon>
          <span>{{ t('scope') }}</span>
        </el-alert>

        <h3 class="group-title">{{ t('regional') }}</h3>
        <p class="group-hint">{{ t('regionalHint') }}</p>
        <div class="field-grid">
          <el-form-item :label="t('defaultLanguage')">
            <el-select v-model="defaultLocale" class="field">
              <el-option
                v-for="option in localeOptions"
                :key="option.value"
                :value="option.value"
                :label="option.label"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('timezone')">
            <el-select v-model="timezone" filterable class="field">
              <el-option
                v-for="zone in supportedTimezones"
                :key="zone"
                :value="zone"
                :label="timezoneLabel(zone)"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-divider />

        <h3 class="group-title">{{ t('plateColors') }}</h3>
        <p class="group-hint">{{ t('plateColorsHint') }}</p>

        <div class="colors-block">
          <span class="field-label">{{ t('allowedPlateColors') }}</span>
          <div class="color-grid">
            <label
              v-for="color in supportedPlateColors"
              :key="color"
              class="color-option"
              :class="{ disabled: isAllowed(color) && allowedPlateColors.length === 1 }"
            >
              <el-checkbox
                :model-value="isAllowed(color)"
                @change="(checked: boolean | string | number) => toggleColor(color, Boolean(checked))"
              />
              <span class="chip" :style="chipStyle(color)">{{ colorLabel(color) }}</span>
            </label>
          </div>
        </div>

        <div class="field-grid single">
          <el-form-item :label="t('defaultPlateColor')">
            <el-select v-model="defaultPlateColor" class="field">
              <el-option
                v-for="option in defaultColorOptions"
                :key="option.value"
                :value="option.value"
                :label="option.label"
                :disabled="option.disabled"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-footer">
          <div class="footer-meta">
            <span v-if="updatedAt" class="meta">
              {{ t('lastUpdated') }}: {{ formatTime(updatedAt) }}
            </span>
          </div>
          <el-button type="primary" :loading="saving" native-type="submit">
            {{ saving ? t('saving') : t('save') }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </section>
</template>

<style scoped>
.settings-page {
  animation: fade-up 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.panel {
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-soft);
}

.settings-form {
  max-width: 860px;
}

.scope-alert {
  margin-bottom: 14px;
}

.group-title {
  margin: 6px 0 2px;
  font-size: 1rem;
}

.group-hint {
  margin: 0 0 14px;
  font-size: 0.8rem;
  color: var(--fp-muted);
  line-height: 1.6;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.field-grid.single {
  grid-template-columns: minmax(0, 1fr);
  max-width: 420px;
}

.field {
  width: 100%;
}

.colors-block {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
}

.field-label {
  font-size: 0.85rem;
  color: var(--fp-text);
  font-weight: 500;
}

.color-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
}

.color-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.color-option.disabled {
  opacity: 0.65;
}

.chip {
  display: inline-block;
  min-width: 56px;
  text-align: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  border: 1px solid rgba(0, 0, 0, 0.08);
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px dashed var(--el-border-color-light, #e4e7ed);
}

.footer-meta {
  display: flex;
  align-items: center;
}

.meta {
  font-size: 0.8rem;
  color: var(--fp-muted);
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

@media (max-width: 640px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
