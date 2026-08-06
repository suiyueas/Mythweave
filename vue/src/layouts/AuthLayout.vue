<template>
  <div class="auth-layout" :class="{ 'auth-exiting': exiting }">
    <!-- 左侧品牌区 -->
    <div class="brand-side">
      <div class="brand-glow"></div>
      <div class="brand-content">
        <h1 class="brand-title">Mythweave</h1>
        <p class="brand-slogan">智能小说创作平台 · 让灵感自由生长</p>
      </div>
    </div>

    <!-- 右侧表单区 -->
    <div class="form-side">
      <div class="form-card">
        <router-view v-slot="{ Component }">
          <transition name="auth-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, provide } from 'vue'

const exiting = ref(false)
provide('authExiting', (callback) => {
  exiting.value = true
  setTimeout(callback, 800)
})
</script>

<style scoped>
/* ═══ 整体布局 ═══ */
.auth-layout {
  width: 100vw;
  height: 100vh;
  height: 100dvh;
  display: flex;
  overflow: hidden;
  background: linear-gradient(135deg, #F9F3EE 0%, #FDF1E0 33%, #F6E6D3 66%, #F9F3EE 100%);
  background-size: 400% 400%;
  animation: warmGradient 10s ease infinite;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  transition: background 0.8s ease;
}

@keyframes warmGradient {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.auth-layout.auth-exiting {
  background: linear-gradient(135deg, #FCE8D8 0%, #FDF0E6 100%);
}

/* ═══ 左侧品牌区 ═══ */
.brand-side {
  flex: 0 0 60%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(232, 122, 62, 0.08) 0%, rgba(249, 230, 214, 0.3) 100%);
  overflow: hidden;
}

.brand-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 500px;
  height: 500px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(232, 122, 62, 0.15) 0%, rgba(249, 230, 214, 0.1) 40%, transparent 70%);
  animation: rotateGlow 20s linear infinite;
}

.brand-glow::before,
.brand-glow::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: inherit;
}

.brand-glow::before {
  transform: rotate(60deg);
  opacity: 0.6;
}

.brand-glow::after {
  transform: rotate(120deg);
  opacity: 0.4;
}

@keyframes rotateGlow {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  animation: slideInLeft 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.brand-title {
  font-size: 72px;
  font-weight: 800;
  margin: 0 0 20px;
  background: linear-gradient(135deg, #E87A3E 0%, #D4692E 50%, #C45A20 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -2px;
  text-shadow: 0 0 60px rgba(232, 122, 62, 0.3);
  animation: titleBreath 3s ease-in-out infinite;
}

@keyframes titleBreath {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.03);
    opacity: 0.92;
  }
}

.brand-slogan {
  font-size: 18px;
  color: rgba(139, 90, 43, 0.8);
  margin: 0;
  letter-spacing: 2px;
  font-weight: 500;
}

/* ═══ 右侧表单区 ═══ */
.form-side {
  flex: 0 0 40%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  position: relative;
}

.form-side::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(232, 122, 62, 0.03) 0%, transparent 50%);
  pointer-events: none;
}

.form-card {
  width: 100%;
  max-width: 380px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 8px 32px rgba(139, 90, 43, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.8);
  animation: fadeInUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) 0.3s both;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ═══ 页面切换动画 ═══ */
.auth-fade-enter-active,
.auth-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.auth-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.auth-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ═══ 响应式布局 ═══ */
@media (max-width: 768px) {
  .auth-layout {
    flex-direction: column;
  }

  .brand-side {
    flex: 0 0 auto;
    min-height: 200px;
    padding: 40px 20px;
  }

  .brand-title {
    font-size: 42px;
  }

  .brand-slogan {
    font-size: 14px;
  }

  .form-side {
    flex: 1;
    padding: 30px 20px;
  }

  .form-card {
    animation-delay: 0.1s;
    padding: 30px 24px;
  }
}
</style>