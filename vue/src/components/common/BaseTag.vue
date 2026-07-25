<template>
  <span class="tag" :class="`tag-${color}`">
    <slot />
  </span>
</template>

<script setup>
defineProps({
  color: {
    type: String,
    default: 'amber',
    validator: (v) => ['amber', 'teal', 'rose', 'emerald', 'purple'].includes(v)
  }
})
</script>

<style scoped>
/* 基础样式已迁移至全局 main.css */
</style>
