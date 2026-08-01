<template>
  <div class="settings-root">
    <!-- 顶部：标题区 + 操作按钮 -->
    <header class="settings-header">
      <div class="header-title-group">
        <h1 class="header-title">个人设置</h1>
        <p class="header-subtitle">配置你的写作环境 · 所有修改即时生效</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-ghost" @click="resetAll">
          <span class="btn-icon">↺</span>
          重置
        </button>
        <button class="btn btn-primary" @click="saveAll">
          <span class="btn-icon">✓</span>
          保存
        </button>
      </div>
    </header>
    
    <!-- ═══ VIP 状态卡片（常驻内容区顶部） ═══ -->
    <div class="vip-card" :class="vipCardClass">
      <div class="vip-card-left">
        <div class="vip-card-badge">👑</div>
        <div class="vip-card-info">
          <div class="vip-card-title-row">
            <span class="vip-level">{{ userStore.vipLevelName }}</span>
            <span v-if="userStore.isAdmin" class="vip-admin-tag">管理员</span>
            <span v-if="userStore.isVip" class="vip-expire">到期时间：{{ userStore.vipExpireDate }}</span>
            <span v-else-if="userStore.vipExpired" class="vip-expired-tag">已过期</span>
            <span v-else class="vip-free-tag">免费用户</span>
          </div>
          <div class="vip-benefits-row">
            <span v-for="b in vipBenefits" :key="b" class="vip-benefit-tag">{{ b }}</span>
          </div>
        </div>
      </div>
      <div class="vip-card-actions">
        <button v-if="userStore.isVip" class="btn-vip-renew" @click="openRecharge">续费</button>
        <button v-else class="btn-vip-upgrade" @click="openRecharge">立即升级 →</button>
      </div>
    </div>
    
    <!-- ═══ Tab 导航 ═══ -->
    <nav class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </button>
      <!-- 滑动的指示器 -->
      <span class="tab-indicator" :style="tabIndicatorStyle"></span>
    </nav>

    <!-- ═══ 面板容器 ═══ -->
    <div class="panel-container">
      <Transition name="panel-slide" mode="out-in">
        <!-- ══ 账户信息 ══ -->
        <section v-if="activeTab === 'account'" key="account" class="panel">
          <div class="profile-hero">
            <div class="avatar-frame">
              <label class="avatar-upload-label" :style="{ cursor: 'pointer' }">
                <img v-if="avatarDataUrl" :src="avatarDataUrl" class="avatar-img" />
                <div v-else class="avatar-letter">墨</div>
                <input type="file" accept="image/*" class="avatar-input" @change="handleAvatarUpload" />
                <div class="avatar-overlay">
                  <span class="avatar-overlay-icon">📷</span>
                </div>
              </label>
            </div>
            <div class="profile-meta">
              <h2 class="profile-name">{{ accountInfo.username || '未命名作者' }}</h2>
              <span class="profile-badge">签约作者 · LV.8</span>
            </div>
          </div>

          <div class="settings-grid">
            <div class="setting-card">
              <div class="card-label">👤 用户名</div>
              <input type="text" v-model="accountInfo.username" class="input-field" placeholder="输入用户名" />
            </div>
            <div class="setting-card">
              <div class="card-label">✉️ 邮箱</div>
              <div class="input-group">
                <input type="email" v-model="accountInfo.email" class="input-field" placeholder="your@email.com" />
                <button class="btn-tag" @click="showToast('验证邮件已发送')">验证</button>
              </div>
            </div>
            <div class="setting-card" style="grid-column: 1 / -1;">
              <div class="card-label">✍️ 个人简介</div>
              <textarea v-model="accountInfo.bio" class="input-field bio-input" rows="2" placeholder="写一句个人简介…"></textarea>
            </div>
            <div class="setting-card">
              <div class="card-label">📅 注册时间</div>
              <input type="text" :value="accountInfo.createdAt" class="input-readonly" readonly />
            </div>
            <div class="setting-card">
              <div class="card-label">🔐 账户安全</div>
              <button class="btn-tag" @click="showPasswordModal = true">修改密码</button>
            </div>
          </div>
        
          <!-- ══ 退出登录 ══ -->
          <div class="logout-zone">
            <div class="logout-info">
              <div class="card-label">🚪 退出登录</div>
              <p class="logout-desc">退出后将返回登录页，未保存的修改可能丢失</p>
            </div>
            <button class="btn-logout" @click="showLogoutModal = true">退出登录</button>
          </div>
        
          <!-- ══ 退出登录确认弹窗 ══ -->
          <Transition name="modal-fade">
            <div v-if="showLogoutModal" class="modal-overlay" @click.self="showLogoutModal = false">
              <div class="password-modal">
                <h3 class="modal-title">🚪 退出登录</h3>
                <p class="logout-confirm-text">确定要退出登录吗？退出后需要重新登录才能继续创作。</p>
                <div class="modal-actions">
                  <button class="btn btn-ghost" @click="showLogoutModal = false">取消</button>
                  <button class="btn btn-logout-confirm" @click="handleConfirmLogout">确定退出</button>
                </div>
              </div>
            </div>
          </Transition>
        
          <!-- ══ 修改密码弹窗 ══ -->
          <Transition name="modal-fade">
            <div v-if="showPasswordModal" class="modal-overlay" @click.self="showPasswordModal = false">
              <div class="password-modal">
                <h3 class="modal-title">修改密码</h3>
                <div class="modal-body">
                  <label class="modal-field">
                    <span class="modal-field-label">当前密码</span>
                    <input type="password" v-model="passwordForm.current" class="input-field" placeholder="输入当前密码" />
                  </label>
                  <label class="modal-field">
                    <span class="modal-field-label">新密码</span>
                    <input type="password" v-model="passwordForm.newPass" class="input-field" placeholder="至少 6 位" />
                  </label>
                  <label class="modal-field">
                    <span class="modal-field-label">确认新密码</span>
                    <input type="password" v-model="passwordForm.confirm" class="input-field" placeholder="再次输入新密码" />
                  </label>
                </div>
                <div class="modal-actions">
                  <button class="btn btn-ghost" @click="showPasswordModal = false">取消</button>
                  <button class="btn btn-primary" @click="handleChangePassword">确认修改</button>
                </div>
              </div>
            </div>
          </Transition>
        </section>

        <!-- ══ 外观 ══ -->
        <section v-else-if="activeTab === 'appearance'" key="appearance" class="panel">
          <div class="setting-section">
            <h3 class="section-title">主题色</h3>
            <div class="color-grid">
              <button
                v-for="c in themeColors"
                :key="c.key"
                class="color-swatch-btn"
                :class="{ active: settings.themeColor === c.key }"
                :style="{
                  '--swatch': c.value,
                  '--swatch-glow': c.value + '44'
                }"
                @click="settings.themeColor = c.key"
                :title="c.name"
              >
                <span class="swatch-dot"></span>
                <span class="swatch-label">{{ c.name }}</span>
              </button>
            </div>
          </div>

          <div class="setting-section">
            <h3 class="section-title">界面布局</h3>
            <div class="layout-options">
              <div class="layout-row">
                <span class="row-label">侧边栏位置</span>
                <div class="btn-group">
                  <button
                    class="btn-option"
                    :class="{ selected: settings.sidebarPosition === 'left' }"
                    @click="settings.sidebarPosition = 'left'"
                  >左侧</button>
                  <button
                    class="btn-option"
                    :class="{ selected: settings.sidebarPosition === 'right' }"
                    @click="settings.sidebarPosition = 'right'"
                  >右侧</button>
                </div>
              </div>
              <div class="layout-row">
                <span class="row-label">侧边栏宽度</span>
                <div class="btn-group btn-group-3">
                  <button
                    class="btn-option"
                    :class="{ selected: settings.sidebarWidth === 'compact' }"
                    @click="settings.sidebarWidth = 'compact'"
                  >窄</button>
                  <button
                    class="btn-option"
                    :class="{ selected: settings.sidebarWidth === 'standard' }"
                    @click="settings.sidebarWidth = 'standard'"
                  >标准</button>
                  <button
                    class="btn-option"
                    :class="{ selected: settings.sidebarWidth === 'wide' }"
                    @click="settings.sidebarWidth = 'wide'"
                  >宽</button>
                </div>
              </div>
            </div>
          </div>

          <div class="setting-section">
            <h3 class="section-title">开关选项</h3>
            <div class="toggle-grid">
              <label class="toggle-row" v-for="t in toggleItems" :key="t.key">
                <div class="toggle-info">
                  <span class="toggle-label">{{ t.label }}</span>
                  <span class="toggle-desc">{{ t.desc }}</span>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="settings[t.key]" />
                  <span class="switch-track">
                    <span class="switch-thumb"></span>
                  </span>
                </label>
              </label>
            </div>
          </div>
        </section>

        <!-- ══ 通知提醒 ══ -->
        <section v-else-if="activeTab === 'notification'" key="notification" class="panel">
          <div class="setting-section">
            <h3 class="section-title">通知事件</h3>
            <div class="toggle-grid">
              <label class="toggle-row" v-for="n in notifItems" :key="n.key">
                <div class="toggle-info">
                  <span class="toggle-label">{{ n.label }}</span>
                  <span class="toggle-desc">{{ n.desc }}</span>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="settings[n.key]" />
                  <span class="switch-track">
                    <span class="switch-thumb"></span>
                  </span>
                </label>
              </label>
            </div>
          </div>

          <div class="setting-section">
            <h3 class="section-title">提醒方式</h3>
            <div class="toggle-grid">
              <label class="toggle-row">
                <div class="toggle-info">
                  <span class="toggle-label">站内通知</span>
                  <span class="toggle-desc">顶部铃铛图标提示</span>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="settings.notificationBell" />
                  <span class="switch-track">
                    <span class="switch-thumb"></span>
                  </span>
                </label>
              </label>
              <label class="toggle-row">
                <div class="toggle-info">
                  <span class="toggle-label">声音提醒</span>
                  <span class="toggle-desc">收到通知时播放提示音</span>
                </div>
                <label class="switch">
                  <input type="checkbox" v-model="settings.soundAlert" />
                  <span class="switch-track">
                    <span class="switch-thumb"></span>
                  </span>
                </label>
              </label>
            </div>
            <div class="layout-row" style="margin-top: 16px;">
              <span class="row-label">严重度阈值</span>
              <div class="btn-group btn-group-3">
                <button
                  class="btn-option"
                  :class="{ selected: settings.severityThreshold === 'all' }"
                  @click="settings.severityThreshold = 'all'"
                >全部</button>
                <button
                  class="btn-option"
                  :class="{ selected: settings.severityThreshold === 'warning' }"
                  @click="settings.severityThreshold = 'warning'"
                >警告以上</button>
                <button
                  class="btn-option"
                  :class="{ selected: settings.severityThreshold === 'critical' }"
                  @click="settings.severityThreshold = 'critical'"
                >仅严重</button>
              </div>
            </div>
            <div class="layout-row" style="margin-top: 16px;">
              <span class="row-label">免打扰</span>
              <label class="switch">
                <input type="checkbox" v-model="settings.quietHoursEnabled" />
                <span class="switch-track">
                  <span class="switch-thumb"></span>
                </span>
              </label>
            </div>
            <div v-if="settings.quietHoursEnabled" class="quiet-hours-row">
              <div class="time-picker">
                <span class="time-label">开始</span>
                <select v-model="settings.quietHoursStart" class="select-field">
                  <option value="20:00">20:00</option>
                  <option value="21:00">21:00</option>
                  <option value="22:00">22:00</option>
                  <option value="23:00">23:00</option>
                </select>
              </div>
              <span class="time-connector">→</span>
              <div class="time-picker">
                <span class="time-label">结束</span>
                <select v-model="settings.quietHoursEnd" class="select-field">
                  <option value="06:00">06:00</option>
                  <option value="07:00">07:00</option>
                  <option value="08:00">08:00</option>
                  <option value="09:00">09:00</option>
                </select>
              </div>
            </div>
          </div>
        </section>

        <!-- ══ 快捷键 ══ -->
        <section v-else-if="activeTab === 'shortcut'" key="shortcut" class="panel">
          <div class="shortcut-header">
            <h3 class="section-title" style="margin:0">快捷键</h3>
            <button class="btn btn-ghost btn-sm" @click="resetShortcuts">
              <span class="btn-icon">↺</span> 恢复默认
            </button>
          </div>

          <div v-if="shortcutConflict" class="conflict-banner">
            ⚠ 存在冲突的快捷键组合，请检查
          </div>

          <div class="shortcut-groups">
            <div v-for="group in shortcutGroups" :key="group.name" class="shortcut-group">
              <div class="group-label">{{ group.name }}</div>
              <div
                v-for="item in group.items"
                :key="item.key"
                class="shortcut-row"
                @click="startEditShortcut(item.key)"
              >
                <span class="row-label">{{ item.label }}</span>
                <div class="shortcut-key-wrap">
                  <template v-if="editingShortcut !== item.key">
                    <span class="keycaps">
                      <kbd v-for="(k, i) in parseKeys(store.shortcuts[item.key])" :key="i">{{ k }}</kbd>
                      <span v-if="!store.shortcuts[item.key]" class="key-unset">未设置</span>
                    </span>
                    <span class="key-hint">点击修改</span>
                  </template>
                  <input
                    v-else
                    ref="shortcutInputRef"
                    v-model="shortcutInputValue"
                    @keydown="handleShortcutKeydown($event, item.key)"
                    @blur="cancelEditShortcut"
                    class="shortcut-editor"
                    :placeholder="'按下快捷键组合...'"
                  />
                </div>
              </div>
            </div>
          </div>
        </section>
      </Transition>
    </div>

    <!-- Toast 通知 -->
    <Transition name="toast-pop">
      <div v-if="toast.show" class="toast" :class="'toast-' + toast.type">
        <span class="toast-icon">{{ toast.type === 'success' ? '✓' : '✕' }}</span>
        <span>{{ toast.message }}</span>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSettingsStore } from '@/stores/settings'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/user'
