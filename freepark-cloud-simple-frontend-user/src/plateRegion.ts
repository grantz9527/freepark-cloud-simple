/**
 * 车牌版式（区域）预设注册表 —— 与云端 system_settings.plateRegion 一一对应。
 *
 * 决定用户端查费页默认采用的车牌输入 UI：
 * - CN：中国大陆（省简称点选 + 分段输入，见 PlateInput CN 分支）；
 * - 其它区域：按当地常见版式做「分段提示」，输入与版式不匹配时自动退回自由输入。
 *
 * 版式模式串语法：
 *   L = 仅字母    D = 仅数字    N = 字母或数字
 *   分组之间可用空格 / - 作分隔展示（不进入值，仅视觉提示）。
 */
export interface PlateRegionPreset {
  /** 区域码（与云端配置一致） */
  code: string
  labelZh: string
  labelEn: string
  /** 输入提示文案 */
  hintZh: string
  /** 样例车牌（仅展示） */
  sample: string
  /** 常见版式（CN 为空，走独立分支） */
  formats: string[]
  /** 车牌观感默认底色枚举（仅用于无颜色识别结果时的展示） */
  defaultTone: string
}

export const PLATE_REGIONS: PlateRegionPreset[] = [
  {
    code: 'CN',
    labelZh: '中国大陆',
    labelEn: 'Mainland China',
    hintZh: '点选省份与后续字符，最多 8 位',
    sample: '粤B12345',
    formats: [],
    defaultTone: 'BLUE'
  },
  {
    code: 'HK',
    labelZh: '中国香港',
    labelEn: 'Hong Kong, China',
    hintZh: '示例 HK 1234，不符格式自动转自由输入',
    sample: 'HK 1234',
    formats: ['AA 9999', 'AA 999999'],
    defaultTone: 'WHITE'
  },
  {
    code: 'MO',
    labelZh: '中国澳门',
    labelEn: 'Macao, China',
    hintZh: '示例 M-12-34，不符格式自动转自由输入',
    sample: 'M-12-34',
    formats: ['A-NN-NN', 'NN NNNN'],
    defaultTone: 'WHITE'
  },
  {
    code: 'TW',
    labelZh: '中国台湾',
    labelEn: 'Taiwan, China',
    hintZh: '示例 AB-1234，不符格式自动转自由输入',
    sample: 'AB-1234',
    formats: ['AA 9999', 'AAA 9999'],
    defaultTone: 'WHITE'
  },
  {
    code: 'EU',
    labelZh: '欧盟',
    labelEn: 'EU',
    hintZh: '示例 B-AB 123，不符格式自动转自由输入',
    sample: 'B-AB 123',
    formats: ['AA-999-AA', 'AA-9999-AA', 'A-999-AAA', 'AAA-999', 'AA-999'],
    defaultTone: 'WHITE'
  },
  {
    code: 'GB',
    labelZh: '英国',
    labelEn: 'United Kingdom',
    hintZh: '示例 AB12 CDE，不符格式自动转自由输入',
    sample: 'AB12 CDE',
    formats: ['AA 99 AAA', 'AAA 999', '999 AAA'],
    defaultTone: 'WHITE'
  },
  {
    code: 'US',
    labelZh: '美国',
    labelEn: 'United States',
    hintZh: '示例 ABC 1234，不符格式自动转自由输入',
    sample: 'ABC 1234',
    formats: ['NNN NNNN', 'NNNN NNN', 'NNNNNN'],
    defaultTone: 'WHITE'
  },
  {
    code: 'JP',
    labelZh: '日本',
    labelEn: 'Japan',
    hintZh: '输入车牌上的字母与数字即可，无需输入汉字/假名',
    sample: '123 あ 45-6',
    formats: ['NNN NNN', 'NN-NNN'],
    defaultTone: 'WHITE'
  },
  {
    code: 'KR',
    labelZh: '韩国',
    labelEn: 'Korea',
    hintZh: '输入车牌上的字母与数字即可，无需输入韩文',
    sample: '12가3456',
    formats: ['NN NNNN', 'NNN NNNN'],
    defaultTone: 'WHITE'
  },
  {
    code: 'SG',
    labelZh: '新加坡',
    labelEn: 'Singapore',
    hintZh: '示例 SGB1234A，不符格式自动转自由输入',
    sample: 'SGB1234A',
    formats: ['AAA NNNN', 'AAA NNNNN'],
    defaultTone: 'WHITE'
  }
]

const REGION_MAP: Record<string, PlateRegionPreset> = Object.fromEntries(
  PLATE_REGIONS.map((preset) => [preset.code, preset])
)

/** 未知区域兜底：退化为“自由输入”的通用预设，避免渲染异常 */
const GENERIC_PRESET: PlateRegionPreset = {
  code: 'GENERIC',
  labelZh: '其它地区',
  labelEn: 'Other',
  hintZh: '请输入车牌上的字母与数字',
  sample: 'ABC-123',
  formats: [],
  defaultTone: 'WHITE'
}

export function regionPresetOf(code?: string): PlateRegionPreset {
  return (code && REGION_MAP[code]) || GENERIC_PRESET
}

/* ===== 版式模式解析与匹配 ===== */

export type CharKind = 'L' | 'D' | 'N'

export interface ParsedFormat {
  /** 每个字符位的类型 */
  kinds: CharKind[]
  /** 前缀分隔符：字符位 index 前是否需要展示分隔符（空格或 -） */
  gapAt: Record<number, string>
}

const KIND_OF: Record<string, CharKind> = { L: 'L', D: 'D', N: 'N' }

export function charKindOf(ch: string): CharKind | null {
  if (/[A-Z]/.test(ch)) return 'L'
  if (/[0-9]/.test(ch)) return 'D'
  return null
}

export function charMatchesKind(ch: string, kind: CharKind): boolean {
  if (kind === 'N') return charKindOf(ch) !== null
  return charKindOf(ch) === kind
}

export function parsePattern(pattern: string): ParsedFormat {
  const kinds: CharKind[] = []
  const gapAt: Record<number, string> = {}
  let pendingGap = ''
  for (const ch of pattern) {
    if (ch === ' ' || ch === '-') {
      pendingGap = ch
      continue
    }
    const kind = KIND_OF[ch]
    if (!kind) continue
    if (kinds.length > 0 && pendingGap) {
      gapAt[kinds.length] = pendingGap
    }
    pendingGap = ''
    kinds.push(kind)
  }
  return { kinds, gapAt }
}

/**
 * 在给定版式中选出与当前已输入字符最贴合的一条：
 * 按顺序取第一条“前缀完全匹配”的版式；全部不匹配则返回 null（自由输入）。
 */
export function matchFormat(chars: string[], formats: string[]): ParsedFormat | null {
  if (formats.length === 0) return null
  for (const pattern of formats) {
    const parsed = parsePattern(pattern)
    if (chars.length === 0 || chars.every((ch, i) => charMatchesKind(ch, parsed.kinds[i]))) {
      return parsed
    }
  }
  return null
}

/** 车牌观感底色：用户未选颜色且非 CN 时展示为当地默认白底 */
export function regionDefaultTone(code?: string): string {
  return regionPresetOf(code).defaultTone
}
