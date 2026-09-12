<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, type UploadFile, type UploadInstance } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

const d: BiDict = {
  loading: { 'zh-CN': '正在加载配置…', en: 'Loading configuration…' },
  scopeAlert: {
    'zh-CN':
      '配置微信支付所需的商户信息与公众号授权信息。字段可分次填写保存；是否开放微信支付由「系统配置 → 收费方式」控制，正式收款前请确保商户与公众号参数齐全。',
    en: 'Configure the merchant and Official Account credentials for WeChat Pay. Fields can be saved gradually. Whether WeChat Pay is open is controlled by System Settings → Payment Methods; complete all credentials before taking live payments.'
  },
  scope: {
    'zh-CN': '仅超级管理员可修改微信支付配置。',
    en: 'Only super administrators can modify the WeChat Pay configuration.'
  },
  sectionMerchant: { 'zh-CN': '商户参数', en: 'Merchant Parameters' },
  sectionMerchantHint: {
    'zh-CN': '用于发起收款与签名的微信支付商户凭据，在微信商户平台申请。可分次填写保存。',
    en: 'WeChat Pay merchant credentials used to create and sign payments. Apply via the WeChat Merchant Platform. Fields can be saved gradually.'
  },
  mchId: { 'zh-CN': '商户号', en: 'Merchant ID' },
  mchIdPlaceholder: { 'zh-CN': '微信支付商户号（纯数字）', en: 'WeChat Pay merchant ID (digits only)' },
  mchIdHint: { 'zh-CN': '在微信商户平台“账户中心 → 商户信息”中查看。', en: 'Find it under Account Center → Merchant Info on the WeChat Merchant Platform.' },
  mchName: { 'zh-CN': '商户名称', en: 'Merchant name' },
  mchNamePlaceholder: { 'zh-CN': '商户平台登记的商户名称', en: 'Merchant name registered on the platform' },
  mchNameHint: {
    'zh-CN': '与微信商户平台「账户中心 → 商户信息」中的商户名称保持一致即可。',
    en: 'Match the merchant name shown under Account Center → Merchant Info.'
  },
  mchCertSerialNo: { 'zh-CN': '商户序列号', en: 'Merchant serial number' },
  mchCertSerialNoPlaceholder: {
    'zh-CN': '商户 API 证书序列号（十六进制）',
    en: 'Merchant API certificate serial (hex)'
  },
  mchCertSerialNoHint: {
    'zh-CN': '可手填；若上传商户 API 证书，保存时会自动覆盖为证书解析值。',
    en: 'You can type it manually; uploading the merchant API certificate overwrites it with the parsed value on save.'
  },
  mchApiKey: { 'zh-CN': '商户 API 密钥', en: 'Merchant API Key' },
  mchApiKeyPlaceholder: { 'zh-CN': '32 位字母数字（API v3）', en: 'Alphanumeric (API v3)' },
  mchApiKeySetPlaceholder: { 'zh-CN': '已配置，留空保持不变', en: 'Configured. Leave blank to keep' },
  mchApiKeyHint: {
    'zh-CN': '用于支付请求签名与回调解密。密钥仅保存不回显，重新填写即覆盖。',
    en: 'Used to sign payment requests and decrypt notify payloads. It is never returned; type a new value to replace it.'
  },
  sectionCert: { 'zh-CN': '商户 API 证书', en: 'Merchant API Certificate' },
  sectionCertHint: {
    'zh-CN': '请求签名所需的商户 API 证书（证书与私钥在商户平台成对下载）。选择文件后保存，系统自动校验并解析颁发者与有效期；文件内容仅保存不回显。',
    en: 'Upload the merchant API certificate pair (certificate + private key, downloaded together) for request signing. The system validates and parses issuer and validity after saving. Files are stored but never returned.'
  },
  certStatus: { 'zh-CN': '商户 API 证书', en: 'Merchant API cert' },
  configured: { 'zh-CN': '已配置', en: 'Configured' },
  notConfigured: { 'zh-CN': '未配置', en: 'Not configured' },
  certFile: { 'zh-CN': '商户 API 证书文件', en: 'Merchant API Certificate File' },
  certFileHint: { 'zh-CN': '商户平台下载的 apiclient_cert.pem', en: 'apiclient_cert.pem from the merchant platform' },
  keyFile: { 'zh-CN': '商户 API 证书私钥', en: 'Merchant API Private Key File' },
  keyFileHint: { 'zh-CN': '商户平台下载的 apiclient_key.pem', en: 'apiclient_key.pem from the merchant platform' },
  certPairHint: {
    'zh-CN': '证书与私钥需同时选择并保存；后端会校验两者是否配套（同一张证书），并自动写入商户序列号、颁发者与有效期。',
    en: 'Select both the certificate and its private key before saving. The backend verifies they match and fills serial number, issuer and validity automatically.'
  },
  certInfoTitle: { 'zh-CN': '已保存证书信息（上次上传解析）', en: 'Saved certificate info (parsed on last upload)' },
  certIssuer: { 'zh-CN': '颁发者', en: 'Issuer' },
  certValidUntil: { 'zh-CN': '有效期至', en: 'Valid until' },
  notConfiguredYet: {
    'zh-CN': '尚未上传商户 API 证书，上传并保存后此处将展示解析信息。',
    en: 'No merchant API certificate uploaded yet. Parsed info appears here after upload and save.'
  },
  chooseFile: { 'zh-CN': '选择文件', en: 'Choose file' },
  chooseFileTip: { 'zh-CN': '支持 .pem / .crt / .cer / .txt', en: '.pem / .crt / .cer / .txt supported' },
  fileTypeHint: {
    'zh-CN': '仅支持 .pem / .crt / .cer / .txt 格式的证书或密钥文件',
    en: 'Only .pem / .crt / .cer / .txt certificate or key files are supported'
  },
  fileSizeHint: { 'zh-CN': '文件过大，请确认选择的是证书/密钥文本文件', en: 'File too large; please pick a certificate/key text file' },
  sectionMp: { 'zh-CN': '缴费授权公众号', en: 'Official Account for Payment Auth' },
  sectionMpHint: {
    'zh-CN': '用户在缴费流程中通过该公众号完成微信授权（OAuth），需为已认证的服务号。',
    en: 'Users authorize via this Official Account (OAuth) in the payment flow. A verified Service Account is required.'
  },
  mpAppId: { 'zh-CN': '公众号 AppID', en: 'Official Account AppID' },
  mpAppIdPlaceholder: { 'zh-CN': '形如 wx + 16 位字符', en: 'e.g. wx followed by 16 chars' },
  mpAppSecret: { 'zh-CN': '公众号 AppSecret', en: 'Official Account AppSecret' },
  mpAppSecretPlaceholder: {
    'zh-CN': '公众号后台生成的密钥',
    en: 'Generated in the Official Account console'
  },
  mpAppSecretSetPlaceholder: { 'zh-CN': '已配置，留空保持不变', en: 'Configured. Leave blank to keep' },
  save: { 'zh-CN': '保存配置', en: 'Save configuration' },
  saving: { 'zh-CN': '保存中…', en: 'Saving…' },
  saved: { 'zh-CN': '配置已保存', en: 'Configuration saved' },
  loadFailed: { 'zh-CN': '加载配置失败，请重试', en: 'Failed to load configuration. Try again.' },
  lastUpdated: { 'zh-CN': '最近更新', en: 'Last updated' },
  sectionNotify: { 'zh-CN': '支付回调地址', en: 'Payment notify URL' },
  sectionNotifyHint: {
    'zh-CN':
      '默认由「系统配置 → 后台基础地址」拼接，可手动修改并保存；也可一键恢复默认。把该地址填到微信商户平台「产品中心 → 开发配置 → 支付回调 URL」，须公网可访问。若默认仍是 localhost，请先在系统配置填写云端公网 HTTPS 域名。回调解密使用本页 APIv3 密钥。',
    en: 'Defaults from System Settings → Admin base URL; you can edit and save, or restore the default in one click. Paste this URL into WeChat Merchant Platform → Product Center → Dev Config → Payment Notify URL. It must be publicly reachable. If the default is still localhost, set the public HTTPS origin in System Settings first. Notify payloads are decrypted with the APIv3 key on this page.'
  },
  notifyUrl: { 'zh-CN': '回调地址', en: 'Notify URL' },
  notifyUrlHint: {
    'zh-CN': '默认由「系统配置 → 后台基础地址」拼接；可手动修改。与默认相同或留空保存后会跟随默认。路径建议为 /api/public/payment/wechat/notify。',
    en: 'Defaults from System Settings → Admin base URL; you can edit it. Saving the default or blank keeps following the default. Preferred path: /api/public/payment/wechat/notify.'
  },
  restoreDefault: { 'zh-CN': '恢复默认', en: 'Restore default' },
  restoredDefault: { 'zh-CN': '已恢复为默认地址，请保存配置', en: 'Restored to default. Save to apply.' },
  copy: { 'zh-CN': '复制', en: 'Copy' },
  copySuccess: { 'zh-CN': '已复制回调地址', en: 'Notify URL copied' },
  copyFailed: { 'zh-CN': '复制失败，请手动选择复制', en: 'Copy failed, please copy it manually' }
}

