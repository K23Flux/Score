import request from '@/utils/request'
import type { ApiResponse, CaptchaData } from '@/types'

/**
 * 获取滑块验证码
 */
export function getCaptcha(): Promise<ApiResponse<CaptchaData>> {
  return request.get('/captcha/get')
}

/**
 * 校验滑块验证码
 * @param captchaId 验证码ID
 * @param moveX 滑动距离
 */
export function checkCaptcha(captchaId: string, moveX: number): Promise<ApiResponse<string>> {
  return request.post('/captcha/check', { captchaId, moveX })
}