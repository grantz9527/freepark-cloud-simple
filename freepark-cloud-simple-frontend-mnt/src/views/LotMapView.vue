<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import request, { ApiError } from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'
import { useUserStore } from '../stores/user'

/** 后端视图模型（本仓库各页面内联定义；id 统一视为 string，运行时数字/字符串均可用） */
interface LotView {
  id: string
  name: string
  code?: string
  mapData?: string | null
  [key: string]: any
}
interface LocationView {
  id: string
  name: string
  [key: string]: any
}
interface AreaView {
  id: string
  name: string
  [key: string]: any
}
interface LaneView {
  id: string
  name?: string
  code?: string
  [key: string]: any
}
interface SpaceView {
  id: string
  [key: string]: any
}

/** 页面双语字典：对应参考项目 i18n 的 lotMap.*，key 保留前缀使模板可直接复用 */
const dict: BiDict = {
  'lotMap.title': { 'zh-CN': '车场地图', en: 'Lot Map' },
  'lotMap.back': { 'zh-CN': '返回', en: 'Back' },
  'lotMap.loading': { 'zh-CN': '加载中…', en: 'Loading…' },
  'lotMap.loadFailed': { 'zh-CN': '加载地图失败', en: 'Failed to load map' },
  'lotMap.lotNotFound': { 'zh-CN': '未找到该车场', en: 'Parking lot not found' },
  'lotMap.toolSelect': { 'zh-CN': '选择', en: 'Select' },
  'lotMap.toolSpace': { 'zh-CN': '车位', en: 'Space' },
  'lotMap.toolEntrance': { 'zh-CN': '入口', en: 'Entrance' },
  'lotMap.toolExit': { 'zh-CN': '出口', en: 'Exit' },
  'lotMap.toolWall': { 'zh-CN': '墙体', en: 'Wall' },
  'lotMap.toolText': { 'zh-CN': '文字', en: 'Text' },
  'lotMap.toolBuilding': { 'zh-CN': '建筑', en: 'Building' },
  'lotMap.hintSelect': { 'zh-CN': '点击选中元素，拖拽移动', en: 'Click to select, drag to move' },
  'lotMap.hintSpace': { 'zh-CN': '点击画布放置车位', en: 'Click canvas to place a parking space' },
  'lotMap.hintEntrance': { 'zh-CN': '点击画布放置入口', en: 'Click canvas to place an entrance' },
  'lotMap.hintExit': { 'zh-CN': '点击画布放置出口', en: 'Click canvas to place an exit' },
  'lotMap.hintWall': { 'zh-CN': '点击起点再点击终点绘制墙体', en: 'Click start point then end point to draw a wall' },
  'lotMap.hintText': { 'zh-CN': '点击画布放置文字标注', en: 'Click canvas to place a text label' },
  'lotMap.hintBuilding': { 'zh-CN': '点击两点绘制建筑外框', en: 'Click two points to draw a building outline' },
  'lotMap.entrance': { 'zh-CN': '入口', en: 'Entrance' },
  'lotMap.exit': { 'zh-CN': '出口', en: 'Exit' },
  'lotMap.rotate': { 'zh-CN': '旋转', en: 'Rotate' },
  'lotMap.rename': { 'zh-CN': '改名', en: 'Rename' },
  'lotMap.delete': { 'zh-CN': '删除', en: 'Delete' },
  'lotMap.clear': { 'zh-CN': '清空', en: 'Clear' },
  'lotMap.save': { 'zh-CN': '保存', en: 'Save' },
  'lotMap.saving': { 'zh-CN': '保存中…', en: 'Saving…' },
  'lotMap.saveSuccess': { 'zh-CN': '车场地图已保存', en: 'Lot map saved' },
  'lotMap.saveFailed': { 'zh-CN': '保存车场地图失败', en: 'Failed to save lot map' },
  'lotMap.confirmClear': { 'zh-CN': '确定要清空所有元素吗？', en: 'Clear all elements?' },
  'lotMap.spaceLabel': { 'zh-CN': '车位编号', en: 'Space No.' },
  'lotMap.spaceLabelPlaceholder': { 'zh-CN': '如 A001', en: 'e.g. A001' },
  'lotMap.textPrompt': { 'zh-CN': '请输入文字内容', en: 'Enter text content' },
  'lotMap.textDefault': { 'zh-CN': '文字', en: 'Text' },
  'lotMap.buildingPrompt': { 'zh-CN': '请输入建筑名称', en: 'Enter building name' },
  'lotMap.buildingDefault': { 'zh-CN': '建筑', en: 'Building' },
  'lotMap.toolTree': { 'zh-CN': '树木', en: 'Tree' },
  'lotMap.toolLawn': { 'zh-CN': '草坪', en: 'Lawn' },
  'lotMap.hintTree': { 'zh-CN': '点击画布放置树木', en: 'Click canvas to place a tree' },
  'lotMap.hintLawn': { 'zh-CN': '点击两点绘制草坪', en: 'Click two points to draw a lawn' },
  'lotMap.elementCount': { 'zh-CN': '共 {count} 个元素', en: '{count} elements' },
  'lotMap.noFloors': { 'zh-CN': '该车场暂无楼层/位置，请先在车位管理中创建', en: 'No floors/locations in this lot. Please create them in Space Management first.' },
  'lotMap.hintPlaceSpace': { 'zh-CN': '已选中车位 {code}，点击画布放置', en: 'Space {code} selected. Click canvas to place.' },
  'lotMap.spaceProgress': { 'zh-CN': '车位 {placed}/{total}', en: 'Spaces {placed}/{total}' },
  'lotMap.placingSpace': { 'zh-CN': '正在放置车位 {code}…', en: 'Placing space {code}…' },
  'lotMap.cancel': { 'zh-CN': '取消', en: 'Cancel' },
  'lotMap.searchSpace': { 'zh-CN': '搜索车位编号', en: 'Search space code' },
  'lotMap.loadingSpaces': { 'zh-CN': '加载车位中…', en: 'Loading spaces…' },
  'lotMap.noSpaces': { 'zh-CN': '该楼层暂无车位', en: 'No spaces on this floor' },
  'lotMap.disabled': { 'zh-CN': '已停用', en: 'Disabled' },
  'lotMap.edit': { 'zh-CN': '编辑', en: 'Edit' },
  'lotMap.hintViewing': { 'zh-CN': '查看模式（只读）', en: 'Viewing (read-only)' },
  'lotMap.areas': { 'zh-CN': '区域', en: 'Areas' },
  'lotMap.hintPlaceArea': { 'zh-CN': '已选中区域 {name}，点击两点绘制矩形', en: 'Area {name} selected. Click two points to draw.' },
  'lotMap.placingArea': { 'zh-CN': '正在绘制区域 {name}…', en: 'Drawing area {name}…' },
  'lotMap.shapeRect': { 'zh-CN': '矩形', en: 'Rectangle' },
  'lotMap.shapePolygon': { 'zh-CN': '多边形', en: 'Polygon' },
  'lotMap.hintPlacePolygon': { 'zh-CN': '多边形：点击添加顶点，双击或 Enter 完成（至少 3 点）', en: 'Polygon: click to add vertices, double-click or Enter to finish (min 3 points)' },
  'lotMap.resetView': { 'zh-CN': '重置', en: 'Reset' },
  'lotMap.fitToContent': { 'zh-CN': '适应内容', en: 'Fit' },
  'lotMap.lanes': { 'zh-CN': '通道', en: 'Lanes' },
  'lotMap.laneEntrance': { 'zh-CN': '入口通道', en: 'Entrance lanes' },
  'lotMap.laneExit': { 'zh-CN': '出口通道', en: 'Exit lanes' },
  'lotMap.noLanes': { 'zh-CN': '暂无可用通道', en: 'No lanes available' },
  'lotMap.placingLane': { 'zh-CN': '正在放置通道 {name}（{code}）…', en: 'Placing lane {name} ({code})…' },
  'lotMap.hintPlaceLane': { 'zh-CN': '已选中通道 {name}，点击画布放置', en: 'Lane {name} selected, click canvas to place' },
  'lotMap.hintNoLanes': { 'zh-CN': '该车场暂无可用通道，请在通道管理中配置', en: 'No lanes available for this lot. Please configure them in lane management.' },
  'lotMap.hintSelectLane': { 'zh-CN': '请先在侧边栏选择通道，再点击画布放置', en: 'Select a lane from the sidebar first, then click canvas to place' },
  'lotMap.infoWall': { 'zh-CN': '长度 {len} · 角度 {deg}°', en: 'Length {len} · Angle {deg}°' },
  'lotMap.infoSize': { 'zh-CN': '宽 {w} × 高 {h}', en: 'Width {w} × Height {h}' },
  'lotMap.infoVertex': { 'zh-CN': '顶点坐标 ({x}, {y})', en: 'Vertex ({x}, {y})' }
}

const { t: bt, locale } = useBiText(dict)

/** 双语文案（兼容参考项目 t(key, params) 的占位符插值写法） */
function t(key: string, params?: Record<string, string | number | undefined>): string {
  let text = bt(key)
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      if (v !== undefined) {
        text = text.split(`{${k}}`).join(String(v))
      }
    }
  }
  return text
}

/** API 包装（参考项目来自 @/api/client，本仓库统一走 request，拦截器已解包 ApiResult） */
async function listLots(_locale?: string): Promise<{ data: LotView[] }> {
  return { data: await request.get<never, LotView[]>('/lots') }
}
async function listLocations(lotId: string, _locale?: string): Promise<{ data: LocationView[] }> {
  return { data: await request.get<never, LocationView[]>(`/lots/${lotId}/locations`) }
}
async function listLanes(_locale?: string, lotId?: string): Promise<{ data: LaneView[] }> {
  return { data: await request.get<never, LaneView[]>('/lanes', { params: { lotId } }) }
}
async function listAreas(lotId: string, _locale?: string, locationId?: string | number): Promise<{ data: AreaView[] }> {
  return { data: await request.get<never, AreaView[]>(`/lots/${lotId}/areas`, { params: { locationId } }) }
}
async function listSpaces(
  lotId: string,
  _locale?: string,
  opts?: { locationId?: string | number; size?: number }
): Promise<{ data: { items: SpaceView[] } }> {
  const page = await request.get<never, { list: SpaceView[] }>(`/lots/${lotId}/spaces`, {
    params: { locationId: opts?.locationId, page: 1, size: opts?.size }
  })
  return { data: { items: page.list } }
}
async function updateLot(
  lotId: string,
  payload: Record<string, unknown>,
  _locale?: string
): Promise<{ data: LotView }> {
  return { data: await request.put<never, LotView>(`/lots/${lotId}`, payload) }
}

const userStore = useUserStore()

type ElementType = 'space' | 'entrance' | 'exit' | 'wall' | 'text' | 'area' | 'building' | 'tree' | 'lawn'
type ToolType = 'select' | 'entrance' | 'exit' | 'wall' | 'text' | 'building' | 'tree' | 'lawn'

interface MapElement {
  id: string
  type: ElementType
  x: number
  y: number
  w?: number
  h?: number
  rotation?: number
  label?: string
  x2?: number
  y2?: number
  text?: string
  spaceId?: string
  spaceCode?: string
  areaId?: string
  areaName?: string
  laneId?: string
  laneName?: string
  laneCode?: string
  shape?: 'rect' | 'polygon'
  points?: { x: number; y: number }[]
}

