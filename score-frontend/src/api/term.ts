import request from '@/utils/request'
import type { ApiResponse, Term } from '@/types'

/**
 * 学期列表（学生查分页与教师端共用，无需登录）
 */
export function getTerms(): Promise<ApiResponse<Term[]>> {
  return request.get('/scoreCheck/terms')
}
