import { post, put } from '@/utils/request'
import type { LoginRequest, LoginResponse, RegisterRequest } from './types'

/** C 端会员登录（loginType 固定为 member） */
export function login(data: LoginRequest) {
  return post<LoginResponse>('/auth/login', { ...data, loginType: 'member' })
}

export function register(data: RegisterRequest) {
  return post<void>('/auth/register', data)
}

export function logout() {
  return post<void>('/auth/logout')
}

export function changePassword(oldPassword: string, newPassword: string) {
  return put<void>('/auth/password', { oldPassword, newPassword })
}
