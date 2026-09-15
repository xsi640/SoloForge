import { createRouter, createWebHashHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import { fetchCurrentUser } from '../api/auth'
import { clearUser, isLoggedIn, setUser } from '../stores/session'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/',
    name: 'board',
    component: () => import('../views/BoardView.vue')
  },
  {
    path: '/members',
    name: 'members',
    component: () => import('../views/MembersView.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.path === '/login' || isLoggedIn()) {
    return true
  }
  try {
    const user = await fetchCurrentUser()
    setUser(user)
    return true
  } catch (error) {
    clearUser()
    return { path: '/login' }
  }
})

export default router
