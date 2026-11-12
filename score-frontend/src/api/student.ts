import request from '@/utils/request'
import type { ApiResponse, PageResult, Student, StudentListParams } from '@/types'

/**
 * 查询学生列表
 */
export function getStudentList(data: StudentListParams): Promise<ApiResponse<PageResult<Student>>> {
  return request.post('/scoreCheck/getStudent', data)
}

/**
 * 编辑学生联系方式（学生手机号、家长信息）
 */
export function updateStudentContact(data: {
  id: number
  phone?: string
  parentName?: string
  parentPhone?: string
  parentRelation?: string
}): Promise<ApiResponse> {
  return request.post('/scoreCheck/updateStudent', data)
}