import { openVipModal } from '@/services/vipService'

const router = useRouter()
const store = useSettingsStore()
const userStore = useUserStore()

// ─── VIP 卡片 ───
const vipBenefits = ['AI 无限生成', '专属模板', '优先支持']

const vipCardClass = computed(() => {
  if (userStore.isVip) return 'vip-card-active'
  if (userStore.vipExpired) return 'vip-card-expired'
  return ''
})

function openRecharge() {
  openVipModal({ mode: 'recharge' })
}

// ─── Tab 切换 ───
const activeTab = ref('account')
const tabs = [
  { key: 'account', icon: '👤', label: '账户' },
  { key: 'appearance', icon: '🎨', label: '外观' },
  { key: 'notification', icon: '🔔', label: '通知' },
  { key: 'shortcut', icon: '⌨️', label: '快捷键' }
]

// 计算 Tab 指示器位置
const tabIndicatorStyle = computed(() => {
  const idx = tabs.findIndex(t => t.key === activeTab.value)
  const width = 100 / tabs.length
  return {
    width: width + '%',
    transform: `translateX(${idx * 100}%)`
  }
})

// ─── 格式化数字 ───
function formatNumber(n) {
  return (n || 0).toLocaleString()
}

// ─── Toast ───
const toast = reactive({ show: false, message: '', type: 'success' })
let toastTimer = null

