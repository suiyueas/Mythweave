import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'
import { authApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || null)
  const profile = ref({
    id: null,
    nickname: '',
    email: '',
    phone: '',
    bio: '',
    avatar: '',
    emailVerified: false,
    role: 'user',
    vipLevel: 0,
    vipExpireAt: null,
    vipPurchasedAt: null,
    vipStatus: 'none'
  })

  const stats = ref({
    totalWords: 0,
    continuousDays: 0,
    worksCount: 0,
    level: 1
  })

  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  const displayName = computed(() => profile.value.nickname || '未设置昵称')

  // ─── VIP 状态 ───
  // vipStatus: none-未开通 / active-生效中 / expired-已过期
  const isVip = computed(() => {
    const p = profile.value
    return Number(p.vipLevel) > 0 && !!p.vipExpireAt && new Date(p.vipExpireAt) > new Date()
  })

  const vipExpired = computed(() => {
    const p = profile.value
    return Number(p.vipLevel) > 0 && !!p.vipExpireAt && new Date(p.vipExpireAt) <= new Date()
  })

  const vipLevelName = computed(() => {
    const names = { 0: '普通用户', 1: 'VIP 白银', 2: 'VIP 黄金', 3: 'VIP 钻石' }
    return names[Number(profile.value.vipLevel)] || '普通用户'
  })

  const vipExpireDate = computed(() => {
    const at = profile.value.vipExpireAt
    if (!at) return ''
    const d = new Date(at)
    if (isNaN(d.getTime())) return ''
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  })

  const avatarText = computed(() => {
    const name = profile.value.nickname || '用'
    return name.charAt(0)
  })
  
  // 是否为管理员（role=admin）
  const isAdmin = computed(() => profile.value.role === 'admin')

  function setAuth(data) {
    token.value = data.token
    profile.value = {
      id: data.user.id,
      nickname: data.user.nickname || '',
      email: data.user.email || '',
      phone: '',
      bio: '',
      avatar: data.user.avatar || '',
      emailVerified: false,
      role: data.user.role || 'user',
      vipLevel: data.user.vipLevel || 0,
      vipExpireAt: data.user.vipExpireAt || null,
      vipPurchasedAt: data.user.vipPurchasedAt || null,
      vipStatus: data.user.vipStatus || 'none'
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
  }

  function clearAuth() {
    token.value = null
    profile.value = {
      id: null,
      nickname: '',
      email: '',
      phone: '',
      bio: '',
      avatar: '',
      emailVerified: false,
      role: 'user',
      vipLevel: 0,
      vipExpireAt: null,
      vipPurchasedAt: null,
      vipStatus: 'none'
    }
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function loadFromStorage() {
    const savedToken = localStorage.getItem('token')
    const savedUser = localStorage.getItem('user')
    if (savedToken && savedUser) {
      token.value = savedToken
      try {
        const user = JSON.parse(savedUser)
        profile.value = {
          id: user.id || null,
          nickname: user.nickname || '',
          email: user.email || '',
          phone: '',
          bio: '',
          avatar: user.avatar || '',
          emailVerified: false,
          role: user.role || 'user',
          vipLevel: user.vipLevel || 0,
          vipExpireAt: user.vipExpireAt || null,
          vipPurchasedAt: user.vipPurchasedAt || null,
          vipStatus: user.vipStatus || 'none'
        }
      } catch (e) {
        console.warn('解析用户信息失败', e)
      }
    }
  }

  async function login(username, password) {
    try {
      const data = await authApi.login({ username, password })
      setAuth(data)
      return { success: true }
    } catch (e) {
      return { success: false, message: e.message || '登录失败' }
    }
  }

  async function register(username, password, email) {
    try {
      await authApi.register({ username, password, email })
      return { success: true }
    } catch (e) {
      return { success: false, message: e.message || '注册失败' }
    }
  }

  function logout() {
    clearAuth()
  }

  async function loadProfile() {
    if (!token.value) return
    loading.value = true
    try {
      const data = await userApi.getProfile()
      if (data) {
        profile.value = {
          id: data.id || profile.value.id,
          nickname: data.nickname || profile.value.nickname,
          email: data.email || '',
          phone: data.phone || '',
          bio: data.bio || '',
          avatar: data.avatar || profile.value.avatar,
          emailVerified: data.emailVerified || false,
          role: data.role || profile.value.role,
          vipLevel: data.vipLevel !== undefined ? data.vipLevel : profile.value.vipLevel,
          vipExpireAt: data.vipExpireAt !== undefined ? data.vipExpireAt : profile.value.vipExpireAt,
          vipPurchasedAt: data.vipPurchasedAt !== undefined ? data.vipPurchasedAt : profile.value.vipPurchasedAt,
          vipStatus: data.vipStatus || profile.value.vipStatus
        }
        if (data.totalWords !== undefined) {
          stats.value = {
            totalWords: data.totalWords || 0,
            continuousDays: data.continuousDays || 0,
            worksCount: data.worksCount || 0,
            level: data.level || 1
          }
        }
      }
    } catch (e) {
      console.warn('加载用户信息失败', e)
    } finally {
      loading.value = false
    }
  }

  async function loadStats() {
    if (!token.value) return
    try {
      if (stats.value.totalWords > 0) return
      const data = await userApi.getStats()
      if (data) {
        stats.value = {
          totalWords: data.totalWords || 0,
          continuousDays: data.continuousDays || 0,
          worksCount: data.worksCount || 0,
          level: data.userLevel || 1
        }
      }
    } catch (e) {
      console.warn('加载用户统计失败', e)
    }
  }

  async function updateProfile(data) {
    try {
      await userApi.updateProfile(data)
      profile.value = { ...profile.value, ...data }
      return { success: true }
    } catch (e) {
      return { success: false, message: e.message || '更新失败' }
    }
  }

  async function uploadAvatar(file) {
    try {
      const formData = new FormData()
      formData.append('file', file)
      const data = await userApi.uploadAvatar(formData)
      if (data?.avatarUrl) {
        profile.value.avatar = data.avatarUrl
      }
      return { success: true, avatarUrl: data?.avatarUrl }
    } catch (e) {
      return { success: false, message: e.message || '上传失败' }
    }
  }

  async function deleteAvatar() {
    try {
      await userApi.deleteAvatar()
      profile.value.avatar = ''
      return { success: true }
    } catch (e) {
      return { success: false, message: e.message || '删除失败' }
    }
  }

  async function changePassword(oldPassword, newPassword) {
    try {
      await userApi.changePassword({ oldPassword, newPassword })
      return { success: true }
    } catch (e) {
      return { success: false, message: e.message || '修改失败' }
    }
  }

  async function sendEmailVerification() {
    try {
      await userApi.sendEmailVerification()
      return { success: true }
    } catch (e) {
      return { success: false, message: e.message || '发送失败' }
    }
  }
  
  // ─── VIP 会员 ───
  async function getVipPlans() {
    try {
      return await userApi.getVipPlans()
    } catch (e) {
      return []
    }
  }
  
  async function activateVip(planId) {
    try {
      const data = await userApi.activateVip(planId)
      if (data) {
        profile.value.vipLevel = data.vipLevel ?? profile.value.vipLevel
        profile.value.vipExpireAt = data.vipExpireAt || profile.value.vipExpireAt
        profile.value.vipPurchasedAt = data.vipPurchasedAt || profile.value.vipPurchasedAt
        profile.value.vipStatus = data.vipStatus || 'active'
      }
      return { success: true, data }
    } catch (e) {
      return { success: false, message: e.message || '开通失败' }
    }
  }
  
  async function refreshVip() {
    try {
      const data = await userApi.getProfile()
      if (data) {
        profile.value.vipLevel = data.vipLevel !== undefined ? data.vipLevel : profile.value.vipLevel
        profile.value.vipExpireAt = data.vipExpireAt !== undefined ? data.vipExpireAt : profile.value.vipExpireAt
        profile.value.vipPurchasedAt = data.vipPurchasedAt !== undefined ? data.vipPurchasedAt : profile.value.vipPurchasedAt
        profile.value.vipStatus = data.vipStatus || profile.value.vipStatus
      }
      return true
    } catch (e) {
      return false
    }
  }

  return {
    token,
    profile,
    stats,
    loading,
    isLoggedIn,
    displayName,
    avatarText,
    isVip,
    vipExpired,
    vipLevelName,
    vipExpireDate,
    isAdmin,
    setAuth,
    clearAuth,
    loadFromStorage,
    login,
    register,
    logout,
    loadProfile,
    loadStats,
    updateProfile,
    uploadAvatar,
    deleteAvatar,
    changePassword,
    sendEmailVerification,
    getVipPlans,
    activateVip,
    refreshVip
  }
})