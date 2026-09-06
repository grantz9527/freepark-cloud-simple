const TOKEN_KEY = 'fp_admin_token'
const USER_KEY = 'fp_admin_user'

export interface CurrentUser {
  username: string
  nickname?: string
  role?: string
}

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUser(): CurrentUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as CurrentUser
  } catch {
    return null
  }
}

export function setUser(user: CurrentUser): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearUser(): void {
  localStorage.removeItem(USER_KEY)
}

/** 退出/登录失效时清除本地登录态 */
export function clearAuth(): void {
  removeToken()
  clearUser()
}