function showToast(message, type = 'success') {
  clearTimeout(toastTimer)
  toast.message = message
  toast.type = type
  toast.show = true
  toastTimer = setTimeout(() => { toast.show = false }, 2500)
}

// ─── 设置数据 ───
const settings = store.settings

// ─── 账户信息持久化 ───
const PROFILE_KEY = 'novel-profile'

function loadProfile() {
  try {
    const raw = localStorage.getItem(PROFILE_KEY)
    if (raw) return JSON.parse(raw)
  } catch (_) {}
  return {
    username: '墨染青衫',
    email: 'writer@example.com',
    bio: '以笔为剑，以墨为锋。',
    createdAt: '2024-01-01'
  }
}

function saveProfile() {
  localStorage.setItem(PROFILE_KEY, JSON.stringify({ ...accountInfo, avatarDataUrl: avatarDataUrl.value }))
}

const accountInfo = reactive(loadProfile())
const avatarDataUrl = ref(localStorage.getItem('novel-avatar') || '')

// 自动保存个人资料
watch(() => ({ ...accountInfo }), () => { saveProfile() }, { deep: true })

// ─── 头像上传 ───
function handleAvatarUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showToast('请选择图片文件', 'error')
    return
  }
  const reader = new FileReader()
  reader.onload = (ev) => {
    avatarDataUrl.value = ev.target.result
    localStorage.setItem('novel-avatar', avatarDataUrl.value)
    saveProfile()
    showToast('头像已更新')
  }
  reader.readAsDataURL(file)
}

