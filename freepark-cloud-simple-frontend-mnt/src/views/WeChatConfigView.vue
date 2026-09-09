<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, type UploadFile, type UploadInstance } from 'element-plus'
import request from '../utils/request'
import { useBiText, type BiDict } from '../utils/biText'

const d: BiDict = {
  loading: { 'zh-CN': '正在加载配置…', en: 'Loading configuration…' },
  scopeAlert: {
    'zh-CN':
      '配置微信支付所需的商户信息与公众号授权信息。是否开放微信支付由「系统配置 → 收费方式」控制，开启前请先在此填齐商户与公众号参数。',
    en: 'Configure the merchant and Official Account credentials for WeChat Pay. Whether WeChat Pay is open is controlled by System Settings → Payment Methods; fill in the parameters here before enabling it.'
  },
  scope: {
    'zh-CN': '仅超级管理员可修改微信支付配置。',
    en: 'Only super administrators can modify the WeChat Pay configuration.'
  },
  sectionMerchant: { 'zh-CN': '商户参数', en: 'Merchant Parameters' },
  sectionMerchantHint: {
    'zh-CN': '用于发起收款与签名验签的微信支付商户凭据，在微信商户平台申请。',
    en: 'WeChat Pay merchant credentials used to create and verify payments. Apply via the WeChat Merchant Platform.'
  },
  mchId: { 'zh-CN': '商户号', en: 'Merchant ID' },
  mchIdPlaceholder: { 'zh-CN': '微信支付商户号（纯数字）', en: 'WeChat Pay merchant ID (digits only)' },
  mchIdHint: { 'zh-CN': '在微信商户平台“账户中心 → 商户信息”中查看。', en: 'Find it under Account Center → Merchant Info on the WeChat Merchant Platform.' },
  mchApiKey: { 'zh-CN': '商户 API 密钥', en: 'Merchant API Key' },
  mchApiKeyPlaceholder: { 'zh-CN': '32 位字母数字（API v3）', en: 'Alphanumeric (API v3)' },
  mchApiKeySetPlaceholder: { 'zh-CN': '已配置，留空保持不变', en: 'Configured. Leave blank to keep' },
  mchApiKeyHint: {
    'zh-CN': '用于支付请求签名。密钥仅保存不回显，重新填写即覆盖。',
    en: 'Used to sign payment requests. It is never returned; type a new value to replace it.'
  },
  sectionCert: { 'zh-CN': '商户 API 证书与平台验签', en: 'Merchant API Certificate & Platform Verification' },
  sectionCertHint: {
    'zh-CN': '请求签名所需的商户 API 证书（证书与私钥在商户平台成对下载）与平台验签所需的微信支付公钥。选择文件后保存，系统自动校验并解析证书序列号等信息；文件内容仅保存不回显。',
    en: 'Upload the merchant API certificate pair (certificate + private key, downloaded together) for request signing, and the WeChat Pay public key for response verification. The system validates and parses the certificate after saving. Files are stored but never returned.'
  },
  certStatus: { 'zh-CN': '商户 API 证书', en: 'Merchant API cert' },
  pubKeyStatus: { 'zh-CN': '验签公钥', en: 'Verification key' },
  configured: { 'zh-CN': '已配置', en: 'Configured' },
  notConfigured: { 'zh-CN': '未配置', en: 'Not configured' },
  certFile: { 'zh-CN': '商户 API 证书文件', en: 'Merchant API Certificate File' },
  certFileHint: { 'zh-CN': '商户平台下载的 apiclient_cert.pem', en: 'apiclient_cert.pem from the merchant platform' },
  keyFile: { 'zh-CN': '商户 API 证书私钥', en: 'Merchant API Private Key File' },
  keyFileHint: { 'zh-CN': '商户平台下载的 apiclient_key.pem', en: 'apiclient_key.pem from the merchant platform' },
  certPairHint: {
    'zh-CN': '证书与私钥需同时选择并保存；后端会校验两者是否配套（同一张证书），并自动解析序列号、颁发者与有效期。',
    en: 'Select both the certificate and its private key before saving. The backend verifies they match and parses the serial number, issuer and validity automatically.'
  },
  publicKeyFile: { 'zh-CN': '微信支付公钥文件', en: 'WeChat Pay Public Key File' },
  publicKeyFileHint: {
    'zh-CN': '商户平台「API 安全 → 微信支付公钥」下载的 pub_key.pem',
    en: 'pub_key.pem from API Security → WeChat Pay Public Key'
  },
  publicKeyPairHint: {
    'zh-CN': '上传 pub_key.pem 时，需同时填写上方的公钥 ID。',
    en: 'When uploading pub_key.pem, fill in the public key ID above as well.'
  },
  wechatPayPublicKeyId: { 'zh-CN': '微信支付公钥 ID', en: 'WeChat Pay Public Key ID' },
  wechatPayPublicKeyIdPlaceholder: {
    'zh-CN': '形如 PUB_KEY_ID_xxx',
    en: 'e.g. PUB_KEY_ID_xxx'
  },
  wechatPayPublicKeyIdHint: {
    'zh-CN': '商户平台「API 安全 → 微信支付公钥」中查看；上传公钥文件时必填。',
    en: 'Found at API Security → WeChat Pay Public Key. Required when uploading the key file.'
  },
  certInfoTitle: { 'zh-CN': '已保存证书信息（上次上传解析）', en: 'Saved certificate info (parsed on last upload)' },
  certSerialNo: { 'zh-CN': '证书序列号', en: 'Serial number' },
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
  lastUpdated: { 'zh-CN': '最近更新', en: 'Last updated' }
}

const { t } = useBiText(d)

