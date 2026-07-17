/**
 * API 响应结果统一封装
 */
export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
}

/**
 * 分页结果
 */
export interface PageResult<T> {
  total: number
  records: T[]
}

/**
 * 学生信息
 */
export interface Student {
  id?: number
  studentName: string
  className: string
  idCard: string
  createTime?: string
  delFlag?: string
}

/**
 * 成绩信息
 */
export interface Score {
  id?: number
  studentName: string
  className: string
  idCard: string
  examName: string
  subject: string
  score: number
  createTime?: string
  delFlag?: string
}

/**
 * 成绩查询结果（教师端聚合后的格式）
 */
export interface ScoreGroupItem {
  studentName: string
  examName: string
  className: string
  createTime: string
  subjects: Record<string, number>
  totalScore: number
}

/**
 * 管理员用户
 */
export interface AdminUser {
  id?: number
  userName: string
  password?: string
  className: string
  createTime?: string
  delFlag?: string
}

/**
 * 登录请求参数
 */
export interface LoginParams {
  userName: string
  password: string
  captchaId: string
}

/**
 * 验证码信息
 */
export interface CaptchaData {
  captchaId: string
  backgroundImage: string
  sliderImage: string
}

/**
 * 学生成绩查询响应
 */
export interface StudentQueryResponse {
  score: Score[]
  totalScore: number
  subjectCount: number
}

/**
 * 学生成绩查询参数
 */
export interface StudentQueryParams {
  studentName: string
  idCard: string
}

/**
 * 教师成绩查询参数
 */
export interface TeacherQueryParams {
  studentName?: string
  examName?: string
}

/**
 * 学生列表查询参数（分页）
 */
export interface StudentListParams {
  className?: string
  pageNum?: number
  pageSize?: number
}