const { t } = useBiText(d)

interface WeChatConfigView {
  mchId: string
  mchName: string
  mchApiKeySet: boolean
  mchPrivateKeySet: boolean
  mchCertSerialNo: string
  mchCertIssuer: string
  mchCertValidUntil: string
  mpAppId: string
  mpAppSecretSet: boolean
  updatedAt: string
  notifyUrl: string
  defaultNotifyUrl: string
}

type SlotKey = 'cert' | 'key'

const loading = ref(true)
const saving = ref(false)

const mchId = ref('')
const mchName = ref('')
const mchCertSerialNo = ref('')
const mchApiKey = ref('')
const mpAppId = ref('')
const mpAppSecret = ref('')

const mchApiKeySet = ref(false)
const mchPrivateKeySet = ref(false)
const mchCertIssuer = ref('')
const mchCertValidUntil = ref('')
const mpAppSecretSet = ref(false)
const updatedAt = ref('')
const notifyUrl = ref('')
const defaultNotifyUrl = ref('')

/** 待上传文件（每次保存后清空；留空表示不更新该项） */
const chosenFiles = ref<Record<SlotKey, File | null>>({
  cert: null,
  key: null
})
const certUploadRef = ref<UploadInstance>()
const keyUploadRef = ref<UploadInstance>()

