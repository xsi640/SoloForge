<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { fetchCurrentUser } from '../api/auth'
import { fetchBoard, fetchColumns } from '../api/board'
import { listMembers } from '../api/member'
import { queryTasks, updateTaskStatus } from '../api/task'
import { isAdmin, sessionState, setUser } from '../stores/session'
import { showError, showSuccess } from '../stores/toast'
import AppTopbar from '../components/board/AppTopbar.vue'
import FilterBar from '../components/board/FilterBar.vue'
import NewTaskModal from '../components/board/NewTaskModal.vue'
import TaskCard from '../components/board/TaskCard.vue'
import TaskDetailModal from '../components/board/TaskDetailModal.vue'

const CONTENT_002 = '先创建第一个任务，团队成员登录后即可看到'
const CONTENT_003 = '当前筛选为「负责人 = 我」，可以查看全部任务或创建新任务'
const CONTENT_004 = '换个条件试试，或清除全部筛选条件'
const CONTENT_005 = '看板加载失败，请检查内网连接后重试'
const CONTENT_006 = '操作失败，请重试'
const SEARCH_DEBOUNCE = 250
const HIGHLIGHT_DURATION = 1600
const SKELETON_COLUMNS = [1, 2, 3]
const PRIORITY_LABELS = { HIGH: '高', MEDIUM: '中', LOW: '低' }
const DUE_FILTER_LABELS = { OVERDUE: '已超期', DUE_SOON: '临期' }

const loading = ref(true)
const loadError = ref('')
const boardName = ref('任务看板')
const columns = ref([])
const tasks = ref([])
const members = ref([])
const boardTotal = ref(0)

const keyword = ref('')
const assigneeScope = ref('MINE')
const assigneeId = ref(null)
const priority = ref('')
const dueFilter = ref('ALL')
const filterOpen = ref(true)

const newTaskVisible = ref(false)
const newTaskColumnId = ref(null)
const detailVisible = ref(false)
const detailTaskId = ref(null)

const dragTask = ref(null)
const dragOverColumnId = ref(null)
const highlightTaskId = ref(null)
const moving = ref(false)

let searchTimer = 0
let highlightTimer = 0
let taskSeed = 0

const currentUser = computed(() => sessionState.currentUser)
const currentUserId = computed(() => (currentUser.value ? currentUser.value.id : null))
const currentUserName = computed(() => (currentUser.value ? currentUser.value.displayName : ''))
const firstColumnId = computed(() => (columns.value.length > 0 ? columns.value[0].id : null))

const tasksByColumn = computed(() => {
  const map = {}
  columns.value.forEach((column) => {
    map[column.id] = []
  })
  tasks.value.forEach((task) => {
    if (map[task.columnId]) {
      map[task.columnId].push(task)
    }
  })
  return map
})

const totalCount = computed(() => tasks.value.length)
const dueSoonCount = computed(() => tasks.value.filter((task) => task.dueState === 'DUE_SOON').length)
const overdueCount = computed(() => tasks.value.filter((task) => task.dueState === 'OVERDUE').length)

const conditionChips = computed(() => {
  const chips = []
  const trimmed = keyword.value.trim()
  if (trimmed) {
    chips.push(`关键词：${trimmed}`)
  }
  if (assigneeScope.value === 'ALL') {
    const target = members.value.find((member) => member.id === assigneeId.value)
    chips.push(target ? `负责人：${target.displayName}` : '全部任务')
  }
  if (priority.value) {
    chips.push(`优先级：${PRIORITY_LABELS[priority.value] || priority.value}`)
  }
  if (dueFilter.value !== 'ALL') {
    chips.push(`截止日期：${DUE_FILTER_LABELS[dueFilter.value] || dueFilter.value}`)
  }
  return chips
})

const conditionSummary = computed(() => conditionChips.value.join(' · '))
const boardIsEmpty = computed(() => boardTotal.value === 0)
const myTasksEmpty = computed(
  () => !boardIsEmpty.value && tasks.value.length === 0 && conditionChips.value.length === 0
)
const filterEmpty = computed(
  () => !boardIsEmpty.value && tasks.value.length === 0 && conditionChips.value.length > 0
)

