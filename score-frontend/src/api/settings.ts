import request from '@/utils/request'
import type { AiSettingView, ApiResponse, Term } from '@/types'

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