interface FloorData {
  elements: MapElement[]
}

interface MapData {
  version: number
  floors: Record<string, FloorData>
}

const CANVAS_W = 2400
const CANVAS_H = 1600
const SPACE_W = 44
const SPACE_H = 88
const MARKER_W = 90
const MARKER_H = 40
const TREE_SIZE = 28
const WALL_MIN = 20
const PAN_LIMIT = 200000
/** 编辑把手在屏幕上的固定半径（像素），不受缩放影响 */
const HANDLE_R_PX = 9

const route = useRoute()
const router = useRouter()

const isAdmin = computed(() => userStore.isSuperAdmin)
const readOnly = computed(() => route.name !== 'lot-map-edit')

const loading = ref(false)
const saving = ref(false)
const spacesLoading = ref(false)
const errorMessage = ref('')
const statusMessage = ref('')
const statusOk = ref(true)

const lot = ref<LotView | null>(null)
const lotId = computed(() => String(route.params.lotId ?? ''))

const locations = ref<LocationView[]>([])
const selectedLocationId = ref<string>('')
const allSpaces = ref<SpaceView[]>([])
const allAreas = ref<AreaView[]>([])
const allLanes = ref<LaneView[]>([])
const spaceSearch = ref('')

const mapData = ref<MapData>({ version: 2, floors: {} })
const elements = ref<MapElement[]>([])
const selectedId = ref<string | null>(null)
const activeTool = ref<ToolType>('select')
const pendingSpace = ref<SpaceView | null>(null)
const pendingArea = ref<AreaView | null>(null)
const pendingLane = ref<LaneView | null>(null)

const wallStart = ref<{ x: number; y: number } | null>(null)
const wallPreview = ref<{ x: number; y: number } | null>(null)

const areaDrawStart = ref<{ x: number; y: number } | null>(null)
const areaDrawPreview = ref<{ x: number; y: number } | null>(null)

const buildingDrawStart = ref<{ x: number; y: number } | null>(null)
const buildingDrawPreview = ref<{ x: number; y: number } | null>(null)

const lawnDrawStart = ref<{ x: number; y: number } | null>(null)
const lawnDrawPreview = ref<{ x: number; y: number } | null>(null)

type AreaDrawMode = 'rect' | 'polygon'
const areaDrawMode = ref<AreaDrawMode>('rect')
const buildingDrawMode = ref<AreaDrawMode>('rect')
const lawnDrawMode = ref<AreaDrawMode>('rect')
const polygonPoints = ref<{ x: number; y: number }[]>([])
const polygonPreview = ref<{ x: number; y: number } | null>(null)

const svgRef = ref<SVGSVGElement | null>(null)

const zoom = ref(1.0)
const panX = ref(0)
const panY = ref(0)
const spacePressed = ref(false)
/** svg 渲染区域像素宽度，用于把手固定屏幕尺寸换算 */
const svgPxWidth = ref(0)

const viewBoxStr = computed(() => {
  const w = CANVAS_W / zoom.value
  const h = CANVAS_H / zoom.value
  return `${panX.value} ${panY.value} ${w} ${h}`
})

const viewWidth = computed(() => CANVAS_W / zoom.value)
const viewHeight = computed(() => CANVAS_H / zoom.value)

const compassTransform = computed(() => {
  const margin = 60 / zoom.value
  const cx = panX.value + viewWidth.value - margin
  const cy = panY.value + margin
  return `translate(${cx}, ${cy}) scale(${1 / zoom.value})`
})

function clampPan(): void {
  panX.value = Math.max(-PAN_LIMIT, Math.min(PAN_LIMIT, panX.value))
  panY.value = Math.max(-PAN_LIMIT, Math.min(PAN_LIMIT, panY.value))
}

function zoomBy(factor: number, centerX?: number, centerY?: number): void {
  const oldZoom = zoom.value
  const newZoom = Math.max(0.05, Math.min(20, oldZoom * factor))
  if (newZoom === oldZoom) return
  if (centerX !== undefined && centerY !== undefined) {
    panX.value = centerX - (centerX - panX.value) * (oldZoom / newZoom)
    panY.value = centerY - (centerY - panY.value) * (oldZoom / newZoom)
  }
  zoom.value = newZoom
  clampPan()
}

function getElementBounds(): { minX: number; minY: number; maxX: number; maxY: number } | null {
  if (elements.value.length === 0) return null
  let minX = Infinity
  let minY = Infinity
  let maxX = -Infinity
  let maxY = -Infinity
  for (const el of elements.value) {
    const xs: number[] = [el.x]
    const ys: number[] = [el.y]
    if (el.x2 !== undefined) xs.push(el.x2)
    if (el.y2 !== undefined) ys.push(el.y2)
    if (el.w !== undefined) xs.push(el.x + el.w)
    if (el.h !== undefined) ys.push(el.y + el.h)
    if (el.points) {
      for (const p of el.points) {
        xs.push(p.x)
        ys.push(p.y)
      }
    }
    minX = Math.min(minX, ...xs)
    minY = Math.min(minY, ...ys)
    maxX = Math.max(maxX, ...xs)
    maxY = Math.max(maxY, ...ys)
  }
  if (!isFinite(minX) || !isFinite(minY) || !isFinite(maxX) || !isFinite(maxY)) return null
  return { minX, minY, maxX, maxY }
}

function fitToContent(padding = 120): void {
  const bounds = getElementBounds()
  if (!bounds) {
    zoom.value = 1.0
    panX.value = 0
    panY.value = 0
    return
  }
  const contentW = Math.max(1, bounds.maxX - bounds.minX)
  const contentH = Math.max(1, bounds.maxY - bounds.minY)
  const scaleX = CANVAS_W / (contentW + padding * 2)
  const scaleY = CANVAS_H / (contentH + padding * 2)
  const newZoom = Math.max(0.05, Math.min(20, Math.min(scaleX, scaleY)))
  zoom.value = newZoom
  const w = CANVAS_W / newZoom
  const h = CANVAS_H / newZoom
  panX.value = bounds.minX - (w - contentW) / 2 - padding / newZoom
  panY.value = bounds.minY - (h - contentH) / 2 - padding / newZoom
  clampPan()
}

function onCanvasWheel(event: WheelEvent): void {
  event.preventDefault()
  const svg = svgRef.value
  if (!svg) return
  const pt = svg.createSVGPoint()
  pt.x = event.clientX
  pt.y = event.clientY
  const ctm = svg.getScreenCTM()
  if (!ctm) return
  const transformed = pt.matrixTransform(ctm.inverse())
  zoomBy(event.deltaY > 0 ? 0.85 : 1.18, transformed.x, transformed.y)
}

let panState: { startClientX: number; startClientY: number; startPanX: number; startPanY: number } | null = null

function onCanvasTouchStart(event: TouchEvent): void {
  if (event.touches.length === 1) {
    const touch = event.touches[0]
    if (!touch) return
    panState = {
      startClientX: touch.clientX,
      startClientY: touch.clientY,
      startPanX: panX.value,
      startPanY: panY.value,
    }
  }
}

function onCanvasTouchMove(event: TouchEvent): void {
  if (panState && event.touches.length === 1) {
    event.preventDefault()
    const touch = event.touches[0]
    if (!touch) return
    const svg = svgRef.value
    if (!svg) return
    const rect = svg.getBoundingClientRect()
    const scaleX = CANVAS_W / zoom.value / rect.width
    const scaleY = CANVAS_H / zoom.value / rect.height
    panX.value = panState.startPanX - (touch.clientX - panState.startClientX) * scaleX
    panY.value = panState.startPanY - (touch.clientY - panState.startClientY) * scaleY
    clampPan()
  }
}

function onCanvasTouchEnd(): void {
  panState = null
}

const tools: { id: ToolType; labelKey: string }[] = [
  { id: 'select', labelKey: 'lotMap.toolSelect' },
  { id: 'entrance', labelKey: 'lotMap.toolEntrance' },
  { id: 'exit', labelKey: 'lotMap.toolExit' },
  { id: 'wall', labelKey: 'lotMap.toolWall' },
  { id: 'building', labelKey: 'lotMap.toolBuilding' },
  { id: 'tree', labelKey: 'lotMap.toolTree' },
  { id: 'lawn', labelKey: 'lotMap.toolLawn' },
  { id: 'text', labelKey: 'lotMap.toolText' },
]

const selectedElement = computed(() =>
  elements.value.find((e) => e.id === selectedId.value) ?? null,
)

const placedSpaceIds = computed(() => {
  const ids = new Set<string>()
  for (const el of elements.value) {
    if (el.type === 'space' && el.spaceId) {
      ids.add(el.spaceId)
    }
  }
  return ids
})

const placedAreaIds = computed(() => {
  const ids = new Set<string>()
  for (const el of elements.value) {
    if (el.type === 'area' && el.areaId) {
      ids.add(el.areaId)
    }
  }
  return ids
})

const placedLaneIds = computed(() => {
  const ids = new Set<string>()
  for (const el of elements.value) {
    if ((el.type === 'entrance' || el.type === 'exit') && el.laneId) {
      ids.add(el.laneId)
    }
  }
  return ids
})

const entranceLanes = computed(() =>
  allLanes.value.filter(
    (l) => l.enabled && (l.laneType === 'ENTRANCE' || l.laneType === 'BIDIRECTIONAL'),
  ),
)

const exitLanes = computed(() =>
  allLanes.value.filter(
    (l) => l.enabled && (l.laneType === 'EXIT' || l.laneType === 'BIDIRECTIONAL'),
  ),
)

const relevantLanes = computed<LaneView[]>(() => {
  if (activeTool.value === 'entrance') return entranceLanes.value
  if (activeTool.value === 'exit') return exitLanes.value
  return []
})

const showLanePicker = computed(
  () => !readOnly.value && (activeTool.value === 'entrance' || activeTool.value === 'exit'),
)

const filteredSpaces = computed(() => {
  const query = spaceSearch.value.trim().toLowerCase()
  if (!query) return allSpaces.value
  return allSpaces.value.filter((s) => s.code.toLowerCase().includes(query))
})

const spacesByArea = computed(() => {
  const map = new Map<string, { areaId: string; spaces: SpaceView[] }>()
  for (const space of filteredSpaces.value) {
    const key = space.areaName || space.areaId
    if (!map.has(key)) {
      map.set(key, { areaId: space.areaId, spaces: [] })
    }
    map.get(key)!.spaces.push(space)
  }
  return Array.from(map.entries()).map(([name, val]) => ({ name, ...val }))
})

const placedCount = computed(() => placedSpaceIds.value.size)
const totalSpaces = computed(() => allSpaces.value.length)

const buildingElements = computed(() => elements.value.filter((e) => e.type === 'building'))
const areaElements = computed(() => elements.value.filter((e) => e.type === 'area'))
const lawnElements = computed(() => elements.value.filter((e) => e.type === 'lawn'))
const nonAreaElements = computed(() =>
  elements.value.filter(
    (e) => e.type !== 'area' && e.type !== 'building' && e.type !== 'lawn',
  ),
)

