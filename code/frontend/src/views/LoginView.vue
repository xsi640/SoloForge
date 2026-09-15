<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { fieldErrorOf } from '../api/http'
import { setUser } from '../stores/session'

const router = useRouter()

const form = reactive({
  username: '',
  password: ''
})

const fieldErrors = reactive({
  username: '',
  password: ''
})

const errorMessage = ref('')
const submitting = ref(false)

function clearErrors() {
  fieldErrors.username = ''
  fieldErrors.password = ''
  errorMessage.value = ''
}

function applyFieldErrors(error) {
  fieldErrors.username = fieldErrorOf(error, 'username')
  fieldErrors.password = fieldErrorOf(error, 'password')
}

function messageOf(error) {
  if (error.code === 1004) {
    return '账号或密码错误'
  }
  if (error.code === 1003) {
    return '账号已停用，请联系管理员'
  }
  return error.message || '操作失败，请重试'
}

async function handleSubmit() {
  if (submitting.value) {
    return
  }
  clearErrors()
  submitting.value = true
  try {
    const user = await login(form.username, form.password)
    setUser(user)
    await router.replace({ path: '/' })
  } catch (error) {
    applyFieldErrors(error)
    if (!fieldErrors.username && !fieldErrors.password) {
      errorMessage.value = messageOf(error)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <form class="login-card" novalidate @submit.prevent="handleSubmit">
      <div class="brand-lg">
        <span class="mark-lg">S</span>
        <h1>任务看板</h1>
      </div>
      <p class="sub">团队任务记录与进度跟踪</p>
      <p v-if="errorMessage" class="alert">{{ errorMessage }}</p>
      <div class="field">
        <label for="login-username">登录名</label>
        <input
          id="login-username"
          v-model="form.username"
          class="control"
          name="username"
          type="text"
          autocomplete="username"
          placeholder="请输入登录名"
        />
        <p v-if="fieldErrors.username" class="err">{{ fieldErrors.username }}</p>
      </div>
      <div class="field">
        <label for="login-password">密码</label>
        <input
          id="login-password"
          v-model="form.password"
          class="control"
          name="password"
          type="password"
          autocomplete="current-password"
          placeholder="请输入密码"
        />
        <p v-if="fieldErrors.password" class="err">{{ fieldErrors.password }}</p>
      </div>
      <button class="btn btn-primary" type="submit" :disabled="submitting">
        <span v-if="submitting" class="spin dark"></span>
        {{ submitting ? '登录中' : '登录' }}
      </button>
      <p class="hint">账号由管理员分配，如无法登录请联系管理员</p>
    </form>
  </div>
</template>