const certConfiguredText = computed(() =>
  mchPrivateKeySet.value ? t('configured') : t('notConfigured')
)

/** 仅保留数字（商户号） */
function restrictDigits(event: Event): void {
  const input = event.target as HTMLInputElement
  input.value = input.value.replace(/\D/g, '')
}

/** 密钥输入仅允许字母数字 */
function restrictApiKey(event: Event): void {
  const input = event.target as HTMLInputElement
  input.value = input.value.replace(/[^a-zA-Z0-9]/g, '')
}

function applyView(view: WeChatConfigView): void {
  mchId.value = view.mchId
  mchName.value = view.mchName ?? ''
  mchCertSerialNo.value = view.mchCertSerialNo ?? ''
  mpAppId.value = view.mpAppId
  mchApiKeySet.value = view.mchApiKeySet
  mchPrivateKeySet.value = view.mchPrivateKeySet
  mchCertIssuer.value = view.mchCertIssuer ?? ''
  mchCertValidUntil.value = view.mchCertValidUntil ?? ''
  mpAppSecretSet.value = view.mpAppSecretSet
  updatedAt.value = view.updatedAt
  const effective = (view.notifyUrl ?? '').trim()
  const defaults = (view.defaultNotifyUrl ?? '').trim()
  defaultNotifyUrl.value = defaults
  // 与默认相同则留空，用 placeholder 展示默认，避免长 URL 把输入框撑挤后无法点选编辑
  notifyUrl.value = effective && effective !== defaults ? effective : ''
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
    const view = await request.get<never, WeChatConfigView>('/system/wechat/config')
    applyView(view)
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('loadFailed'))
  } finally {
    loading.value = false
  }
}