const toolHintKey = computed(() => {
  if (readOnly.value) return 'lotMap.hintViewing'
  if (pendingArea.value) {
    return areaDrawMode.value === 'polygon' ? 'lotMap.hintPlacePolygon' : 'lotMap.hintPlaceArea'
  }
  if (pendingSpace.value) return 'lotMap.hintPlaceSpace'
  if (pendingLane.value) return 'lotMap.hintPlaceLane'
  if (activeTool.value === 'building' && buildingDrawMode.value === 'polygon') {
    return 'lotMap.hintPlacePolygon'
  }
  if (activeTool.value === 'lawn' && lawnDrawMode.value === 'polygon') {
    return 'lotMap.hintPlacePolygon'
  }
  if (showLanePicker.value && relevantLanes.value.length === 0) return 'lotMap.hintNoLanes'
  if (showLanePicker.value) return 'lotMap.hintSelectLane'
  const map: Record<ToolType, string> = {
    select: 'lotMap.hintSelect',
    entrance: 'lotMap.hintEntrance',
    exit: 'lotMap.hintExit',
    wall: 'lotMap.hintWall',
    building: 'lotMap.hintBuilding',
    tree: 'lotMap.hintTree',
    lawn: 'lotMap.hintLawn',
    text: 'lotMap.hintText',
  }
  return map[activeTool.value]
})

const canvasCursorClass = computed(() => {
  if (readOnly.value) return 'cursor-default'
  if (pendingSpace.value || pendingArea.value || pendingLane.value) return 'cursor-place'
  if (activeTool.value === 'select') return 'cursor-default'
  return ''
})

function measureCanvas(): void {
  const el = svgRef.value
  svgPxWidth.value = el ? el.getBoundingClientRect().width : 0
}

/** 把手半径：把“屏幕固定像素”换算成 SVG 场景单位，保证任意缩放级别下尺寸与手感一致 */
const handleR = computed(() => {
  const pxW = svgPxWidth.value || 1200
  return (HANDLE_R_PX * (CANVAS_W / zoom.value)) / pxW
})

/** 屏幕坐标 → SVG 用户坐标；拖动时用不取整的原始坐标，避免低缩放下的“跳格” */
function toSvgPoint(event: MouseEvent): { x: number; y: number } {
  const svg = svgRef.value
  if (!svg) return { x: 0, y: 0 }
  const pt = svg.createSVGPoint()
  pt.x = event.clientX
  pt.y = event.clientY
  const ctm = svg.getScreenCTM()
  if (!ctm) return { x: 0, y: 0 }
  const transformed = pt.matrixTransform(ctm.inverse())
  return { x: transformed.x, y: transformed.y }
}

function svgPoint(event: MouseEvent): { x: number; y: number } {
  const p = toSvgPoint(event)
  return { x: Math.round(p.x), y: Math.round(p.y) }
}

function svgPointRaw(event: MouseEvent): { x: number; y: number } {
  return toSvgPoint(event)
}

function newId(): string {
  return crypto.randomUUID()
}

function pointsToStr(points?: { x: number; y: number }[]): string {
  if (!points || points.length === 0) return ''
  return points.map((p) => `${p.x},${p.y}`).join(' ')
}

function polygonCenter(points?: { x: number; y: number }[]): { x: number; y: number } {
  if (!points || points.length === 0) return { x: 0, y: 0 }
  let cx = 0
  let cy = 0
  for (const p of points) {
    cx += p.x
    cy += p.y
  }
  return { x: cx / points.length, y: cy / points.length }
}

function syncCurrentFloor(): void {
  const locId = selectedLocationId.value
  if (!locId) return
  if (!mapData.value.floors[locId]) {
    mapData.value.floors[locId] = { elements: [] }
  }
  mapData.value.floors[locId].elements = [...elements.value]
}

function loadFloor(locId: string): void {
  const floor = mapData.value.floors[locId]
  elements.value = floor ? [...floor.elements] : []
  selectedId.value = null
  handleDrag = null
  pendingSpace.value = null
  pendingArea.value = null
  wallStart.value = null
  wallPreview.value = null
  buildingDrawStart.value = null
  buildingDrawPreview.value = null
  buildingDrawMode.value = 'rect'
  lawnDrawStart.value = null
  lawnDrawPreview.value = null
  lawnDrawMode.value = 'rect'
  areaDrawStart.value = null
  areaDrawPreview.value = null
  polygonPoints.value = []
  polygonPreview.value = null
  fitToContent()
}

async function loadFloorData(locId: string): Promise<void> {
  spacesLoading.value = true
  try {
    const [areaResult, spaceResult] = await Promise.all([
      listAreas(lotId.value, locale.value, locId),
      listSpaces(lotId.value, locale.value, { locationId: locId, size: 9999 }),
    ])
    allAreas.value = areaResult.data
    allSpaces.value = spaceResult.data.items
  } catch {
    allAreas.value = []
    allSpaces.value = []
  } finally {
    spacesLoading.value = false
  }
}

async function onFloorChange(locId: string): Promise<void> {
  if (locId === selectedLocationId.value) return
  syncCurrentFloor()
  selectedLocationId.value = locId
  loadFloor(locId)
  await loadFloorData(locId)
}

function selectPendingSpace(space: SpaceView): void {
  if (placedSpaceIds.value.has(space.id)) return
  pendingSpace.value = space
  selectedId.value = null
  activeTool.value = 'select'
}

function cancelPendingSpace(): void {
  pendingSpace.value = null
}

function selectPendingLane(lane: LaneView): void {
  if (placedLaneIds.value.has(lane.id)) return
  pendingLane.value = lane
  selectedId.value = null
  pendingSpace.value = null
  pendingArea.value = null
}

function cancelPendingLane(): void {
  pendingLane.value = null
}

function isLanePlaced(laneId: string): boolean {
  return placedLaneIds.value.has(laneId)
}

function selectPendingArea(area: AreaView): void {
  if (placedAreaIds.value.has(area.id)) return
  pendingArea.value = area
  pendingSpace.value = null
  selectedId.value = null
  areaDrawStart.value = null
  areaDrawPreview.value = null
  polygonPoints.value = []
  polygonPreview.value = null
  areaDrawMode.value = 'rect'
  activeTool.value = 'select'
}

function cancelPendingArea(): void {
  pendingArea.value = null
  areaDrawStart.value = null
  areaDrawPreview.value = null
  polygonPoints.value = []
  polygonPreview.value = null
}

function setAreaDrawMode(mode: AreaDrawMode): void {
  areaDrawMode.value = mode
  areaDrawStart.value = null
  areaDrawPreview.value = null
  polygonPoints.value = []
  polygonPreview.value = null
}

function setBuildingDrawMode(mode: AreaDrawMode): void {
  buildingDrawMode.value = mode
  buildingDrawStart.value = null
  buildingDrawPreview.value = null
  polygonPoints.value = []
  polygonPreview.value = null
}

function finishPolygonArea(): void {
  if (!pendingArea.value || polygonPoints.value.length < 3) return
  const pts = [...polygonPoints.value]
  const first = pts[0]
  if (!first) return
  elements.value.push({
    id: newId(),
    type: 'area',
    x: first.x,
    y: first.y,
    shape: 'polygon',
    points: pts,
    areaId: pendingArea.value.id,
    areaName: pendingArea.value.name,
  })
  pendingArea.value = null
  polygonPoints.value = []
  polygonPreview.value = null
}

function finishAreaDraw(x: number, y: number): void {
  if (!pendingArea.value || !areaDrawStart.value) return
  const x1 = Math.min(areaDrawStart.value.x, x)
  const y1 = Math.min(areaDrawStart.value.y, y)
  const x2 = Math.max(areaDrawStart.value.x, x)
  const y2 = Math.max(areaDrawStart.value.y, y)
  if (x2 - x1 >= 20 && y2 - y1 >= 20) {
    elements.value.push({
      id: newId(),
      type: 'area',
      x: x1,
      y: y1,
      x2,
      y2,
      shape: 'rect',
      areaId: pendingArea.value.id,
      areaName: pendingArea.value.name,
    })
  }
  pendingArea.value = null
  areaDrawStart.value = null
  areaDrawPreview.value = null
}

function isAreaPlaced(areaId: string): boolean {
  return placedAreaIds.value.has(areaId)
}

function placeSpaceOnMap(space: SpaceView, x: number, y: number): void {
  elements.value.push({
    id: newId(),
    type: 'space',
    x: x - SPACE_W / 2,
    y: y - SPACE_H / 2,
    w: SPACE_W,
    h: SPACE_H,
    rotation: 0,
    spaceId: space.id,
    spaceCode: space.code,
    label: space.code,
  })
}

function placeMarker(type: 'entrance' | 'exit', x: number, y: number): void {
  const lane = pendingLane.value
  elements.value.push({
    id: newId(),
    type,
    x: x - MARKER_W / 2,
    y: y - MARKER_H / 2,
    w: MARKER_W,
    h: MARKER_H,
    laneId: lane?.id,
    laneName: lane?.name,
    laneCode: lane?.code,
    label: lane?.name,
  })
  pendingLane.value = null
}

function placeText(x: number, y: number): void {
  const text = window.prompt(t('lotMap.textPrompt'), '')
  if (text === null) return
  elements.value.push({
    id: newId(),
    type: 'text',
    x,
    y,
    text: text || t('lotMap.textDefault'),
  })
}

function startWall(x: number, y: number): void {
  wallStart.value = { x, y }
  wallPreview.value = { x, y }
}

function finishWall(x: number, y: number): void {
  if (!wallStart.value) return
  const dx = x - wallStart.value.x
  const dy = y - wallStart.value.y
  if (Math.hypot(dx, dy) >= WALL_MIN) {
    elements.value.push({
      id: newId(),
      type: 'wall',
      x: wallStart.value.x,
      y: wallStart.value.y,
      x2: x,
      y2: y,
    })
  }
  wallStart.value = null
  wallPreview.value = null
}

function finishBuildingDraw(x: number, y: number): void {
  if (!buildingDrawStart.value) return
  const start = buildingDrawStart.value
  const dx = x - start.x
  const dy = y - start.y
  const name = window.prompt(t('lotMap.buildingPrompt'), t('lotMap.buildingDefault'))
  if (name === null) {
    buildingDrawStart.value = null
    buildingDrawPreview.value = null
    return
  }
  if (Math.hypot(dx, dy) >= WALL_MIN) {
    elements.value.push({
      id: newId(),
      type: 'building',
      x: start.x,
      y: start.y,
      x2: x,
      y2: y,
      label: name || t('lotMap.buildingDefault'),
    })
  }
  buildingDrawStart.value = null
  buildingDrawPreview.value = null
}

function finishPolygonBuilding(): void {
  if (polygonPoints.value.length < 3) return
  const pts = [...polygonPoints.value]
  const first = pts[0]
  if (!first) return
  const name = window.prompt(t('lotMap.buildingPrompt'), t('lotMap.buildingDefault'))
  if (name === null) {
    polygonPoints.value = []
    polygonPreview.value = null
    return
  }
  elements.value.push({
    id: newId(),
    type: 'building',
    x: first.x,
    y: first.y,
    shape: 'polygon',
    points: pts,
    label: name || t('lotMap.buildingDefault'),
  })
  polygonPoints.value = []
  polygonPreview.value = null
}

