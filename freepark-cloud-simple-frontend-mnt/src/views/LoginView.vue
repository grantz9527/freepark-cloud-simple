<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import LocaleSwitcher from '../components/LocaleSwitcher.vue'
import { login } from '../api/user'
import { setToken } from '../utils/auth'
import { useUiModeStore } from '../stores/uiMode'
import { useUserStore } from '../stores/user'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const uiMode = useUiModeStore()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: t('login.usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('login.passwordRequired'), trigger: 'blur' }]
}))

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const result = await login(form.username.trim(), form.password)
    setToken(result.token)
    userStore.save({
      username: result.username,
      nickname: result.nickname ?? '',
      role: result.role
    })
    ElMessage.success(t('login.success'))
    if (typeof route.query.redirect === 'string' && route.query.redirect) {
      router.push(route.query.redirect)
    } else {
      router.push(uiMode.homePath())
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('login.failed'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-toolbar">
      <LocaleSwitcher />
    </div>

    <div class="login-atmosphere" aria-hidden="true">
      <div class="lane lane-a" />
      <div class="lane lane-b" />
      <div class="glow" />
      <div class="grid" />
    </div>

    <section class="login-panel">
      <div class="brand-block">
        <h1 class="brand-mark">{{ t('common.brand') }}</h1>
        <p class="brand-title">{{ t('login.title') }}</p>
        <p class="brand-desc">{{ t('login.desc') }}</p>
      </div>

      <el-form
        ref="formRef"
        class="login-form"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            :placeholder="t('login.username')"
            clearable
            autocomplete="username"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            :placeholder="t('login.password')"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button class="login-btn" type="primary" :loading="loading" @click="handleLogin">
            {{ t('login.submit') }}
          </el-button>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 100svh;
  padding: 32px 20px;
  overflow: hidden;
  background:
    radial-gradient(ellipse 80% 60% at 15% 20%, rgba(201, 137, 42, 0.16), transparent 55%),
    radial-gradient(ellipse 70% 50% at 85% 80%, rgba(13, 122, 111, 0.22), transparent 50%),
    linear-gradient(160deg, #0f1c24 0%, #16333a 42%, #0d4a45 100%);
}

.login-toolbar {
  position: absolute;
  top: 18px;
  right: 18px;
  z-index: 2;
}

.login-atmosphere {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 70% 60% at 50% 50%, black, transparent 75%);
  opacity: 0.55;
}

.lane {
  position: absolute;
  width: 220%;
  height: 3px;
  left: -40%;
  background: repeating-linear-gradient(
    90deg,
    rgba(247, 239, 223, 0.55) 0 28px,
    transparent 28px 52px
  );
  transform: rotate(-18deg);
  animation: drift 18s linear infinite;
}

.lane-a {
  top: 28%;
  opacity: 0.45;
}

.lane-b {
  top: 62%;
  opacity: 0.28;
  animation-duration: 26s;
  animation-direction: reverse;
}

.glow {
  position: absolute;
  width: 420px;
  height: 420px;
  right: -80px;
  bottom: -100px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(13, 122, 111, 0.35), transparent 68%);
  animation: pulse 8s ease-in-out infinite;
}

.login-panel {
  position: relative;
  z-index: 1;
  width: min(420px, 100%);
  padding: 40px 36px 36px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.28),
    0 0 0 1px rgba(255, 255, 255, 0.35);
  backdrop-filter: blur(12px);
  animation: rise 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.brand-block {
  margin-bottom: 28px;
  text-align: left;
}

.brand-mark {
  margin: 0 0 10px;
  font-family: var(--fp-font-display);
  font-size: clamp(2rem, 5vw, 2.55rem);
  font-weight: 700;
  letter-spacing: -0.04em;
  line-height: 1;
  color: var(--fp-ink);
  animation: rise 0.8s 0.08s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.brand-title {
  margin: 0 0 10px;
  font-family: var(--fp-font-display);
  font-size: 1.15rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--fp-teal);
  animation: rise 0.8s 0.14s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.brand-desc {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.55;
  color: var(--fp-muted);
  animation: rise 0.8s 0.2s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.login-form {
  animation: rise 0.8s 0.26s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-input__wrapper) {
  padding: 4px 14px;
  box-shadow: 0 0 0 1px var(--fp-line) inset;
  background: #f8faf9;
  transition: box-shadow 0.2s ease, background 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover),
.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--fp-teal) inset;
}

.login-btn {
  width: 100%;
  height: 46px;
  margin-top: 4px;
  font-weight: 600;
  letter-spacing: 0.04em;
  border: none;
  background: linear-gradient(135deg, var(--fp-teal) 0%, var(--fp-teal-deep) 100%);
  transition: transform 0.2s ease, filter 0.2s ease;
}

.login-btn:hover {
  filter: brightness(1.06);
  transform: translateY(-1px);
}

@keyframes rise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes drift {
  from {
    transform: rotate(-18deg) translateX(0);
  }
  to {
    transform: rotate(-18deg) translateX(-80px);
  }
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.7;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.08);
  }
}

@media (max-width: 480px) {
  .login-panel {
    padding: 32px 22px 26px;
  }
}
</style>
