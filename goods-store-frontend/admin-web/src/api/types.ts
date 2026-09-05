export interface UserInfo {
  userId: number
  account: string
  type: string
  roles: string[]
  permissions: string[]
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  userInfo: UserInfo
}

export interface LoginRequest {
  account: string
  password: string
  loginType?: string
}
