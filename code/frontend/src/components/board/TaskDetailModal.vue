<script setup>
import { reactive, ref, watch } from 'vue'
import { fieldErrorOf } from '../../api/http'
import { deleteTask, fetchTask, updateTask, updateTaskStatus } from '../../api/task'
import { showError } from '../../stores/toast'

const TITLE_MAX = 100
const DESCRIPTION_MAX = 1000
const PRIORITY_OPTIONS = [
  { value: 'HIGH', label: '高' },
  { value: 'MEDIUM', label: '中' },
  { value: 'LOW', label: '低' }
]
const ERROR_FIELDS = ['title', 'assigneeId', 'columnId', 'priority', 'dueDate', 'description']

const props = defineProps({
  visible: { type: Boolean, required: true },
  taskId: { type: Number, required: true },
  columns: { type: Array, required: true },
  members: { type: Array, required: true }
})

const emit = defineEmits(['close', 'updated', 'deleted'])

const form = reactive({
  title: '',
  assigneeId: '',
  columnId: '',
  priority: 'MEDIUM',
  dueDate: '',
  description: ''
})
const errors = reactive({
  title: '',
  assigneeId: '',
  columnId: '',
  priority: '',
  dueDate: '',
  description: ''
})
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const confirming = ref(false)
const original = ref(null)

function clearErrors() {
  ERROR_FIELDS.forEach((field) => {
    errors[field] = ''
  })
}

function fill(task) {
  form.title = task.title ? task.title : ''
  form.assigneeId = task.assigneeId === null || task.assigneeId === undefined ? '' : String(task.assigneeId)
  form.columnId = task.columnId === null || task.columnId === undefined ? '' : String(task.columnId)
  form.priority = task.priority ? task.priority : 'MEDIUM'
  form.dueDate = task.dueDate ? String(task.dueDate) : ''
  form.description = task.description ? task.description : ''
  original.value = task
  confirming.value = false
  clearErrors()
}

async function loadDetail() {
  loading.value = true
  saving.value = false
  deleting.value = false
  confirming.value = false
  clearErrors()
  try {
    const task = await fetchTask(props.taskId)
    fill(task)
  } catch (error) {
    showError(error.message)
    emit('close')
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.visible, props.taskId],
  () => {
    if (props.visible) {
      loadDetail()
    }
  },
  { immediate: true }
)

function buildPayload() {
  return {
    title: form.title.trim(),
    assigneeId: Number(form.assigneeId),
    columnId: Number(form.columnId),
    priority: form.priority,
    dueDate: form.dueDate === '' ? null : form.dueDate,
    description: form.description === '' ? null : form.description
  }
}

function fieldsChanged() {
  const base = original.value
  if (!base) {
    return false
  }
  const payload = buildPayload()
  return (
    payload.title !== (base.title ? base.title : '') ||
    payload.assigneeId !== base.assigneeId ||
    payload.priority !== (base.priority ? base.priority : 'MEDIUM') ||
    (payload.dueDate ? payload.dueDate : '') !== (base.dueDate ? base.dueDate : '') ||
    (payload.description ? payload.description : '') !== (base.description ? base.description : '')
  )
}

function validate() {
  clearErrors()
  const title = form.title.trim()
  if (!title) {
    errors.title = '请输入任务标题'
  } else if (title.length > TITLE_MAX) {
    errors.title = `标题最多 ${TITLE_MAX} 个字符`
  }
  if (form.assigneeId === '' || form.assigneeId === null) {
    errors.assigneeId = '请选择负责人'
  }
  if (form.columnId === '' || form.columnId === null) {
    errors.columnId = '请选择状态'
  }
  if (form.description.length > DESCRIPTION_MAX) {
    errors.description = `描述最多 ${DESCRIPTION_MAX} 个字符`
  }
  return !ERROR_FIELDS.some((field) => errors[field])
}

function applyFieldErrors(error) {
  clearErrors()
  ERROR_FIELDS.forEach((field) => {
    errors[field] = fieldErrorOf(error, field)
  })
}

function close() {
  if (saving.value || deleting.value) {
    return
  }
  emit('close')
}

async function save() {
  if (saving.value || deleting.value) {
    return
  }
  if (!validate()) {
    return
  }
  const base = original.value
  if (!base) {
    return
  }
  const statusChanged = Number(form.columnId) !== base.columnId
  const changed = fieldsChanged()
  if (!statusChanged && !changed) {
    emit('close')
    return
  }
  saving.value = true
  let latest = base
  try {
    if (statusChanged) {
      latest = await updateTaskStatus(props.taskId, Number(form.columnId))
    }
    if (changed) {
      latest = await updateTask(props.taskId, buildPayload())
    }
    emit('updated', latest)
    emit('close')
  } catch (error) {
    applyFieldErrors(error)
    if (latest !== base) {
      emit('updated', latest)
    }
    showError(error.message)
  } finally {
    saving.value = false
  }
}

