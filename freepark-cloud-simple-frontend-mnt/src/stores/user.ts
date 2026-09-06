import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { clearUser, getUser, setUser, type CurrentUser } from '../utils/auth'

export const ROLE_SUPER_ADMIN = 'SUPER_ADMIN'

export const useUserStore = defineStore('user', () => {
  const user = ref<CurrentUser | null>(getUser())

  const isSuperAdmin = computed(() => user.value?.role === ROLE_SUPER_ADMIN)

  function save(next: CurrentUser) {
    user.value = next
    setUser(next)
  }

  function clear() {
    user.value = null
    clearUser()
  }

  return {
    user,
    isSuperAdmin,
    save,
    clear
  }
})
