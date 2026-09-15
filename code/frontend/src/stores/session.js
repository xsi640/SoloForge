import { reactive } from 'vue'

export const sessionState = reactive({
  currentUser: null
})

export function setUser(user) {
  sessionState.currentUser = user || null
}

export function clearUser() {
  sessionState.currentUser = null
}

export function isLoggedIn() {
  return sessionState.currentUser !== null
}

export function isAdmin() {
  return sessionState.currentUser !== null && sessionState.currentUser.isAdmin === true
}
