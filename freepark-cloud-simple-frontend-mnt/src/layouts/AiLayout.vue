<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import LocaleSwitcher from '../components/LocaleSwitcher.vue'
import { clearAuth } from '../utils/auth'
import { mockAiReply } from '../utils/mockAi'
import { useUiModeStore } from '../stores/uiMode'
import { useUserStore } from '../stores/user'

interface ChatMessage {
  id: number
  role: 'assistant' | 'user'
  content: string
}

const { t, locale } = useI18n()
const router = useRouter()
const uiMode = useUiModeStore()
const userStore = useUserStore()

const messages = ref<ChatMessage[]>([])
const input = ref('')
const sending = ref(false)
const listRef = ref<HTMLElement | null>(null)
let seq = 0

function pushMessage(role: ChatMessage['role'], content: string) {
  seq += 1
  messages.value.push({ id: seq, role, content })
}

function resetWelcome() {
  messages.value = []
  seq = 0
  pushMessage('assistant', t('ai.welcome'))
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

async function handleSend() {
  const text = input.value.trim()
  if (!text || sending.value) {
    return
  }
  input.value = ''
  pushMessage('user', text)
  await scrollToBottom()

  sending.value = true
  await new Promise((resolve) => setTimeout(resolve, 650))
  pushMessage('assistant', mockAiReply(text, t))
  sending.value = false
  await scrollToBottom()
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm(t('common.logoutConfirm'), t('common.logoutTitle'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel')
    })
  } catch {
    return
  }
  clearAuth()
  userStore.clear()
  ElMessage.success(t('common.logoutSuccess'))
  router.push('/login')
}

function switchToClassic() {
  uiMode.setMode('classic')
  router.push('/')
}

onMounted(() => {
  resetWelcome()
})

watch(locale, () => {
  const onlyWelcome = messages.value.length <= 1 && messages.value[0]?.role === 'assistant'
  if (onlyWelcome) {
    resetWelcome()
  }
})
</script>

<template>
  <div class="ai-shell">
    <header class="topbar">
      <div class="brand">
        <span class="brand-mark">{{ t('common.brand') }}</span>
        <span class="brand-divider" aria-hidden="true" />
        <div class="brand-text">
          <strong>{{ t('ai.title') }}</strong>
          <span>{{ t('ai.subtitle') }}</span>
        </div>
      </div>
      <div class="topbar-actions">
        <LocaleSwitcher />
        <button class="ghost-btn" type="button" @click="switchToClassic">
          {{ t('common.switchToClassic') }}
        </button>
        <button class="ghost-btn" type="button" @click="handleLogout">
          {{ t('common.logout') }}
        </button>
      </div>
    </header>

    <main class="chat-main">
      <div ref="listRef" class="chat-list">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="bubble"
          :class="msg.role === 'user' ? 'is-user' : 'is-assistant'"
        >
          {{ msg.content }}
        </div>
        <div v-if="sending" class="bubble is-assistant is-thinking">
          {{ t('ai.thinking') }}
        </div>
      </div>

      <form class="composer" @submit.prevent="handleSend">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          :placeholder="t('ai.placeholder')"
          resize="none"
          @keydown.enter.exact.prevent="handleSend"
        />
        <el-button type="primary" class="send-btn" :loading="sending" native-type="submit">
          {{ t('ai.send') }}
        </el-button>
      </form>
    </main>
  </div>
</template>

<style scoped>
.ai-shell {
  display: flex;
  flex-direction: column;
  min-height: 100svh;
  background:
    radial-gradient(ellipse 55% 40% at 100% 0%, rgba(13, 122, 111, 0.12), transparent 55%),
    radial-gradient(ellipse 45% 35% at 0% 100%, rgba(201, 137, 42, 0.08), transparent 50%),
    linear-gradient(180deg, #e8efec 0%, var(--fp-surface) 45%, #e6eeeb 100%);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px clamp(16px, 3vw, 28px);
  border-bottom: 1px solid rgba(213, 224, 228, 0.9);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(10px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-mark {
  font-family: var(--fp-font-display);
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--fp-ink);
}

.brand-divider {
  width: 1px;
  height: 28px;
  background: var(--fp-line);
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.brand-text strong {
  font-size: 0.95rem;
  color: var(--fp-ink);
}

.brand-text span {
  font-size: 0.78rem;
  color: var(--fp-muted);
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.ghost-btn {
  padding: 7px 12px;
  border: 1px solid var(--fp-line);
  border-radius: var(--fp-radius-sm);
  background: transparent;
  color: var(--fp-ink-soft);
  cursor: pointer;
  font-size: 0.88rem;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.ghost-btn:hover {
  border-color: var(--fp-teal);
  color: var(--fp-teal-deep);
  background: var(--fp-teal-soft);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: min(860px, 100%);
  margin: 0 auto;
  padding: 20px clamp(16px, 3vw, 24px) 24px;
  min-height: 0;
}

.chat-list {
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 4px 20px;
  min-height: 280px;
}

.bubble {
  max-width: min(78%, 560px);
  padding: 12px 14px;
  border-radius: 14px;
  line-height: 1.55;
  font-size: 0.95rem;
  white-space: pre-wrap;
  word-break: break-word;
  animation: rise 0.35s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.is-assistant {
  align-self: flex-start;
  background: #fff;
  color: var(--fp-ink);
  border: 1px solid var(--fp-line);
  border-bottom-left-radius: 4px;
}

.is-user {
  align-self: flex-end;
  background: linear-gradient(135deg, var(--fp-teal) 0%, var(--fp-teal-deep) 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.is-thinking {
  opacity: 0.75;
  font-style: italic;
}

.composer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: end;
  padding-top: 12px;
  border-top: 1px solid var(--fp-line);
}

.send-btn {
  height: 42px;
  padding: 0 18px;
  font-weight: 600;
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 640px) {
  .brand-divider,
  .brand-text span {
    display: none;
  }

  .composer {
    grid-template-columns: 1fr;
  }

  .send-btn {
    width: 100%;
  }
}
</style>
