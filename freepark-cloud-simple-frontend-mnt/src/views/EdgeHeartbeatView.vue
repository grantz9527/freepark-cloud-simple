<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

/** 自动轮询间隔：与心跳离线判定的最小阈值同量级，保证状态及时翻转 */
const REFRESH_MS = 5000

const d: BiDict = {
  pageHint: {
    'zh-CN':
      '展示各车场上行心跳的实时在线状态；云端按“最近一次心跳到达时间”与配置的离线阈值动态判定，无需手动触发。',
    en: 'Live online status of heartbeats reported by lots. The cloud evaluates each lot against the configured offline threshold from its last heartbeat.'
  },
  statOnline: { 'zh-CN': '在线车场', en: 'Online lots' },
  statOffline: { 'zh-CN': '离线车场', en: 'Offline lots' },
  statUnknown: { 'zh-CN': '未知车场', en: 'Unknown lots' },
  statTotal: { 'zh-CN': '目标车场', en: 'Target lots' },
  colParkCode: { 'zh-CN': '车场编码', en: 'Lot code' },
  colParkName: { 'zh-CN': '车场名称', en: 'Lot name' },
  colStatus: { 'zh-CN': '状态', en: 'Status' },
  colLastSeen: { 'zh-CN': '最近心跳', en: 'Last heartbeat' },
  online: { 'zh-CN': '在线', en: 'Online' },
  offline: { 'zh-CN': '离线', en: 'Offline' },
  unknown: { 'zh-CN': '未知', en: 'Unknown' },
  neverSeen: { 'zh-CN': '从未收到', en: 'Never received' },
  monitorOn: {
    'zh-CN': '云端已订阅“{topic}”，超过 {seconds} 秒未收到某车场心跳即判定离线。',
    en: 'Subscribed to "{topic}". A lot is offline after {seconds}s without a heartbeat.'
  },
  monitorOffTitle: { 'zh-CN': '未启用心跳监控', en: 'Heartbeat monitoring is off' },
  monitorOffDesc: {
    'zh-CN': '前往「边缘计算配置」填写“心跳订阅主题”并开启边缘计算接入后，本页才会展示车场在线状态。',
    en: 'Go to Edge Computing, fill in the heartbeat subscribe topic, and enable edge computing to monitor lot status here.'
  },
  goConfig: { 'zh-CN': '前往配置', en: 'Go to config' },
  connDownTitle: { 'zh-CN': '云端连接已断开', en: 'Cloud connection is down' },
  connDownDesc: {
    'zh-CN': '云端当前与 Broker 无连接，收不到任何车场心跳。请检查边缘计算配置。',
    en: 'The cloud has no live connection to the broker, so heartbeats cannot be received. Check the edge computing configuration.'
  },
  threshold: { 'zh-CN': '离线阈值', en: 'Offline threshold' },
  secondsUnit: { 'zh-CN': '秒', en: 's' },
  refresh: { 'zh-CN': '刷新', en: 'Refresh' },
  lastUpdated: { 'zh-CN': '最近刷新', en: 'Last refresh' },
  autoTip: {
    'zh-CN': '每 {n} 秒自动刷新',
    en: 'Auto refreshes every {n}s'
  },
  staleTip: {
    'zh-CN': '自动刷新失败，当前展示的是最近一次成功数据',
    en: 'Auto refresh failed; showing the last successful snapshot'
  },
  emptyLots: { 'zh-CN': '暂无已启用的车场', en: 'No enabled lots yet' },
  emptyLotsHint: {
    'zh-CN': '在车场管理中启用至少一个车场后，此处将展示其心跳状态。',
    en: 'Enable at least one lot under Lot Management to see its heartbeat status here.'
  },
  loadFailed: { 'zh-CN': '加载失败，请重试', en: 'Failed to load. Try again.' },
  retry: { 'zh-CN': '重新加载', en: 'Reload' }
}

const { t, locale } = useBiText(d)
const router = useRouter()

type LotStatus = 'online' | 'offline' | 'unknown'

interface LotItem {
  parkCode: string
  parkName: string | null
  status: LotStatus
  lastSeenAt: string | null
}

interface HeartbeatStatus {
  connectionUp: boolean
  monitoring: boolean
  heartbeatSubscribeTopic: string | null
  offlineSeconds: number
  onlineCount: number
  offlineCount: number
  unknownCount: number
  lots: LotItem[]
}

const status = ref<HeartbeatStatus | null>(null)
const loading = ref(false)
const loadFailed = ref(false)
const pollStale = ref(false)
const nowMs = ref(Date.now())
const updatedAt = ref<number | null>(null)

