import request from '@/utils/request'
import type { AiSettingView, ApiResponse, GithubBinding, Term } from '@/types'

/**
 * 读取 AI 表头识别设置（仅管理员）
 */
export function getAiSettings(): Promise<ApiResponse<AiSettingView>> {
  return request.get('/scoreCheck/settings/ai')
}

/**
 * 保存 AI 表头识别设置（仅管理员）
 */
export function updateAiSettings(data: {
  enabled?: boolean
  baseUrl?: string
  model?: string
  apiKey?: string
  clearApiKey?: boolean
}): Promise<ApiResponse> {
  return request.put('/scoreCheck/settings/ai', data)
}

/**
 * 测试 AI 接口连通性（仅管理员）
 */
export function testAiSettings(data: {
  baseUrl?: string
  model?: string
  apiKey?: string
}): Promise<ApiResponse<{ ok: boolean; latencyMs: number; reply: string; error: string | null }>> {
  return request.post('/scoreCheck/settings/ai/test', data)
}

/**
 * 新建学期（仅管理员）
 */
export function createTerm(data: {
  academicYear: string
  semester: string
  current?: boolean
}): Promise<ApiResponse<Term>> {
  return request.post('/scoreCheck/settings/terms', data)
}

/**
 * 设为当前学期（仅管理员）
 */
export function setCurrentTerm(id: number): Promise<ApiResponse> {
  return request.post(`/scoreCheck/settings/terms/${id}/current`)
}

/**
 * 管理员 GitHub 绑定状态
 */
export function getGithubBinding(): Promise<ApiResponse<GithubBinding>> {
  return request.get('/scoreCheck/settings/github')
}

/**
 * 获取绑定 GitHub 的授权地址
 */
export function getGithubBindUrl(): Promise<ApiResponse<{ url: string }>> {
  return request.get('/scoreCheck/settings/github/authorize')
}

/**
 * 解除 GitHub 绑定
 */
export function unbindGithub(): Promise<ApiResponse> {
  return request.post('/scoreCheck/settings/github/unbind')
}

/**
 * 在线保存 GitHub OAuth App 凭据
 */
export function updateGithubConfig(data: {
  clientId?: string
  clientSecret?: string
  clearClientSecret?: boolean
}): Promise<ApiResponse> {
  return request.put('/scoreCheck/settings/github/config', data)
}
