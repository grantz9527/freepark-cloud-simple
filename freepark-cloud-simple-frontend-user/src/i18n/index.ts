/**
 * freepark 用户端轻量 i18n：
 * - 无第三方依赖，纯字典 + 响应式语言状态（切换后全页文案即时刷新）；
 * - 语言选择持久化在 localStorage；云端站点 defaultLocale 仅用于首次访问的初始语言。
 */
import { computed, ref, watch } from 'vue'

export type Locale = 'zh' | 'en'

const STORE_KEY = 'freepark.user.locale.v1'

function storedLocale(): Locale | null {
  try {
    const v = localStorage.getItem(STORE_KEY)
    return v === 'en' || v === 'zh' ? v : null
  } catch {
    return null
  }
}

export const activeLocale = ref<Locale>(storedLocale() ?? 'zh')
watch(activeLocale, (v) => {
  try {
    localStorage.setItem(STORE_KEY, v)
  } catch {
    // 隐私模式写失败可忽略
  }
})

export function setLocale(v: Locale) {
  activeLocale.value = v
}

/** 仅当用户从未手动选择过语言时生效（用于站点 defaultLocale 兜底） */
export function setLocaleIfUnset(v: Locale | null) {
  if (storedLocale() === null && v) activeLocale.value = v
}

export function toggleLocale() {
  activeLocale.value = activeLocale.value === 'zh' ? 'en' : 'zh'
}

/** 顶部按钮展示：当前是中文则显示 EN（点按切英文），反之显示「中文」 */
export const localeButtonText = computed(() => (activeLocale.value === 'zh' ? 'EN' : '中文'))

type Dict = Record<string, string>

