<template>
  <div class="stat-card" :style="{ borderLeftColor: borderColor }">
    <div class="stat-header">
      <div>
        <div class="stat-label">{{ label }}</div>
        <div class="stat-value">{{ value }}</div>
      </div>
      <div v-if="icon" class="stat-icon" :style="{ background: iconBg }">
        <span>{{ icon }}</span>
      </div>
    </div>
    <div v-if="change" class="stat-change" :class="{ up: isUp }">{{ change }}</div>
    <div v-if="subtext" class="stat-sub">{{ subtext }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  change: { type: String, default: '' },
  subtext: { type: String, default: '' },
  icon: { type: String, default: '' },
  color: { type: String, default: 'accent' }
})

const colorMap = {
  accent: { border: '#d97706', bg: 'rgba(217,119,6,0.1)' },
  teal: { border: '#0e7490', bg: 'rgba(14,116,144,0.1)' },
  emerald: { border: '#0d9488', bg: 'rgba(13,148,136,0.1)' },
  purple: { border: '#7c3aed', bg: 'rgba(124,58,237,0.1)' }
}

const borderColor = computed(() => colorMap[props.color]?.border || colorMap.accent.border)
const iconBg = computed(() => colorMap[props.color]?.bg || colorMap.accent.bg)
const isUp = computed(() => props.change?.includes('↑') || props.change?.includes('+'))
</script>

<style scoped>
.stat-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 1.3rem 1.5rem;
  transition: all 0.2s ease;
  border-left: 3px solid var(--accent);
}

.stat-card:hover {
  border-color: var(--border-hover);
  box-shadow: var(--shadow-sm);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.stat-label {
  font-size: 0.72rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 600;
}

.stat-value {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 800;
  margin-top: 0.3rem;
  letter-spacing: 0.01em;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.stat-change {
  font-size: 0.72rem;
  margin-top: 0.2rem;
  color: var(--text-muted);
}

.stat-change.up { color: var(--emerald); }

.stat-sub {
  font-size: 0.6rem;
  color: var(--text-muted);
  margin-top: 2px;
}
</style>
