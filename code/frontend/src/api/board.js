import http from './http'

export function fetchBoard() {
  return http.get('/board')
}

export function fetchColumns() {
  return http.get('/board/columns')
}