const zh: Dict = {
  /* 顶部导航 */
  'brand': '停车缴费',
  'act.back': '返回',

  /* Hero */
  'hero.pill': '无感 · 免登录 · 实时估算',
  'hero.title': '停车缴费',
  'hero.sub.cn': '输入车牌号，在停与历史费用一目了然',
  'hero.sub.region': '本站车牌为「{region}」版式，直接输入车牌查询费用',

  /* 车牌字段 / 最近查询 */
  'field.plate': '车牌号',
  'recent.title': '最近查询',
  'recent.clear': '清空记录',
  'recent.remove': '移除记录',

  /* 车牌颜色 */
  'color.title': '车牌颜色',
  'color.auto': '自动',
  'color.all': '全部',
  'color.autoHint': '自动识别 · {label}',
  'color.regionHint': '地区默认 · {label}',
  'color.hint': '同号不同色的车牌请按需选择，避免金额混淆',
  'color.multi': '检测到该车牌存在多种颜色，可按需切换：',
  'color.querying': '正在查询「{label}」，',
  'color.noRecords': '暂无记录 · 另存在 {list}',
  'color.count': '共 {n} 条记录',

  /* 按钮与通用提示 */
  'btn.query': '立即查询费用',
  'btn.querying': '查询中…',
  'btn.pay': '立即缴费',
  'btn.other': '查询其他车牌',
  'err.needPlate': '请输入车牌号',
  'err.format': '车牌号格式不正确，请重新输入',
  'err.chars': '请仅输入车牌上的字母与数字',
  'err.failed': '查询失败，请稍后重试',

  /* 信任区 */
  'trust.noLogin': '免登录',
  'trust.noLoginSub': '即查即走',
  'trust.all': '全场',
  'trust.allSub': '停车记录',
  'trust.live': '实时',
  'trust.liveSub': '费用估算',

  /* 结果页：状态与汇总 */
  'state.due': '有费用待缴',
  'state.free': '免费停放中',
  'state.none': '暂无待缴',
  'state.clear': '已结清',
  'sum.due': '待缴合计',
  'sum.free': '当前车辆状态',
  'sum.dueNote': '在场按当前时刻估算，出场结算时以实际为准',
  'sum.freeNote': '正在免费时段停放中，暂无需缴纳费用',
  'sum.noRecords': '未查询到该车牌的费用记录，可放心通行',
  'sum.clearNote': '该车牌的在停与历史费用均已结清，暂无待缴金额',

  /* 结果页：明细 */
  'list.ongoing': '正在停放',
  'list.settled': '历史未结',
  'list.unknown': '未知车场',
  'meta.ongoing': '入场 {t} · 已停 {d}',
  'meta.period': '{a} 至 {b} · 停车 {d}',
  'list.est': '预估',
  'list.free': '免费',
  'tag.ongoing': '在场',
  'tag.unpaid': '未结',

  /* 缴费说明 */
  'sheet.title': '线上缴费即将上线',
  'sheet.body': '当前您可以凭本页面金额到所在车场出口缴费，或联系车场工作人员完成结算，感谢理解。',
  'sheet.ok': '我知道了',

  /* 车牌颜色名 */
  'color.BLUE': '蓝牌',
  'color.YELLOW': '黄牌',
  'color.GREEN': '绿牌',
  'color.YELLOW_GREEN': '黄绿牌',
  'color.BLACK': '黑牌',
  'color.WHITE': '白牌',
  'color.OTHER': '其他',

  /* 非 CN 区域名（展示用） */
  'region.HK': '香港',
  'region.MO': '澳门',
  'region.TW': '台湾',
  'region.EU': '欧盟',
  'region.GB': '英国',
  'region.US': '美国',
  'region.JP': '日本',
  'region.KR': '韩国',
  'region.SG': '新加坡',

  /* PlateInput 键盘 */
  'pi.busy': '查询中，暂不可修改…',
  'pi.flag.new': '新能源',
  'pi.flag.cn': '中国',
  'pi.pos.provAria': '省份位，点此选择省份',
  'pi.pos.aria': '第 {n} 位{act}',
  'pi.pos.modify': '，修改',
  'pi.pos.type': '，输入',
  'pi.kbd.province': '请选择省份简称',
  'pi.kbd.region2': '第 2 位：请输入地区字母',
  'pi.kbd.pos': '第 {n} 位：字母或数字',
  'pi.kbd.generic': '{region}车牌 · {kind}（{code}）',
  'pi.kbd.counter': '已输入 {a}/{b} 位',
  'pi.kbd.done': '完成',
  'pi.kbd.del': '删除',
  'pi.kbd.aria': '车牌输入键盘',
  'pi.type.free': '自由',
  'pi.type.L': '字母',
  'pi.type.D': '数字',
  'pi.type.any': '字母或数字',
  'pi.tip.free': '字母与数字均可，最多 {n} 位'
}

