import request from '@/utils/request'
import type { ApiResponse, LoginParams, LoginPolicy } from '@/types'

/**
 * 教师登录
 */
export function login(data: LoginParams): Promise<ApiResponse<{ token: string }>> {
  return request.post('/scoreCheck/login', data)
}

/**
 * 登录前查询是否需要滑块验证或已被限速
 */
export function getLoginPolicy(userName: string): Promise<ApiResponse<LoginPolicy>> {
  return request.get('/scoreCheck/loginPolicy', { params: { userName } })
}

/**
 * 退出登录
 */
export function logout(): Promise<ApiResponse> {
  return request.post('/scoreCheck/logout')
}