function placeTree(x: number, y: number): void {
  elements.value.push({
    id: newId(),
    type: 'tree',
    x: x - TREE_SIZE / 2,
    y: y - TREE_SIZE / 2,
    w: TREE_SIZE,
    h: TREE_SIZE,
  })
}

function setLawnDrawMode(mode: AreaDrawMode): void {
  lawnDrawMode.value = mode
  lawnDrawStart.value = null
  lawnDrawPreview.value = null
  polygonPoints.value = []
  polygonPreview.value = null
}

function finishLawnDraw(x: number, y: number): void {
  if (!lawnDrawStart.value) return
  const start = lawnDrawStart.value
  const x1 = Math.min(start.x, x)
  const y1 = Math.min(start.y, y)
  const x2 = Math.max(start.x, x)
  const y2 = Math.max(start.y, y)
  if (x2 - x1 >= 20 && y2 - y1 >= 20) {
    elements.value.push({
      id: newId(),
      type: 'lawn',
      x: x1,
      y: y1,
      x2,
      y2,
      shape: 'rect',
    })
  }
  lawnDrawStart.value = null
  lawnDrawPreview.value = null
}

function finishPolygonLawn(): void {
  if (polygonPoints.value.length < 3) return
  const pts = [...polygonPoints.value]
  const first = pts[0]
  if (!first) return
  elements.value.push({
    id: newId(),
    type: 'lawn',
    x: first.x,
    y: first.y,
    shape: 'polygon',
    points: pts,
  })
  polygonPoints.value = []
  polygonPreview.value = null
}

function moveElement(id: string, dx: number, dy: number): void {
  const el = elements.value.find((e) => e.id === id)
  if (!el) return
  el.x += dx
  el.y += dy
  if (el.x2 !== undefined) el.x2 += dx
  if (el.y2 !== undefined) el.y2 += dy
  if (el.points) {
    el.points = el.points.map((p) => ({ x: p.x + dx, y: p.y + dy }))
  }
}

let dragState: {
  id: string
  lastX: number
  lastY: number
} | null = null

/**
 * 形状编辑把手拖动状态（wall 端点 / building 角点 / building 多边形顶点）。
 * grabDx/grabDy：按下时“把手中心 − 鼠标”的偏移，拖动以偏移为基准，
 * 使把手不跳到鼠标位置，保证精确可控。
 */
let handleDrag: { id: string; mode: string; grabDx: number; grabDy: number } | null = null

function normalizeRectEl(el: MapElement): void {
  if (el.x2 === undefined || el.y2 === undefined) return
  if (el.x > el.x2) {
    const tmp = el.x
    el.x = el.x2
    el.x2 = tmp
  }
  if (el.y > el.y2) {
    const tmp = el.y
    el.y = el.y2
    el.y2 = tmp
  }
}

/** 返回指定把手当前的锚点位置（供按下时计算抓取偏移，保证拖动不跳变） */
function handleAnchor(el: MapElement, mode: string): { x: number; y: number } {
  if (el.type === 'wall') {
    return mode === 'w0' ? { x: el.x, y: el.y } : { x: el.x2 ?? el.x, y: el.y2 ?? el.y }
  }
  if (el.type === 'building') {
    if (el.shape === 'polygon' && el.points && mode.startsWith('v')) {
      const p = el.points[Number(mode.slice(1))]
      if (p) return { x: p.x, y: p.y }
    }
    const x1 = Math.min(el.x, el.x2 ?? el.x)
    const y1 = Math.min(el.y, el.y2 ?? el.y)
    const x2 = Math.max(el.x, el.x2 ?? el.x)
    const y2 = Math.max(el.y, el.y2 ?? el.y)
    if (mode === 'tr') return { x: x2, y: y1 }
    if (mode === 'br') return { x: x2, y: y2 }
    if (mode === 'bl') return { x: x1, y: y2 }
    return { x: x1, y: y1 }
  }
  return { x: el.x, y: el.y }
}

/** 选中元素的编辑把手（仅墙体 / 建筑，选择工具下可见） */
const editableHandles = computed<{ mode: string; x: number; y: number }[]>(() => {
  const el = selectedElement.value
  if (!el || readOnly.value || activeTool.value !== 'select') return []
  if (el.type === 'wall') {
    return [
      { mode: 'w0', x: el.x, y: el.y },
      { mode: 'w1', x: el.x2 ?? el.x, y: el.y2 ?? el.y },
    ]
  }
  if (el.type === 'building') {
    if (el.shape === 'polygon' && el.points) {
      return el.points.map((p, i) => ({ mode: `v${i}`, x: p.x, y: p.y }))
    }
    const x1 = Math.min(el.x, el.x2 ?? el.x)
    const y1 = Math.min(el.y, el.y2 ?? el.y)
    const x2 = Math.max(el.x, el.x2 ?? el.x)
    const y2 = Math.max(el.y, el.y2 ?? el.y)
    return [
      { mode: 'tl', x: x1, y: y1 },
      { mode: 'tr', x: x2, y: y1 },
      { mode: 'br', x: x2, y: y2 },
      { mode: 'bl', x: x1, y: y2 },
    ]
  }
  return []
})

function onCanvasMouseDown(event: MouseEvent): void {
  if (spacePressed.value || event.button === 1) {
    panState = {
      startClientX: event.clientX,
      startClientY: event.clientY,
      startPanX: panX.value,
      startPanY: panY.value,
    }
    return
  }
  if (readOnly.value) return
  const pt = svgPoint(event)
  statusMessage.value = ''

  if (pendingSpace.value) {
    placeSpaceOnMap(pendingSpace.value, pt.x, pt.y)
    pendingSpace.value = null
    return
  }

  if (pendingArea.value) {
    if (areaDrawMode.value === 'polygon') {
      if (event.detail === 2) {
        finishPolygonArea()
        return
      }
      polygonPoints.value.push({ x: pt.x, y: pt.y })
      polygonPreview.value = { x: pt.x, y: pt.y }
      return
    }
    if (!areaDrawStart.value) {
      areaDrawStart.value = { x: pt.x, y: pt.y }
      areaDrawPreview.value = { x: pt.x, y: pt.y }
    } else {
      finishAreaDraw(pt.x, pt.y)
    }
    return
  }

  if (activeTool.value === 'select') {
    const target = event.target as SVGElement
    const handleEl = target.closest('[data-handle-mode]') as SVGElement | null
    if (handleEl) {
      const elId = handleEl.getAttribute('data-element-id') ?? selectedId.value
      const mode = handleEl.getAttribute('data-handle-mode') ?? ''
      const el = elId ? elements.value.find((e) => e.id === elId) : undefined
      if (elId && el) {
        selectedId.value = elId
        // 矩形建筑先按几何左上归一化，保证把手与拖动语义一致
        if (el.type === 'building' && el.shape !== 'polygon') {
          normalizeRectEl(el)
        }
        const anchor = handleAnchor(el, mode)
        const p0 = svgPointRaw(event)
        handleDrag = { id: elId, mode, grabDx: anchor.x - p0.x, grabDy: anchor.y - p0.y }
        beginCanvasDrag()
        updateDragInfo()
      }
      return
    }
    const elementGroup = target.closest('[data-element-id]') as SVGElement | null
    if (elementGroup) {
      const id = elementGroup.getAttribute('data-element-id') ?? ''
      selectedId.value = id
      const p0 = svgPointRaw(event)
      dragState = { id, lastX: p0.x, lastY: p0.y }
      beginCanvasDrag()
    } else {
      selectedId.value = null
      dragState = null
    }
    return
  }

  if (activeTool.value === 'wall') {
    if (!wallStart.value) {
      startWall(pt.x, pt.y)
    } else {
      finishWall(pt.x, pt.y)
    }
    return
  }

  if (activeTool.value === 'building') {
    if (buildingDrawMode.value === 'polygon') {
      if (event.detail === 2) {
        finishPolygonBuilding()
        return
      }
      polygonPoints.value.push({ x: pt.x, y: pt.y })
      polygonPreview.value = { x: pt.x, y: pt.y }
      return
    }
    if (!buildingDrawStart.value) {
      buildingDrawStart.value = { x: pt.x, y: pt.y }
      buildingDrawPreview.value = { x: pt.x, y: pt.y }
    } else {
      finishBuildingDraw(pt.x, pt.y)
    }
    return
  }

  if (activeTool.value === 'entrance') {
    if (pendingLane.value) placeMarker('entrance', pt.x, pt.y)
  } else if (activeTool.value === 'exit') {
    if (pendingLane.value) placeMarker('exit', pt.x, pt.y)
  } else if (activeTool.value === 'tree') {
    placeTree(pt.x, pt.y)
  } else if (activeTool.value === 'lawn') {
    if (lawnDrawMode.value === 'polygon') {
      if (event.detail === 2) {
        finishPolygonLawn()
        return
      }
      polygonPoints.value.push({ x: pt.x, y: pt.y })
      polygonPreview.value = { x: pt.x, y: pt.y }
      return
    }
    if (!lawnDrawStart.value) {
      lawnDrawStart.value = { x: pt.x, y: pt.y }
      lawnDrawPreview.value = { x: pt.x, y: pt.y }
    } else {
      finishLawnDraw(pt.x, pt.y)
    }
  } else if (activeTool.value === 'text') {
    placeText(pt.x, pt.y)
  }
}

/** 拖动把手时状态栏的实时尺寸信息 */
const dragInfo = ref('')

function dragInfoText(el: MapElement, mode: string): string {
  if (el.type === 'wall') {
    const x1 = el.x
    const y1 = el.y
    const x2 = el.x2 ?? el.x
    const y2 = el.y2 ?? el.y
    const len = Math.hypot(x2 - x1, y2 - y1)
    const deg = Math.round((Math.atan2(y2 - y1, x2 - x1) * 180) / Math.PI)
    return t('lotMap.infoWall', { len: len.toFixed(1), deg })
  }
  if (el.type === 'building') {
    if (el.shape === 'polygon' && el.points && mode.startsWith('v')) {
      const p = el.points[Number(mode.slice(1))]
      if (p) return t('lotMap.infoVertex', { x: Math.round(p.x), y: Math.round(p.y) })
      return ''
    }
    const w = Math.abs((el.x2 ?? el.x) - el.x)
    const h = Math.abs((el.y2 ?? el.y) - el.y)
    return t('lotMap.infoSize', { w: Math.round(w), h: Math.round(h) })
  }
  return ''
}

function updateDragInfo(): void {
  const drag = handleDrag
  if (!drag) return
  const el = elements.value.find((e) => e.id === drag.id)
  dragInfo.value = el ? dragInfoText(el, drag.mode) : ''
}

/** 按住拖动时把 move/up 挂到 window：即使鼠标滑出画布/窗口，拖动也不中断 */
function beginCanvasDrag(): void {
  endCanvasDrag()
  window.addEventListener('mousemove', onWindowMouseMove)
  window.addEventListener('mouseup', onWindowMouseUp)
}
function endCanvasDrag(): void {
  window.removeEventListener('mousemove', onWindowMouseMove)
  window.removeEventListener('mouseup', onWindowMouseUp)
}

/**
 * 把手拖动核心：目标 = 鼠标 + 抓取偏移，可自由拖出画布区域，
 * 不再把坐标截断到 [0, CANVAS]（旧逻辑是拖动“跳变/卡住”的主因）。
 */
