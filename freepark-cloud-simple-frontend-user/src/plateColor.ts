/**
 * 车牌颜色展示元数据：颜色枚举名 ↔ 中文名（用于颜色选择、记录标签）。
 * 与后端 PlateColor 枚举对应（BLUE/YELLOW/GREEN/YELLOW_GREEN/BLACK/WHITE/OTHER）。
 */

export interface PlateColorMeta {
  code: string
  label: string
}

export const PLATE_COLOR_META: PlateColorMeta[] = [
  { code: 'BLUE', label: '蓝牌' },
  { code: 'YELLOW', label: '黄牌' },
  { code: 'GREEN', label: '绿牌' },
  { code: 'YELLOW_GREEN', label: '黄绿牌' },
  { code: 'BLACK', label: '黑牌' },
  { code: 'WHITE', label: '白牌' },
  { code: 'OTHER', label: '其他' }
]

export function plateColorLabel(code?: string | null): string {
  if (!code) return '车牌'
  const meta = PLATE_COLOR_META.find((m) => m.code === code)
  return meta ? meta.label : code
}

/** 车牌颜色的渐变展示（色块/圆点），与车牌输入器配色一致 */
export function plateColorSwatch(code?: string | null): string {
  switch (code) {
    case 'BLUE':
      return 'linear-gradient(135deg,#2f7fe0,#1453ae)'
    case 'GREEN':
      return 'linear-gradient(135deg,#22b573,#0c8a50)'
    case 'YELLOW':
    case 'YELLOW_GREEN':
      return 'linear-gradient(135deg,#ffd257,#f5a623)'
    case 'BLACK':
      return 'linear-gradient(135deg,#3a3f4a,#15181d)'
    case 'WHITE':
      return 'linear-gradient(135deg,#ffffff,#dbe3ea)'
    default:
      return 'linear-gradient(135deg,#7c8aa0,#525e70)'
  }
}

/** 车牌展示位的文字色：黄/白底用深色文字，其余用白字 */
export function plateInkColor(code?: string | null): string {
  return code === 'YELLOW' || code === 'YELLOW_GREEN' || code === 'WHITE' ? '#10131a' : '#ffffff'
}

/** 依据第 2 位自动推断常见颜色：D/F → 绿牌（新能源），否则默认蓝牌 */
export function detectPlateColor(plate: string): 'GREEN' | 'BLUE' {
  const second = plate[1]?.toUpperCase()
  return second === 'D' || second === 'F' ? 'GREEN' : 'BLUE'
}
