<template>
  <div id="loading-overlay" :class="{ hidden: !visible }">
    <div class="loading-content">
      <div class="loading-icon">📖</div>
      <div class="loading-title">NovelCraft AI</div>
      <div class="loading-subtitle">智能写作工作台 · 让AI成为你的副脑</div>
      <div class="loading-bar"><div class="loading-bar-inner"></div></div>
      <div class="loading-features">
        <div class="loading-feature">✨ AI流式续写</div>
        <div class="loading-feature">🧠 全书记忆</div>
        <div class="loading-feature">👥 多Agent协作</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const visible = ref(true)

onMounted(() => {
  setTimeout(() => {
    visible.value = false
  }, 2000)
})
</script>

<style scoped>
#loading-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: var(--bg);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.8s cubic-bezier(0.4, 0, 0.2, 1), visibility 0.8s;
}

#loading-overlay.hidden {
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
}

.loading-content { text-align: center; }

.loading-icon {
  font-size: 4rem;
  margin-bottom: 1.5rem;
  animation: pulse-glow 2s ease-in-out infinite;
  filter: drop-shadow(0 0 20px rgba(217,119,6,0.3));
}

@keyframes pulse-glow {
  0%, 100% { opacity: 0.5; transform: scale(1) rotate(0deg); }
  25% { opacity: 0.8; transform: scale(1.05) rotate(-2deg); }
  50% { opacity: 1; transform: scale(1.1) rotate(0deg); filter: drop-shadow(0 0 24px var(--accent-glow)); }
  75% { opacity: 0.8; transform: scale(1.05) rotate(2deg); }
}

.loading-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: 800;
  color: var(--text);
  letter-spacing: 0.06em;
  background: linear-gradient(135deg, var(--text) 0%, var(--accent) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.loading-subtitle {
  font-family: var(--font-body);
  font-size: 0.85rem;
  color: var(--text-muted);
  margin-top: 0.5rem;
  letter-spacing: 0.1em;
}

.loading-bar {
  margin-top: 1.5rem;
  width: 240px;
  height: 3px;
  background: var(--border);
  border-radius: 2px;
  overflow: hidden;
  margin-left: auto;
  margin-right: auto;
}

.loading-bar-inner {
  height: 100%;
  width: 0;
  background: linear-gradient(90deg, var(--accent), #f59e0b);
  animation: loading-progress 2s cubic-bezier(0.4, 0, 0.2, 1) forwards;
  border-radius: 2px;
  box-shadow: 0 0 8px rgba(217,119,6,0.4);
}

@keyframes loading-progress {
  0% { width: 0; }
  30% { width: 40%; }
  60% { width: 70%; }
  90% { width: 95%; }
  100% { width: 100%; }
}

.loading-features {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: 2rem;
  opacity: 0;
  animation: fadeInUp 0.6s ease forwards 0.8s;
}

.loading-feature {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.72rem;
  color: var(--text-muted);
  padding: 0.3rem 0.6rem;
  background: rgba(217,119,6,0.05);
  border-radius: 20px;
  border: 1px solid rgba(217,119,6,0.1);
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
