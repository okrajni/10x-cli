import { apiClient, ApiResult } from '@lib/api/client'
import { User } from './types'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  user: User
  token: string
  refreshToken: string
}

export async function loginApi(request: LoginRequest): Promise<ApiResult<LoginResponse>> {
  return apiClient('/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export interface RefreshTokenRequest {
  refreshToken: string
}

export interface RefreshTokenResponse {
  token: string
  expiresAt: number
}

export async function refreshTokenApi(
  request: RefreshTokenRequest
): Promise<ApiResult<RefreshTokenResponse>> {
  return apiClient('/auth/refresh', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export interface RegisterRequest {
  email: string
  password: string
}

export interface RegisterResponse {
  user: User
  token: string
  refreshToken?: string
}

export async function registerApi(request: RegisterRequest): Promise<ApiResult<RegisterResponse>> {
  return apiClient('/auth/register', {
    method: 'POST',
    body: JSON.stringify(request),
  })
}

export async function logoutApi(): Promise<ApiResult<void>> {
  return apiClient('/auth/logout', {
    method: 'POST',
  })
}
