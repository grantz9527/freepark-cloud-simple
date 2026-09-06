import request from '../utils/request'

export interface LoginResult {
  token: string
  username: string
  nickname: string
  role: string
}

export interface UserItem {
  id: number
  username: string
  nickname: string
  role: string
  status: number
  createdAt: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 登录 */
export function login(username: string, password: string) {
  return request.post<never, LoginResult>('/user/login', { username, password })
}

/** 当前登录管理员信息 */
export function fetchMe() {
  return request.get<never, UserItem>('/user/me')
}

/** 分页搜索管理员列表（仅超管） */
export function fetchAdmins(params: { keyword?: string; page?: number; size?: number }) {
  return request.get<never, PageResult<UserItem>>('/user/admin/list', { params })
}

/** 新增普通管理员（仅超管） */
export function createAdmin(payload: { username: string; password: string; nickname?: string }) {
  return request.post<never, UserItem>('/user/admin', payload)
}

/** 启用 / 停用管理员（仅超管） */
export function updateAdminStatus(id: number, status: number) {
  return request.put<never, void>(`/user/admin/${id}/status`, { status })
}

/** 重置管理员密码（仅超管） */
export function resetAdminPassword(id: number, password: string) {
  return request.put<never, void>(`/user/admin/${id}/password`, { password })
}