// ─── 修改密码 ───
const showPasswordModal = ref(false)
const passwordForm = reactive({ current: '', newPass: '', confirm: '' })

async function handleChangePassword() {
  if (!passwordForm.current) { showToast('请输入当前密码', 'error'); return }
  if (!passwordForm.newPass || passwordForm.newPass.length < 6) { showToast('新密码至少 6 位', 'error'); return }
  if (passwordForm.newPass !== passwordForm.confirm) { showToast('两次密码不一致', 'error'); return }

  try {
    // 对接后端真实改密接口，不再本地存储密码
    await userApi.changePassword({ oldPassword: passwordForm.current, newPassword: passwordForm.newPass })
    passwordForm.current = ''
    passwordForm.newPass = ''
    passwordForm.confirm = ''
    showPasswordModal.value = false
    showToast('密码修改成功')
  } catch (e) {
    showToast(e?.message || '密码修改失败', 'error')
  }
}

// ─── 退出登录 ───
const showLogoutModal = ref(false)

function handleConfirmLogout() {
  userStore.logout()
  showLogoutModal.value = false
  showToast('已退出登录')
  router.push('/login')
}

// ─── 主题色选项 ───
const themeColors = [
  { key: 'amber', name: '琥珀金', value: '#d97706' },
  { key: 'teal', name: '青色', value: '#0d9488' },
  { key: 'rose', name: '玫瑰红', value: '#be123c' },
  { key: 'purple', name: '紫色', value: '#7c3aed' },
  { key: 'blue', name: '蓝色', value: '#2563eb' },
  { key: 'emerald', name: '翡翠绿', value: '#059669' }
]

