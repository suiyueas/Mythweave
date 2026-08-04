<template>
  <div class="auth-layout">
    <div class="auth-bg">
      <div class="auth-bg-ornament"></div>
      <div class="auth-bg-grain"></div>
    </div>
    <div class="auth-container">
      <!-- 品牌 -->
      <div class="auth-brand">
        <div class="brand-icon">
          <span class="brand-icon-mark">✍</span>
        </div>
        <h1 class="brand-name">Mythweave</h1>
        <p class="brand-tagline">智能小说创作平台</p>
      </div>
      <!-- 卡片 -->
      <div class="auth-card">
        <div class="auth-card-ornament"></div>
        <router-view v-slot="{ Component }">
          <transition name="auth-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
      <!-- 底部 -->
      <p class="auth-footer">用文字构筑世界</p>
    </div>
  </div>
</template>

<script setup>
// AuthLayout — 登录/注册专用布局，无导航栏/侧边栏/搜索框
</script>

<style scoped>
/* ═══ 布局容器 — 整页可滚动 ═══ */
.auth-layout {
  height: 100vh;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  background: #f5f0eb;
  font-family: 'Noto Serif SC', 'Crimson Pro', serif;
  overflow-y: auto;
  position: relative;
}

/* ═══ 背景装饰（固定） ═══ */
.auth-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.auth-bg-ornament {
  position: absolute;
  top: -30%;
  right: -10%;
  width: 70vmin;
  height: 70vmin;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(217, 119, 6, 0.06) 0%, transparent 70%);
  animation: float-ornament 20s ease-in-out infinite;
}

.auth-bg-grain {
  position: absolute;
  inset: 0;
  opacity: 0.35;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.4'/%3E%3C/svg%3E");
  background-repeat: repeat;
  background-size: 256px 256px;
}

@keyframes float-ornament {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-20px, 30px) scale(1.05); }
  66% { transform: translate(15px, -15px) scale(0.95); }
}

/* ═══ 主容器 ═══ */
.auth-container {
  margin: auto 0;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28px;
  width: 100%;
  max-width: 440px;
}

/* ═══ 品牌 ═══ */
.auth-brand {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  animation: brand-enter 0.8s ease-out both;
}

.brand-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #d97706, #b45309);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
  box-shadow: 0 4px 16px rgba(217, 119, 6, 0.25);
}

.brand-icon-mark {
  font-size: 26px;
  color: #fff;
  line-height: 1;
}

.brand-name {
  font-family: 'Playfair Display', serif;
  font-size: 28px;
  font-weight: 800;
  font-style: italic;
  color: #2d2a27;
  margin: 0;
  letter-spacing: -0.5px;
}

.brand-tagline {
  font-size: 13px;
  color: #9c9690;
  margin: 0;
  letter-spacing: 3px;
  font-weight: 400;
}

/* ═══ 卡片 ═══ */
.auth-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 40px 36px;
  width: 100%;
  position: relative;
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 8px 24px rgba(0, 0, 0, 0.06),
    0 24px 64px rgba(217, 119, 6, 0.06);
  animation: card-enter 0.6s ease-out 0.3s both;
}

.auth-card-ornament {
  position: absolute;
  top: -1px;
  left: 32px;
  width: 48px;
  height: 3px;
  background: linear-gradient(90deg, #d97706, #f59e0b);
  border-radius: 0 0 3px 3px;
}

/* ═══ 页面切换动效 ═══ */
.auth-fade-enter-active,
.auth-fade-leave-active {
  transition: all 0.35s ease;
}
.auth-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.auth-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ═══ 底部 ═══ */
.auth-footer {
  font-size: 12px;
  color: #b8b0a8;
  letter-spacing: 4px;
  margin: 0;
  flex-shrink: 0;
  animation: brand-enter 0.8s ease-out 0.6s both;
}

/* ═══ 入场动效 ═══ */
@keyframes brand-enter {
  from { opacity: 0; transform: translateY(-12px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes card-enter {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* ═══ 滚动条样式 ═══ */
.auth-layout::-webkit-scrollbar {
  width: 5px;
}
.auth-layout::-webkit-scrollbar-track {
  background: transparent;
}
.auth-layout::-webkit-scrollbar-thumb {
  background: #d4d0ca;
  border-radius: 4px;
}
.auth-layout::-webkit-scrollbar-thumb:hover {
  background: #b8b4b0;
}

/* ═══ 响应式 ═══ */
@media (max-width: 480px) {
  .auth-layout {
    padding: 20px 14px;
  }
  .auth-card {
    padding: 24px 18px;
    border-radius: 16px;
  }
  .brand-name { font-size: 22px; }
  .brand-icon { width: 48px; height: 48px; }
  .brand-icon-mark { font-size: 22px; }
  .auth-container { gap: 20px; }
}

@media (max-height: 700px) {
  .auth-layout {
    padding: 16px 16px;
  }
  .auth-brand { gap: 4px; }
  .brand-icon { width: 40px; height: 40px; }
  .brand-icon-mark { font-size: 18px; }
  .brand-name { font-size: 20px; }
  .brand-tagline { font-size: 11px; }
  .auth-card { padding: 20px 24px; }
  .auth-container { gap: 16px; }
}

@media (max-height: 600px) {
  .auth-layout {
    padding: 12px 16px;
  }
  .auth-card { padding: 16px 20px; }
  .auth-container { gap: 12px; }
  .auth-footer { display: none; }
}

/* 防止内容被压缩 */
.auth-brand,
.auth-card,
.auth-footer {
  flex-shrink: 0;
}
</style>