let timer: ReturnType<typeof setInterval> | undefined
let seq = 0

const totalCount = computed(() => {
  const s = status.value
  return s ? s.onlineCount + s.offlineCount + s.unknownCount : 0
})

const statusLine = computed(() => {
  const s = status.value
  if (!s || !s.monitoring) {
    return ''
  }
  return t('monitorOn')
    .replace('{topic}', s.heartbeatSubscribeTopic ?? '')
    .replace('{seconds}', String(s.offlineSeconds))
})

const autoTip = computed(() => t('autoTip').replace('{n}', String(REFRESH_MS / 1000)))

function tagOf(row: LotItem): { label: string; type: 'success' | 'danger' | 'info' } {
  if (row.status === 'online') {
    return { label: t('online'), type: 'success' }
  }
  if (row.status === 'offline') {
    return { label: t('offline'), type: 'danger' }
  }
  return { label: t('unknown'), type: 'info' }
}

function formatAbs(date: Date): string {
  const pad = (n: number): string => String(n).padStart(2, '0')
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  )
}

/** 以最近一次心跳时刻为准的相对时间文本 */
function relativeText(ms: number): string {
  const seconds = Math.max(0, Math.floor((nowMs.value - ms) / 1000))
  const isEn = locale.value === 'en'
  if (seconds < 10) {
    return isEn ? 'just now' : '刚刚'
  }
  if (seconds < 60) {
    return isEn ? `${seconds}s ago` : `${seconds} 秒前`
  }
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) {
    return isEn ? `${minutes}m ago` : `${minutes} 分钟前`
  }
  const hours = Math.floor(minutes / 60)
  if (hours < 24) {
    return isEn ? `${hours}h ago` : `${hours} 小时前`
  }
  const days = Math.floor(hours / 24)
  return isEn ? `${days}d ago` : `${days} 天前`
}

function lastSeenCell(row: LotItem): { text: string; absolute: string } | null {
  if (!row.lastSeenAt) {
    return null
  }
  const ms = Date.parse(row.lastSeenAt)
  if (Number.isNaN(ms)) {
    return null
  }
  return { text: relativeText(ms), absolute: formatAbs(new Date(ms)) }
}

function goConfig() {
  router.push('/system/edge-computing')
}

async function fetchStatus(manual = false): Promise<void> {
  const id = ++seq
  if (manual || !status.value) {
    loading.value = true
  }
  try {
    const data = await request.get<never, HeartbeatStatus>('/system/edge-heartbeat/status')
    if (id !== seq) {
      return
    }
    status.value = data
    loadFailed.value = false
    pollStale.value = false
    updatedAt.value = Date.now()
  } catch (error) {
    if (id !== seq) {
      return
    }
    pollStale.value = Boolean(status.value)
    if (manual || !status.value) {
      loadFailed.value = true
      ElMessage.error(error instanceof Error && error.message ? error.message : t('loadFailed'))
    }
  } finally {
    if (id === seq) {
      loading.value = false
    }
  }
}

function handleRefresh() {
  nowMs.value = Date.now()
  void fetchStatus(true)
}

onMounted(() => {
  nowMs.value = Date.now()
  void fetchStatus(true)
  timer = setInterval(() => {
    nowMs.value = Date.now()
    void fetchStatus(false)
  }, REFRESH_MS)
})

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = undefined
  }
})
</script>

