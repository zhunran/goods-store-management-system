import { get, post, put } from '@/utils/request'
import type { CaptchaVO, LoginRequest, LoginResponse, RegisterRequest } from './types'

/** 获取登录图形验证码（返回 base64 图片 + 会话 ID） */
export function getCaptcha() {
  return get<CaptchaVO>('/auth/captcha')
}

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
