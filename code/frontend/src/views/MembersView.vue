<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchCurrentUser } from '../api/auth'
import { fieldErrorOf } from '../api/http'
import { createMember, listMembers, resetPassword, updateMemberStatus } from '../api/member'
import { isAdmin, sessionState, setUser } from '../stores/session'
import { showError, showSuccess } from '../stores/toast'

const CONTENT_005 = '成员列表加载失败，请检查内网连接后重试'
const CONTENT_006 = '操作失败，请重试'
const CONTENT_009 = '不可停用当前登录账号'
const CONTENT_010 = '登录名已存在，请更换'
const FORBIDDEN_TEXT = '当前账号没有成员管理权限，请返回看板；如需维护成员请联系管理员'
const EMPTY_TEXT = '点击右上角「新增成员」创建第一个成员账号'
const USERNAME_PATTERN = /^[A-Za-z0-9_]+$/
const STATUS_ENABLED = 'ENABLED'
const STATUS_DISABLED = 'DISABLED'

const ready = ref(false)
const loading = ref(true)
const loadError = ref('')
const members = ref([])

const formOpen = ref(false)
const saving = ref(false)
const form = reactive({ username: '', displayName: '', password: '', status: STATUS_ENABLED })
const formErrors = reactive({ username: '', displayName: '', password: '' })

const passwordTarget = ref(null)
const passwordValue = ref('')
const passwordError = ref('')
const passwordSaving = ref(false)

const statusTarget = ref(null)
const statusSaving = ref(false)

const admin = computed(() => isAdmin())
const forbidden = computed(() => ready.value && !admin.value)
const currentUserId = computed(() => (sessionState.currentUser ? sessionState.currentUser.id : null))
const statusNextValue = computed(() =>
  statusTarget.value && statusTarget.value.status === STATUS_ENABLED ? STATUS_DISABLED : STATUS_ENABLED
)
const statusIsDisable = computed(() => statusNextValue.value === STATUS_DISABLED)
const statusConfirmText = computed(() => {
  if (!statusTarget.value) {
    return ''
  }
  const name = statusTarget.value.displayName
  return statusIsDisable.value
    ? `停用后「${name}」将无法登录，其名下任务保留、负责人不变。`
    : `启用后「${name}」可以重新登录并使用任务看板。`
})

function goBoard() {
  window.location.hash = '#/'
}

function isSelf(member) {
  return currentUserId.value !== null && String(member.id) === String(currentUserId.value)
}

function statusLabel(member) {
  return member.status === STATUS_ENABLED ? '启用' : '停用'
}

function actionLabel(member) {
  return member.status === STATUS_ENABLED ? '停用' : '启用'
}

function disableBlocked(member) {
  return isSelf(member) && member.status === STATUS_ENABLED
}

function failureMessage(error) {
  if (error && error.code === 1008) {
    return FORBIDDEN_TEXT
  }
  if (error && error.code === 1006) {
    return '该成员不存在，列表已刷新'
  }
  return (error && error.message) || CONTENT_006
}

async function loadMembers() {
  if (!admin.value) {
    loading.value = false
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const list = await listMembers()
    members.value = Array.isArray(list) ? list : []
  } catch (error) {
    members.value = []
    loadError.value = error && error.code === 1008 ? FORBIDDEN_TEXT : CONTENT_005
  } finally {
    loading.value = false
  }
}

function clearFormErrors() {
  formErrors.username = ''
  formErrors.displayName = ''
  formErrors.password = ''
}

function resetForm() {
  form.username = ''
  form.displayName = ''
  form.password = ''
  form.status = STATUS_ENABLED
  clearFormErrors()
}

function openForm() {
  if (saving.value) {
    return
  }
  resetForm()
  formOpen.value = true
}

function closeForm() {
  if (saving.value) {
    return
  }
  formOpen.value = false
  resetForm()
}

