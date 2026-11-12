import request from '@/utils/request'
import type {
  ApiResponse,
  CurrentUser,
  GithubStatus,
  LoginParams,
  LoginPolicy,
  LoginResult,
} from '@/types'

/**
 * 教师登录
 */
export function login(data: LoginParams): Promise<ApiResponse<LoginResult>> {
  return request.post('/scoreCheck/login', data)
}

/**
 * 当前登录账号（角色以数据库为准）
 */
export function getMe(): Promise<ApiResponse<CurrentUser>> {
  return request.get('/scoreCheck/me')
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

/**
 * GitHub 登录是否已配置
 */
export function getGithubStatus(): Promise<ApiResponse<GithubStatus>> {
  return request.get('/scoreCheck/auth/github/status')
}

/**
 * 获取 GitHub 登录授权地址
 */
export function getGithubLoginUrl(): Promise<ApiResponse<{ url: string }>> {
  return request.get('/scoreCheck/auth/github/authorize')
}

/**
 * 用 GitHub 回调带回的一次性票据换取登录态
 */
export function exchangeTicket(ticket: string): Promise<ApiResponse<LoginResult & { userName?: string }>> {
  return request.post('/scoreCheck/auth/ticket', { ticket })
}