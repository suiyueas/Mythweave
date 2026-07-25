<template>
  <button class="btn" :class="btnClass" @click="handleClick">
    <slot />
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (v) => ['primary', 'outline', 'ghost'].includes(v)
  },
  size: {
    type: String,
    default: 'md',
    validator: (v) => ['sm', 'md', 'lg'].includes(v)
  }
})

const emit = defineEmits(['click'])

const handleClick = (event) => {
  if (event && typeof event.stopPropagation === 'function') {
    event.stopPropagation()
  }
  emit('click', event)
}

const btnClass = computed(() => [
  `btn-${props.variant}`,
  `btn-${props.size}`
])
</script>

<style scoped>
/* 基础样式已迁移至全局 main.css */
.btn-ghost { background: transparent; border-color: transparent; color: var(--text-secondary); }
.btn-ghost:hover { color: var(--text); background: rgba(217,119,6,0.06); }
.btn-lg { padding: 0.7rem 1.5rem; font-size: 0.9rem; }
</style>