// ─── 外观开关项 ───
const toggleItems = [
  { key: 'showSidebar', label: '显示侧边栏', desc: '主界面侧边栏的显示状态' },
  { key: 'compactMode', label: '紧凑模式', desc: '缩小间距和圆角，信息密度更高' },
  { key: 'pageTransition', label: '页面切换动画', desc: '在面板之间切换时的过渡效果' },
  { key: 'hoverEffect', label: '悬停效果', desc: '按钮和卡片上的悬停反馈' },
  { key: 'loadingAnimation', label: '加载动画', desc: '数据加载时的动态指示' },
  { key: 'backgroundTexture', label: '背景纹理', desc: '页面背景的细微质感纹理' }
]

// ─── 通知开关项 ───
const notifItems = [
  { key: 'sentinelAlert', label: '智能哨兵告警', desc: '情节逻辑异常检测通知' },
  { key: 'agentComplete', label: 'Agent 分析完成', desc: 'AI 分析任务完成时提醒' },
  { key: 'writingGoal', label: '写作目标提醒', desc: '每日写作进度达标通知' },
  { key: 'versionSave', label: '版本保存通知', desc: '章节版本创建成功通知' }
]

// ─── 快捷键 ───
const editingShortcut = ref(null)
const shortcutInputValue = ref('')
const shortcutInputRef = ref(null)

const shortcutGroups = [
  {
    name: '基础操作',
    items: [
      { key: 'save', label: '保存文档' },
      { key: 'undo', label: '撤销' },
      { key: 'redo', label: '重做' },
      { key: 'findReplace', label: '查找替换' }
    ]
  },
  {
    name: 'AI 功能',
    items: [
      { key: 'aiContinue', label: 'AI 续写' },
      { key: 'aiPolish', label: 'AI 润色' },
      { key: 'aiExpand', label: 'AI 扩写' },
      { key: 'aiSummarize', label: 'AI 缩写' }
    ]
  },
  {
    name: '视图切换',
    items: [
      { key: 'focusMode', label: '专注模式' },
      { key: 'outlineMode', label: '大纲模式' },
      { key: 'toggleSidebar', label: '切换侧边栏' }
    ]
  }
]

const shortcutConflict = computed(() => {
  const values = Object.values(store.shortcuts)
  return values.length !== new Set(values).size
})

function parseKeys(shortcut) {
  if (!shortcut) return []
  return shortcut.split('+').map(k => {
    const map = { Ctrl: '⌘', Shift: '⇧', Alt: '⌥' }
    return map[k] || k
  })
}

function startEditShortcut(key) {
  editingShortcut.value = key
  shortcutInputValue.value = store.shortcuts[key] || ''
  nextTick(() => {
    shortcutInputRef.value?.focus()
  })
}

function cancelEditShortcut() {
  editingShortcut.value = null
  shortcutInputValue.value = ''
}

function handleShortcutKeydown(event, key) {
  event.preventDefault()
  const keys = []
  if (event.ctrlKey || event.metaKey) keys.push('Ctrl')
  if (event.shiftKey) keys.push('Shift')
  if (event.altKey) keys.push('Alt')

  let keyName = event.key
  if (keyName === ' ') keyName = 'Space'
  else if (keyName.length === 1) keyName = keyName.toUpperCase()
  else if (keyName.startsWith('Arrow')) keyName = keyName.replace('Arrow', '')

  if (!['Control', 'Shift', 'Alt', 'Meta'].includes(keyName)) {
    keys.push(keyName)
  }

  if (keys.length > 0) {
    shortcutInputValue.value = keys.join('+')
    store.updateShortcut(key, shortcutInputValue.value)
  }
  cancelEditShortcut()
}

// ─── 操作 ───
function resetAll() {
  if (!confirm('确定恢复所有设置为默认值？')) return
  store.resetAll()
  showToast('已恢复默认设置')
}

function saveAll() {
  store.saveSettings()
  showToast('设置已保存', 'success')
}

function resetShortcuts() {
  store.resetShortcuts()
  showToast('快捷键已恢复默认')
}

// ─── 生命周期 ───
onMounted(() => {
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && editingShortcut.value) {
      cancelEditShortcut()
    }
  })
  // 从后端加载用户统计数据
  userStore.loadProfile()
})

