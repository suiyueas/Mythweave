import { createRouter, createWebHistory } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import AppLayout from '@/layouts/AppLayout.vue'

const routes = [
  {
    path: '/login',
    component: AuthLayout,
    children: [
      {
        path: '',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { title: '登录', guest: true }
      }
    ]
  },
  {
    path: '/register',
    component: AuthLayout,
    children: [
      {
        path: '',
        name: 'Register',
        component: () => import('@/views/Register.vue'),
        meta: { title: '注册', guest: true }
      }
    ]
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        redirect: '/my-works'
      },
      {
        path: 'my-works',
        name: 'Projects',
        component: () => import('@/views/Projects.vue'),
        meta: { title: '我的作品', requiresAuth: true }
      },
      {
        path: 'my-works/:projectId',
        name: 'Workspace',
        component: () => import('@/views/Workspace.vue'),
        meta: { title: '创作工坊', hideSidebar: true, noPadding: true, requiresAuth: true }
      },
      {
        path: 'ai-config',
        name: 'AiConfig',
        component: () => import('@/views/AiConfig.vue'),
        meta: { title: '策略与配置', requiresAuth: true }
      },
      {
        path: 'ai-setup',
        name: 'AiSetup',
        component: () => import('@/views/BootstrapWizard.vue'),
        meta: { title: 'AI先导创作', requiresAuth: true }
      },
      {
        path: 'my-works/:projectId/setup',
        name: 'SetupWizard',
        component: () => import('@/views/BootstrapWizard.vue'),
        meta: { title: 'AI先导创作', requiresAuth: true }
      },
      {
        path: 'my-works/:projectId/world',
        name: 'WorldBuilding',
        component: () => import('@/views/WorldBuilding.vue'),
        meta: { title: '世界观构建', requiresAuth: true }
      },
      {
        path: 'my-works/:projectId/search',
        name: 'SearchResults',
        component: () => import('@/views/SearchResults.vue'),
        meta: { title: '搜索结果', requiresAuth: true }
      }
    ]
  }
]

function isTokenExpired(token) {
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload))
    return decoded.exp * 1000 < Date.now()
  } catch {
    return true
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ─── 路由守卫 ───
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (token && isTokenExpired(token)) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const currentToken = localStorage.getItem('token')

  // 需要认证的页面 → 未登录跳转登录
  if (to.meta.requiresAuth && !currentToken) {
    next('/login')
  }
  // 已登录访问登录/注册页 → 跳转首页
  else if ((to.path === '/login' || to.path === '/register') && currentToken) {
    next('/my-works')
  }
  // 其他情况正常放行
  else {
    next()
  }
})

export default router