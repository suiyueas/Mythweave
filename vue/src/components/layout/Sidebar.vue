<template>
  <aside class="sidebar">
    <!-- 品牌标识 -->
    <div class="sidebar-brand">
      <router-link to="/" class="sidebar-brand-main">
        <div class="brand-icon">📖</div>
        <div>
          <div class="brand-text">NovelCraft AI</div>
          <div class="brand-sub">智能写作工作台</div>
        </div>
      </router-link>
    </div>

    <!-- 导航菜单 -->
    <nav class="sidebar-nav">
      <div v-for="group in navGroups" :key="group.label" class="nav-group">
        <div class="nav-group-label">{{ group.label }}</div>
        <router-link
          v-for="item in group.items"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: currentPath === item.path }"
        >
          <span class="nav-icon">{{ item.icon }}</span>
          {{ item.label }}
          <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
        </router-link>
      </div>
    </nav>

    <!-- 用户信息 -->
    <router-link to="/my-works" class="sidebar-footer">
      <div class="sidebar-user">
        <div class="user-avatar">墨</div>
        <div>
          <div class="user-name">墨染青衫</div>
          <div class="user-role">签约作者 · LV.8</div>
        </div>
      </div>
    </router-link>
  </aside>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const currentPath = computed(() => route.path)

const navGroups = computed(() => [
  {
    label: '',
    items: [
      { path: '/my-works', icon: '📚', label: '我的作品' },
      { path: '/ai-config', icon: '🤖', label: '策略与配置' }
    ]
  }
])
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
  z-index: 10;
}

.sidebar::-webkit-scrollbar { width: 4px; }
.sidebar::-webkit-scrollbar-thumb { background: var(--border-hover); border-radius: 2px; }

.sidebar-brand {
  padding: 1.4rem 1.5rem 1rem;
  border-bottom: 1px solid var(--border);
}

.sidebar-brand-main {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text);
}

.brand-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius);
  background: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  color: #fff;
  flex-shrink: 0;
}

.brand-text {
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: 0.02em;
}

.brand-sub {
  font-size: 0.65rem;
  color: var(--text-muted);
  font-weight: 400;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.sidebar-nav {
  flex: 1;
  padding: 0.75rem 0;
}

.nav-group {
  margin-bottom: 0.25rem;
}

.nav-group-label {
  padding: 0.5rem 1.5rem 0.3rem;
  font-size: 0.65rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-muted);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0.55rem 1.5rem;
  margin: 0 0.5rem;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.18s ease;
  font-size: 0.88rem;
  color: var(--text-secondary);
  font-weight: 500;
  position: relative;
  border: none;
  background: none;
  width: calc(100% - 1rem);
  text-align: left;
  font-family: var(--font-body);
  text-decoration: none;
}

.nav-item:hover {
  background: rgba(217,119,6,0.06);
  color: var(--text);
}

.nav-item.active {
  background: var(--accent-glow);
  color: var(--accent);
  font-weight: 600;
}

.nav-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  background: var(--accent);
  border-radius: 0 2px 2px 0;
}

.nav-icon {
  font-size: 1.05rem;
  width: 20px;
  text-align: center;
  flex-shrink: 0;
}

.nav-badge {
  margin-left: auto;
  font-size: 0.6rem;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 10px;
  background: var(--accent-glow);
  color: var(--accent);
}

.sidebar-footer {
  padding: 0.75rem 1.5rem;
  border-top: 1px solid var(--border);
  display: flex;
  text-decoration: none;
  color: inherit;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--teal), var(--purple));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 0.75rem;
  font-weight: 700;
  flex-shrink: 0;
}

.user-name { font-size: 0.82rem; font-weight: 600; }
.user-role { font-size: 0.65rem; color: var(--text-muted); }
</style>