function validateForm() {
  clearFormErrors()
  const username = form.username.trim()
  const displayName = form.displayName.trim()
  const password = form.password
  if (!username) {
    formErrors.username = '请输入登录名'
  } else if (username.length < 3 || username.length > 32) {
    formErrors.username = '登录名长度需为 3 至 32 个字符'
  } else if (!USERNAME_PATTERN.test(username)) {
    formErrors.username = '登录名只能包含字母、数字与下划线'
  }
  if (!displayName) {
    formErrors.displayName = '请输入显示名'
  } else if (displayName.length > 32) {
    formErrors.displayName = '显示名长度需为 1 至 32 个字符'
  }
  if (!password) {
    formErrors.password = '请输入密码'
  } else if (password.length < 6 || password.length > 64) {
    formErrors.password = '密码长度需为 6 至 64 个字符'
  }
  return !formErrors.username && !formErrors.displayName && !formErrors.password
}

function applyFormReasons(error) {
  formErrors.username = fieldErrorOf(error, 'username')
  formErrors.displayName = fieldErrorOf(error, 'displayName')
  formErrors.password = fieldErrorOf(error, 'password')
  if (error && error.code === 1005 && !formErrors.username) {
    formErrors.username = CONTENT_010
  }
  if (!formErrors.username && !formErrors.displayName && !formErrors.password) {
    showError(failureMessage(error))
  }
}

function applyCreated(created) {
  if (created && created.id !== undefined && created.id !== null) {
    members.value = [...members.value, created]
    return
  }
  loadMembers()
}

async function submitForm() {
  if (saving.value) {
    return
  }
  if (!validateForm()) {
    return
  }
  saving.value = true
  try {
    const created = await createMember({
      username: form.username.trim(),
      displayName: form.displayName.trim(),
      password: form.password,
      status: form.status
    })
    formOpen.value = false
    resetForm()
    applyCreated(created)
    showSuccess('成员已创建')
  } catch (error) {
    applyFormReasons(error)
  } finally {
    saving.value = false
  }
}

function resetPasswordModal() {
  passwordTarget.value = null
  passwordValue.value = ''
  passwordError.value = ''
}

function openPasswordModal(member) {
  passwordTarget.value = member
  passwordValue.value = ''
  passwordError.value = ''
}

function closePasswordModal() {
  if (passwordSaving.value) {
    return
  }
  resetPasswordModal()
}

async function submitPassword() {
  const member = passwordTarget.value
  if (!member || passwordSaving.value) {
    return
  }
  if (!passwordValue.value) {
    passwordError.value = '请输入新密码'
    return
  }
  if (passwordValue.value.length < 6 || passwordValue.value.length > 64) {
    passwordError.value = '密码长度需为 6 至 64 个字符'
    return
  }
  passwordError.value = ''
  passwordSaving.value = true
  try {
    await resetPassword(member.id, passwordValue.value)
    resetPasswordModal()
    showSuccess('密码已重置')
  } catch (error) {
    const reason = fieldErrorOf(error, 'password')
    if (reason) {
      passwordError.value = reason
    } else if (error && error.code === 1006) {
      passwordError.value = '该成员不存在，列表已刷新'
      loadMembers()
    } else {
      resetPasswordModal()
      showError(failureMessage(error))
    }
  } finally {
    passwordSaving.value = false
  }
}

function openStatusModal(member) {
  if (disableBlocked(member)) {
    return
  }
  statusTarget.value = member
}

function closeStatusModal() {
  if (statusSaving.value) {
    return
  }
  statusTarget.value = null
}

function applyUpdated(memberId, updated) {
  const index = members.value.findIndex((item) => item.id === memberId)
  if (index === -1) {
    return
  }
  if (updated && updated.id !== undefined && updated.id !== null) {
    members.value.splice(index, 1, updated)
    return
  }
  loadMembers()
}

async function submitStatusChange() {
  const member = statusTarget.value
  if (!member || statusSaving.value) {
    return
  }
  const nextStatus = statusNextValue.value
  statusSaving.value = true
  try {
    const updated = await updateMemberStatus(member.id, nextStatus)
    statusTarget.value = null
    applyUpdated(member.id, updated)
    showSuccess(nextStatus === STATUS_DISABLED ? '成员已停用' : '成员已启用')
  } catch (error) {
    statusTarget.value = null
    if (error && error.code === 1006) {
      loadMembers()
    }
    showError(failureMessage(error))
  } finally {
    statusSaving.value = false
  }
}

