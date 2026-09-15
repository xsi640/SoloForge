import http from './http'

export function login(username, password) {
  return http.post('/auth/login', { username, password })
}

export function logout() {
  return http.post('/auth/logout')
}

export function fetchCurrentUser() {
  return http.get('/auth/me')
}
