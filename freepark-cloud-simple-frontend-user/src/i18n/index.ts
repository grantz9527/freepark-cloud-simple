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
  'brand': '让开源改变生活',
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
  'sum.selectNote': '可勾选要缴纳的停车记录，未勾选的仍会保留欠费',
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
  'list.selectAll': '全选',
  'list.unselectAll': '取消全选',
  'tag.ongoing': '在场',
  'tag.unpaid': '未结',

  /* 缴费 */
  'sheet.title': '选择缴费方式',
  'sheet.body': '请选择系统已开放的缴费方式完成支付。',
  'sheet.ok': '我知道了',
  'pay.title': '确认缴费',
  'pay.hint': '将结清当前查询到的全部待缴费用',
  'pay.hintSelected': '将缴纳您勾选的停车记录',
  'pay.needSelect': '请先勾选要缴纳的停车记录',
  'pay.items': '本次缴费流水',
  'pay.choose': '缴费方式',
  'pay.submit': '去支付',
  'pay.paying': '处理中…',
  'pay.cancel': '取消',
  'pay.none': '当前未开放线上缴费，请到车场出口或联系现场人员结算。',
  'pay.mockNote': '支付渠道联调中：点确认即可完成本次入账（不会真实扣款）。',
  'pay.mockConfirm': '确认已支付',
  'pay.needGateway': '在线支付渠道尚未接入，请稍后再试或到现场缴费。',
  'pay.failed': '缴费失败，请稍后重试',
  'pay.success': '缴费成功',
  'pay.method.WECHAT_PAY': '微信支付',
  'pay.method.ALIPAY_PAY': '支付宝',
  'pay.env.wechat': '当前在微信中打开，请使用微信支付',
  'pay.env.alipay': '当前在支付宝中打开，请使用支付宝支付',
  'pay.env.locked': '当前环境不可用',
  'pay.env.noMatch': '当前扫码环境无法使用已开放的缴费方式，请换用对应 App 扫码，或到现场缴费。',
  'pay.env.launchWechat': '将在微信中打开该车牌的查询缴费页',
  'pay.env.launchAlipay': '将在支付宝中打开该车牌的查询缴费页',
  'pay.wechat.scanPay': '请使用微信扫码付款',
  'pay.wechat.scanPayHint': '打开微信「扫一扫」，扫描车场缴费码后完成支付',
  'pay.wechat.cancelled': '已取消微信支付',
  'pay.wechat.invokeFailed': '未能调起微信支付，请重试或换一台手机',
  'pay.wechat.openInWechat': '请用微信打开付款',
  'pay.wechat.needAppId': '未配置微信公众号 AppID，请先在管理端「微信配置」填写',
  'pay.wechat.needHttps': '微信授权要求 HTTPS。请用已备案的 https 用户端域名在微信中打开；公众号后台「网页授权域名」只填域名，不要带 http:// 或 https://',
  'pay.alipay.openInAlipay': '请用支付宝打开付款',
  'pay.alipay.guideTitle': '请在支付宝中打开缴费页',
  'pay.alipay.guideBody': '用支付宝扫码即可打开该车牌的查询缴费页。手机已安装支付宝时，点下方按钮会直接唤起支付宝打开。',
  'pay.alipay.scan': '请使用支付宝扫一扫打开该车牌缴费页',
  'pay.alipay.openApp': '在支付宝中打开',
  'pay.alipay.copyLink': '复制链接',
  'pay.alipay.copied': '已复制',
  'pay.alipay.invokeFailed': '无法打开支付宝收银台，请检查支付宝配置后重试',

  /* 支付结果轮询页 */
  'pay.wait.title': '支付结果',
  'pay.wait.pending': '正在确认支付结果',
  'pay.wait.pendingNote': '请在支付完成后稍候，本页会自动更新状态。',
  'pay.wait.paid': '支付成功',
  'pay.wait.paidNote': '费用已入账，可返回查看最新待缴。',
  'pay.wait.closed': '支付未完成',
  'pay.wait.closedNote': '订单已关闭或已取消，可返回重新缴费。',
  'pay.wait.failed': '支付失败！',
  'pay.wait.failedNote': '超过 15 秒未确认支付成功，请返回后重试。',
  'pay.wait.payNo': '缴款单号',
  'pay.wait.items': '对应停车流水',
  'pay.wait.entry': '入场',
  'pay.wait.sessionOpen': '在场',
  'pay.wait.sessionClosed': '已出场',
  'pay.wait.back': '返回查费',
  'pay.wait.abort': '放弃支付',
  'pay.wait.retry': '重新查询',
  'pay.wait.loadFailed': '无法读取支付单，请返回后重试。',

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
  'brand': 'Let open source change life',
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
  'sum.selectNote': 'Select the parking records to pay; unchecked ones stay unpaid',
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
  'list.selectAll': 'Select all',
  'list.unselectAll': 'Clear all',
  'tag.ongoing': 'NOW',
  'tag.unpaid': 'UNPAID',

  'sheet.title': 'Choose a payment method',
  'sheet.body': 'Pay with a method enabled in site settings.',
  'sheet.ok': 'Got it',
  'pay.title': 'Pay parking fee',
  'pay.hint': 'This will settle all unpaid fees for the current query',
  'pay.hintSelected': 'This will pay the records you selected',
  'pay.needSelect': 'Select the parking records to pay first',
  'pay.items': 'Sessions in this payment',
  'pay.choose': 'Payment method',
  'pay.submit': 'Pay',
  'pay.paying': 'Processing…',
  'pay.cancel': 'Cancel',
  'pay.none': 'Online payment is not enabled. Please pay at the exit or ask the lot staff.',
  'pay.mockNote': 'Gateway sandbox: confirm to settle this bill (no real charge).',
  'pay.mockConfirm': 'Confirm payment',
  'pay.needGateway': 'Online payment is not connected yet. Try later or pay on site.',
  'pay.failed': 'Payment failed, please try again',
  'pay.success': 'Paid',
  'pay.method.WECHAT_PAY': 'WeChat Pay',
  'pay.method.ALIPAY_PAY': 'Alipay',
  'pay.env.wechat': 'Opened in WeChat — use WeChat Pay',
  'pay.env.alipay': 'Opened in Alipay — use Alipay',
  'pay.env.locked': 'Not available here',
  'pay.env.noMatch': 'None of the enabled methods work in this app. Scan with WeChat or Alipay, or pay on site.',
  'pay.env.launchWechat': 'Opens this plate’s fee page in WeChat',
  'pay.env.launchAlipay': 'Opens this plate’s fee page in Alipay',
  'pay.wechat.scanPay': 'Please pay by scanning with WeChat',
  'pay.wechat.scanPayHint': 'Open WeChat Scan and use the lot payment QR code to finish paying',
  'pay.wechat.cancelled': 'WeChat Pay was cancelled',
  'pay.wechat.invokeFailed': 'Could not open WeChat Pay. Retry or try another phone.',
  'pay.wechat.openInWechat': 'Open in WeChat to pay',
  'pay.wechat.needAppId': 'WeChat Official Account AppID is missing. Configure it in Admin → WeChat.',
  'pay.wechat.needHttps': 'WeChat OAuth requires HTTPS. Open the public https user site in WeChat. In the Official Account console, set the OAuth callback domain without http:// or https://',
  'pay.alipay.openInAlipay': 'Open in Alipay to pay',
  'pay.alipay.guideTitle': 'Open the fee page in Alipay',
  'pay.alipay.guideBody': 'Scan with Alipay to open this plate’s query and payment page. On a phone with Alipay installed, the button below opens it in Alipay directly.',
  'pay.alipay.scan': 'Scan with Alipay to open this plate’s payment page',
  'pay.alipay.openApp': 'Open in Alipay',
  'pay.alipay.copyLink': 'Copy link',
  'pay.alipay.copied': 'Copied',
  'pay.alipay.invokeFailed': 'Could not open the Alipay checkout. Check Alipay settings and try again.',

  'pay.wait.title': 'Payment status',
  'pay.wait.pending': 'Confirming payment',
  'pay.wait.pendingNote': 'Finish paying in the wallet app. This page updates automatically.',
  'pay.wait.paid': 'Paid',
  'pay.wait.paidNote': 'The fee has been recorded. You can go back to the latest balance.',
  'pay.wait.closed': 'Payment not completed',
  'pay.wait.closedNote': 'The order was closed or cancelled. Go back to pay again.',
  'pay.wait.failed': 'Payment failed!',
  'pay.wait.failedNote': 'No successful payment within 15 seconds. Go back and try again.',
  'pay.wait.payNo': 'Payment no.',
  'pay.wait.items': 'Parking sessions',
  'pay.wait.entry': 'Entry',
  'pay.wait.sessionOpen': 'On site',
  'pay.wait.sessionClosed': 'Exited',
  'pay.wait.back': 'Back to fee check',
  'pay.wait.abort': 'Cancel payment',
  'pay.wait.retry': 'Retry',
  'pay.wait.loadFailed': 'Could not load this payment. Go back and try again.',

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
