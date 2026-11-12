import request from '@/utils/request'
import type { AdminUser, ApiResponse, PageResult } from '@/types'

/**
 * 获取用户列表
 */
export function getUserList(pageNum: number = 1, pageSize: number = 10): Promise<ApiResponse<PageResult<AdminUser>>> {
  return request.get('/scoreCheck/getUserList', { params: { pageNum, pageSize } })
}

/**
 * 创建用户
 */
export function createUser(data: AdminUser): Promise<ApiResponse> {
  return request.post('/scoreCheck/creatAdmin', data)
}

/**
 * 更新用户
 */
export function updateUser(data: AdminUser): Promise<ApiResponse> {
  return request.post('/scoreCheck/updateUser', data)
}

/**
 * 删除用户
 */
export function deleteUser(userName: string): Promise<ApiResponse> {
  return request.delete(`/scoreCheck/${encodeURIComponent(userName)}`)
}
