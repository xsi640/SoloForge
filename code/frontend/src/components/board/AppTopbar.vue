<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '../../api/auth'
import { clearUser, isAdmin, sessionState } from '../../stores/session'
import { showSuccess } from '../../stores/toast'
import ReminderBell from './ReminderBell.vue'

const props = defineProps({
  appName: { type: String, default: '任务看板' },
  keyword: { type: String, default: '' },
  filterOpen: { type: Boolean, default: true },
  activeFilterCount: { type: Number, default: 0 }
})

const emit = defineEmits(['update:keyword', 'toggle-filter', 'open-task'])

const router = useRouter()
const loggingOut = ref(false)

const keywordModel = computed({
  get: () => props.keyword,
  set: (value) => emit('update:keyword', value)
})

const admin = computed(() => isAdmin())
const userName = computed(() => (sessionState.currentUser ? sessionState.currentUser.displayName : ''))
const avatarText = computed(() => {
  const name = userName.value.trim()
  return name ? name.charAt(0) : '用'
})

async function handleLogout() {
  if (loggingOut.value) {
    return
  }
  loggingOut.value = true
  await logout().catch(() => null)
  clearUser()
  showSuccess('已退出登录')
  router.replace({ path: '/login' })
}

function goMembers() {
  window.location.hash = '#/members'
}
</script>

<template>
  <header class="topbar">
    <div class="brand">
      <span class="mark">S</span>
      <span>{{ appName }}</span>
    </div>
    <div class="search">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#94A3B8" stroke-width="2.2" stroke-linecap="round">
        <circle cx="10.5" cy="10.5" r="7" />
        <path d="M20 20l-4.4-4.4" />
      </svg>
      <input v-model="keywordModel" type="text" placeholder="搜索任务标题或描述" />
    </div>
    <span class="spacer"></span>
    <button class="tb-btn" type="button" :aria-pressed="filterOpen" @click="emit('toggle-filter')">
      筛选{{ activeFilterCount > 0 ? `（${activeFilterCount}）` : '' }}
    </button>
    <ReminderBell @open-task="emit('open-task', $event)" />
    <div class="user">
      <span class="av">{{ avatarText }}</span>
      <span>{{ userName }}</span>
      <button class="out" type="button" :disabled="loggingOut" @click="handleLogout">
        {{ loggingOut ? '退出中' : '退出' }}
      </button>
    </div>
    <button v-if="admin" class="link" type="button" @click="goMembers">成员管理</button>
  </header>
</template>
