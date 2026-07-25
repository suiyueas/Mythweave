<template>
  <div class="card" :class="cardClass" :style="cardStyle">
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  accentLeft: { type: Boolean, default: false },
  clickable: { type: Boolean, default: false },
  dashed: { type: Boolean, default: false }
})

const cardClass = computed(() => ({
  'card-accent-l': props.accentLeft,
  'cursor-pointer': props.clickable,
  'border-dashed': props.dashed
}))

const cardStyle = computed(() => ({
  cursor: props.clickable ? 'pointer' : undefined
}))
</script>

<style scoped>
/* 基础样式已迁移至全局 main.css，此处仅保留组件特有样式 */
.cursor-pointer { cursor: pointer; }
.border-dashed { border-style: dashed; }
</style>