function applyHandleDrag(
  drag: { id: string; mode: string; grabDx: number; grabDy: number },
  targetX: number,
  targetY: number,
): void {
  const el = elements.value.find((e) => e.id === drag.id)
  if (!el) return
  const mode = drag.mode
  if (el.type === 'wall') {
    if (mode === 'w0') {
      el.x = targetX
      el.y = targetY
    } else if (mode === 'w1') {
      el.x2 = targetX
      el.y2 = targetY
    }
    return
  }
  if (el.type !== 'building') return
  if (el.shape === 'polygon' && el.points && mode.startsWith('v')) {
    const i = Number(mode.slice(1))
    if (el.points[i]) el.points[i] = { x: targetX, y: targetY }
    return
  }
  // 矩形建筑：仅约束最小边长防止翻转，不做画布边界截断
  const ex = el.x
  const ey = el.y
  const ex2 = el.x2 ?? el.x
  const ey2 = el.y2 ?? el.y
  if (mode === 'tl') {
    el.x = Math.min(targetX, ex2 - 1)
    el.y = Math.min(targetY, ey2 - 1)
  } else if (mode === 'tr') {
    el.x2 = Math.max(targetX, ex + 1)
    el.y = Math.min(targetY, ey2 - 1)
  } else if (mode === 'br') {
    el.x2 = Math.max(targetX, ex + 1)
    el.y2 = Math.max(targetY, ey + 1)
  } else if (mode === 'bl') {
    el.x = Math.min(targetX, ex2 - 1)
    el.y2 = Math.max(targetY, ey + 1)
  }
}

/** window 级 mousemove：统一处理平移 / 把手拖动 / 整体元素拖动 */
function onWindowMouseMove(event: MouseEvent): void {
  if (panState) {
    const svg = svgRef.value
    if (!svg) return
    const rect = svg.getBoundingClientRect()
    const scaleX = CANVAS_W / zoom.value / rect.width
    const scaleY = CANVAS_H / zoom.value / rect.height
    panX.value = panState.startPanX - (event.clientX - panState.startClientX) * scaleX
    panY.value = panState.startPanY - (event.clientY - panState.startClientY) * scaleY
    clampPan()
    return
  }
  const pt = svgPointRaw(event)
  const drag = handleDrag
  if (drag) {
    applyHandleDrag(drag, pt.x + drag.grabDx, pt.y + drag.grabDy)
    updateDragInfo()
    return
  }
  if (dragState) {
    const dx = pt.x - dragState.lastX
    const dy = pt.y - dragState.lastY
    moveElement(dragState.id, dx, dy)
    dragState.lastX = pt.x
    dragState.lastY = pt.y
  }
}

function onWindowMouseUp(): void {
  endCanvasDrag()
  dragState = null
  panState = null
  if (handleDrag) {
    handleDrag = null
    dragInfo.value = ''
  }
}

function onCanvasMouseMove(event: MouseEvent): void {
  // 拖动过程中由 window 级监听持续处理，这里只负责悬停/绘制预览
  if (panState || handleDrag || dragState) return
  const pt = svgPointRaw(event)
  if (wallStart.value) {
    wallPreview.value = pt
  }
  if (buildingDrawStart.value) {
    buildingDrawPreview.value = pt
  }
  if (lawnDrawStart.value) {
    lawnDrawPreview.value = pt
  }
  if (areaDrawStart.value) {
    areaDrawPreview.value = pt
  }
  if (polygonPoints.value.length > 0) {
    polygonPreview.value = pt
  }
}

function onCanvasMouseUp(): void {
  endCanvasDrag()
  dragState = null
  panState = null
  if (handleDrag) {
    handleDrag = null
    dragInfo.value = ''
  }
}

function onCanvasMouseLeave(): void {
  // 按住拖拽/平移时离开画布不打断：由 window 级监听继续处理，直到松开鼠标
  if (panState || handleDrag || dragState) return
  wallPreview.value = null
  buildingDrawPreview.value = null
  lawnDrawPreview.value = null
  areaDrawPreview.value = null
  polygonPreview.value = null
}

function deleteSelected(): void {
  if (!selectedId.value) return
  elements.value = elements.value.filter((e) => e.id !== selectedId.value)
  selectedId.value = null
}

const canRotate = computed(() => {
  const el = selectedElement.value
  return el !== null && ['space', 'entrance', 'exit'].includes(el.type)
})

const rotationInput = computed(() => selectedElement.value?.rotation ?? 0)

function setRotation(val: number): void {
  const el = selectedElement.value
  if (!el) return
  if (!['space', 'entrance', 'exit'].includes(el.type)) return
  el.rotation = ((Math.round(val) % 360) + 360) % 360
}

function rotateBy(deg: number): void {
  const el = selectedElement.value
  if (!el) return
  setRotation((el.rotation ?? 0) + deg)
}

function renameBuilding(): void {
  const el = selectedElement.value
  if (!el || el.type !== 'building') return
  const name = window.prompt(
    t('lotMap.buildingPrompt'),
    el.label || t('lotMap.buildingDefault'),
  )
  if (name !== null) {
    el.label = name || t('lotMap.buildingDefault')
  }
}

function clearAll(): void {
  if (elements.value.length === 0) return
  if (!window.confirm(t('lotMap.confirmClear'))) return
  elements.value = []
  selectedId.value = null
  wallStart.value = null
  wallPreview.value = null
  buildingDrawStart.value = null
  buildingDrawPreview.value = null
  buildingDrawMode.value = 'rect'
  lawnDrawStart.value = null
  lawnDrawPreview.value = null
  lawnDrawMode.value = 'rect'
  areaDrawStart.value = null
  areaDrawPreview.value = null
  polygonPoints.value = []
  polygonPreview.value = null
  pendingArea.value = null
  pendingSpace.value = null
  pendingLane.value = null
}

function serializeMapData(): string {
  syncCurrentFloor()
  return JSON.stringify(mapData.value)
}

function parseMapData(raw: string | null): void {
  mapData.value = { version: 2, floors: {} }
  if (!raw) return
  try {
    const data = JSON.parse(raw)
    if (data && typeof data === 'object') {
      if (data.version === 2 && data.floors) {
        mapData.value = data as MapData
      } else if (Array.isArray(data.elements)) {
        // v1 migration: put all elements in an unknown floor
        mapData.value = { version: 2, floors: { _legacy: { elements: data.elements } } }
      }
    }
  } catch {
    // ignore malformed data
  }
}

async function loadPage(): Promise<void> {
  loading.value = true
  errorMessage.value = ''
  statusMessage.value = ''
  try {
    const result = await listLots(locale.value)
    lot.value = result.data.find((item) => String(item.id) === lotId.value) ?? null
    if (!lot.value) {
      errorMessage.value = t('lotMap.lotNotFound')
      return
    }
    parseMapData(lot.value.mapData ?? null)

    const [locResult, laneResult] = await Promise.all([
      listLocations(lotId.value, locale.value),
      listLanes(locale.value, lotId.value),
    ])
    locations.value = locResult.data
    allLanes.value = laneResult.data
    if (locations.value.length === 0) {
      errorMessage.value = t('lotMap.noFloors')
      return
    }
    const firstLoc = locations.value[0]
    if (!firstLoc) {
      errorMessage.value = t('lotMap.noFloors')
      return
    }
    selectedLocationId.value = firstLoc.id
    loadFloor(firstLoc.id)
    await loadFloorData(firstLoc.id)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : t('lotMap.loadFailed')
  } finally {
    loading.value = false
    void requestAnimationFrame(measureCanvas)
  }
}

async function onSave(): Promise<void> {
  if (!lot.value) return
  saving.value = true
  statusMessage.value = ''
  statusOk.value = true
  try {
    const result = await updateLot(
      lotId.value,
      {
        name: lot.value.name,
        lotType: lot.value.lotType,
        address: lot.value.address ?? undefined,
        totalSpaces: lot.value.totalSpaces,
        enabled: lot.value.enabled,
        mapData: serializeMapData(),
      },
      locale.value,
    )
    lot.value = result.data
    statusMessage.value = t('lotMap.saveSuccess')
    statusOk.value = true
  } catch (error) {
    statusMessage.value = error instanceof ApiError ? error.message : t('lotMap.saveFailed')
    statusOk.value = false
  } finally {
    saving.value = false
  }
}

function onKeydown(event: KeyboardEvent): void {
  if (event.code === 'Space') {
    const target = event.target as HTMLElement
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return
    spacePressed.value = true
    return
  }
  if (event.key === 'Escape') {
    if (pendingArea.value && areaDrawMode.value === 'polygon' && polygonPoints.value.length > 0) {
      polygonPoints.value = []
      polygonPreview.value = null
      return
    }
    if (activeTool.value === 'building' && buildingDrawMode.value === 'polygon' && polygonPoints.value.length > 0) {
      polygonPoints.value = []
      polygonPreview.value = null
      return
    }
    if (activeTool.value === 'lawn' && lawnDrawMode.value === 'polygon' && polygonPoints.value.length > 0) {
      polygonPoints.value = []
      polygonPreview.value = null
      return
    }
    if (pendingArea.value) {
      cancelPendingArea()
      return
    }
    if (pendingLane.value) {
      pendingLane.value = null
      return
    }
    if (pendingSpace.value) {
      pendingSpace.value = null
      return
    }
    if (buildingDrawStart.value) {
      buildingDrawStart.value = null
      buildingDrawPreview.value = null
      return
    }
    if (lawnDrawStart.value) {
      lawnDrawStart.value = null
      lawnDrawPreview.value = null
      return
    }
    selectedId.value = null
    wallStart.value = null
    wallPreview.value = null
    buildingDrawStart.value = null
    buildingDrawPreview.value = null
    buildingDrawMode.value = 'rect'
    lawnDrawStart.value = null
    lawnDrawPreview.value = null
    lawnDrawMode.value = 'rect'
    areaDrawStart.value = null
    areaDrawPreview.value = null
    polygonPoints.value = []
    polygonPreview.value = null
    return
  }
  if (event.key === 'Enter' && pendingArea.value && areaDrawMode.value === 'polygon') {
    event.preventDefault()
    finishPolygonArea()
    return
  }
  if (event.key === 'Enter' && activeTool.value === 'building' && buildingDrawMode.value === 'polygon') {
    event.preventDefault()
    finishPolygonBuilding()
    return
  }
  if (event.key === 'Enter' && activeTool.value === 'lawn' && lawnDrawMode.value === 'polygon') {
    event.preventDefault()
    finishPolygonLawn()
    return
  }
  if (event.key === 'Delete' || event.key === 'Backspace') {
    const target = event.target as HTMLElement
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return
    if (selectedId.value) {
      event.preventDefault()
      deleteSelected()
    }
  }
}

function goBack(): void {
  void router.push({ name: 'lots' })
}

function enterEdit(): void {
  void router.push({ name: 'lot-map-edit', params: { lotId: lotId.value } })
}

function isSpacePlaced(spaceId: string): boolean {
  return placedSpaceIds.value.has(spaceId)
}

function onKeyup(event: KeyboardEvent): void {
  if (event.code === 'Space') {
    spacePressed.value = false
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('keyup', onKeyup)
  void loadPage()
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('keyup', onKeyup)
})
</script>

