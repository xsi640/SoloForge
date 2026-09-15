import axios from 'axios'
import { clearUser } from '../stores/session'

const DEFAULT_MESSAGES = {
  1001: '参数校验失败',
  1002: '未登录或会话失效',
  1003: '账号已停用，请联系管理员',
  1004: '账号或密码错误',
  1005: '登录名已存在',
  1006: '目标不存在',
  1007: '不允许的操作',
  1008: '无权限访问',
  9999: '操作失败，请重试'
}

const STATUS_CODES = {
  400: 1001,
  401: 1002,
  403: 1008,
  404: 1006,
  500: 9999
}

const SESSION_EXPIRED_CODES = [1002, 1003]

function toFieldErrors(data) {
  if (!data) {
    return []
  }
  const list = Array.isArray(data) ? data : [data]
  return list
    .filter((item) => item && typeof item.field === 'string')
    .map((item) => ({ field: item.field, reason: item.reason || '' }))
}

function createError(code, message, data) {
  return {
    code,
    message: message || DEFAULT_MESSAGES[code] || DEFAULT_MESSAGES[9999],
    fieldErrors: toFieldErrors(data)
  }
}

export function fieldErrorOf(error, field) {
  if (!error || !Array.isArray(error.fieldErrors)) {
    return ''
  }
  const hit = error.fieldErrors.find((item) => item.field === field)
  return hit ? hit.reason : ''
}

function clearSessionAndRedirect() {
  clearUser()
  if (window.location.hash !== '#/login') {
    window.location.hash = '#/login'
  }
}

const http = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (!body || typeof body !== 'object' || typeof body.code !== 'number') {
      return body
    }
    if (body.code === 0) {
      return body.data
    }
    return Promise.reject(createError(body.code, body.message, body.data))
  },
  (error) => {
    const response = error.response
    if (!response) {
      return Promise.reject(createError(9999, '网络异常，请稍后重试', null))
    }
    const body = response.data && typeof response.data === 'object' ? response.data : null
    const code = body && typeof body.code === 'number' ? body.code : STATUS_CODES[response.status] || 9999
    const message = body && body.message ? body.message : DEFAULT_MESSAGES[code]
    if (response.status === 401 && SESSION_EXPIRED_CODES.includes(code)) {
      clearSessionAndRedirect()
    }
    return Promise.reject(createError(code, message, body ? body.data : null))
  }
)

export default http