interface WeChatConfigView {
  mchId: string
  mchApiKeySet: boolean
  mchPrivateKeySet: boolean
  mchCertSerialNo: string
  mchCertIssuer: string
  mchCertValidUntil: string
  wechatPayPublicKeySet: boolean
  wechatPayPublicKeyId: string
  mpAppId: string
  mpAppSecretSet: boolean
  updatedAt: string
}

type SlotKey = 'cert' | 'key' | 'publicKey'

const loading = ref(true)
const saving = ref(false)

const mchId = ref('')
const mchApiKey = ref('')
const mpAppId = ref('')
const mpAppSecret = ref('')
const wechatPayPublicKeyId = ref('')

const mchApiKeySet = ref(false)
const mchPrivateKeySet = ref(false)
const mchCertSerialNo = ref('')
const mchCertIssuer = ref('')
const mchCertValidUntil = ref('')
const wechatPayPublicKeySet = ref(false)
const mpAppSecretSet = ref(false)
const updatedAt = ref('')

/** 待上传文件（每次保存后清空；留空表示不更新该项） */
const chosenFiles = ref<Record<SlotKey, File | null>>({
  cert: null,
  key: null,
  publicKey: null
})
const certUploadRef = ref<UploadInstance>()
const keyUploadRef = ref<UploadInstance>()
const publicKeyUploadRef = ref<UploadInstance>()

const certConfiguredText = computed(() =>
  mchPrivateKeySet.value ? t('configured') : t('notConfigured')
)
const pubKeyConfiguredText = computed(() =>
  wechatPayPublicKeySet.value ? t('configured') : t('notConfigured')
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
  mpAppId.value = view.mpAppId
  mchApiKeySet.value = view.mchApiKeySet
  mchPrivateKeySet.value = view.mchPrivateKeySet
  mchCertSerialNo.value = view.mchCertSerialNo ?? ''
  mchCertIssuer.value = view.mchCertIssuer ?? ''
  mchCertValidUntil.value = view.mchCertValidUntil ?? ''
  wechatPayPublicKeySet.value = view.wechatPayPublicKeySet
  wechatPayPublicKeyId.value = view.wechatPayPublicKeyId ?? ''
  mpAppSecretSet.value = view.mpAppSecretSet
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
  if (slotKey === 'key') {
    return keyUploadRef.value
  }
  return publicKeyUploadRef.value
}

function clearChosenFiles(): void {
  chosenFiles.value = { cert: null, key: null, publicKey: null }
  certUploadRef.value?.clearFiles()
  keyUploadRef.value?.clearFiles()
  publicKeyUploadRef.value?.clearFiles()
}

async function handleSave(): Promise<void> {
  saving.value = true
  try {
    const fd = new FormData()
    fd.append('mchId', mchId.value.trim())
    // 敏感字段留空表示保持不变，由后端处理
    fd.append('mchApiKey', mchApiKey.value.trim())
    fd.append('mpAppId', mpAppId.value.trim())
    fd.append('mpAppSecret', mpAppSecret.value.trim())
    fd.append('wechatPayPublicKeyId', wechatPayPublicKeyId.value.trim())
    const certFile = chosenFiles.value.cert
    const keyFile = chosenFiles.value.key
    const publicKeyFile = chosenFiles.value.publicKey
    if (certFile) {
      fd.append('certFile', certFile)
    }
    if (keyFile) {
      fd.append('keyFile', keyFile)
    }
    if (publicKeyFile) {
      fd.append('wechatPayPublicKeyFile', publicKeyFile)
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
          <el-tag size="small" :type="wechatPayPublicKeySet ? 'success' : 'info'" effect="plain">
            {{ t('pubKeyStatus') }}：{{ pubKeyConfiguredText }}
          </el-tag>
        </div>

        <div v-if="mchPrivateKeySet" class="cert-info">
          <div class="cert-info-title">{{ t('certInfoTitle') }}</div>
          <div class="cert-info-grid">
            <div class="ci-row">
              <span class="ci-label">{{ t('certSerialNo') }}</span>
              <span class="ci-value mono">{{ mchCertSerialNo }}</span>
            </div>
            <div class="ci-row">
              <span class="ci-label">{{ t('certIssuer') }}</span>
              <span class="ci-value">{{ mchCertIssuer }}</span>
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

        <el-form-item :label="t('wechatPayPublicKeyId')">
          <el-input
            v-model="wechatPayPublicKeyId"
            :placeholder="t('wechatPayPublicKeyIdPlaceholder')"
            maxlength="64"
            clearable
            class="field"
          />
          <div class="field-hint">{{ t('wechatPayPublicKeyIdHint') }}</div>
        </el-form-item>

        <el-form-item :label="t('publicKeyFile')">
          <el-upload
            ref="publicKeyUploadRef"
            :auto-upload="false"
            :show-file-list="false"
            accept=".pem,.crt,.cer,.txt"
            class="upload-slot"
            :on-change="(f: UploadFile) => handleSlotChange('publicKey', f)"
          >
            <div class="upload-trigger">
              <el-tag
                v-if="chosenFiles.publicKey"
                closable
                class="file-tag"
                @close="removeSlotFile('publicKey')"
              >
                {{ chosenFiles.publicKey.name }}
              </el-tag>
              <template v-else>
                <span class="choose-text">{{ t('chooseFile') }}</span>
                <span class="choose-tip">{{ t('chooseFileTip') }}</span>
              </template>
            </div>
          </el-upload>
          <div class="field-hint">{{ t('publicKeyFileHint') }}</div>
        </el-form-item>
        <p class="section-hint">{{ t('publicKeyPairHint') }}</p>

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