function askDelete() {
  if (saving.value || deleting.value) {
    return
  }
  confirming.value = true
}

function cancelDelete() {
  if (deleting.value) {
    return
  }
  confirming.value = false
}

async function confirmDelete() {
  if (deleting.value) {
    return
  }
  deleting.value = true
  try {
    await deleteTask(props.taskId)
    emit('deleted', props.taskId)
    emit('close')
  } catch (error) {
    showError(error.message)
  } finally {
    deleting.value = false
  }
}
</script>

<template>
  <div class="overlay" @click.self="close">
    <div class="modal w560">
      <div class="modal-head">
        <span>任务详情</span>
        <button class="modal-close" type="button" aria-label="关闭" :disabled="saving || deleting" @click="close">✕</button>
      </div>
      <div class="modal-body">
        <div v-if="loading" class="detail-loading">
          <span class="sk"></span>
          <span class="sk short"></span>
          <span class="sk"></span>
          <span class="sk short"></span>
        </div>
        <template v-else>
          <div class="field">
            <label>标题 <span class="req">*</span></label>
            <input
              v-model="form.title"
              class="control"
              type="text"
              maxlength="100"
              placeholder="例如：整理客户反馈清单"
              :disabled="saving"
            />
            <div v-if="errors.title" class="err">{{ errors.title }}</div>
          </div>

          <div class="row2">
            <div class="field">
              <label>负责人</label>
              <select v-model="form.assigneeId" class="control" :class="{ ph: form.assigneeId === '' }" :disabled="saving">
                <option value="">选择负责人</option>
                <option v-for="member in members" :key="member.id" :value="String(member.id)">{{ member.displayName }}</option>
              </select>
              <div v-if="errors.assigneeId" class="err">{{ errors.assigneeId }}</div>
            </div>
            <div class="field">
              <label>状态</label>
              <select v-model="form.columnId" class="control" :class="{ ph: form.columnId === '' }" :disabled="saving">
                <option value="">选择状态</option>
                <option v-for="column in columns" :key="column.id" :value="String(column.id)">{{ column.name }}</option>
              </select>
              <div v-if="errors.columnId" class="err">{{ errors.columnId }}</div>
            </div>
          </div>

          <div class="row2">
            <div class="field">
              <label>截止日期</label>
              <input v-model="form.dueDate" class="control" type="date" :disabled="saving" />
              <div v-if="errors.dueDate" class="err">{{ errors.dueDate }}</div>
            </div>
            <div class="field">
              <label>优先级</label>
              <select v-model="form.priority" class="control" :disabled="saving">
                <option v-for="option in PRIORITY_OPTIONS" :key="option.value" :value="option.value">{{ option.label }}</option>
              </select>
              <div v-if="errors.priority" class="err">{{ errors.priority }}</div>
            </div>
          </div>

          <div class="field">
            <label>描述</label>
            <textarea
              v-model="form.description"
              class="control"
              maxlength="1000"
              placeholder="补充说明、验收要点等"
              :disabled="saving"
            ></textarea>
            <div v-if="errors.description" class="err">{{ errors.description }}</div>
          </div>
        </template>
      </div>
      <div class="modal-foot split">
        <button class="btn btn-danger" type="button" :disabled="loading || saving || deleting" @click="askDelete">删除</button>
        <span class="foot-actions">
          <button class="btn btn-secondary" type="button" :disabled="saving || deleting" @click="close">取消</button>
          <button class="btn btn-primary" type="button" :disabled="loading || saving || deleting" @click="save">
            <span v-if="saving" class="spin dark"></span>{{ saving ? '保存中' : '保存' }}
          </button>
        </span>
      </div>
    </div>
  </div>

  <div v-if="confirming" class="overlay confirm-layer" @click.self="cancelDelete">
    <div class="modal w400">
      <div class="modal-head">删除任务</div>
      <div class="modal-body">
        <p class="confirm-text">删除后不可恢复，任务「{{ form.title }}」将被永久移除。</p>
      </div>
      <div class="modal-foot">
        <button class="btn btn-secondary" type="button" :disabled="deleting" @click="cancelDelete">取消</button>
        <button class="btn btn-danger" type="button" :disabled="deleting" @click="confirmDelete">
          <span v-if="deleting" class="spin dark"></span>{{ deleting ? '删除中' : '确认删除' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-close {
  border: 0;
  background: none;
  padding: 0;
  width: 24px;
  height: 24px;
  font-size: 15px;
  line-height: 1;
  color: #94A3B8;
}
.modal-close:hover:not(:disabled) {
  color: #475569;
}
.modal-close:disabled {
  color: #CBD5E1;
  cursor: not-allowed;
}
.detail-loading {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0;
}
.detail-loading .sk {
  width: 100%;
}
.detail-loading .sk.short {
  width: 64%;
}
.foot-actions {
  display: flex;
  gap: 10px;
}
.confirm-layer {
  z-index: 70;
}
</style>
