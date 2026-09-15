import http from './http'

export function listMembers() {
  return http.get('/members')
}

export function createMember(payload) {
  return http.post('/members', payload)
}

export function resetPassword(id, password) {
  return http.put(`/members/${id}/password`, { password })
}

export function updateMemberStatus(id, status) {
  return http.put(`/members/${id}/status`, { status })
}