/** 选择文件：校验后缀与大小后暂存，保存时随表单上传 */
function handleSlotChange(slotKey: SlotKey, file: UploadFile): void {
  const raw = file.raw
  if (!raw) {
    return
  }
  const slotUploadRef = uploadRefOf(slotKey)
  if (!/\.(pem|crt|cer|txt)$/i.test(raw.name)) {
    ElMessage.warning(t('fileTypeHint'))
    slotUploadRef?.clearFiles()
    return
  }
  if (raw.size > 512 * 1024) {
    ElMessage.warning(t('fileSizeHint'))
    slotUploadRef?.clearFiles()
    return
  }
  chosenFiles.value[slotKey] = raw
  slotUploadRef?.clearFiles()
}

function removeSlotFile(slotKey: SlotKey): void {
  chosenFiles.value[slotKey] = null
  uploadRefOf(slotKey)?.clearFiles()
}

function uploadRefOf(slotKey: SlotKey): UploadInstance | undefined {
  if (slotKey === 'cert') {
    return certUploadRef.value
  }
  return keyUploadRef.value
}

function clearChosenFiles(): void {
  chosenFiles.value = { cert: null, key: null }
  certUploadRef.value?.clearFiles()
  keyUploadRef.value?.clearFiles()
}

async function handleSave(): Promise<void> {
  saving.value = true
  try {
    const fd = new FormData()
    fd.append('mchId', mchId.value.trim())
    fd.append('mchName', mchName.value.trim())
    fd.append('mchCertSerialNo', mchCertSerialNo.value.trim())
    // 敏感字段留空表示保持不变，由后端处理
    fd.append('mchApiKey', mchApiKey.value.trim())
    fd.append('mpAppId', mpAppId.value.trim())
    fd.append('mpAppSecret', mpAppSecret.value.trim())
    fd.append('notifyUrl', notifyUrl.value.trim())
    const certFile = chosenFiles.value.cert
    const keyFile = chosenFiles.value.key
    if (certFile) {
      fd.append('certFile', certFile)
    }
    if (keyFile) {
      fd.append('keyFile', keyFile)
    }
    const view = await request.post<never, WeChatConfigView>('/system/wechat/config', fd)
    mchApiKey.value = ''
    mpAppSecret.value = ''
    applyView(view)
    clearChosenFiles()
    ElMessage.success(t('saved'))
  } catch (error) {
    ElMessage.error(error instanceof Error && error.message ? error.message : t('loadFailed'))
  } finally {
    saving.value = false
  }
}

async function copyNotifyUrl(): Promise<void> {
  const text = (notifyUrl.value.trim() || defaultNotifyUrl.value.trim())
  if (!text) {
    return
  }
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const area = document.createElement('textarea')
      area.value = text
      area.style.position = 'fixed'
      area.style.opacity = '0'
      document.body.appendChild(area)
      area.select()
      document.execCommand('copy')
      document.body.removeChild(area)
    }
    ElMessage.success(t('copySuccess'))
  } catch {
    ElMessage.error(t('copyFailed'))
  }
}

function restoreDefaultNotifyUrl(): void {
  notifyUrl.value = ''
  ElMessage.success(t('restoredDefault'))
}

const notifyIsDefault = computed(
  () => !notifyUrl.value.trim() || notifyUrl.value.trim() === defaultNotifyUrl.value.trim()
)

const effectiveNotifyUrl = computed(
  () => notifyUrl.value.trim() || defaultNotifyUrl.value.trim()
)

onMounted(loadConfig)
</script>

