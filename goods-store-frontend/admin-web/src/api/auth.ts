import { post } from '@/utils/request'
import type { LoginRequest, LoginResponse } from './types'

export function adminLogin(data: LoginRequest) {
  return post<LoginResponse>('/auth/admin/login', data)
}