<template>
  <section class="page">
    <div class="header">
      <div class="title-row">
        <button type="button" class="ghost back-btn" @click="goBack">
          {{ t('lotMap.back') }}
        </button>
        <h2>
          {{ lot?.name }}
          <span class="lot-code">{{ lot?.code }}</span>
        </h2>
      </div>
      <button
        v-if="readOnly && isAdmin"
        type="button"
        class="tool-btn primary"
        @click="enterEdit"
      >
        {{ t('lotMap.edit') }}
      </button>
    </div>

    <p v-if="errorMessage" class="banner error">{{ errorMessage }}</p>

    <div v-if="lot && locations.length > 0" class="editor">
      <div class="toolbar">
        <div class="floor-tabs">
          <button
            v-for="loc in locations"
            :key="loc.id"
            type="button"
            class="floor-tab"
            :class="{ active: selectedLocationId === loc.id }"
            @click="onFloorChange(loc.id)"
          >
            {{ loc.name }}
          </button>
        </div>
        <div class="zoom-controls">
          <button type="button" class="zoom-btn" @click="zoomBy(1.25)">+</button>
          <span class="zoom-level">{{ Math.round(zoom * 100) }}%</span>
          <button type="button" class="zoom-btn" @click="zoomBy(0.8)">&minus;</button>
          <button type="button" class="zoom-btn reset" @click="fitToContent()">{{ t('lotMap.fitToContent') }}</button>
        </div>
      </div>

      <div v-if="!readOnly" class="toolbar">
        <div class="tool-group">
          <button
            v-for="tool in tools"
            :key="tool.id"
            type="button"
            class="tool-btn"
            :class="{ active: activeTool === tool.id && !pendingSpace }"
            :disabled="!!pendingSpace"
            @click="activeTool = tool.id"
          >
            {{ t(tool.labelKey) }}
          </button>
        </div>
        <div class="tool-group">
          <template v-if="canRotate">
            <input
              type="number"
              class="angle-input"
              :value="rotationInput"
              min="0"
              max="359"
              @input="setRotation(Number(($event.target as HTMLInputElement).value))"
            />
            <button type="button" class="tool-btn" @click="rotateBy(-15)">-15°</button>
            <button type="button" class="tool-btn" @click="rotateBy(15)">+15°</button>
            <button type="button" class="tool-btn" @click="rotateBy(90)">+90°</button>
          </template>
          <button
            v-if="selectedElement?.type === 'building'"
            type="button"
            class="tool-btn"
            @click="renameBuilding"
          >
            {{ t('lotMap.rename') }}
          </button>
          <button
            v-if="selectedId"
            type="button"
            class="tool-btn danger"
            @click="deleteSelected"
          >
            {{ t('lotMap.delete') }}
          </button>
        </div>
        <div class="tool-group">
          <button type="button" class="tool-btn" :disabled="saving" @click="clearAll">
            {{ t('lotMap.clear') }}
          </button>
          <button type="button" class="tool-btn primary" :disabled="saving" @click="onSave">
            {{ saving ? t('lotMap.saving') : t('lotMap.save') }}
          </button>
        </div>
      </div>

      <div class="status-bar">
        <span class="hint">{{ t(toolHintKey, { code: pendingSpace?.code, name: pendingArea?.name ?? pendingLane?.name }) }}</span>
        <span v-if="dragInfo" class="status-msg ok drag-info">{{ dragInfo }}</span>
        <span class="count">
          {{ t('lotMap.spaceProgress', { placed: placedCount, total: totalSpaces }) }}
          ·
          {{ t('lotMap.elementCount', { count: elements.length }) }}
        </span>
        <span v-if="statusMessage" class="status-msg" :class="{ ok: statusOk }">
          {{ statusMessage }}
        </span>
      </div>

      <div v-if="!readOnly && pendingSpace" class="pending-banner">
        <span>{{ t('lotMap.placingSpace', { code: pendingSpace.code }) }}</span>
        <button type="button" class="tool-btn" @click="cancelPendingSpace">
          {{ t('lotMap.cancel') }}
        </button>
      </div>

      <div v-if="!readOnly && pendingArea" class="pending-banner">
        <span>{{ t('lotMap.placingArea', { name: pendingArea.name }) }}</span>
        <div class="mode-switch">
          <button
            type="button"
            class="mode-btn"
            :class="{ active: areaDrawMode === 'rect' }"
            @click="setAreaDrawMode('rect')"
          >
            {{ t('lotMap.shapeRect') }}
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ active: areaDrawMode === 'polygon' }"
            @click="setAreaDrawMode('polygon')"
          >
            {{ t('lotMap.shapePolygon') }}
          </button>
        </div>
        <button type="button" class="tool-btn" @click="cancelPendingArea">
          {{ t('lotMap.cancel') }}
        </button>
      </div>

      <div v-if="!readOnly && activeTool === 'building'" class="pending-banner">
        <span>{{ t('lotMap.toolBuilding') }}</span>
        <div class="mode-switch">
          <button
            type="button"
            class="mode-btn"
            :class="{ active: buildingDrawMode === 'rect' }"
            @click="setBuildingDrawMode('rect')"
          >
            {{ t('lotMap.shapeRect') }}
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ active: buildingDrawMode === 'polygon' }"
            @click="setBuildingDrawMode('polygon')"
          >
            {{ t('lotMap.shapePolygon') }}
          </button>
        </div>
      </div>

      <div v-if="!readOnly && activeTool === 'lawn'" class="pending-banner">
        <span>{{ t('lotMap.toolLawn') }}</span>
        <div class="mode-switch">
          <button
            type="button"
            class="mode-btn"
            :class="{ active: lawnDrawMode === 'rect' }"
            @click="setLawnDrawMode('rect')"
          >
            {{ t('lotMap.shapeRect') }}
          </button>
          <button
            type="button"
            class="mode-btn"
            :class="{ active: lawnDrawMode === 'polygon' }"
            @click="setLawnDrawMode('polygon')"
          >
            {{ t('lotMap.shapePolygon') }}
          </button>
        </div>
      </div>

      <div v-if="!readOnly && pendingLane" class="pending-banner">
        <span>{{ t('lotMap.placingLane', { name: pendingLane.name, code: pendingLane.code }) }}</span>
        <button type="button" class="tool-btn" @click="cancelPendingLane">
          {{ t('lotMap.cancel') }}
        </button>
      </div>

      <div class="editor-body">
        <aside v-if="!readOnly" class="sidebar">
          <div v-if="showLanePicker" class="lane-section">
            <div class="sidebar-header">
              <div class="section-title">
                {{ activeTool === 'entrance' ? t('lotMap.laneEntrance') : t('lotMap.laneExit') }}
              </div>
            </div>
            <div v-if="relevantLanes.length === 0" class="sidebar-empty">
              {{ t('lotMap.noLanes') }}
            </div>
            <div v-else class="lane-list">
              <div
                v-for="lane in relevantLanes"
                :key="lane.id"
                class="lane-item"
                :class="{
                  placed: isLanePlaced(lane.id),
                  pending: pendingLane?.id === lane.id,
                }"
                @click="selectPendingLane(lane)"
              >
                <span class="lane-name">{{ lane.name }}</span>
                <span class="lane-code">{{ lane.code }}</span>
                <span v-if="isLanePlaced(lane.id)" class="space-check">✓</span>
              </div>
            </div>
          </div>
          <template v-else>
            <div v-if="allAreas.length > 0" class="area-section">
              <div class="section-title">{{ t('lotMap.areas') }}</div>
              <div class="area-list">
                <div
                  v-for="area in allAreas"
                  :key="area.id"
                  class="area-item"
                  :class="{
                    placed: isAreaPlaced(area.id),
                    pending: pendingArea?.id === area.id,
                  }"
                  @click="selectPendingArea(area)"
                >
                  <span class="area-name">{{ area.name }}</span>
                  <span v-if="isAreaPlaced(area.id)" class="space-check">✓</span>
                </div>
              </div>
            </div>

            <div class="sidebar-header">
              <input
                v-model="spaceSearch"
                type="search"
                :placeholder="t('lotMap.searchSpace')"
                class="space-search"
              />
            </div>
            <div v-if="spacesLoading" class="sidebar-empty">
              {{ t('lotMap.loadingSpaces') }}
            </div>
            <div v-else-if="spacesByArea.length === 0" class="sidebar-empty">
              {{ t('lotMap.noSpaces') }}
            </div>
            <div v-else class="space-list">
              <div v-for="group in spacesByArea" :key="group.areaId" class="space-group">
                <div class="group-name">{{ group.name }}</div>
                <div
                  v-for="space in group.spaces"
                  :key="space.id"
                  class="space-item"
                  :class="{
                    placed: isSpacePlaced(space.id),
                    pending: pendingSpace?.id === space.id,
                    disabled: !space.enabled,
                  }"
                  @click="selectPendingSpace(space)"
                >
                  <span class="space-code">{{ space.code }}</span>
                  <span v-if="isSpacePlaced(space.id)" class="space-check">✓</span>
                  <span v-else-if="!space.enabled" class="space-tag">{{ t('lotMap.disabled') }}</span>
                </div>
              </div>
            </div>
          </template>
        </aside>

        <div class="canvas-wrapper">
          <svg
            ref="svgRef"
            class="canvas"
            :class="[canvasCursorClass, { 'cursor-pan': spacePressed }]"
            :viewBox="viewBoxStr"
            xmlns="http://www.w3.org/2000/svg"
            @mousedown="onCanvasMouseDown"
            @mousemove="onCanvasMouseMove"
            @mouseup="onCanvasMouseUp"
            @mouseleave="onCanvasMouseLeave"
            @wheel="onCanvasWheel"
            @touchstart="onCanvasTouchStart"
            @touchmove="onCanvasTouchMove"
            @touchend="onCanvasTouchEnd"
          >
            <defs>
              <pattern id="grid-small" width="20" height="20" patternUnits="userSpaceOnUse">
                <path d="M 20 0 L 0 0 0 20" fill="none" stroke="#e8ece9" stroke-width="0.5" />
              </pattern>
              <pattern id="grid-large" width="100" height="100" patternUnits="userSpaceOnUse">
                <rect width="100" height="100" fill="url(#grid-small)" />
                <path d="M 100 0 L 0 0 0 100" fill="none" stroke="#d0d8d4" stroke-width="1" />
              </pattern>
              <radialGradient id="tree-grad" cx="38%" cy="32%" r="72%">
                <stop offset="0%" stop-color="#a8dcb2" />
                <stop offset="55%" stop-color="#5fa070" />
                <stop offset="100%" stop-color="#3f7d4e" />
              </radialGradient>
              <pattern id="lawn-texture" width="16" height="16" patternUnits="userSpaceOnUse">
                <rect width="16" height="16" fill="rgba(91,154,106,0.22)" />
                <path
                  d="M3 13 L3.5 8 M7 13 L7.8 7 M11 13 L11.6 8 M14 13 L14.4 9"
                  stroke="#4f8a5e"
                  stroke-width="1.1"
                  stroke-linecap="round"
                  fill="none"
                  opacity="0.55"
                />
              </pattern>
            </defs>

            <rect :x="panX" :y="panY" :width="viewWidth" :height="viewHeight" fill="url(#grid-large)" />

            <g class="compass" :transform="compassTransform">
              <circle r="26" fill="#fff" stroke="#aaa" stroke-width="1.5" />
              <polygon points="0,-17 6,7 0,3 -6,7" class="compass-north" />
              <polygon points="0,17 6,-7 0,-3 -6,-7" class="compass-south" />
              <text y="-30" text-anchor="middle" class="compass-label">N</text>
            </g>

            <g
              v-for="el in buildingElements"
              :key="el.id"
              :data-element-id="el.id"
              :class="{ selected: el.id === selectedId }"
              class="map-element"
            >
              <rect
                v-if="el.shape !== 'polygon'"
                :x="Math.min(el.x, el.x2 ?? el.x)"
                :y="Math.min(el.y, el.y2 ?? el.y)"
                :width="Math.abs((el.x2 ?? el.x) - el.x)"
                :height="Math.abs((el.y2 ?? el.y) - el.y)"
                rx="6"
                class="el-building"
              />
              <polygon
                v-else
                :points="pointsToStr(el.points)"
                class="el-building"
              />
              <text
                :x="el.shape === 'polygon' ? polygonCenter(el.points).x : (el.x + (el.x2 ?? el.x)) / 2"
                :y="(el.shape === 'polygon' ? polygonCenter(el.points).y : (el.y + (el.y2 ?? el.y)) / 2) + 5"
                text-anchor="middle"
                class="el-building-label"
              >
                {{ el.label }}
              </text>
            </g>

            <g
              v-for="el in areaElements"
              :key="el.id"
              :data-element-id="el.id"
              :class="{ selected: el.id === selectedId }"
              class="map-element"
            >
              <rect
                v-if="el.shape !== 'polygon'"
                :x="Math.min(el.x, el.x2 ?? el.x)"
                :y="Math.min(el.y, el.y2 ?? el.y)"
                :width="Math.abs((el.x2 ?? el.x) - el.x)"
                :height="Math.abs((el.y2 ?? el.y) - el.y)"
                rx="4"
                class="el-area"
              />
              <polygon
                v-else
                :points="pointsToStr(el.points)"
                class="el-area"
              />
              <text
                :x="el.shape === 'polygon' ? polygonCenter(el.points).x : (el.x + (el.x2 ?? el.x)) / 2"
                :y="(el.shape === 'polygon' ? polygonCenter(el.points).y : (el.y + (el.y2 ?? el.y)) / 2) + 4"
                text-anchor="middle"
                class="el-area-label"
              >
                {{ el.areaName }}
              </text>
            </g>

            <g
              v-for="el in lawnElements"
              :key="el.id"
              :data-element-id="el.id"
              :class="{ selected: el.id === selectedId }"
              class="map-element"
            >
              <rect
                v-if="el.shape !== 'polygon'"
                :x="Math.min(el.x, el.x2 ?? el.x)"
                :y="Math.min(el.y, el.y2 ?? el.y)"
                :width="Math.abs((el.x2 ?? el.x) - el.x)"
                :height="Math.abs((el.y2 ?? el.y) - el.y)"
                rx="3"
                class="el-lawn"
              />
              <polygon
                v-else
                :points="pointsToStr(el.points)"
                class="el-lawn"
              />
            </g>

            <g
              v-for="el in nonAreaElements"
              :key="el.id"
              :data-element-id="el.id"
              :class="{ selected: el.id === selectedId }"
              class="map-element"
            >
              <g
                v-if="el.type === 'space'"
                :transform="`rotate(${el.rotation ?? 0} ${el.x + (el.w ?? 0) / 2} ${el.y + (el.h ?? 0) / 2})`"
              >
                <rect
                  :x="el.x"
                  :y="el.y"
                  :width="el.w ?? SPACE_W"
                  :height="el.h ?? SPACE_H"
                  rx="3"
                  class="el-space"
                />
                <line
                  :x1="el.x + 2"
                  :y1="el.y + 4"
                  :x2="el.x + (el.w ?? 0) - 2"
                  :y2="el.y + 4"
                  class="el-space-line"
                />
                <text
                  :x="el.x + (el.w ?? 0) / 2"
                  :y="el.y + (el.h ?? 0) / 2 + 4"
                  text-anchor="middle"
                  class="el-space-label"
                >
                  {{ el.spaceCode || el.label || '' }}
                </text>
              </g>

              <g
                v-else-if="el.type === 'entrance'"
                :transform="`rotate(${el.rotation ?? 0} ${el.x + (el.w ?? 0) / 2} ${el.y + (el.h ?? 0) / 2})`"
              >
                <rect
                  :x="el.x"
                  :y="el.y"
                  :width="el.w ?? MARKER_W"
                  :height="el.h ?? MARKER_H"
                  rx="6"
                  class="el-entrance"
                />
                <text
                  :x="el.x + (el.w ?? 0) / 2"
                  :y="el.y + (el.h ?? 0) / 2 + 5"
                  text-anchor="middle"
                  class="el-marker-label"
                >
                  {{ el.laneName || t('lotMap.entrance') }}
                </text>
              </g>

              <g
                v-else-if="el.type === 'exit'"
                :transform="`rotate(${el.rotation ?? 0} ${el.x + (el.w ?? 0) / 2} ${el.y + (el.h ?? 0) / 2})`"
              >
                <rect
                  :x="el.x"
                  :y="el.y"
                  :width="el.w ?? MARKER_W"
                  :height="el.h ?? MARKER_H"
                  rx="6"
                  class="el-exit"
                />
                <text
                  :x="el.x + (el.w ?? 0) / 2"
                  :y="el.y + (el.h ?? 0) / 2 + 5"
                  text-anchor="middle"
                  class="el-marker-label"
                >
                  {{ el.laneName || t('lotMap.exit') }}
                </text>
              </g>

              <g v-else-if="el.type === 'tree'">
                <circle
                  :cx="el.x + (el.w ?? TREE_SIZE) / 2"
                  :cy="el.y + (el.h ?? TREE_SIZE) / 2"
                  :r="(el.w ?? TREE_SIZE) / 2"
                  class="el-tree-crown"
                />
                <ellipse
                  :cx="el.x + (el.w ?? TREE_SIZE) * 0.38"
                  :cy="el.y + (el.h ?? TREE_SIZE) * 0.34"
                  :rx="(el.w ?? TREE_SIZE) * 0.16"
                  :ry="(el.h ?? TREE_SIZE) * 0.12"
                  class="el-tree-shine"
                />
                <rect
                  :x="el.x + (el.w ?? TREE_SIZE) / 2 - 2"
                  :y="el.y + (el.h ?? TREE_SIZE) - 5"
                  :width="4"
                  :height="5"
                  class="el-tree-trunk"
                />
              </g>

              <g v-else-if="el.type === 'wall'">
                <line
                  :x1="el.x"
                  :y1="el.y"
                  :x2="el.x2 ?? el.x"
                  :y2="el.y2 ?? el.y"
                  class="el-wall"
                />
                <!-- 透明加宽命中线：便于点中细墙后整体平移（拖中段移动、拖端点改形） -->
                <line
                  :x1="el.x"
                  :y1="el.y"
                  :x2="el.x2 ?? el.x"
                  :y2="el.y2 ?? el.y"
                  class="el-wall-hit"
                />
              </g>

              <text
                v-else-if="el.type === 'text'"
                :x="el.x"
                :y="el.y"
                class="el-text"
              >
                {{ el.text }}
              </text>
            </g>

            <g v-if="wallStart && wallPreview">
              <line
                :x1="wallStart.x"
                :y1="wallStart.y"
                :x2="wallPreview.x"
                :y2="wallPreview.y"
                class="el-wall-preview"
              />
              <circle :cx="wallStart.x" :cy="wallStart.y" r="3" class="el-wall-point" />
            </g>

            <g v-if="buildingDrawStart && buildingDrawPreview">
              <rect
                :x="Math.min(buildingDrawStart.x, buildingDrawPreview.x)"
                :y="Math.min(buildingDrawStart.y, buildingDrawPreview.y)"
                :width="Math.abs(buildingDrawPreview.x - buildingDrawStart.x)"
                :height="Math.abs(buildingDrawPreview.y - buildingDrawStart.y)"
                rx="6"
                class="el-building-preview"
              />
            </g>

            <g v-if="areaDrawStart && areaDrawPreview">
              <rect
                :x="Math.min(areaDrawStart.x, areaDrawPreview.x)"
                :y="Math.min(areaDrawStart.y, areaDrawPreview.y)"
                :width="Math.abs(areaDrawPreview.x - areaDrawStart.x)"
                :height="Math.abs(areaDrawPreview.y - areaDrawStart.y)"
                rx="4"
                class="el-area-preview"
              />
            </g>

            <g v-if="lawnDrawStart && lawnDrawPreview">
              <rect
                :x="Math.min(lawnDrawStart.x, lawnDrawPreview.x)"
                :y="Math.min(lawnDrawStart.y, lawnDrawPreview.y)"
                :width="Math.abs(lawnDrawPreview.x - lawnDrawStart.x)"
                :height="Math.abs(lawnDrawPreview.y - lawnDrawStart.y)"
                rx="3"
                class="el-lawn-preview"
              />
            </g>

            <g v-if="polygonPoints.length > 0 && polygonPreview">
              <polygon
                :points="pointsToStr([...polygonPoints, polygonPreview])"
                class="el-area-preview"
              />
              <circle
                v-for="(pt, i) in polygonPoints"
                :key="i"
                :cx="pt.x"
                :cy="pt.y"
                r="4"
                class="el-polygon-point"
              />
            </g>

            <g v-if="editableHandles.length > 0" class="edit-handles">
              <circle
                v-for="h in editableHandles"
                :key="h.mode"
                :cx="h.x"
                :cy="h.y"
                :r="handleR"
                vector-effect="non-scaling-stroke"
                :data-element-id="selectedId"
                :data-handle-mode="h.mode"
                class="el-handle"
              />
            </g>
          </svg>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="empty">
      <p>{{ t('lotMap.loading') }}</p>
    </div>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 0.9rem;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.title-row h2 {
  margin: 0;
  font-size: 1.25rem;
}