async function bootstrap() {
  if (!sessionState.currentUser) {
    try {
      setUser(await fetchCurrentUser())
    } catch (error) {
      setUser(null)
    }
  }
  ready.value = true
  if (admin.value) {
    await loadMembers()
    return
  }
  loading.value = false
}

onMounted(bootstrap)
</script>

<template>
  <div v-if="forbidden" class="content">
    <button class="back" type="button" @click="goBoard">← 返回看板</button>
    <div class="content-head">
      <h1>成员管理</h1>
    </div>
    <div class="panel">
      <div class="empty">
        <span class="ic">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#64748B" stroke-width="2" stroke-linecap="round">
            <rect x="4.5" y="10.5" width="15" height="9.5" rx="2" />
            <path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" />
          </svg>
        </span>
        <h5>无权限访问</h5>
        <p>{{ FORBIDDEN_TEXT }}</p>
        <div class="acts">
          <button class="btn btn-primary" type="button" @click="goBoard">返回看板</button>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="content">
    <button class="back" type="button" @click="goBoard">← 返回看板</button>
    <div class="content-head">
      <h1>成员管理</h1>
      <button class="btn btn-primary" type="button" :disabled="formOpen || loading || saving" @click="openForm">
        ＋ 新增成员
      </button>
    </div>

    <div v-if="formOpen" class="panel">
      <div class="panel-title">新增成员</div>
      <form class="form-row" novalidate @submit.prevent="submitForm">
        <div class="field">
          <label for="member-username">登录名 <span class="req">*</span></label>
          <input
            id="member-username"
            v-model="form.username"
            class="control"
            type="text"
            autocomplete="off"
            placeholder="字母、数字或下划线"
          />
          <p v-if="formErrors.username" class="err">{{ formErrors.username }}</p>
        </div>
        <div class="field">
          <label for="member-display-name">显示名 <span class="req">*</span></label>
          <input
            id="member-display-name"
            v-model="form.displayName"
            class="control"
            type="text"
            autocomplete="off"
            placeholder="团队成员看到的名字"
          />
          <p v-if="formErrors.displayName" class="err">{{ formErrors.displayName }}</p>
        </div>
        <div class="field">
          <label for="member-password">密码 <span class="req">*</span></label>
          <input
            id="member-password"
            v-model="form.password"
            class="control"
            type="password"
            autocomplete="new-password"
            placeholder="至少 6 位"
          />
          <p v-if="formErrors.password" class="err">{{ formErrors.password }}</p>
        </div>
        <div class="field">
          <label for="member-status">状态</label>
          <select id="member-status" v-model="form.status" class="control">
            <option value="ENABLED">启用</option>
            <option value="DISABLED">停用</option>
          </select>
        </div>
        <div class="btns">
          <button class="btn btn-primary" type="submit" :disabled="saving">
            <span v-if="saving" class="spin"></span>
            {{ saving ? '保存中' : '保存' }}
          </button>
          <button class="btn btn-secondary" type="button" :disabled="saving" @click="closeForm">取消</button>
        </div>
      </form>
    </div>

    <div class="panel">
      <div v-if="loading" class="empty">
        <span class="ic"><span class="spin dark"></span></span>
        <h5>正在加载成员列表</h5>
      </div>

      <div v-else-if="loadError" class="state-pad">
        <div class="notice">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#DC2626" stroke-width="2" stroke-linecap="round">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7.5v5.5" />
            <path d="M12 16.5v.01" />
          </svg>
          <span>{{ loadError }}</span>
        </div>
        <div class="retry-row">
          <button class="btn btn-secondary" type="button" @click="loadMembers">重试</button>
        </div>
      </div>

      <div v-else-if="members.length === 0" class="empty">
        <span class="ic">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#64748B" stroke-width="2" stroke-linecap="round">
            <circle cx="12" cy="8.5" r="3.5" />
            <path d="M5 20c0-3.3 3.1-6 7-6s7 2.7 7 6" />
          </svg>
        </span>
        <h5>还没有成员</h5>
        <p>{{ EMPTY_TEXT }}</p>
      </div>

      <table v-else>
        <thead>
          <tr>
            <th class="col-login">登录名</th>
            <th>显示名</th>
            <th class="col-status">状态</th>
            <th class="col-actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="member in members" :key="member.id">
            <td>{{ member.username }}</td>
            <td>
              {{ member.displayName }}
              <span v-if="member.isAdmin" class="tag admin">管理员</span>
            </td>
            <td>
              <span class="tag" :class="member.status === 'ENABLED' ? 'on' : 'off'">{{ statusLabel(member) }}</span>
            </td>
            <td>
              <div class="row-actions">
                <button class="btn btn-secondary btn-sm" type="button" @click="openPasswordModal(member)">重置密码</button>
                <template v-if="disableBlocked(member)">
                  <button class="btn btn-secondary btn-sm" type="button" disabled>{{ actionLabel(member) }}</button>
                  <span class="row-hint">{{ CONTENT_009 }}</span>
                </template>
                <button v-else class="btn btn-secondary btn-sm" type="button" @click="openStatusModal(member)">
                  {{ actionLabel(member) }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <div v-if="passwordTarget" class="overlay" @click.self="closePasswordModal">
    <form class="modal" novalidate @submit.prevent="submitPassword">
      <div class="modal-head">重置密码</div>
      <div class="modal-body">
        <p class="confirm-text">为成员「{{ passwordTarget.displayName }}」设置新密码，保存后立即生效。</p>
        <div class="field">
          <label for="reset-password">新密码 <span class="req">*</span></label>
          <input
            id="reset-password"
            v-model="passwordValue"
            class="control"
            type="password"
            autocomplete="new-password"
            placeholder="至少 6 位"
          />
          <p v-if="passwordError" class="err">{{ passwordError }}</p>
        </div>
      </div>
      <div class="modal-foot">
        <button class="btn btn-secondary" type="button" :disabled="passwordSaving" @click="closePasswordModal">取消</button>
        <button class="btn btn-primary" type="submit" :disabled="passwordSaving">
          <span v-if="passwordSaving" class="spin"></span>
          {{ passwordSaving ? '保存中' : '确认' }}
        </button>
      </div>
    </form>
  </div>

  <div v-if="statusTarget" class="overlay" @click.self="closeStatusModal">
    <div class="modal w400">
      <div class="modal-head">{{ statusIsDisable ? '停用成员' : '启用成员' }}</div>
      <div class="modal-body">
        <p class="confirm-text">{{ statusConfirmText }}</p>
      </div>
      <div class="modal-foot">
        <button class="btn btn-secondary" type="button" :disabled="statusSaving" @click="closeStatusModal">取消</button>
        <button
          class="btn"
          :class="statusIsDisable ? 'btn-danger' : 'btn-primary'"
          type="button"
          :disabled="statusSaving"
          @click="submitStatusChange"
        >
          <span v-if="statusSaving" class="spin"></span>
          {{ statusSaving ? '处理中' : statusIsDisable ? '确认停用' : '确认启用' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  padding: 0;
  background: none;
  border: 0;
  color: #2563EB;
  font-size: 13px;
}
.back:hover {
  color: #1D4ED8;
  text-decoration: underline;
}
.back:focus-visible {
  outline: 2px solid #2563EB;
  outline-offset: 2px;
}
.form-row .btns {
  padding-top: 22px;
}
.col-login {
  width: 180px;
}
.col-status {
  width: 120px;
}
.col-actions {
  width: 300px;
}
.row-hint {
  align-self: center;
  font-size: 12px;
  color: #94A3B8;
}
.state-pad {
  padding: 16px;
}
.retry-row {
  display: flex;
  justify-content: center;
  margin-top: 14px;
}
</style>
