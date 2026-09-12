<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { currencyNameOf, currencyOptionLabel } from '../utils/currency'
import { paymentMethodNameOf } from '../utils/payment'
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
  site: { 'zh-CN': '公网地址', en: 'Public URLs' },
  siteHint: {
    'zh-CN': '后台与用户端可分开部署。填 http:// 或 https:// 地址即可，可带部署子路径（如 /fangzhi、/freepark-user）。查询参数、# 和尾斜杠会自动去掉。微信要求支付回调为公网 HTTPS。',
    en: 'Admin API and user site may be hosted separately. Use http:// or https://, optional subpath (e.g. /fangzhi, /freepark-user). Query, hash and trailing slash are stripped on save. WeChat requires a public HTTPS notify URL.'
  },
  adminBaseUrl: { 'zh-CN': '后台基础地址', en: 'Admin base URL' },
  adminBaseUrlPlaceholder: {
    'zh-CN': '例如 https://cloud.example.com 或 https://cloud.example.com/fangzhi',
    en: 'e.g. https://cloud.example.com or https://cloud.example.com/fangzhi'
  },
  adminBaseUrlHint: {
    'zh-CN': '用于拼接微信 / 支付宝支付异步回调。可含网关前缀。留空则按当前访问 Host 推断。',
    en: 'Used to build WeChat / Alipay payment notify URLs. May include a gateway prefix. Leave blank to infer from the current request host.'
  },
  userBaseUrl: { 'zh-CN': '用户端基础地址', en: 'User base URL' },
  userBaseUrlPlaceholder: {
    'zh-CN': '例如 https://pay.example.com 或 https://pay.example.com/freepark-user',
    en: 'e.g. https://pay.example.com or https://pay.example.com/freepark-user'
  },
  userBaseUrlHint: {
    'zh-CN': '用于用户端首页、缴费结果页与支付同步跳回。可含前端部署子路径。留空则回落后台基础地址。',
    en: 'Used for the user home page, payment result page, and channel return URL. May include the frontend subpath. Leave blank to fall back to the admin base URL.'
  },
  wechatNotifyPreview: { 'zh-CN': '微信支付回调', en: 'WeChat Pay notify' },
  alipayNotifyPreview: { 'zh-CN': '支付宝支付回调', en: 'Alipay notify' },
  userSitePreview: { 'zh-CN': '用户端首页', en: 'User home' },
  userPayPreview: { 'zh-CN': '用户端缴费页', en: 'User payment page' },
  plateRegion: { 'zh-CN': '车牌版式（区域）', en: 'Plate style (region)' },
  plateRegionHint: {
    'zh-CN': '选择该站点车牌的默认格式，用户端查询页将按此区域渲染车牌输入交互。',
    en: 'Pick the default plate format of this site. The public search page renders its plate input UI based on this region.'
  },
  regionCN: { 'zh-CN': '中国大陆', en: 'Mainland China' },
  regionHK: { 'zh-CN': '中国香港', en: 'Hong Kong, China' },
  regionMO: { 'zh-CN': '中国澳门', en: 'Macao, China' },
  regionTW: { 'zh-CN': '中国台湾', en: 'Taiwan, China' },
  regionEU: { 'zh-CN': '欧盟', en: 'EU' },
  regionGB: { 'zh-CN': '英国', en: 'United Kingdom' },
  regionUS: { 'zh-CN': '美国', en: 'United States' },
  regionJP: { 'zh-CN': '日本', en: 'Japan' },
  regionKR: { 'zh-CN': '韩国', en: 'Korea' },
  regionSG: { 'zh-CN': '新加坡', en: 'Singapore' },
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
  feeCurrency: { 'zh-CN': '收费金额单位', en: 'Fee Currency' },
  feeCurrencyHint: {
    'zh-CN': '站点计费金额的录入与展示单位。勾选允许使用的币种并指定默认币种；默认币种必须是允许币种之一。',
    en: 'The unit used when entering and showing billing amounts. Check the allowed currencies and set the default one; the default must be within the allowed list.'
  },
  allowedCurrencies: { 'zh-CN': '允许的币种', en: 'Allowed currencies' },
  defaultCurrency: { 'zh-CN': '默认币种', en: 'Default currency' },
  atLeastOneCurrency: {
    'zh-CN': '至少保留一种允许的币种',
    en: 'Keep at least one allowed currency'
  },
  paymentMethods: { 'zh-CN': '收费方式', en: 'Payment Methods' },
  paymentMethodsHint: {
    'zh-CN': '勾选站点允许使用的缴费方式。至少保留一种；启用后可在用户端缴费流程中展示。',
    en: 'Check the payment methods this site supports. Keep at least one; enabled methods are shown in the public payment flow.'
  },
  allowedPaymentMethods: { 'zh-CN': '允许的收费方式', en: 'Allowed payment methods' },
  atLeastOnePaymentMethod: {
    'zh-CN': '至少保留一种收费方式',
    en: 'Keep at least one payment method'
  },
  userPayMode: { 'zh-CN': '用户端缴费范围', en: 'User payment scope' },
  userPayModeHint: {
    'zh-CN': '强制全部支付：车主必须一次缴清该车牌当前全部欠费。允许选择指定订单：车主可勾选要缴的停车记录。',
    en: 'Pay all: the driver must settle every unpaid record for the plate. Select records: the driver can choose which sessions to pay.'
  },
  userPayForceAll: { 'zh-CN': '强制全部支付', en: 'Pay all records' },
  userPaySelectable: { 'zh-CN': '允许选择指定订单', en: 'Allow selecting records' },
  atLeastOne: { 'zh-CN': '至少保留一种允许的车牌颜色', en: 'Keep at least one allowed plate color' },
  saved: { 'zh-CN': '配置已保存', en: 'Settings saved' },
  invalidSiteBaseUrl: {
    'zh-CN': '后台/用户端基础地址须为 http:// 或 https:// 地址，可带 /fangzhi、/freepark-user 这类子路径',
    en: 'Admin / user base URL must be http:// or https://, and may include a subpath such as /fangzhi'
  },
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
  adminBaseUrl: string
  userBaseUrl: string
  plateRegion: string
  defaultPlateColor: string
  allowedPlateColors: string[]
  defaultCurrency: string
  allowedCurrencies: string[]
  allowedPaymentMethods: string[]
  forcePayAll: boolean
  supportedLocales: string[]
  supportedTimezones: string[]
  supportedPlateRegions: string[]
  supportedPlateColors: string[]
  supportedCurrencies: string[]
  supportedPaymentMethods: string[]
  updatedAt: string
}