function buildQuery() {
  const params = {}
  const trimmed = keyword.value.trim()
  if (trimmed) {
    params.keyword = trimmed
  }
  if (assigneeScope.value === 'MINE') {
    if (currentUserId.value !== null) {
      params.assigneeId = currentUserId.value
    }
  } else if (assigneeId.value !== null && assigneeId.value !== undefined && assigneeId.value !== '') {
    params.assigneeId = assigneeId.value
  }
  if (priority.value) {
    params.priority = priority.value
  }
  if (dueFilter.value !== 'ALL') {
    params.dueFilter = dueFilter.value
  }
  return params
}

async function requestTasks() {
  const seed = ++taskSeed
  const result = await queryTasks(buildQuery())
  if (seed !== taskSeed) {
    return null
  }
  return Array.isArray(result) ? result : []
}

async function refreshTasks() {
  try {
    const list = await requestTasks()
    if (list) {
      tasks.value = list
    }
  } catch (error) {
    showError('加载失败，请重试')
  }
}

function scheduleRefresh(delay) {
  window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    refreshTasks()
  }, delay)
}

function normalizeColumns(list) {
  if (!Array.isArray(list)) {
    return []
  }
  return [...list].sort((left, right) => (left.sortOrder || 0) - (right.sortOrder || 0))
}

function ownMember() {
  const user = sessionState.currentUser
  if (!user) {
    return []
  }
  return [
    {
      id: user.id,
      username: user.username,
      displayName: user.displayName,
      isAdmin: user.isAdmin === true,
      status: 'ENABLED',
      createdAt: ''
    }
  ]
}

async function ensureUser() {
  if (sessionState.currentUser) {
    return
  }
  const user = await fetchCurrentUser()
  setUser(user)
}

async function loadMembers() {
  const own = ownMember()
  if (!isAdmin()) {
    members.value = own
    return
  }
  try {
    const list = await listMembers()
    members.value = Array.isArray(list) && list.length > 0 ? list : own
  } catch (error) {
    members.value = own
  }
}

async function loadGlobalTotal() {
  const list = await queryTasks({})
  boardTotal.value = Array.isArray(list) ? list.length : 0
}

async function loadPage() {
  loading.value = true
  loadError.value = ''
  try {
    await ensureUser()
    const [board, columnList] = await Promise.all([
      fetchBoard(),
      fetchColumns(),
      loadMembers(),
      loadGlobalTotal(),
      requestTasks().then((list) => {
        if (list) {
          tasks.value = list
        }
      })
    ])
    boardName.value = board && board.name ? board.name : '任务看板'
    columns.value = normalizeColumns(columnList)
  } catch (error) {
    loadError.value = CONTENT_005
  } finally {
    loading.value = false
  }
}

function highlight(taskId) {
  highlightTaskId.value = taskId
  window.clearTimeout(highlightTimer)
  highlightTimer = window.setTimeout(() => {
    highlightTaskId.value = null
  }, HIGHLIGHT_DURATION)
}

function applyUpdatedTask(updated) {
  if (!updated || typeof updated !== 'object' || updated.id === undefined || updated.id === null) {
    return false
  }
  const index = tasks.value.findIndex((item) => item.id === updated.id)
  if (index === -1) {
    return false
  }
  tasks.value.splice(index, 1, updated)
  return true
}

function onDragStart(event, task) {
  dragTask.value = task
  dragOverColumnId.value = null
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(task.id))
  }
}

function onDragEnd() {
  dragTask.value = null
  dragOverColumnId.value = null
}

function onDragOver(column) {
  if (!dragTask.value || dragTask.value.columnId === column.id) {
    return
  }
  dragOverColumnId.value = column.id
}

function onDragLeave(event, column) {
  const next = event.relatedTarget
  if (next && event.currentTarget.contains(next)) {
    return
  }
  if (dragOverColumnId.value === column.id) {
    dragOverColumnId.value = null
  }
}