.lot-code {
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 400;
}

.back-btn {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  padding: 0.4rem 0.8rem;
  font-weight: 600;
  cursor: pointer;
}

.back-btn:hover {
  background: #f4f6f5;
}

.editor {
  display: grid;
  gap: 0.6rem;
}

.toolbar {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: center;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 0.5rem 0.75rem;
}

.floor-tabs {
  display: flex;
  gap: 0.3rem;
  flex-wrap: wrap;
}

.floor-tab {
  border: 1px solid var(--border);
  border-radius: 7px;
  background: #fff;
  color: var(--text);
  padding: 0.42rem 0.9rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.floor-tab:hover {
  background: #f4f6f5;
}

.floor-tab.active {
  background: var(--accent);
  color: #fff;
  border-color: transparent;
}

.zoom-controls {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  margin-left: auto;
}

.zoom-btn {
  border: 1px solid var(--border);
  border-radius: 6px;
  background: #fff;
  color: var(--text);
  width: 1.8rem;
  height: 1.8rem;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.zoom-btn:hover {
  background: #f4f6f5;
}

.zoom-btn.reset {
  width: auto;
  padding: 0 0.6rem;
  font-size: 0.78rem;
  font-weight: 600;
}

.zoom-level {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--muted);
  min-width: 3rem;
  text-align: center;
}

.canvas.cursor-pan {
  cursor: grab;
}

.canvas.cursor-pan:active {
  cursor: grabbing;
}

.tool-group {
  display: flex;
  gap: 0.4rem;
  align-items: center;
}

.tool-group + .tool-group {
  padding-left: 1rem;
  border-left: 1px solid var(--border);
}

.tool-btn {
  border: 1px solid var(--border);
  border-radius: 7px;
  background: #fff;
  color: var(--text);
  padding: 0.42rem 0.8rem;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.tool-btn:hover:not(:disabled) {
  background: #f4f6f5;
}

.tool-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.tool-btn.active {
  background: var(--accent);
  color: #fff;
  border-color: transparent;
}

.tool-btn.primary {
  background: var(--accent);
  color: #fff;
  border-color: transparent;
}

.tool-btn.primary:disabled {
  opacity: 0.6;
}

.tool-btn.danger {
  color: var(--danger);
  border-color: #f0c8c8;
}

.tool-btn.danger:hover {
  background: #fdecec;
}

.angle-input {
  width: 3.5rem;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 0.42rem 0.3rem;
  font-size: 0.83rem;
  text-align: center;
  background: #fff;
  color: var(--text);
}

.angle-input::-webkit-inner-spin-button,
.angle-input::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.status-bar {
  display: flex;
  gap: 1.2rem;
  align-items: center;
  font-size: 0.83rem;
  color: var(--muted);
  padding: 0 0.3rem;
}

.status-bar .hint {
  flex: 1;
}

.status-msg.ok {
  color: var(--ok);
}

.status-msg:not(.ok) {
  color: var(--danger);
}

.pending-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  background: #eef5ff;
  border: 1px solid #b3d4ff;
  border-radius: 8px;
  padding: 0.5rem 0.9rem;
  font-size: 0.85rem;
  color: #1a56db;
}

.editor-body {
  display: flex;
  gap: 0.6rem;
  align-items: stretch;
}

.sidebar {
  width: 15rem;
  min-width: 15rem;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 0.5rem;
  border-bottom: 1px solid var(--border);
}

.space-search {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 0.4rem 0.6rem;
  background: #fff;
  color: var(--text);
  font-size: 0.83rem;
  box-sizing: border-box;
}

.sidebar-empty {
  padding: 2rem 1rem;
  text-align: center;
  color: var(--muted);
  font-size: 0.83rem;
}

.space-list {
  flex: 1;
  overflow-y: auto;
  max-height: 70vh;
  padding: 0.25rem 0;
}

.space-group {
  margin-bottom: 0.25rem;
}

.group-name {
  padding: 0.35rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  background: #f7faf8;
}

.space-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.35rem 0.75rem;
  cursor: pointer;
  transition: background 0.1s;
  gap: 0.5rem;
}