const loading = ref(true)
const saving = ref(false)
const data = ref<SystemSettingsData | null>(null)

const defaultLocale = ref('zh-CN')
const timezone = ref('Asia/Shanghai')
const adminBaseUrl = ref('')
const userBaseUrl = ref('')
const plateRegion = ref('CN')
const defaultPlateColor = ref('BLUE')
const allowedPlateColors = ref<string[]>([])
const defaultCurrency = ref('CNY')
const allowedCurrencies = ref<string[]>([])
const allowedPaymentMethods = ref<string[]>([])
const forcePayAll = ref(true)
const supportedLocales = ref<string[]>([])
const supportedTimezones = ref<string[]>([])
const supportedPlateRegions = ref<string[]>([])
const supportedPlateColors = ref<string[]>([])
const supportedCurrencies = ref<string[]>([])
const supportedPaymentMethods = ref<string[]>([])
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

/** 车牌版式（区域）选项 */
const regionOptions = computed(() =>
  supportedPlateRegions.value.map((code) => ({
    value: code,
    label: regionLabel(code)
  }))
)

function regionLabel(code: string): string {
  const key = `region${code}`
  const dict = d[key]
  if (!dict) return code
  return locale.value === 'en' ? dict.en : dict['zh-CN']
}

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

/** 默认币种下拉展示全部支持币种，但只能选择“已允许”的币种 */
const defaultCurrencyOptions = computed(() =>
  supportedCurrencies.value.map((code) => ({
    value: code,
    label: currencyOptionLabel(code, locale.value),
    disabled: !allowedCurrencies.value.includes(code)
  }))
)

