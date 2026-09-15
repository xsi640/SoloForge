import { reactive } from 'vue'

export const toastState = reactive({
  items: []
})

let seed = 0

export function dismissToast(id) {
  const index = toastState.items.findIndex((item) => item.id === id)
  if (index !== -1) {
    toastState.items.splice(index, 1)
  }
}

export function showToast(message, type = 'success', duration = 2400) {
  if (!message) {
    return
  }
  seed += 1
  const id = seed
  toastState.items.push({
    id,
    message,
    type: type === 'error' ? 'error' : 'success'
  })
  if (duration > 0) {
    window.setTimeout(() => dismissToast(id), duration)
  }
}

export function showSuccess(message) {
  showToast(message, 'success')
}

export function showError(message) {
  showToast(message, 'error')
}
