<script setup>
import { reactive, ref, watch } from 'vue'
import { fieldErrorOf } from '../../api/http'
import { createTask } from '../../api/task'
import { showError, showSuccess } from '../../stores/toast'

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
  columns: { type: Array, required: true },
  members: { type: Array, required: true },
  defaultColumnId: { type: Number, default: null }
})

const emit = defineEmits(['close', 'created'])

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
const submitting = ref(false)

function clearErrors() {
  ERROR_FIELDS.forEach((field) => {
    errors[field] = ''
  })
}

function resetForm() {
  form.title = ''
  form.assigneeId = ''
  form.columnId = props.defaultColumnId === null || props.defaultColumnId === undefined ? '' : String(props.defaultColumnId)
  form.priority = 'MEDIUM'
  form.dueDate = ''
  form.description = ''
  submitting.value = false
  clearErrors()
}

watch(
  () => props.visible,
  (value) => {
    if (value) {
      resetForm()
    }
  },
  { immediate: true }
)

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
  if (submitting.value) {
    return
  }
  emit('close')
}

async function submit() {
  if (submitting.value) {
    return
  }
  if (!validate()) {
    return
  }
  submitting.value = true
  try {
    const task = await createTask({
      title: form.title.trim(),
      assigneeId: Number(form.assigneeId),
      columnId: Number(form.columnId),
      priority: form.priority,
      dueDate: form.dueDate === '' ? null : form.dueDate,
      description: form.description === '' ? null : form.description
    })
    showSuccess('任务已创建')
    emit('created', task)
    emit('close')
  } catch (error) {
    applyFieldErrors(error)
    showError(error.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="overlay" @click.self="close">
    <div class="modal">
      <div class="modal-head">
        <span>新建任务</span>
        <button class="modal-close" type="button" aria-label="关闭" :disabled="submitting" @click="close">✕</button>
      </div>
      <div class="modal-body">
        <div class="field">
          <label>标题 <span class="req">*</span></label>
          <input
            v-model="form.title"
            class="control"
            type="text"
            maxlength="100"
            placeholder="例如：整理客户反馈清单"
            :disabled="submitting"
          />
          <div v-if="errors.title" class="err">{{ errors.title }}</div>
        </div>

        <div class="row2">
          <div class="field">
            <label>负责人</label>
            <select v-model="form.assigneeId" class="control" :class="{ ph: form.assigneeId === '' }" :disabled="submitting">
              <option value="">选择负责人</option>
              <option v-for="member in members" :key="member.id" :value="String(member.id)">{{ member.displayName }}</option>
            </select>
            <div v-if="errors.assigneeId" class="err">{{ errors.assigneeId }}</div>
          </div>
          <div class="field">
            <label>状态</label>
            <select v-model="form.columnId" class="control" :class="{ ph: form.columnId === '' }" :disabled="submitting">
              <option value="">选择状态</option>
              <option v-for="column in columns" :key="column.id" :value="String(column.id)">{{ column.name }}</option>
            </select>
            <div v-if="errors.columnId" class="err">{{ errors.columnId }}</div>
          </div>
        </div>

        <div class="row2">
          <div class="field">
            <label>截止日期</label>
            <input v-model="form.dueDate" class="control" type="date" :disabled="submitting" />
            <div v-if="errors.dueDate" class="err">{{ errors.dueDate }}</div>
          </div>
          <div class="field">
            <label>优先级</label>
            <select v-model="form.priority" class="control" :disabled="submitting">
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
            :disabled="submitting"
          ></textarea>
          <div v-if="errors.description" class="err">{{ errors.description }}</div>
        </div>
      </div>
      <div class="modal-foot">
        <button class="btn btn-secondary" type="button" :disabled="submitting" @click="close">取消</button>
        <button class="btn btn-primary" type="button" :disabled="submitting" @click="submit">
          <span v-if="submitting" class="spin dark"></span>{{ submitting ? '保存中' : '保存' }}
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
</style>
