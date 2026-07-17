import request from '@/utils/request'
import type { ApiResponse } from '@/types'

/**
 * 导入学生信息
 * @param file Excel文件
 * @param className 班级名称
 */
export function importStudent(file: File, className: string): Promise<ApiResponse> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('className', className)
  return request.post('/scoreCheck/importStudent', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 导入成绩
 * @param file Excel文件
 * @param examName 考试名称
 * @param className 班级名称
 */
export function importScore(file: File, examName: string, className: string): Promise<ApiResponse> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('examName', examName)
  formData.append('className', className)
  return request.post('/scoreCheck/importScore', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}