function isDropTarget(column) {
  return Boolean(
    dragTask.value && dragTask.value.columnId !== column.id && dragOverColumnId.value === column.id
  )
}

async function onDrop(column) {
  const task = dragTask.value
  dragOverColumnId.value = null
  if (!task || task.columnId === column.id) {
    dragTask.value = null
    return
  }
  if (moving.value) {
    return
  }
  moving.value = true
  try {
    const updated = await updateTaskStatus(task.id, column.id)
    const applied = applyUpdatedTask(updated)
    if (!applied) {
      await refreshTasks()
    }
    highlight(task.id)
    showSuccess('任务状态已更新')
  } catch (error) {
    showError(CONTENT_006)
  } finally {
    moving.value = false
    dragTask.value = null
  }
}

function openDetail(task) {
  if (!task || task.id === undefined || task.id === null) {
    return
  }
  detailTaskId.value = task.id
  detailVisible.value = true
}

function closeDetail() {
  detailVisible.value = false
  detailTaskId.value = null
}

function openNewTask(columnId) {
  if (columnId === null || columnId === undefined) {
    return
  }
  newTaskColumnId.value = columnId
  newTaskVisible.value = true
}

function closeNewTask() {
  newTaskVisible.value = false
}

async function onTaskCreated(task) {
  newTaskVisible.value = false
  boardTotal.value += 1
  await refreshTasks()
  if (task && task.id !== undefined && task.id !== null) {
    highlight(task.id)
  }
  showSuccess('任务已创建')
}

async function onTaskUpdated(task) {
  await refreshTasks()
  if (task && task.id !== undefined && task.id !== null && tasks.value.some((item) => item.id === task.id)) {
    highlight(task.id)
  }
  showSuccess('任务已保存')
}

async function onTaskDeleted(taskId) {
  detailVisible.value = false
  detailTaskId.value = null
  boardTotal.value = Math.max(0, boardTotal.value - 1)
  await refreshTasks()
  showSuccess('任务已删除')
}

function clearAllFilters() {
  keyword.value = ''
  assigneeScope.value = 'MINE'
  assigneeId.value = null
  priority.value = ''
  dueFilter.value = 'ALL'
}

function showAllTasks() {
  assigneeScope.value = 'ALL'
  assigneeId.value = null
}

watch(keyword, () => {
  scheduleRefresh(SEARCH_DEBOUNCE)
})

watch([assigneeScope, assigneeId, priority, dueFilter], () => {
  scheduleRefresh(0)
})

onBeforeUnmount(() => {
  window.clearTimeout(searchTimer)
  window.clearTimeout(highlightTimer)
})

loadPage()
</script>