const en: Dict = {
  'brand': 'Freepark Parking',
  'act.back': 'Back',

  'hero.pill': 'Instant · No login · Fee check',
  'hero.title': 'Parking Fee',
  'hero.sub.cn': 'Enter the plate to see live and past parking fees',
  'hero.sub.region': 'Plate format: {region}. Enter the plate to check the fee',

  'field.plate': 'Plate number',
  'recent.title': 'Recent',
  'recent.clear': 'Clear',
  'recent.remove': 'Remove',

  'color.title': 'Plate color',
  'color.auto': 'Auto',
  'color.all': 'All',
  'color.autoHint': 'Auto · {label}',
  'color.regionHint': 'Default · {label}',
  'color.hint': 'Plates with the same number may differ in color — pick one to avoid confusion',
  'color.multi': 'This plate number has several colors:',
  'color.querying': 'Viewing {label} plates,',
  'color.noRecords': 'no records · also exists as {list}',
  'color.count': '{n} record(s) in total',

  'btn.query': 'Check fee',
  'btn.querying': 'Loading…',
  'btn.pay': 'Pay now',
  'btn.other': 'Check another plate',
  'err.needPlate': 'Please enter a plate number',
  'err.format': 'Plate number format is invalid, please re-enter',
  'err.chars': 'Letters and digits only',
  'err.failed': 'Check failed, please try again later',

  'trust.noLogin': 'No login',
  'trust.noLoginSub': 'check & go',
  'trust.all': 'All',
  'trust.allSub': 'parking records',
  'trust.live': 'Live',
  'trust.liveSub': 'fee estimate',

  'state.due': 'Unpaid balance',
  'state.free': 'Free parking now',
  'state.none': 'Nothing due',
  'state.clear': 'Settled',
  'sum.due': 'Total due',
  'sum.free': 'Status',
  'sum.dueNote': 'Live session estimated now; final charge applies on exit',
  'sum.freeNote': 'Currently in the free parking period, nothing to pay',
  'sum.noRecords': 'No fee records found for this plate',
  'sum.clearNote': 'All sessions are settled, nothing due',

  'list.ongoing': 'Parking now',
  'list.settled': 'Past unpaid',
  'list.unknown': 'Unknown lot',
  'meta.ongoing': 'In {t} · parked {d}',
  'meta.period': '{a} to {b} · parked {d}',
  'list.est': 'est.',
  'list.free': 'FREE',
  'tag.ongoing': 'NOW',
  'tag.unpaid': 'UNPAID',

  'sheet.title': 'Online payment coming soon',
  'sheet.body': 'For now, pay at the lot exit using the amount shown, or ask the lot staff to settle. Thank you.',
  'sheet.ok': 'Got it',

  'color.BLUE': 'Blue',
  'color.YELLOW': 'Yellow',
  'color.GREEN': 'Green',
  'color.YELLOW_GREEN': 'Yellow-green',
  'color.BLACK': 'Black',
  'color.WHITE': 'White',
  'color.OTHER': 'Other',

  'region.HK': 'Hong Kong',
  'region.MO': 'Macau',
  'region.TW': 'Taiwan',
  'region.EU': 'EU',
  'region.GB': 'UK',
  'region.US': 'USA',
  'region.JP': 'Japan',
  'region.KR': 'Korea',
  'region.SG': 'Singapore',

  'pi.busy': 'Checking… not editable',
  'pi.flag.new': 'NEV',
  'pi.flag.cn': 'CN',
  'pi.pos.provAria': 'Province slot — tap to choose',
  'pi.pos.aria': 'Slot {n}{act}',
  'pi.pos.modify': ', edit',
  'pi.pos.type': ', type',
  'pi.kbd.province': 'Choose a province',
  'pi.kbd.region2': 'Slot 2: enter the region letter',
  'pi.kbd.pos': 'Slot {n}: letter or digit',
  'pi.kbd.generic': '{region} plate · {kind} ({code})',
  'pi.kbd.counter': '{a}/{b} entered',
  'pi.kbd.done': 'Done',
  'pi.kbd.del': 'Delete',
  'pi.kbd.aria': 'Plate input keyboard',
  'pi.type.free': 'Free',
  'pi.type.L': 'Letter',
  'pi.type.D': 'Digit',
  'pi.type.any': 'Letter or digit',
  'pi.tip.free': 'Letters and digits, up to {n} characters'
}

const dict: Record<Locale, Dict> = { zh, en }

/** 取当前语言文案；无 key 时回退中文原文（再回退 key 本身） */
export function t(key: string): string {
  return dict[activeLocale.value][key] ?? zh[key] ?? key
}

/** 带占位符文案：{name} 会被 vars 替换 */
export function tpl(key: string, vars: Record<string, string | number>): string {
  let s = t(key)
  for (const [k, v] of Object.entries(vars)) {
    s = s.replaceAll(`{${k}}`, String(v))
  }
  return s
}

/** 车牌颜色 → 当前语言名称（code 为后端 PlateColor 枚举） */
export function tColor(code?: string | null): string {
  if (!code) return t('field.plate')
  const label = t(`color.${code}`)
  return label === `color.${code}` ? code : label
}

/** 车牌版式区域 → 当前语言名称 */
export function tRegion(code?: string | null): string {
  if (!code) return ''
  return t(`region.${code}`)
}

/** 站点 defaultLocale（如 zh-CN / en）归一为 Locale */
export function localeFromSite(defaultLocale?: string | null): Locale {
  return String(defaultLocale ?? '').toLowerCase().startsWith('en') ? 'en' : 'zh'
}
