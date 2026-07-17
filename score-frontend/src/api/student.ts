import request from '@/utils/request'
import type { ApiResponse, PageResult, Student, StudentListParams } from '@/types'

/**
 * 查询学生列表
 */
export function getStudentList(data: StudentListParams): Promise<ApiResponse<PageResult<Student>>> {
  return request.post('/scoreCheck/getStudent', data)
}
