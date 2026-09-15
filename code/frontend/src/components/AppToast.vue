<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { dismissToast, showToast, toastState } from '../stores/toast'

function handleToastEvent(event) {
  const detail = event.detail || {}
  showToast(detail.message, detail.type === 'error' ? 'error' : 'success', detail.duration)
}

onMounted(() => {
  window.addEventListener('app-toast', handleToastEvent)
})

onBeforeUnmount(() => {
  window.removeEventListener('app-toast', handleToastEvent)
})
</script>

<template>
  <div class="toast-stack">
    <div
      v-for="item in toastState.items"
      :key="item.id"
      class="toast"
      :class="item.type"
      role="status"
      @click="dismissToast(item.id)"
    >
      <span class="toast-dot"></span>
      <span>{{ item.message }}</span>
    </div>
  </div>
</template>