watch(allowedCurrencies, (codes) => {
  if (codes.length > 0 && !codes.includes(defaultCurrency.value)) {
    defaultCurrency.value = codes[0]
  }
})

function normalizedSiteBase(value: string): string {
  const raw = value.trim().replace(/\s+/g, '')
  if (!raw) return ''
  try {
    const url = new URL(raw.includes('://') ? raw : `https://${raw}`)
    if (url.protocol !== 'http:' && url.protocol !== 'https:') return ''
    const path = url.pathname.replace(/\/+$/, '')
    const suffix = !path || path === '/' ? '' : path
    return `${url.protocol}//${url.host}${suffix}`
  } catch {
    return ''
  }
}

const wechatNotifyPreview = computed(() => {
  const base = normalizedSiteBase(adminBaseUrl.value)
  return base ? `${base}/api/public/payment/wechat/notify` : ''
})

const alipayNotifyPreview = computed(() => {
  const base = normalizedSiteBase(adminBaseUrl.value)
  return base ? `${base}/api/public/payment/alipay/notify` : ''
})

const effectiveUserBase = computed(() => {
  return normalizedSiteBase(userBaseUrl.value) || normalizedSiteBase(adminBaseUrl.value)
})

const userSitePreview = computed(() => {
  const base = effectiveUserBase.value
  return base ? `${base}/` : ''
})

const userPayPreview = computed(() => {
  const base = effectiveUserBase.value
  return base ? `${base}/pay/{payNo}` : ''
})

const hasAdminPreview = computed(() => Boolean(wechatNotifyPreview.value || alipayNotifyPreview.value))
const hasUserPreview = computed(() => Boolean(userSitePreview.value || userPayPreview.value))

function currencyName(code: string): string {
  return currencyNameOf(code, locale.value)
}

function isAllowedCurrency(code: string): boolean {
  return allowedCurrencies.value.includes(code)
}

function toggleCurrency(code: string, checked: boolean): void {
  if (checked) {
    if (!allowedCurrencies.value.includes(code)) {
      allowedCurrencies.value = [...allowedCurrencies.value, code]
    }
    return
  }
  if (allowedCurrencies.value.length <= 1) {
    ElMessage.warning(t('atLeastOneCurrency'))
    return
  }
  allowedCurrencies.value = allowedCurrencies.value.filter((item) => item !== code)
}

function isAllowedPaymentMethod(code: string): boolean {
  return allowedPaymentMethods.value.includes(code)
}

function togglePaymentMethod(code: string, checked: boolean): void {
  if (checked) {
    if (!allowedPaymentMethods.value.includes(code)) {
      allowedPaymentMethods.value = [...allowedPaymentMethods.value, code]
    }
    return
  }
  if (allowedPaymentMethods.value.length <= 1) {
    ElMessage.warning(t('atLeastOnePaymentMethod'))
    return
  }
  allowedPaymentMethods.value = allowedPaymentMethods.value.filter((item) => item !== code)
}

