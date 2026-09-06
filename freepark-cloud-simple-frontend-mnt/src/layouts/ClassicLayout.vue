<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import LocaleSwitcher from '../components/LocaleSwitcher.vue'
import SideMenu from '../components/SideMenu.vue'
import { clearAuth } from '../utils/auth'
import { useUiModeStore } from '../stores/uiMode'
import { useUserStore } from '../stores/user'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const uiMode = useUiModeStore()
const userStore = useUserStore()

const pageTitle = computed(() => {
  const key = typeof route.meta.titleKey === 'string' ? route.meta.titleKey : 'menu.overview'
  return t(key)
})


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

function switchToAi() {
  uiMode.setMode('ai')
  router.push('/ai')
}
</script>

<template>
  <div class="classic-shell">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <span class="brand-mark">{{ t('common.brand') }}</span>
        <span class="brand-sub">{{ t('common.platform') }}</span>
      </div>
      <nav class="sidebar-nav">
        <SideMenu />
      </nav>
    </aside>

    <div class="main-column">
      <header class="topbar">
        <h1 class="page-title">{{ pageTitle }}</h1>
        <div class="topbar-actions">
          <LocaleSwitcher />
          <button class="ghost-btn" type="button" @click="switchToAi">
            {{ t('common.switchToAi') }}
          </button>
          <button class="ghost-btn" type="button" @click="handleLogout">
            {{ t('common.logout') }}
          </button>
        </div>
      </header>
      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.classic-shell {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: 100svh;
  background: var(--fp-surface);
}

.sidebar {
  display: flex;
  flex-direction: column;
  padding: 22px 16px;
  background: linear-gradient(180deg, #12262d 0%, #0f1c24 100%);
  color: #e8efec;
}

.sidebar-brand {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 10px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 16px;
}

.brand-mark {
  font-family: var(--fp-font-display);
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.brand-sub {
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  color: rgba(232, 239, 236, 0.55);
}

.sidebar-nav {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.main-column {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 24px;
  border-bottom: 1px solid var(--fp-line);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(10px);
}

.page-title {
  margin: 0;
  font-family: var(--fp-font-display);
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--fp-ink);
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

.content {
  flex: 1;
  padding: 24px;
}

@media (max-width: 800px) {
  .classic-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 14px 16px;
  }

  .sidebar-brand {
    border-bottom: none;
    margin-bottom: 0;
    padding: 0;
  }

  .brand-sub {
    display: none;
  }

  .sidebar-nav {
    display: none;
  }

  .topbar {
    padding: 12px 16px;
  }

  .content {
    padding: 16px;
  }
}
</style>
