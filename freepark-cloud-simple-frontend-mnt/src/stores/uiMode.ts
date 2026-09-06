import { ref, watch } from 'vue'
import { defineStore } from 'pinia'

export type UiMode = 'classic' | 'ai'

export const UI_MODE_STORAGE_KEY = 'fp_admin_ui_mode'

function resolveInitialMode(): UiMode {
  const saved = localStorage.getItem(UI_MODE_STORAGE_KEY)
  return saved === 'ai' ? 'ai' : 'classic'
}

export const useUiModeStore = defineStore('uiMode', () => {
  const mode = ref<UiMode>(resolveInitialMode())

  function setMode(next: UiMode) {
    mode.value = next
  }

  function homePath(): string {
    return mode.value === 'ai' ? '/ai' : '/'
  }

  watch(
    mode,
    (value) => {
      localStorage.setItem(UI_MODE_STORAGE_KEY, value)
    },
    { immediate: true }
  )

  return {
    mode,
    setMode,
    homePath
  }
})
