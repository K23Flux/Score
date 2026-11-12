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
  academicYear?: string
  semester?: string
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
  academicYear?: string
  semester?: string
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
  role?: string
  createTime?: string
  delFlag?: string
}

/**
 * 登录请求参数
 */
export interface LoginParams {
  userName: string
  password: string
  captchaId?: string
}

/**
 * 登录策略：是否需要滑块验证、是否已被限速
 */
export interface LoginPolicy {
  captchaRequired: boolean
  rateLimited: boolean
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
  academicYear?: string
  semester?: string
}

/**
 * 教师成绩查询参数
 */
export interface TeacherQueryParams {
  studentName?: string
  examName?: string
  academicYear?: string
  semester?: string
}

/**
 * 学生列表查询参数（分页）
 */
export interface StudentListParams {
  className?: string
  pageNum?: number
  pageSize?: number
  academicYear?: string
  semester?: string
}

/**
 * 学年学期
 */
export interface Term {
  id: number
  academicYear: string
  semester: string
  name: string
  current: boolean
}

/**
 * AI 表头识别设置（密钥只返回掩码）
 */
export interface AiSettingView {
  enabled: boolean
  baseUrl: string
  model: string
  apiKeySet: boolean
  apiKeyMasked: string
  ready: boolean
}

/**
 * 登录结果
 */
export interface LoginResult {
  token: string
  role?: string
  className?: string
}

/**
 * 当前登录账号
 */
export interface CurrentUser {
  userName: string
  className: string
  role: string
}

/**
 * 导入文件中的一列
 */
export interface ImportColumn {
  index: number
  header: string
  sample: string
}

/**
 * 列映射（列索引）
 */
export interface ImportMapping {
  nameColumn: number | null
  idColumn: number | null
  subjectColumns: number[]
  ignoredColumns: number[]
}

/**
 * 导入预览行
 */
export interface ImportPreviewRow {
  rowNumber: number
  studentName: string
  idCard: string
  className: string
  subjects: Record<string, string>
  valid: boolean
  message?: string
}

/**
 * 导入校验错误
 */
export interface ImportRowError {
  rowNumber: number
  message: string
}

/**
 * 导入预览结果
 */
export interface ImportPreview {
  columns: ImportColumn[]
  mapping: ImportMapping
  mappingSource: string
  aiEnabled: boolean
  rows: ImportPreviewRow[]
  errors: ImportRowError[]
  headerErrors: string[]
  totalRows: number
  validRows: number
  invalidRows: number
}

/**
 * 导入提交结果
 */
export interface ImportResult {
  inserted: number
  updated: number
  skipped: number
  message: string
}