onUnmounted(() => {
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style scoped>
/* ═══════════════════════════════════════
   设定集 · 编辑/杂志风
   深色聚焦空间，金色强调，衬线排版
   ═══════════════════════════════════════ */

/* ─── 根容器 ─── */
.settings-root {
  padding: 32px 40px 40px;
  min-height: 100%;
  animation: rootEnter 0.45s ease-out;
}

@keyframes rootEnter {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ─── 顶部标题 ─── */
.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 28px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-title-group {
  flex: 1;
  min-width: 200px;
}

.header-title {
  font-family: 'Playfair Display', 'Noto Serif SC', serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: 0.02em;
  margin: 0;
  line-height: 1.2;
}

.header-subtitle {
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 14px;
  color: var(--text-muted);
  margin: 4px 0 0;
  font-style: italic;
}

.header-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* ─── 按钮系统 ─── */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  letter-spacing: 0.01em;
}

.btn-icon { font-size: 14px; }

.btn-primary {
  background: var(--accent);
  color: #fff;
  box-shadow: 0 2px 12px rgba(217, 119, 6, 0.25);
}
.btn-primary:hover {
  background: #b45309;
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(217, 119, 6, 0.35);
}

.btn-ghost {
  background: transparent;
  color: var(--text-secondary);
  border: 1px solid var(--border);
}
.btn-ghost:hover {
  background: rgba(217, 119, 6, 0.06);
  border-color: var(--accent);
  color: var(--accent);
}

.btn-sm { padding: 5px 12px; font-size: 12px; }

/* ─── VIP 状态卡片 ─── */
.vip-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 22px;
  margin-bottom: 24px;
  border-radius: 14px;
  background: var(--card);
  border: 1px solid var(--border);
  flex-wrap: wrap;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.vip-card-active {
  background: linear-gradient(120deg, #fffdf5, #fef3c7, #fffdf5);
  border-color: #fbbf24;
  box-shadow: 0 6px 24px rgba(217, 119, 6, 0.16);
}

.vip-card-expired {
  border-color: #fca5a5;
  background: linear-gradient(120deg, #fff, #fef2f2, #fff);
}

.vip-card-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.vip-card-badge {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  box-shadow: 0 4px 14px rgba(217, 119, 6, 0.3);
}

.vip-card-info { min-width: 0; }

.vip-card-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.vip-level {
  font-family: 'Playfair Display', 'Noto Serif SC', serif;
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: 0.02em;
}

.vip-expire {
  font-size: 12px;
  color: #b45309;
  background: rgba(217, 119, 6, 0.1);
  padding: 2px 10px;
  border-radius: 999px;
}

.vip-expired-tag {
  font-size: 12px;
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.1);
  padding: 2px 10px;
  border-radius: 999px;
}

.vip-admin-tag {
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #1e3a8a, #3b82f6);
  padding: 2px 10px;
  border-radius: 999px;
  letter-spacing: 0.5px;
}

.vip-free-tag {
  font-size: 12px;
  color: var(--text-muted);
  background: rgba(120, 113, 108, 0.08);
  padding: 2px 10px;
  border-radius: 999px;
}

.vip-benefits-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.vip-benefit-tag {
  font-size: 11px;
  color: var(--text-secondary);
  border: 1px solid var(--border);
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.6);
}

.vip-card-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.btn-vip-upgrade {
  padding: 9px 22px;
  border: none;
  border-radius: 10px;
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #d97706, #f59e0b);
  box-shadow: 0 4px 16px rgba(217, 119, 6, 0.35);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.btn-vip-upgrade:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 22px rgba(217, 119, 6, 0.45);
}

.btn-vip-renew {
  padding: 9px 22px;
  border: 1px solid #fbbf24;
  border-radius: 10px;
  background: rgba(217, 119, 6, 0.08);
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 14px;
  font-weight: 600;
  color: #b45309;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-vip-renew:hover { background: rgba(217, 119, 6, 0.16); }

/* ─── Tab 导航条 ─── */
.tab-bar {
  position: relative;
  display: flex;
  gap: 0;
  background: rgba(217, 119, 6, 0.04);
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 24px;
  border: 1px solid var(--border);
  overflow: hidden;
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
  border: none;
  background: transparent;
  border-radius: 10px;
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  transition: color 0.25s ease;
  position: relative;
  z-index: 2;
  white-space: nowrap;
}

.tab-btn.active {
  color: var(--accent);
  font-weight: 600;
}

.tab-icon { font-size: 15px; }

.tab-indicator {
  position: absolute;
  top: 4px;
  left: 4px;
  height: calc(100% - 8px);
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 1;
  transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* ─── 面板容器 ─── */
.panel-container {
  position: relative;
}

/* 面板切换动画 */
.panel-slide-enter-active {
  animation: panelIn 0.35s ease-out;
}
.panel-slide-leave-active {
  animation: panelOut 0.25s ease-in;
}

@keyframes panelIn {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes panelOut {
  from { opacity: 1; transform: translateY(0); }
  to { opacity: 0; transform: translateY(-10px); }
}

.panel {
  animation: panelContent 0.5s ease-out both;
  padding-bottom: 8px;
}

/* ─── 账户信息 - 头像区 ─── */
.profile-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 16px;
  margin-bottom: 16px;
}

.avatar-frame {
  position: relative;
  flex-shrink: 0;
}

.avatar-letter {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0e7490, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-family: 'Playfair Display', serif;
  font-size: 24px;
  font-weight: 700;
}

/* ═══ 头像上传 ═══ */
.avatar-upload-label {
  display: block;
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}
.avatar-upload-label:hover .avatar-overlay { opacity: 1; }
.avatar-overlay-icon { font-size: 20px; }

/* ═══ Bio 输入框 ═══ */
.bio-input {
  resize: vertical;
  font-family: 'Crimson Pro', serif;
  font-size: 13px;
  line-height: 1.6;
}

.profile-meta { min-width: 0; }

.profile-name {
  font-family: 'Playfair Display', 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 2px;
}

.profile-badge {
  font-family: 'Crimson Pro', serif;
  font-size: 12px;
  color: var(--text-muted);
}

/* ─── 设置卡片网格 ─── */
.settings-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 0;
}

.setting-card {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 18px 20px;
}

.card-label {
  font-family: 'Crimson Pro', serif;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.input-field, .input-readonly {
  width: 100%;
  padding: 8px 12px;
  border: 1.5px solid var(--border);
  border-radius: 8px;
  font-family: 'Crimson Pro', serif;
  font-size: 13px;
  color: var(--text);
  background: #faf8f5;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.input-field:focus { border-color: var(--accent); }

.input-readonly {
  background: #f5f3ef;
  color: var(--text-muted);
  cursor: not-allowed;
}

.input-group {
  display: flex;
  gap: 8px;
}
.input-group .input-field { flex: 1; }

.btn-tag {
  padding: 6px 14px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: transparent;
  font-family: 'Crimson Pro', serif;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.btn-tag:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(217, 119, 6, 0.04);
}



/* ─── 设置区块 ─── */
.setting-section {
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 16px;
}

.section-title {
  font-family: 'Playfair Display', 'Noto Serif SC', serif;
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 18px;
  letter-spacing: 0.01em;
}

/* ─── 主题色网格 ─── */
.color-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.color-swatch-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1.5px solid var(--border);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.color-swatch-btn:hover {
  border-color: var(--swatch);
  background: color-mix(in srgb, var(--swatch) 6%, transparent);
}

.color-swatch-btn.active {
  border-color: var(--swatch);
  background: color-mix(in srgb, var(--swatch) 10%, transparent);
  box-shadow: 0 0 0 3px var(--swatch-glow);
}

.swatch-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--swatch);
  flex-shrink: 0;
}

.swatch-label {
  font-family: 'Crimson Pro', serif;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
}

.active .swatch-label { color: var(--text); font-weight: 600; }

/* ─── 布局选项 ─── */
.layout-rows {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.layout-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.row-label {
  font-family: 'Crimson Pro', serif;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

/* ─── 按钮组 ─── */
.btn-group {
  display: flex;
  border-radius: 8px;
  border: 1px solid var(--border);
  overflow: hidden;
}

.btn-option {
  padding: 6px 16px;
  border: none;
  background: transparent;
  font-family: 'Crimson Pro', serif;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-muted);
  cursor: pointer;
  transition: all 0.2s;
}

.btn-option:not(:last-child) { border-right: 1px solid var(--border); }

.btn-option:hover {
  background: rgba(217, 119, 6, 0.04);
  color: var(--text-secondary);
}

.btn-option.selected {
  background: var(--accent);
  color: #fff;
  font-weight: 600;
}

.btn-group-3 {
  display: flex;
}
.btn-group-3 .btn-option { flex: 1; text-align: center; }

/* ─── Toggle 开关 ─── */
.toggle-grid {
  display: flex;
  flex-direction: column;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0ece6;
  cursor: pointer;
  gap: 16px;
}

.toggle-row:last-child { border-bottom: none; }

.toggle-info {
  min-width: 0;
  flex: 1;
}

.toggle-label {
  display: block;
  font-family: 'Crimson Pro', serif;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.toggle-desc {
  display: block;
  font-family: 'Crimson Pro', serif;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 1px;
}

.switch {
  position: relative;
  width: 40px;
  height: 22px;
  flex-shrink: 0;
  cursor: pointer;
}

.switch input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.switch-track {
  position: absolute;
  inset: 0;
  background: #d4cec6;
  border-radius: 22px;
  transition: background 0.3s ease;
}

.switch-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}

.switch input:checked + .switch-track {
  background: var(--accent);
}

.switch input:checked + .switch-track .switch-thumb {
  transform: translateX(18px);
}

/* ─── 免打扰时间选择 ─── */
.quiet-hours-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding: 12px 16px;
  background: #faf8f5;
  border-radius: 10px;
}

.time-picker {
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-label {
  font-family: 'Crimson Pro', serif;
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.time-connector {
  font-family: 'Crimson Pro', serif;
  font-size: 16px;
  color: var(--text-muted);
}

.select-field {
  padding: 6px 12px;
  border: 1.5px solid var(--border);
  border-radius: 8px;
  font-family: 'Crimson Pro', serif;
  font-size: 13px;
  background: #fff;
  color: var(--text);
  outline: none;
  min-width: 80px;
}
.select-field:focus { border-color: var(--accent); }

/* ─── 快捷键 ─── */
.shortcut-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.conflict-banner {
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  color: #dc2626;
  font-family: 'Crimson Pro', serif;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 16px;
}

.shortcut-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shortcut-group {
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
}

.group-label {
  padding: 10px 16px;
  font-family: 'Playfair Display', serif;
  font-size: 12px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
  background: #faf8f5;
  border-bottom: 1px solid var(--border);
}

.shortcut-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid #f0ece6;
  cursor: pointer;
  transition: background 0.15s;
  gap: 12px;
}
.shortcut-row:last-child { border-bottom: none; }
.shortcut-row:hover { background: rgba(217, 119, 6, 0.03); }

.shortcut-key-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.keycaps {
  display: flex;
  gap: 3px;
}

.keycaps kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  background: #f5f3ef;
  border: 1px solid #e0dbd4;
  border-radius: 5px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  line-height: 1;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.key-unset {
  font-family: 'Crimson Pro', serif;
  font-size: 12px;
  color: #ccc;
  font-style: italic;
}

.key-hint {
  font-family: 'Crimson Pro', serif;
  font-size: 11px;
  color: #ccc;
  opacity: 0;
  transition: opacity 0.2s;
}

.shortcut-row:hover .key-hint { opacity: 1; }

.shortcut-editor {
  padding: 4px 10px;
  border-radius: 6px;
  border: 2px solid var(--accent);
  background: #fff;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  outline: none;
  width: 140px;
  color: var(--text);
}
.shortcut-editor::placeholder {
  color: #ccc;
  font-family: 'Crimson Pro', serif;
  font-style: italic;
}

/* ─── Toast ─── */
.toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 12px;
  font-family: 'Crimson Pro', serif;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  z-index: 9999;
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}

.toast-success {
  background: #065f46;
  border: 1px solid #34d399;
}

.toast-error {
  background: #7f1d1d;
  border: 1px solid #f87171;
}

.toast-icon {
  font-size: 16px;
  font-weight: 700;
}

.toast-pop-enter-active {
  animation: toastIn 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toast-pop-leave-active {
  animation: toastOut 0.25s ease-in;
}

@keyframes toastIn {
  from { opacity: 0; transform: translateY(16px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes toastOut {
  from { opacity: 1; transform: translateY(0) scale(1); }
  to { opacity: 0; transform: translateY(8px) scale(0.95); }
}

/* ─── 退出登录 ─── */
.logout-zone {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  padding: 14px 18px;
  background: rgba(239, 68, 68, 0.04);
  border: 1px solid rgba(239, 68, 68, 0.18);
  border-radius: 12px;
  flex-wrap: wrap;
}

.logout-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin: 4px 0 0;
}

.btn-logout {
  padding: 9px 20px;
  border: 1px solid rgba(239, 68, 68, 0.35);
  border-radius: 10px;
  background: transparent;
  color: #dc2626;
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-logout:hover {
  background: #dc2626;
  color: #fff;
}

.logout-confirm-text {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 14px 0 22px;
  line-height: 1.6;
}

.btn-logout-confirm {
  background: #dc2626;
  box-shadow: 0 2px 12px rgba(220, 38, 38, 0.25);
}
.btn-logout-confirm:hover {
  background: #b91c1c;
  transform: translateY(-1px);
}

/* ═══ 密码修改弹窗 ═══ */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.password-modal {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  width: 380px;
  max-width: 90vw;
  box-shadow: 0 24px 64px rgba(0,0,0,0.15);
}

.modal-title {
  font-family: 'Playfair Display', serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
  margin: 0 0 20px;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.modal-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.modal-field-label {
  font-family: 'Crimson Pro', serif;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* ═══ 弹窗动画 ═══ */
.modal-fade-enter-active { animation: modalIn 0.3s ease-out; }
.modal-fade-leave-active { animation: modalOut 0.2s ease-in; }

@keyframes modalIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes modalOut {
  from { opacity: 1; }
  to { opacity: 0; }
}

.modal-fade-enter-active .password-modal {
  animation: modalScaleIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes modalScaleIn {
  from { opacity: 0; transform: scale(0.92) translateY(12px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

/* ─── 响应式 ─── */
@media (max-width: 768px) {
  .settings-root {
    padding: 16px 20px 28px;
  }

  .settings-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-title {
    font-size: 22px;
  }

  .tab-btn {
    padding: 8px 6px;
    font-size: 12px;
  }
  .tab-label {
    display: none;
  }
  .tab-icon {
    font-size: 18px;
  }

  .settings-grid {
    grid-template-columns: 1fr;
  }

  .profile-hero {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .color-grid {
    justify-content: center;
  }

  .layout-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .quiet-hours-row {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
