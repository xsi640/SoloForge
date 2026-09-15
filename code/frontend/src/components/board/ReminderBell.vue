<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { fetchMyReminders } from '../../api/reminder'

const REFRESH_INTERVAL = 5 * 60 * 1000

const emit = defineEmits(['open-task'])

const items = ref([])
const overdueCount = ref(0)
const dueSoonCount = ref(0)
const panelOpen = ref(false)
const bellRef = ref(null)

let refreshTimer = 0

const pendingCount = computed(() => overdueCount.value + dueSoonCount.value)
const hasPending = computed(() => pendingCount.value > 0)

function shortDate(value) {
  const text = value ? String(value) : ''
  return text.length >= 10 ? text.slice(5, 10) : text
}

function stateLabel(task) {
  return task.dueState === 'OVERDUE' ? '已超期' : '临期'
}

async function refresh() {
  try {
    const data = await fetchMyReminders()
    items.value = data && Array.isArray(data.items) ? data.items : []
    overdueCount.value = data && typeof data.overdueCount === 'number' ? data.overdueCount : 0
    dueSoonCount.value = data && typeof data.dueSoonCount === 'number' ? data.dueSoonCount : 0
  } catch {
    return
  }
}

function togglePanel() {
  panelOpen.value = !panelOpen.value
}

function closePanel() {
  panelOpen.value = false
}

function openTask(task) {
  panelOpen.value = false
  emit('open-task', task)
}

function onDocumentClick(event) {
  if (!panelOpen.value) {
    return
  }
  const bell = bellRef.value
  if (bell && !bell.contains(event.target)) {
    closePanel()
  }
}

onMounted(() => {
  refresh()
  refreshTimer = window.setInterval(refresh, REFRESH_INTERVAL)
  document.addEventListener('click', onDocumentClick)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    window.clearInterval(refreshTimer)
  }
  document.removeEventListener('click', onDocumentClick)
})
</script>

<template>
  <div ref="bellRef" class="bell-wrap">
    <button
      class="tb-icon"
      type="button"
      aria-label="我的提醒"
      :aria-expanded="panelOpen"
      @click="togglePanel"
    >
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#F8FAFC" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 15.5V10a6 6 0 1 0-12 0v5.5L4.5 18h15L18 15.5z" />
        <path d="M10.2 21a2 2 0 0 0 3.6 0" />
      </svg>
      <span v-if="hasPending" class="badge">{{ pendingCount }}</span>
    </button>

    <div v-if="panelOpen" class="bell-panel">
      <div class="ph">
        <span>我的提醒</span>
        <span class="count">{{ pendingCount }} 项待处理</span>
      </div>
      <button
        v-for="task in items"
        :key="task.id"
        class="bell-item"
        type="button"
        @click="openTask(task)"
      >
        <span class="t">{{ task.title }}</span>
        <span class="m">
          <span class="flag" :class="task.dueState === 'OVERDUE' ? 'overdue' : 'soon'">{{ stateLabel(task) }}</span>
          <span>截止 {{ shortDate(task.dueDate) }}</span>
        </span>
      </button>
      <div v-if="items.length === 0" class="bell-empty">暂无临期或超期任务</div>
    </div>
  </div>
</template>

<style scoped>
.bell-wrap {
  position: relative;
  display: flex;
  align-items: center;
}
.bell-wrap .bell-panel {
  position: absolute;
  top: 40px;
  right: 0;
  width: 300px;
  z-index: 70;
  max-height: 320px;
  overflow-y: auto;
}
.ph .count {
  font-size: 12px;
  font-weight: 400;
  color: #64748B;
}
.bell-empty {
  padding: 16px 14px;
  font-size: 12px;
  color: #94A3B8;
  text-align: center;
}
</style>