<template>
  <AppTopbar
    v-model:keyword="keyword"
    :app-name="boardName"
    :filter-open="filterOpen"
    :active-filter-count="conditionChips.length"
    @toggle-filter="filterOpen = !filterOpen"
    @open-task="openDetail"
  />

  <FilterBar
    v-if="filterOpen"
    v-model:assignee-scope="assigneeScope"
    v-model:assignee-id="assigneeId"
    v-model:priority="priority"
    v-model:due-filter="dueFilter"
    :members="members"
    :current-user-id="currentUserId"
    :current-user-name="currentUserName"
    :chips="conditionChips"
    :total="totalCount"
    :due-soon-count="dueSoonCount"
    :overdue-count="overdueCount"
    @clear="clearAllFilters"
  />

  <div v-if="loading" class="board">
    <div v-for="column in SKELETON_COLUMNS" :key="column" class="col">
      <div class="sk-card">
        <span class="sk"></span>
        <span class="sk h8"></span>
      </div>
      <div class="sk-card">
        <span class="sk"></span>
        <span class="sk h8"></span>
      </div>
      <div class="sk-card">
        <span class="sk"></span>
        <span class="sk h8"></span>
      </div>
    </div>
  </div>

  <div v-else-if="loadError" class="state-area">
    <div class="notice">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#DC2626" stroke-width="2" stroke-linecap="round">
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7.5v5.5" />
        <path d="M12 16.5v.01" />
      </svg>
      <span>{{ loadError }}</span>
    </div>
    <div class="retry-row">
      <button class="btn btn-secondary" type="button" @click="loadPage">重试</button>
    </div>
  </div>

  <div v-else-if="boardIsEmpty" class="state-area">
    <div class="panel">
      <div class="empty">
        <span class="ic">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#64748B" stroke-width="2" stroke-linecap="round">
            <rect x="3" y="4" width="7" height="16" rx="1.5" />
            <rect x="14" y="4" width="7" height="10" rx="1.5" />
          </svg>
        </span>
        <h5>看板还没有任务</h5>
        <p>{{ CONTENT_002 }}</p>
        <div class="acts">
          <button class="btn btn-primary" type="button" @click="openNewTask(firstColumnId)">＋ 新建任务</button>
        </div>
      </div>
    </div>
  </div>

  <div v-else-if="myTasksEmpty" class="state-area">
    <div class="panel">
      <div class="empty">
        <span class="ic">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#64748B" stroke-width="2" stroke-linecap="round">
            <circle cx="12" cy="8" r="3.5" />
            <path d="M5 20c0-3.3 3.1-6 7-6s7 2.7 7 6" />
          </svg>
        </span>
        <h5>你还没有任务</h5>
        <p>{{ CONTENT_003 }}</p>
        <div class="acts">
          <button class="btn btn-secondary" type="button" @click="showAllTasks">查看全部任务</button>
          <button class="btn btn-primary" type="button" @click="openNewTask(firstColumnId)">＋ 新建任务</button>
        </div>
      </div>
    </div>
  </div>

  <div v-else-if="filterEmpty" class="state-area">
    <div class="panel">
      <div class="empty">
        <span class="ic">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#64748B" stroke-width="2" stroke-linecap="round">
            <circle cx="10.5" cy="10.5" r="6.5" />
            <path d="M20 20l-4.8-4.8" />
          </svg>
        </span>
        <h5>没有匹配的任务</h5>
        <p>{{ CONTENT_004 }}</p>
        <p>当前条件：{{ conditionSummary }}</p>
        <div class="acts">
          <button class="btn btn-secondary" type="button" @click="clearAllFilters">清除筛选条件</button>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="board">
    <div
      v-for="column in columns"
      :key="column.id"
      class="col"
      @dragover.prevent="onDragOver(column)"
      @dragleave="onDragLeave($event, column)"
      @drop.prevent="onDrop(column)"
    >
      <div class="col-head">
        <h3>{{ column.name }}</h3>
        <span class="count-pill">{{ tasksByColumn[column.id].length }}</span>
      </div>
      <div v-if="isDropTarget(column)" class="dropzone">松手放入「{{ column.name }}」</div>
      <div
        v-for="task in tasksByColumn[column.id]"
        :key="task.id"
        class="card-slot"
        draggable="true"
        @dragstart="onDragStart($event, task)"
        @dragend="onDragEnd"
      >
        <TaskCard
          :task="task"
          :highlighted="highlightTaskId === task.id"
          :class="{ dragging: dragTask && dragTask.id === task.id }"
          @open="openDetail"
        />
      </div>
      <button class="add-btn" type="button" @click="openNewTask(column.id)">＋ 添加任务</button>
    </div>
  </div>

  <NewTaskModal
    v-if="newTaskVisible"
    :visible="true"
    :columns="columns"
    :members="members"
    :default-column-id="newTaskColumnId"
    @close="closeNewTask"
    @created="onTaskCreated"
  />

  <TaskDetailModal
    v-if="detailVisible && detailTaskId !== null"
    :visible="true"
    :task-id="detailTaskId"
    :columns="columns"
    :members="members"
    @close="closeDetail"
    @updated="onTaskUpdated"
    @deleted="onTaskDeleted"
  />
</template>

<style scoped>
.state-area {
  margin: 0 24px 24px;
}
.retry-row {
  display: flex;
  justify-content: center;
  margin-top: 14px;
}
</style>
