import http from './http'

export function createTask(payload) {
  return http.post('/tasks', payload)
}

export function updateTask(id, payload) {
  return http.put(`/tasks/${id}`, payload)
}

export function deleteTask(id) {
  return http.delete(`/tasks/${id}`)
}

export function fetchTask(id) {
  return http.get(`/tasks/${id}`)
}

export function updateTaskStatus(id, columnId) {
  return http.patch(`/tasks/${id}/status`, { columnId })
}

function compactQuery(params) {
  const query = {}
  Object.keys(params).forEach((key) => {
    const value = params[key]
    if (value === undefined || value === null || value === '') {
      return
    }
    query[key] = value
  })
  return query
}

export function queryTasks(params = {}) {
  return http.get('/tasks', { params: compactQuery(params) })
}