<template>
  <section class="wechat-page">
    <el-card class="panel" shadow="never" v-loading="loading">
      <el-form label-position="top" class="wechat-form" @submit.prevent="handleSave">
        <el-alert type="info" :closable="false" class="scope-alert" show-icon>
          <span>{{ t('scopeAlert') }}</span>
        </el-alert>
        <el-alert type="warning" :closable="false" class="scope-alert" show-icon>
          <span>{{ t('scope') }}</span>
        </el-alert>

        <h3 class="group-title">{{ t('sectionNotify') }}</h3>
        <p class="section-hint">{{ t('sectionNotifyHint') }}</p>
        <el-form-item :label="t('notifyUrl')" class="notify-item">
          <el-input
            v-model="notifyUrl"
            clearable
            class="field notify-field"
            :placeholder="defaultNotifyUrl || t('notifyUrl')"
          />
          <div class="notify-actions">
            <el-button native-type="button" :disabled="!effectiveNotifyUrl" @click="copyNotifyUrl">
              {{ t('copy') }}
            </el-button>
            <el-button
              native-type="button"
              :disabled="!defaultNotifyUrl || notifyIsDefault"
              @click="restoreDefaultNotifyUrl"
            >
              {{ t('restoreDefault') }}
            </el-button>
          </div>
          <div class="field-hint">{{ t('notifyUrlHint') }}</div>
        </el-form-item>

        <el-divider />

        <h3 class="group-title">{{ t('sectionMerchant') }}</h3>
        <p class="section-hint">{{ t('sectionMerchantHint') }}</p>
        <div class="field-grid">
          <el-form-item :label="t('mchId')">
            <el-input
              v-model="mchId"
              :placeholder="t('mchIdPlaceholder')"
              maxlength="32"
              clearable
              class="field"
              @input="restrictDigits"
            />
            <div class="field-hint">{{ t('mchIdHint') }}</div>
          </el-form-item>
          <el-form-item :label="t('mchName')">
            <el-input
              v-model="mchName"
              :placeholder="t('mchNamePlaceholder')"
              maxlength="128"
              clearable
              class="field"
            />
            <div class="field-hint">{{ t('mchNameHint') }}</div>
          </el-form-item>
          <el-form-item :label="t('mchCertSerialNo')">
            <el-input
              v-model="mchCertSerialNo"
              :placeholder="t('mchCertSerialNoPlaceholder')"
              maxlength="64"
              clearable
              class="field"
            />
            <div class="field-hint">{{ t('mchCertSerialNoHint') }}</div>
          </el-form-item>
          <el-form-item :label="t('mchApiKey')">
            <el-input
              v-model="mchApiKey"
              type="password"
              show-password
              :placeholder="mchApiKeySet ? t('mchApiKeySetPlaceholder') : t('mchApiKeyPlaceholder')"
              maxlength="64"
              clearable
              autocomplete="new-password"
              class="field"
              @input="restrictApiKey"
            />
            <div class="field-hint">{{ t('mchApiKeyHint') }}</div>
          </el-form-item>
        </div>

        <el-divider />

        <h3 class="group-title">{{ t('sectionCert') }}</h3>
        <p class="section-hint">{{ t('sectionCertHint') }}</p>

        <div class="status-row">
          <el-tag size="small" :type="mchPrivateKeySet ? 'success' : 'info'" effect="plain">
            {{ t('certStatus') }}：{{ certConfiguredText }}
          </el-tag>
        </div>

        <div v-if="mchPrivateKeySet" class="cert-info">
          <div class="cert-info-title">{{ t('certInfoTitle') }}</div>
          <div class="cert-info-grid">
            <div class="ci-row">
              <span class="ci-label">{{ t('mchCertSerialNo') }}</span>
              <span class="ci-value mono">{{ mchCertSerialNo || '-' }}</span>
            </div>
            <div class="ci-row">
              <span class="ci-label">{{ t('certIssuer') }}</span>
              <span class="ci-value">{{ mchCertIssuer || '-' }}</span>
            </div>
            <div class="ci-row">
              <span class="ci-label">{{ t('certValidUntil') }}</span>
              <span class="ci-value">{{ mchCertValidUntil || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="field-grid">
          <el-form-item :label="t('certFile')">
            <el-upload
              ref="certUploadRef"
              :auto-upload="false"
              :show-file-list="false"
              accept=".pem,.crt,.cer,.txt"
              class="upload-slot"
              :on-change="(f: UploadFile) => handleSlotChange('cert', f)"
            >
              <div class="upload-trigger">
                <el-tag v-if="chosenFiles.cert" closable class="file-tag" @close="removeSlotFile('cert')">
                  {{ chosenFiles.cert.name }}
                </el-tag>
                <template v-else>
                  <span class="choose-text">{{ t('chooseFile') }}</span>
                  <span class="choose-tip">{{ t('chooseFileTip') }}</span>
                </template>
              </div>
            </el-upload>
            <div class="field-hint">{{ t('certFileHint') }}</div>
          </el-form-item>
          <el-form-item :label="t('keyFile')">
            <el-upload
              ref="keyUploadRef"
              :auto-upload="false"
              :show-file-list="false"
              accept=".pem,.crt,.cer,.txt"
              class="upload-slot"
              :on-change="(f: UploadFile) => handleSlotChange('key', f)"
            >
              <div class="upload-trigger">
                <el-tag v-if="chosenFiles.key" closable class="file-tag" @close="removeSlotFile('key')">
                  {{ chosenFiles.key.name }}
                </el-tag>
                <template v-else>
                  <span class="choose-text">{{ t('chooseFile') }}</span>
                  <span class="choose-tip">{{ t('chooseFileTip') }}</span>
                </template>
              </div>
            </el-upload>
            <div class="field-hint">{{ t('keyFileHint') }}</div>
          </el-form-item>
        </div>
        <p class="section-hint">{{ t('certPairHint') }}</p>

        <el-divider />

        <h3 class="group-title">{{ t('sectionMp') }}</h3>
        <p class="section-hint">{{ t('sectionMpHint') }}</p>
        <div class="field-grid">
          <el-form-item :label="t('mpAppId')">
            <el-input
              v-model="mpAppId"
              :placeholder="t('mpAppIdPlaceholder')"
              maxlength="32"
              clearable
              class="field"
            />
          </el-form-item>
          <el-form-item :label="t('mpAppSecret')">
            <el-input
              v-model="mpAppSecret"
              type="password"
              show-password
              :placeholder="mpAppSecretSet ? t('mpAppSecretSetPlaceholder') : t('mpAppSecretPlaceholder')"
              maxlength="128"
              clearable
              autocomplete="new-password"
              class="field"
            />
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
.wechat-page {
  animation: fade-up 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.panel {
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-soft);
}

.wechat-form {
  max-width: 860px;
}

.scope-alert {
  margin-bottom: 12px;
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

.status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.cert-info {
  margin-bottom: 16px;
  padding: 10px 12px;
  border: 1px dashed var(--fp-line, #dcdfe6);
  border-radius: var(--fp-radius);
  background: var(--fp-surface-soft, rgba(0, 0, 0, 0.02));
}

.cert-info-title {
  margin-bottom: 6px;
  font-size: 0.78rem;
  color: var(--fp-muted);
}

.cert-info-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ci-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  font-size: 0.82rem;
}

.ci-label {
  flex: 0 0 auto;
  color: var(--fp-muted);
}

.ci-value.mono {
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.field {
  width: 100%;
}

.notify-item :deep(.el-form-item__content) {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
}

.notify-field {
  width: 100%;
}

.notify-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.notify-field :deep(.el-input__inner) {
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 0.82rem;
}

.upload-slot {
  width: 100%;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-height: 76px;
  padding: 10px 12px;
  border: 1px dashed var(--el-border-color, #dcdfe6);
  border-radius: var(--fp-radius, 6px);
  background: var(--fp-surface-soft, rgba(0, 0, 0, 0.02));
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;
  box-sizing: border-box;
}

.upload-slot :deep(.el-upload),
.upload-slot :deep(.el-upload-dragger) {
  width: 100%;
}

.upload-trigger:hover {
  border-color: var(--el-color-primary, #409eff);
  background: var(--fp-surface-soft-hover, rgba(0, 0, 0, 0.04));
}

.upload-trigger .el-tag.file-tag {
  max-width: 100%;
}

.file-tag :deep(.el-tag__content) {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.choose-text {
  font-size: 0.85rem;
  color: var(--el-color-primary, #409eff);
}

.choose-tip {
  font-size: 0.74rem;
  color: var(--fp-muted);
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
