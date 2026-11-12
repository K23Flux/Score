import request from '@/utils/request'
import type { ApiResponse, ImportMapping, ImportPreview, ImportResult } from '@/types'

function buildForm(
  file: File,
  fields: Record<string, string>,
  mapping?: ImportMapping,
  ignoreErrors?: boolean,
): FormData {
  const formData = new FormData()
  formData.append('file', file)
  Object.entries(fields).forEach(([key, value]) => formData.append(key, value))
  if (mapping) formData.append('mapping', JSON.stringify(mapping))
  if (ignoreErrors !== undefined) formData.append('ignoreErrors', String(ignoreErrors))
  return formData
}

const config = { headers: { 'Content-Type': 'multipart/form-data' } }

interface TermFields {
  academicYear?: string
  semester?: string
}

function termFields(term?: TermFields): Record<string, string> {
  if (!term?.academicYear || !term?.semester) return {}
  return { academicYear: term.academicYear, semester: term.semester }
}

/**
 * 预览学生信息导入，返回列映射和逐行校验结果。
 */
export function previewStudentImport(
  file: File,
  className: string,
  mapping?: ImportMapping,
  term?: TermFields,
): Promise<ApiResponse<ImportPreview>> {
  return request.post(
    '/scoreCheck/import/student/preview',
    buildForm(file, { className, ...termFields(term) }, mapping),
    config,
  )
}

/**
 * 预览成绩导入，返回列映射和逐行校验结果。
 */
export function previewScoreImport(
  file: File,
  examName: string,
  className: string,
  mapping?: ImportMapping,
  term?: TermFields,
): Promise<ApiResponse<ImportPreview>> {
  return request.post(
    '/scoreCheck/import/score/preview',
    buildForm(file, { examName, className, ...termFields(term) }, mapping),
    config,
  )
}

/**
 * 按确认后的列映射提交学生信息。
 */
export function commitStudentImport(
  file: File,
  className: string,
  mapping: ImportMapping,
  ignoreErrors: boolean,
  term?: TermFields,
): Promise<ApiResponse<ImportResult>> {
  return request.post(
    '/scoreCheck/import/student/commit',
    buildForm(file, { className, ...termFields(term) }, mapping, ignoreErrors),
    config,
  )
}

/**
 * 按确认后的列映射提交成绩。
 */
export function commitScoreImport(
  file: File,
  examName: string,
  className: string,
  mapping: ImportMapping,
  ignoreErrors: boolean,
  term?: TermFields,
): Promise<ApiResponse<ImportResult>> {
  return request.post(
    '/scoreCheck/import/score/commit',
    buildForm(file, { examName, className, ...termFields(term) }, mapping, ignoreErrors),
    config,
  )
}

/**
 * 旧版直接导入接口，保留给旧页面或脚本使用。
 */
export function importStudent(file: File, className: string): Promise<ApiResponse> {
  return request.post('/scoreCheck/importStudent', buildForm(file, { className }), config)
}

export function importScore(file: File, examName: string, className: string): Promise<ApiResponse> {
  return request.post('/scoreCheck/importScore', buildForm(file, { examName, className }), config)
}
