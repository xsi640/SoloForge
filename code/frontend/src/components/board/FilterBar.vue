<script setup>
import { computed } from 'vue'

const PRIORITY_LABELS = { HIGH: '高', MEDIUM: '中', LOW: '低' }
const DUE_FILTER_LABELS = { OVERDUE: '已超期', DUE_SOON: '临期' }

const props = defineProps({
  assigneeScope: { type: String, default: 'MINE' },
  assigneeId: { type: Number, default: null },
  priority: { type: String, default: '' },
  dueFilter: { type: String, default: 'ALL' },
  members: { type: Array, default: () => [] },
  currentUserId: { type: Number, default: null },
  currentUserName: { type: String, default: '' },
  chips: { type: Array, default: () => [] },
  total: { type: Number, default: 0 },
  dueSoonCount: { type: Number, default: 0 },
  overdueCount: { type: Number, default: 0 }
})

const emit = defineEmits([
  'update:assigneeScope',
  'update:assigneeId',
  'update:priority',
  'update:dueFilter',
  'clear'
])

const otherMembers = computed(() => props.members.filter((member) => member.id !== props.currentUserId))

const assigneeValue = computed(() => {
  if (props.assigneeScope === 'MINE') {
    return props.currentUserId === null ? '' : String(props.currentUserId)
  }
  return props.assigneeId === null || props.assigneeId === undefined ? '' : String(props.assigneeId)
})

const assigneeText = computed(() => {
  if (assigneeValue.value === '') {
    return '全部'
  }
  if (props.currentUserId !== null && assigneeValue.value === String(props.currentUserId)) {
    return `我（${props.currentUserName}）`
  }
  const target = props.members.find((member) => String(member.id) === assigneeValue.value)
  return target ? target.displayName : '全部'
})

const priorityText = computed(() =>
  props.priority ? PRIORITY_LABELS[props.priority] || props.priority : '全部'
)

const dueFilterText = computed(() =>
  props.dueFilter && props.dueFilter !== 'ALL'
    ? DUE_FILTER_LABELS[props.dueFilter] || props.dueFilter
    : '全部'
)

function selectMine() {
  emit('update:assigneeScope', 'MINE')
  emit('update:assigneeId', null)
}

function selectAll() {
  emit('update:assigneeScope', 'ALL')
  emit('update:assigneeId', null)
}

function onAssigneeChange(event) {
  const value = event.target.value
  if (value === '') {
    selectAll()
    return
  }
  if (props.currentUserId !== null && value === String(props.currentUserId)) {
    emit('update:assigneeScope', 'MINE')
    emit('update:assigneeId', null)
    return
  }
  emit('update:assigneeScope', 'ALL')
  emit('update:assigneeId', Number(value))
}

function onPriorityChange(event) {
  emit('update:priority', event.target.value)
}

function onDueFilterChange(event) {
  emit('update:dueFilter', event.target.value)
}
</script>

<template>
  <div class="filterbar">
    <div class="seg">
      <span :class="assigneeScope === 'MINE' ? 'on' : 'off'" @click="selectMine">我的任务</span>
      <span :class="assigneeScope === 'ALL' ? 'on' : 'off'" @click="selectAll">查看全部</span>
    </div>

    <label class="select">
      <span>负责人：</span>
      <span :class="{ ph: assigneeValue === '' }">{{ assigneeText }}</span>
      <span class="caret">▾</span>
      <select class="native" :value="assigneeValue" @change="onAssigneeChange">
        <option value="">全部</option>
        <option v-if="currentUserId !== null" :value="String(currentUserId)">我（{{ currentUserName }}）</option>
        <option v-for="member in otherMembers" :key="member.id" :value="String(member.id)">
          {{ member.displayName }}
        </option>
      </select>
    </label>

    <label class="select">
      <span>优先级：</span>
      <span :class="{ ph: !priority }">{{ priorityText }}</span>
      <span class="caret">▾</span>
      <select class="native" :value="priority" @change="onPriorityChange">
        <option value="">全部</option>
        <option value="HIGH">高</option>
        <option value="MEDIUM">中</option>
        <option value="LOW">低</option>
      </select>
    </label>

    <label class="select">
      <span>截止日期：</span>
      <span :class="{ ph: dueFilter === 'ALL' }">{{ dueFilterText }}</span>
      <span class="caret">▾</span>
      <select class="native" :value="dueFilter" @change="onDueFilterChange">
        <option value="ALL">全部</option>
        <option value="OVERDUE">已超期</option>
        <option value="DUE_SOON">临期</option>
      </select>
    </label>

    <span v-for="chip in chips" :key="chip" class="chip">{{ chip }}</span>

    <button v-if="chips.length > 0" class="btn btn-sm btn-secondary" type="button" @click="emit('clear')">
      清除筛选
    </button>

    <div class="result">共 {{ total }} 项任务 · {{ dueSoonCount }} 项临期 · {{ overdueCount }} 项已超期</div>
  </div>
</template>

<style scoped>
.select {
  position: relative;
}
.select .native {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  border: 0;
  padding: 0;
  cursor: pointer;
  appearance: none;
}
</style>
