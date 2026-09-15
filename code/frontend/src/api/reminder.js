import http from './http'

export function fetchMyReminders() {
  return http.get('/reminders/mine')
}
