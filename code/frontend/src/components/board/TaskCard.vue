<script setup>
import { computed } from 'vue'

const PRIORITY_LABELS = { HIGH: '高', MEDIUM: '中', LOW: '低' }
const PRIORITY_CLASSES = { HIGH: 'high', MEDIUM: 'mid', LOW: 'low' }
const AVATAR_CLASSES = ['', 'b', 'c', 'd']

const props = defineProps({
  task: { type: Object, required: true },
  highlighted: { type: Boolean, default: false }
})

const emit = defineEmits(['open'])

const priorityLabel = computed(() => PRIORITY_LABELS[props.task.priority] || PRIORITY_LABELS.MEDIUM)
const priorityClass = computed(() => PRIORITY_CLASSES[props.task.priority] || PRIORITY_CLASSES.MEDIUM)
const isDone = computed(() => props.task.columnCode === 'DONE')
const assigneeName = computed(() => (props.task.assigneeName ? props.task.assigneeName : '未指定'))
const avatarText = computed(() => assigneeName.value.trim().charAt(0) || '?')
const avatarClass = computed(() => {
  const id = Number(props.task.assigneeId)
  return Number.isFinite(id) ? AVATAR_CLASSES[Math.abs(id) % AVATAR_CLASSES.length] : ''
})
const dueDate = computed(() => (props.task.dueDate ? String(props.task.dueDate) : ''))
const shortDue = computed(() => {
  const value = dueDate.value
  return value.length >= 10 ? value.slice(5, 10) : value
})
const isOverdue = computed(() => props.task.dueState === 'OVERDUE')
const isDueSoon = computed(() => props.task.dueState === 'DUE_SOON')

function open() {
  emit('open', props.task)
}
</script>

<template>
  <article
    class="card"
    :class="{ done: isDone, highlighted }"
    draggable="true"
    role="button"
    tabindex="0"
    @click="open"
    @keydown.enter.prevent="open"
    @keydown.space.prevent="open"
  >
    <h4>{{ task.title }}</h4>
    <div class="meta">
      <span class="who">
        <span class="av-s" :class="avatarClass">{{ avatarText }}</span>{{ assigneeName }}
      </span>
      <span class="right">
        <template v-if="dueDate">
          <span class="due" :class="{ overdue: isOverdue, soon: isDueSoon }">{{ shortDue }}</span>
          <span v-if="isOverdue" class="flag overdue">已超期</span>
          <span v-else-if="isDueSoon" class="flag soon">临期</span>
        </template>
        <span class="pill" :class="priorityClass">{{ priorityLabel }}</span>
      </span>
    </div>
  </article>
</template>

<style scoped>
.card[role='button'] {
  cursor: pointer;
}
.card.highlighted {
  border-color: #2563EB;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.18);
  animation: card-highlight 1.6s ease-out;
}
@keyframes card-highlight {
  0% {
    background: #EFF6FF;
  }
  100% {
    background: #fff;
  }
}
</style>
