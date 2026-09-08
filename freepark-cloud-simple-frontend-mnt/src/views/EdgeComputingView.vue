<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

const d: BiDict = {
  loading: { 'zh-CN': '正在加载配置…', en: 'Loading configuration…' },
  description: {
    'zh-CN':
      '云端通过 MQTT 与停车系统及边缘计算服务通信：订阅停车系统上报的数据，并按周期将配置发布给其它边缘计算服务，由其同步到本地。',
    en: 'The cloud communicates via MQTT with parking systems and edge services: it subscribes to reported data and periodically publishes configuration for edge services to sync locally.'
  },
  scope: {
    'zh-CN': '仅超级管理员可修改边缘计算配置。',
    en: 'Only super administrators can modify edge computing configuration.'
  },
  sectionConnection: { 'zh-CN': '连接参数', en: 'Connection' },
  sectionTopics: { 'zh-CN': '主题与周期', en: 'Topics & Interval' },
  sectionTopicsHint: {
    'zh-CN': '订阅主题接收停车系统上报的数据；配置同步发布主题填写“前缀”，云端会按边缘节点自动拼接出专属主题“{前缀}/{节点编号}”，并把该节点管辖的全部车场配置打包定时下发。',
    en: 'The subscribe topic receives data reported by parking systems. The config sync publish topic is a prefix: the cloud appends "/<node code>" and delivers that node\u2019s managed lot configurations on schedule.'
  },
  enabled: { 'zh-CN': '启用边缘计算接入', en: 'Enable edge computing' },
  enabledHint: {
    'zh-CN': '开启后，云端将订阅停车系统上报的数据，并按“配置同步周期”把各边缘节点管辖的车场配置打包，通过“{前缀}/{节点编号}”专属主题下发到对应边缘盒子。',
    en: 'When enabled, the cloud subscribes to parking system reports and publishes each edge node\u2019s managed lot configurations to its own "{prefix}/{node code}" topic on the sync interval.'
  },
  brokerHost: { 'zh-CN': 'Broker 地址', en: 'Broker host' },
  brokerHostPlaceholder: { 'zh-CN': '如 127.0.0.1 或 mqtt.example.com', en: 'e.g. 127.0.0.1 or mqtt.example.com' },
  brokerPort: { 'zh-CN': 'Broker 端口', en: 'Broker port' },
  clientId: { 'zh-CN': 'Client ID', en: 'Client ID' },
  clientIdPlaceholder: { 'zh-CN': '客户端唯一标识', en: 'Unique client identifier' },
  username: { 'zh-CN': '用户名', en: 'Username' },
  usernamePlaceholder: { 'zh-CN': '可选', en: 'Optional' },
  password: { 'zh-CN': '密码', en: 'Password' },
  passwordPlaceholder: { 'zh-CN': '留空表示保持不变', en: 'Leave blank to keep unchanged' },
  reportSubscribeTopic: { 'zh-CN': '上报数据订阅主题', en: 'Report data subscribe topic' },
  reportSubscribeTopicPlaceholder: { 'zh-CN': '如 parking/report/#（与本地“上报主题前缀”一致，末尾加 /#）', en: 'e.g. parking/report/# (match the local report topic prefix, append /#)' },
  configSyncPublishTopic: { 'zh-CN': '配置同步发布主题前缀', en: 'Config sync publish topic prefix' },
  configSyncPublishTopicPlaceholder: { 'zh-CN': '如 cloud/config/sync（云端自动拼接 /节点编号）', en: 'e.g. cloud/config/sync (node code appended)' },
  configSyncPublishTopicHint: {
    'zh-CN': '不允许包含空格或 MQTT 通配符（# / +），末尾的 / 会被自动去掉。',
    en: 'Whitespace and MQTT wildcards (# / +) are not allowed; a trailing / is trimmed automatically.'
  },
  sectionHeartbeat: { 'zh-CN': '心跳监控', en: 'Heartbeat Monitoring' },
  sectionHeartbeatHint: {
    'zh-CN': '开启后，云端订阅各边缘节点上行的心跳（主题末段即节点编号）：超过“离线判定阈值”未收到心跳即判定该节点离线，并在“边缘监控”页展示节点及其管辖车场的在线状态。',
    en: 'When enabled, the cloud subscribes to heartbeats from edge nodes (the topic\u2019s last segment is the node code): a node is flagged offline if no heartbeat arrives within the offline threshold, and its status is shown on the Edge Monitoring page.'
  },
  heartbeatSubscribeTopic: { 'zh-CN': '心跳订阅主题', en: 'Heartbeat subscribe topic' },
  heartbeatSubscribeTopicPlaceholder: {
    'zh-CN': '如 parking/heartbeat/#；留空表示关闭心跳监控',
    en: 'e.g. parking/heartbeat/#; blank disables monitoring'
  },
  heartbeatSubscribeTopicHint: {
    'zh-CN':
      '与本地边缘节点填写的“心跳主题路径”一致，并在末尾加 /# 通配：本地填 parking/heartbeat 时此处填 parking/heartbeat/#，即可监控该路径下的所有节点。',
    en: 'Keep it identical to the local node\u2019s “Heartbeat topic path”, appending /# so it covers every node: if the node fills parking/heartbeat locally, fill parking/heartbeat/# here.'
  },
  heartbeatOfflineSeconds: { 'zh-CN': '离线判定阈值（秒）', en: 'Offline threshold (s)' },
  heartbeatOfflineSecondsHint: {
    'zh-CN': '支持 MQTT 通配符；超过阈值未收到某节点心跳即判定该节点离线。',
    en: 'MQTT wildcards are supported; a node is offline after this many seconds without a heartbeat.'
  },
  qos: { 'zh-CN': 'QoS', en: 'QoS' },
  configSyncInterval: { 'zh-CN': '配置同步周期（秒）', en: 'Config sync interval (s)' },
  keepAlive: { 'zh-CN': '保活间隔（秒）', en: 'Keep-alive (s)' },
  save: { 'zh-CN': '保存配置', en: 'Save configuration' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  test: { 'zh-CN': '测试连接', en: 'Test connection' },
  testing: { 'zh-CN': '测试中…', en: 'Testing…' },
  saved: { 'zh-CN': '配置已保存', en: 'Configuration saved' },
  testSuccess: { 'zh-CN': '连接成功', en: 'Connection succeeded' },
  loadFailed: { 'zh-CN': '加载配置失败，请重试', en: 'Failed to load configuration. Try again.' },
  lastUpdated: { 'zh-CN': '最近更新', en: 'Last updated' }
}

const { t } = useBiText(d)

interface EdgeMqttConfigView {
  enabled: boolean
  brokerHost: string
  brokerPort: number
  clientId: string
  username: string | null
  reportSubscribeTopic: string | null
  configSyncPublishTopic: string | null
  heartbeatSubscribeTopic: string | null
  heartbeatOfflineSeconds: number
  qos: number
  configSyncIntervalSeconds: number
  keepAliveSeconds: number
  updatedAt: string
}

interface SavePayload {
  enabled: boolean
  brokerHost: string
  brokerPort: number
  clientId: string
  username: string | null
  password: string | null
  reportSubscribeTopic: string | null
  configSyncPublishTopic: string | null
  heartbeatSubscribeTopic: string | null
  heartbeatOfflineSeconds: number
  qos: number
  configSyncIntervalSeconds: number
  keepAliveSeconds: number
}

const loading = ref(true)
const saving = ref(false)
const testing = ref(false)

const enabled = ref(false)
const brokerHost = ref('')
const brokerPort = ref(1883)
const clientId = ref('')
const username = ref('')
const password = ref('')
const reportSubscribeTopic = ref('')
const configSyncPublishTopic = ref('')
const heartbeatSubscribeTopic = ref('')
const heartbeatOfflineSeconds = ref(90)
const qos = ref(1)
const configSyncIntervalSeconds = ref(60)
const keepAliveSeconds = ref(60)
const updatedAt = ref('')

const qosOptions = [0, 1, 2]

// 留空时自动补用的默认主题参数
const DEFAULT_REPORT_TOPIC = 'parking/report/#'
const DEFAULT_CONFIG_SYNC_PREFIX = 'cloud/config/sync'

function emptyToNull(value: string): string | null {
  return value.trim() === '' ? null : value.trim()
}

/** 主题/前缀为空时补默认值，保证开箱即用 */
function fillTopicDefault(value: string, fallback: string): string {
  return emptyToNull(value) ?? fallback
}

function buildPayload(): SavePayload {
  return {
    enabled: enabled.value,
    brokerHost: brokerHost.value.trim(),
    brokerPort: brokerPort.value,
    clientId: clientId.value.trim(),
    username: emptyToNull(username.value),
    // 密码不回显且后端按“空串保持不变”处理，这里始终携带表单值即可
    password: password.value,
    reportSubscribeTopic: fillTopicDefault(reportSubscribeTopic.value, DEFAULT_REPORT_TOPIC),
    configSyncPublishTopic: fillTopicDefault(configSyncPublishTopic.value, DEFAULT_CONFIG_SYNC_PREFIX),
    // 心跳订阅主题无默认值：留空（null）即关闭心跳监控
    heartbeatSubscribeTopic: emptyToNull(heartbeatSubscribeTopic.value),
    heartbeatOfflineSeconds: heartbeatOfflineSeconds.value,
    qos: qos.value,
    configSyncIntervalSeconds: configSyncIntervalSeconds.value,
    keepAliveSeconds: keepAliveSeconds.value
  }
}

function applyView(view: EdgeMqttConfigView): void {
  enabled.value = view.enabled
  brokerHost.value = view.brokerHost
  brokerPort.value = view.brokerPort
  clientId.value = view.clientId
  username.value = view.username ?? ''
  reportSubscribeTopic.value = fillTopicDefault(view.reportSubscribeTopic ?? '', DEFAULT_REPORT_TOPIC)
  configSyncPublishTopic.value = fillTopicDefault(view.configSyncPublishTopic ?? '', DEFAULT_CONFIG_SYNC_PREFIX)
  heartbeatSubscribeTopic.value = view.heartbeatSubscribeTopic ?? ''
  heartbeatOfflineSeconds.value = view.heartbeatOfflineSeconds
  qos.value = view.qos
  configSyncIntervalSeconds.value = view.configSyncIntervalSeconds
  keepAliveSeconds.value = view.keepAliveSeconds
  updatedAt.value = view.updatedAt
}

/**
 * 后端返回的本地挂钟时间文本（如 2026-09-07T03:01:02），
 * 该时间在站点时区下解释，因此按纯文本格式化，不经过浏览器本地时区换算。
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

async function loadConfig(): Promise<void> {
  loading.value = true
  try {
    const view = await request.get<never, EdgeMqttConfigView>('/system/edge-mqtt')
    applyView(view)
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('loadFailed'))
  } finally {
    loading.value = false
  }
}

async function handleSave(): Promise<void> {
  saving.value = true
  try {
    const view = await request.put<never, EdgeMqttConfigView>(
      '/system/edge-mqtt',
      buildPayload()
    )
    password.value = ''
    applyView(view)
    ElMessage.success(t('saved'))
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('loadFailed'))
  } finally {
    saving.value = false
  }
}

async function handleTest(): Promise<void> {
  testing.value = true
  try {
    await request.post<never, void>('/system/edge-mqtt/test', buildPayload())
    ElMessage.success(t('testSuccess'))
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('loadFailed'))
  } finally {
    testing.value = false
  }
}

onMounted(loadConfig)
</script>

<template>
  <section class="edge-page">
    <el-card class="panel" shadow="never" v-loading="loading">
      <el-form label-position="top" class="edge-form" @submit.prevent="handleSave">
        <el-alert type="info" :closable="false" class="scope-alert" show-icon>
          <span>{{ t('description') }}</span>
        </el-alert>

        <h3 class="group-title">{{ t('sectionConnection') }}</h3>
        <el-form-item :label="t('enabled')">
          <el-switch v-model="enabled" />
          <span class="switch-hint">{{ t('enabledHint') }}</span>
        </el-form-item>
        <div class="field-grid">
          <el-form-item :label="t('brokerHost')" required>
            <el-input
              v-model="brokerHost"
              :placeholder="t('brokerHostPlaceholder')"
              maxlength="255"
              clearable
              class="field"
            />
          </el-form-item>
          <el-form-item :label="t('brokerPort')" required>
            <el-input-number
              v-model="brokerPort"
              :min="1"
              :max="65535"
              controls-position="right"
              class="field"
            />
          </el-form-item>
          <el-form-item :label="t('clientId')" required>
            <el-input
              v-model="clientId"
              :placeholder="t('clientIdPlaceholder')"
              maxlength="128"
              clearable
              class="field"
            />
          </el-form-item>
          <el-form-item :label="t('username')">
            <el-input
              v-model="username"
              :placeholder="t('usernamePlaceholder')"
              maxlength="128"
              clearable
              class="field"
            />
          </el-form-item>
          <el-form-item :label="t('password')">
            <el-input
              v-model="password"
              type="password"
              show-password
              :placeholder="t('passwordPlaceholder')"
              maxlength="255"
              clearable
              autocomplete="new-password"
              class="field"
            />
          </el-form-item>
          <el-form-item :label="t('keepAlive')" required>
            <el-input-number
              v-model="keepAliveSeconds"
              :min="0"
              :max="65535"
              controls-position="right"
              class="field"
            />
          </el-form-item>
        </div>

        <el-divider />

        <h3 class="group-title">{{ t('sectionTopics') }}</h3>
        <p class="section-hint">{{ t('sectionTopicsHint') }}</p>
        <div class="field-grid">
          <el-form-item :label="t('reportSubscribeTopic')">
            <el-input
              v-model="reportSubscribeTopic"
              :placeholder="t('reportSubscribeTopicPlaceholder')"
              maxlength="255"
              clearable
              class="field"
            />
          </el-form-item>
          <el-form-item :label="t('configSyncPublishTopic')">
            <el-input
              v-model="configSyncPublishTopic"
              :placeholder="t('configSyncPublishTopicPlaceholder')"
              maxlength="255"
              clearable
              class="field"
            />
            <div class="field-hint">{{ t('configSyncPublishTopicHint') }}</div>
          </el-form-item>
          <el-form-item :label="t('qos')" required>
            <el-select v-model="qos" class="field">
              <el-option v-for="q in qosOptions" :key="q" :value="q" :label="String(q)" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('configSyncInterval')" required>
            <el-input-number
              v-model="configSyncIntervalSeconds"
              :min="1"
              :max="604800"
              controls-position="right"
              class="field"
            />
          </el-form-item>
        </div>

        <el-divider />

        <h3 class="group-title">{{ t('sectionHeartbeat') }}</h3>
        <p class="section-hint">{{ t('sectionHeartbeatHint') }}</p>
        <div class="field-grid">
          <el-form-item :label="t('heartbeatSubscribeTopic')" class="wide-item">
            <el-input
              v-model="heartbeatSubscribeTopic"
              :placeholder="t('heartbeatSubscribeTopicPlaceholder')"
              maxlength="255"
              clearable
              class="field"
            />
            <div class="field-hint">{{ t('heartbeatSubscribeTopicHint') }}</div>
          </el-form-item>
          <el-form-item :label="t('heartbeatOfflineSeconds')" required>
            <el-input-number
              v-model="heartbeatOfflineSeconds"
              :min="5"
              :max="86400"
              controls-position="right"
              class="field"
            />
            <div class="field-hint">{{ t('heartbeatOfflineSecondsHint') }}</div>
          </el-form-item>
        </div>

        <div class="form-footer">
          <div class="footer-meta">
            <span v-if="updatedAt" class="meta">
              {{ t('lastUpdated') }}: {{ formatTime(updatedAt) }}
            </span>
          </div>
          <div class="footer-actions">
            <el-button :loading="testing" :disabled="saving" @click="handleTest">
              {{ testing ? t('testing') : t('test') }}
            </el-button>
            <el-button type="primary" :loading="saving" native-type="submit">
              {{ saving ? t('saving') : t('save') }}
            </el-button>
          </div>
        </div>
      </el-form>
    </el-card>
  </section>
</template>

<style scoped>
.edge-page {
  animation: fade-up 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.panel {
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-soft);
}

.edge-form {
  max-width: 960px;
}

.scope-alert {
  margin-bottom: 14px;
}

.group-title {
  margin: 6px 0 14px;
  font-size: 1rem;
}

.section-hint {
  margin: -6px 0 14px;
  font-size: 0.8rem;
  line-height: 1.6;
  color: var(--fp-muted);
}

.switch-hint {
  margin-left: 10px;
  font-size: 0.8rem;
  color: var(--fp-muted);
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 20px;
}

.wide-item {
  grid-column: span 2;
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

.footer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
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

@media (max-width: 900px) {
  .field-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
