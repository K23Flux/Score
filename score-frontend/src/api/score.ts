import request from '@/utils/request'
import type { ApiResponse, Score, ScoreGroupItem, StudentQueryParams, TeacherQueryParams, StudentQueryResponse } from '@/types'

/**
 * 学生查询成绩
 */
export function studentQuery(data: StudentQueryParams): Promise<ApiResponse<StudentQueryResponse>> {
  return request.post('/scoreCheck/StudentCheck', data)
}

/**
 * 教师查询成绩列表
 */
export function checkList(data: TeacherQueryParams): Promise<ApiResponse<ScoreGroupItem[]>> {
  return request.post('/scoreCheck/checkList', data)
}