import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('satoken')
    if (token && config.headers) config.headers['satoken'] = token
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') return response
    if (res.code !== 200) {
      if (res.code === 401) {
        localStorage.removeItem('satoken')
        localStorage.removeItem('userName')
        localStorage.removeItem('className')
        ElMessage.warning('登录已过期，请重新登录')
        setTimeout(() => router.push('/login'), 1500)
      } else {
        ElMessage.error(res.msg || '请求失败')
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    console.error('响应错误:', error)
    let message = error.message
    if (message === 'Network Error') message = '网络连接异常'
    else if (message.includes('timeout')) message = '请求超时，请稍后重试'
    else if (error.response?.status === 401) {
      localStorage.removeItem('satoken')
      localStorage.removeItem('userName')
      localStorage.removeItem('className')
      router.push('/login')
      return Promise.reject(error)
    }
    ElMessage.error(message || '系统异常')
    return Promise.reject(error)
  }
)

export default request