<template>
  <section class="hb-page">
    <el-card class="panel" shadow="never">
      <template #header>
        <div class="panel-head">
          <span>{{ t('pageHint') }}</span>
          <el-button size="small" :loading="loading" @click="handleRefresh">
            {{ t('refresh') }}
          </el-button>
        </div>
      </template>

      <div v-loading="loading && !status" class="body">
        <!-- 未启用心跳监控 -->
        <el-alert
          v-if="status && !status.monitoring"
          type="warning"
          :closable="false"
          show-icon
          class="state-alert"
        >
          <template #title>
            <span>{{ t('monitorOffTitle') }}</span>
          </template>
          <p class="alert-body">{{ t('monitorOffDesc') }}</p>
          <el-button size="small" @click="goConfig">{{ t('goConfig') }}</el-button>
        </el-alert>

        <!-- 启用中但连接断开 -->
        <el-alert
          v-else-if="status && status.monitoring && !status.connectionUp"
          type="error"
          :closable="false"
          show-icon
          class="state-alert"
        >
          <template #title>
            <span>{{ t('connDownTitle') }}</span>
          </template>
          <p class="alert-body">{{ t('connDownDesc') }}</p>
        </el-alert>

        <!-- 正常运行中的状态说明 -->
        <el-alert
          v-else-if="statusLine"
          type="info"
          :closable="false"
          show-icon
          class="state-alert"
        >
          <p class="alert-body">{{ statusLine }}</p>
        </el-alert>

        <!-- 自动刷新失败提示（保留最近一次快照） -->
        <el-alert v-if="pollStale" type="warning" :closable="false" class="stale-alert">
          <p class="alert-body">{{ t('staleTip') }}</p>
        </el-alert>

        <!-- 首次加载失败 -->
        <el-empty
          v-if="loadFailed && !status"
          :description="t('loadFailed')"
          :image-size="80"
        >
          <el-button type="primary" @click="handleRefresh">{{ t('retry') }}</el-button>
        </el-empty>

        <template v-else-if="status">
          <div class="stat-row">
            <div class="stat-card online">
              <span class="stat-label">{{ t('statOnline') }}</span>
              <span class="stat-value">{{ status.onlineCount }}</span>
            </div>
            <div class="stat-card offline">
              <span class="stat-label">{{ t('statOffline') }}</span>
              <span class="stat-value">{{ status.offlineCount }}</span>
            </div>
            <div class="stat-card unknown">
              <span class="stat-label">{{ t('statUnknown') }}</span>
              <span class="stat-value">{{ status.unknownCount }}</span>
            </div>
            <div class="stat-card total">
              <span class="stat-label">{{ t('statTotal') }}</span>
              <span class="stat-value">{{ totalCount }}</span>
            </div>
          </div>

          <el-table :data="status.lots" stripe class="lot-table">
            <el-table-column prop="parkCode" :label="t('colParkCode')" min-width="120" />
            <el-table-column :label="t('colParkName')" min-width="160">
              <template #default="{ row }">{{ (row as LotItem).parkName || '—' }}</template>
            </el-table-column>
            <el-table-column :label="t('colStatus')" width="120">
              <template #default="{ row }">
                <el-tag :type="tagOf(row as LotItem).type" effect="light" round>
                  {{ tagOf(row as LotItem).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('colLastSeen')" min-width="220">
              <template #default="{ row }">
                <template v-if="lastSeenCell(row as LotItem)">
                  <span class="ago">{{ lastSeenCell(row as LotItem)!.text }}</span>
                  <span class="wall">· {{ lastSeenCell(row as LotItem)!.absolute }}</span>
                </template>
                <span v-else class="never">{{ t('neverSeen') }}</span>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty :description="t('emptyLots')" :image-size="70">
                <p class="empty-hint">{{ t('emptyLotsHint') }}</p>
              </el-empty>
            </template>
          </el-table>

          <div class="table-meta">
            <span>{{ autoTip }}</span>
            <span v-if="updatedAt">{{ t('lastUpdated') }}: {{ formatAbs(new Date(updatedAt)) }}</span>
          </div>
        </template>
      </div>
    </el-card>
  </section>
</template>

<style scoped>
.hb-page {
  animation: fade-up 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.panel {
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-soft);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 0.85rem;
  color: var(--fp-muted);
  line-height: 1.6;
}

.body {
  min-height: 220px;
}

.state-alert {
  margin-bottom: 14px;
}

.state-alert .el-button {
  margin-top: 8px;
}

.alert-body {
  margin: 2px 0 0;
  line-height: 1.6;
}

.stale-alert {
  margin: -6px 0 14px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 16px 18px;
  border-radius: var(--fp-radius-sm);
  background: var(--fp-surface);
  border: 1px solid var(--fp-line);
}

.stat-card .stat-label {
  font-size: 0.8rem;
  color: var(--fp-muted);
}

.stat-card .stat-value {
  font-family: var(--fp-font-display);
  font-size: 1.8rem;
  font-weight: 600;
  line-height: 1;
}

.stat-card.online .stat-value {
  color: var(--el-color-success, #67c23a);
}

.stat-card.offline .stat-value {
  color: var(--fp-danger);
}

.stat-card.unknown .stat-value {
  color: var(--fp-muted);
}

.stat-card.total .stat-value {
  color: var(--fp-teal);
}

.lot-table {
  width: 100%;
}

.ago {
  font-weight: 600;
  color: var(--fp-ink);
}

.wall {
  font-size: 0.8rem;
  color: var(--fp-muted);
}

.never {
  color: var(--fp-muted);
}

.empty-hint {
  margin: 6px 0 0;
  font-size: 0.8rem;
  color: var(--fp-muted);
}

.table-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  margin-top: 14px;
  font-size: 0.78rem;
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

@media (max-width: 720px) {
  .stat-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