function paymentMethodName(code: string): string {
  return paymentMethodNameOf(code, locale.value)
}

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
    adminBaseUrl.value = view.adminBaseUrl ?? ''
    userBaseUrl.value = view.userBaseUrl ?? ''
    plateRegion.value = view.plateRegion ?? 'CN'
    defaultPlateColor.value = view.defaultPlateColor
    allowedPlateColors.value = [...view.allowedPlateColors]
    defaultCurrency.value = view.defaultCurrency
    allowedCurrencies.value = [...view.allowedCurrencies]
    allowedPaymentMethods.value = [...view.allowedPaymentMethods]
    forcePayAll.value = view.forcePayAll !== false
    supportedLocales.value = [...view.supportedLocales]
    supportedTimezones.value = [...view.supportedTimezones]
    supportedPlateRegions.value = [...view.supportedPlateRegions]
    supportedPlateColors.value = [...view.supportedPlateColors]
    supportedCurrencies.value = [...view.supportedCurrencies]
    supportedPaymentMethods.value = [...view.supportedPaymentMethods]
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
  if (allowedCurrencies.value.length === 0) {
    ElMessage.warning(t('atLeastOneCurrency'))
    return
  }
  if (allowedPaymentMethods.value.length === 0) {
    ElMessage.warning(t('atLeastOnePaymentMethod'))
    return
  }
  const admin = adminBaseUrl.value.trim()
    ? normalizedSiteBase(adminBaseUrl.value)
    : ''
  const user = userBaseUrl.value.trim()
    ? normalizedSiteBase(userBaseUrl.value)
    : ''
  if (adminBaseUrl.value.trim() && !admin) {
    ElMessage.warning(t('invalidSiteBaseUrl'))
    return
  }
  if (userBaseUrl.value.trim() && !user) {
    ElMessage.warning(t('invalidSiteBaseUrl'))
    return
  }
  saving.value = true
  try {
    const view = await request.put<never, SystemSettingsData>('/system/settings', {
      defaultLocale: defaultLocale.value,
      timezone: timezone.value,
      adminBaseUrl: admin,
      userBaseUrl: user,
      plateRegion: plateRegion.value,
      defaultPlateColor: defaultPlateColor.value,
      allowedPlateColors: allowedPlateColors.value,
      defaultCurrency: defaultCurrency.value,
      allowedCurrencies: allowedCurrencies.value,
      allowedPaymentMethods: allowedPaymentMethods.value,
      forcePayAll: forcePayAll.value
    })
    defaultLocale.value = view.defaultLocale
    timezone.value = view.timezone
    adminBaseUrl.value = view.adminBaseUrl ?? ''
    userBaseUrl.value = view.userBaseUrl ?? ''
    plateRegion.value = view.plateRegion ?? 'CN'
    defaultPlateColor.value = view.defaultPlateColor
    allowedPlateColors.value = [...view.allowedPlateColors]
    defaultCurrency.value = view.defaultCurrency
    allowedCurrencies.value = [...view.allowedCurrencies]
    allowedPaymentMethods.value = [...view.allowedPaymentMethods]
    forcePayAll.value = view.forcePayAll !== false
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

        <div class="field-grid single">
          <el-form-item :label="t('plateRegion')">
            <el-select v-model="plateRegion" class="field">
              <el-option
                v-for="option in regionOptions"
                :key="option.value"
                :value="option.value"
                :label="option.label"
              />
            </el-select>
          </el-form-item>
        </div>
        <p class="group-hint">{{ t('plateRegionHint') }}</p>

        <el-divider />

        <h3 class="group-title">{{ t('site') }}</h3>
        <p class="group-hint">{{ t('siteHint') }}</p>
        <el-form-item :label="t('adminBaseUrl')">
          <el-input
            v-model="adminBaseUrl"
            :placeholder="t('adminBaseUrlPlaceholder')"
            maxlength="255"
            clearable
            class="field"
          />
          <div class="field-hint">{{ t('adminBaseUrlHint') }}</div>
        </el-form-item>
        <div v-if="hasAdminPreview" class="notify-preview">
          <div class="preview-row">
            <span class="preview-label">{{ t('wechatNotifyPreview') }}</span>
            <code class="preview-url">{{ wechatNotifyPreview }}</code>
          </div>
          <div class="preview-row">
            <span class="preview-label">{{ t('alipayNotifyPreview') }}</span>
            <code class="preview-url">{{ alipayNotifyPreview }}</code>
          </div>
        </div>

        <el-form-item :label="t('userBaseUrl')">
          <el-input
            v-model="userBaseUrl"
            :placeholder="t('userBaseUrlPlaceholder')"
            maxlength="255"
            clearable
            class="field"
          />
          <div class="field-hint">{{ t('userBaseUrlHint') }}</div>
        </el-form-item>
        <div v-if="hasUserPreview" class="notify-preview">
          <div class="preview-row">
            <span class="preview-label">{{ t('userSitePreview') }}</span>
            <code class="preview-url">{{ userSitePreview }}</code>
          </div>
          <div class="preview-row">
            <span class="preview-label">{{ t('userPayPreview') }}</span>
            <code class="preview-url">{{ userPayPreview }}</code>
          </div>
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

        <el-divider />

        <h3 class="group-title">{{ t('feeCurrency') }}</h3>
        <p class="group-hint">{{ t('feeCurrencyHint') }}</p>

        <div class="colors-block">
          <span class="field-label">{{ t('allowedCurrencies') }}</span>
          <div class="currency-grid">
            <label
              v-for="code in supportedCurrencies"
              :key="code"
              class="currency-option"
              :class="{ disabled: isAllowedCurrency(code) && allowedCurrencies.length === 1 }"
            >
              <el-checkbox
                :model-value="isAllowedCurrency(code)"
                @change="(checked: boolean | string | number) => toggleCurrency(code, Boolean(checked))"
              />
              <span class="chip currency-chip">{{ code }} · {{ currencyName(code) }}</span>
            </label>
          </div>
        </div>

        <div class="field-grid single">
          <el-form-item :label="t('defaultCurrency')">
            <el-select v-model="defaultCurrency" class="field">
              <el-option
                v-for="option in defaultCurrencyOptions"
                :key="option.value"
                :value="option.value"
                :label="option.label"
                :disabled="option.disabled"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-divider />

        <h3 class="group-title">{{ t('paymentMethods') }}</h3>
        <p class="group-hint">{{ t('paymentMethodsHint') }}</p>

        <div class="colors-block">
          <span class="field-label">{{ t('allowedPaymentMethods') }}</span>
          <div class="currency-grid">
            <label
              v-for="code in supportedPaymentMethods"
              :key="code"
              class="currency-option"
              :class="{ disabled: isAllowedPaymentMethod(code) && allowedPaymentMethods.length === 1 }"
            >
              <el-checkbox
                :model-value="isAllowedPaymentMethod(code)"
                @change="(checked: boolean | string | number) => togglePaymentMethod(code, Boolean(checked))"
              />
              <span class="chip currency-chip payment-chip">{{ paymentMethodName(code) }}</span>
            </label>
          </div>
        </div>

        <el-form-item :label="t('userPayMode')" class="pay-mode-item">
          <el-radio-group v-model="forcePayAll">
            <el-radio :value="true">{{ t('userPayForceAll') }}</el-radio>
            <el-radio :value="false">{{ t('userPaySelectable') }}</el-radio>
          </el-radio-group>
          <div class="field-hint">{{ t('userPayModeHint') }}</div>
        </el-form-item>

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

.field-hint {
  margin-top: 6px;
  font-size: 0.78rem;
  line-height: 1.5;
  color: var(--fp-muted);
}

.pay-mode-item :deep(.el-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.notify-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: -4px 0 8px;
  padding: 10px 12px;
  border: 1px dashed var(--fp-line, #dcdfe6);
  border-radius: var(--fp-radius, 6px);
  background: var(--fp-surface-soft, rgba(0, 0, 0, 0.02));
}

.preview-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
}

.preview-label {
  flex: 0 0 auto;
  font-size: 0.78rem;
  color: var(--fp-muted);
}

.preview-url {
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.78rem;
  word-break: break-all;
  color: var(--fp-text, #2f3640);
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

.currency-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
}

.color-option,
.currency-option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.color-option.disabled,
.currency-option.disabled {
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

.currency-chip {
  background: var(--fp-bg-soft, #f2f3f5);
  border-color: var(--el-border-color, #dcdfe6);
  font-weight: 600;
  color: var(--fp-text, #2f3640);
  min-width: 72px;
  white-space: nowrap;
}

.payment-chip {
  background: rgba(7, 193, 96, 0.12);
  border-color: rgba(7, 193, 96, 0.35);
  color: #07c160;
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