.space-item:hover {
  background: #f0f5f3;
}

.space-item.placed {
  opacity: 0.45;
  cursor: default;
}

.space-item.placed:hover {
  background: transparent;
}

.space-item.pending {
  background: #eef5ff;
  border-left: 3px solid #3b82f6;
  padding-left: calc(0.75rem - 3px);
}

.space-item.disabled .space-code {
  text-decoration: line-through;
  color: var(--muted);
}

.space-code {
  font-size: 0.83rem;
  font-weight: 600;
}

.space-check {
  color: var(--ok);
  font-weight: 700;
  font-size: 0.85rem;
}

.space-tag {
  font-size: 0.7rem;
  color: var(--muted);
  background: #f2f4f3;
  border-radius: 4px;
  padding: 0.05rem 0.3rem;
}

.area-section {
  border-bottom: 1px solid var(--border);
  padding: 0.5rem;
}

.section-title {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  margin-bottom: 0.35rem;
}

.area-list {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.area-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.35rem 0.5rem;
  cursor: pointer;
  border-radius: 5px;
  transition: background 0.1s;
  gap: 0.5rem;
}

.area-item:hover {
  background: #f0f5f3;
}

.area-item.placed {
  opacity: 0.45;
  cursor: default;
}

.area-item.placed:hover {
  background: transparent;
}

.area-item.pending {
  background: #eef5ff;
  border-left: 3px solid #3b82f6;
  padding-left: calc(0.5rem - 3px);
}

.area-name {
  font-size: 0.83rem;
  font-weight: 600;
}

.lane-section {
  border-bottom: 1px solid var(--border);
  padding: 0.5rem;
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.lane-list {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  overflow-y: auto;
  flex: 1;
  max-height: 70vh;
}

.lane-item {
  display: flex;
  align-items: center;
  padding: 0.4rem 0.5rem;
  cursor: pointer;
  border-radius: 5px;
  transition: background 0.1s;
  gap: 0.4rem;
}

.lane-item:hover {
  background: #f0f5f3;
}

.lane-item.placed {
  opacity: 0.45;
  cursor: default;
}

.lane-item.placed:hover {
  background: transparent;
}

.lane-item.pending {
  background: #eef5ff;
  border-left: 3px solid #3b82f6;
  padding-left: calc(0.5rem - 3px);
}

.lane-name {
  font-size: 0.83rem;
  font-weight: 600;
  flex: 1;
}

.lane-code {
  font-size: 0.72rem;
  color: var(--muted);
  background: #f2f4f3;
  border-radius: 4px;
  padding: 0.05rem 0.3rem;
}

.canvas-wrapper {
  flex: 1;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: var(--shadow);
}

.canvas {
  width: 100%;
  height: 70vh;
  min-height: 500px;
  display: block;
  cursor: crosshair;
}

.canvas.cursor-default {
  cursor: default;
}

.canvas.cursor-place {
  cursor: cell;
}

.map-element {
  cursor: pointer;
}

.compass {
  pointer-events: none;
}

.compass-north {
  fill: #c0392b;
}

.compass-south {
  fill: #bbb;
}

.compass-label {
  font-size: 13px;
  font-weight: 700;
  fill: #333;
  user-select: none;
}

.el-space {
  fill: #f2f3f5;
  stroke: #9aa0a8;
  stroke-width: 1.5;
}

.map-element.selected .el-space {
  stroke: #3b82f6;
  stroke-width: 2;
}

.el-space-line {
  stroke: #9aa0a8;
  stroke-width: 1;
}

.el-space-label {
  font-size: 10px;
  fill: #5a6068;
  user-select: none;
  pointer-events: none;
}

.el-entrance {
  fill: #f2f3f5;
  stroke: #9aa0a8;
  stroke-width: 2;
}

.map-element.selected .el-entrance {
  stroke: #3b82f6;
}

.el-exit {
  fill: #f2f3f5;
  stroke: #9aa0a8;
  stroke-width: 2;
}

.map-element.selected .el-exit {
  stroke: #3b82f6;
}

.el-marker-label {
  font-size: 13px;
  font-weight: 700;
  fill: #333;
  user-select: none;
  pointer-events: none;
}

.el-wall {
  stroke: #555;
  stroke-width: 4;
  stroke-linecap: round;
}

.map-element.selected .el-wall {
  stroke: #3b82f6;
}

.el-wall-preview {
  stroke: #3b82f6;
  stroke-width: 3;
  stroke-dasharray: 6 4;
  stroke-linecap: round;
}

.el-wall-point {
  fill: #3b82f6;
}

/* 透明加宽命中线：细墙也好点选，点中后可整体平移 */
.el-wall-hit {
  stroke: transparent;
  stroke-width: 16;
  stroke-linecap: round;
  fill: none;
  pointer-events: stroke;
  cursor: move;
}

.edit-handles {
  cursor: move;
}

.el-handle {
  fill: #fff;
  stroke: #3b82f6;
  stroke-width: 2;
  cursor: move;
  transition: fill 0.12s ease;
}
.el-handle:hover {
  fill: #93c5fd;
}

.status-msg.drag-info {
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.el-area {
  fill: rgba(123, 168, 142, 0.15);
  stroke: #7ba88e;
  stroke-width: 1.5;
  stroke-dasharray: 8 4;
}

.map-element.selected .el-area {
  stroke: #3b82f6;
  stroke-width: 2;
}

.el-area-label {
  font-size: 14px;
  font-weight: 700;
  fill: #4a6b58;
  user-select: none;
  pointer-events: none;
}

.el-area-preview {
  fill: rgba(59, 130, 246, 0.1);
  stroke: #3b82f6;
  stroke-width: 1.5;
  stroke-dasharray: 6 4;
}

.el-polygon-point {
  fill: #3b82f6;
  stroke: #fff;
  stroke-width: 1.5;
}

.el-building {
  fill: rgba(120, 120, 130, 0.18);
  stroke: #5a5a66;
  stroke-width: 2.5;
}

.map-element.selected .el-building {
  stroke: #3b82f6;
  stroke-width: 3;
}

.el-building-label {
  font-size: 15px;
  font-weight: 700;
  fill: #3a3a44;
  user-select: none;
  pointer-events: none;
}

.el-building-preview {
  fill: rgba(90, 90, 102, 0.12);
  stroke: #5a5a66;
  stroke-width: 2;
  stroke-dasharray: 8 4;
}

.el-tree-crown {
  fill: url(#tree-grad);
  stroke: #3f7d4e;
  stroke-width: 1;
}

.map-element.selected .el-tree-crown {
  stroke: #3b82f6;
  stroke-width: 2;
}

.el-tree-shine {
  fill: rgba(255, 255, 255, 0.45);
  pointer-events: none;
}

.el-tree-trunk {
  fill: #7a5a3a;
}

.el-lawn {
  fill: url(#lawn-texture);
  stroke: #4f8a5e;
  stroke-width: 1.5;
}

.map-element.selected .el-lawn {
  stroke: #3b82f6;
  stroke-width: 2;
}

.el-lawn-preview {
  fill: rgba(91, 154, 106, 0.15);
  stroke: #4f8a5e;
  stroke-width: 1.5;
  stroke-dasharray: 6 4;
}

.mode-switch {
  display: flex;
  gap: 0.2rem;
}

.mode-btn {
  border: 1px solid var(--border);
  border-radius: 5px;
  background: #fff;
  color: var(--text);
  padding: 0.3rem 0.6rem;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.mode-btn:hover {
  background: #f4f6f5;
}

.mode-btn.active {
  background: #1a56db;
  color: #fff;
  border-color: transparent;
}

.el-text {
  font-size: 14px;
  font-weight: 600;
  fill: #333;
  user-select: none;
}

.map-element.selected .el-text {
  fill: #3b82f6;
}

.empty {
  padding: 3rem 1.5rem;
  text-align: center;
  color: var(--muted);
}

.banner.error {
  margin: 0;
  padding: 0.65rem 0.9rem;
  border-radius: 8px;
  color: var(--danger);
  background: #fdecec;
}
</style>

<!-- 参考项目把这些设计令牌放在全局 css；本仓库主题变量不同，这里就近补全编辑器所用令牌 -->
<style>
:root {
  --surface: #ffffff;
  --text: #12231e;
  --muted: #66756f;
  --border: #d8e2dd;
  --accent: #0f766e;
  --danger: #b42318;
  --ok: #067647;
  --shadow: 0 1px 2px rgb(16 33 29 / 6%);
}
</style>
