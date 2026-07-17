import request from '@/utils/request'
import type { ApiResponse, LoginParams } from '@/types'

/**
 * 教师登录
 */
export function login(data: LoginParams): Promise<ApiResponse<{ token: string }>> {
  return request.post('/scoreCheck/login', data)
}

/**
 * 退出登录
 */
export function logout(): Promise<ApiResponse> {
  return request.post('/scoreCheck/logout')
}