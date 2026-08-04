<template>
  <div class="register-view">
    <h2 class="register-title">创建账号</h2>
    <p class="register-desc">加入 Mythweave，开启你的创作之旅</p>

    <form @submit.prevent="handleRegister" class="register-form">
      <div class="field-group">
        <label class="field-label">用户名</label>
        <div class="field-input-wrapper">
          <span class="field-icon">👤</span>
          <input
            v-model="form.username"
            type="text"
            placeholder="给自己取个笔名"
            required
            class="field-input"
          />
        </div>
      </div>

      <div class="field-group">
        <label class="field-label">邮箱</label>
        <div class="field-input-wrapper">
          <span class="field-icon">✉</span>
          <input
            v-model="form.email"
            type="email"
            placeholder="用于找回密码（选填）"
            class="field-input"
          />
        </div>
      </div>

      <div class="field-group">
        <label class="field-label">密码</label>
        <div class="field-input-wrapper">
          <span class="field-icon">🔑</span>
          <input
            v-model="form.password"
            type="password"
            placeholder="至少 6 位字符"
            required
            class="field-input"
          />
        </div>
      </div>

      <div class="field-group">
        <label class="field-label">确认密码</label>
        <div class="field-input-wrapper">
          <span class="field-icon">🔐</span>
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="再次输入密码"
            required
            class="field-input"
          />
        </div>
      </div>

      <transition name="msg">
        <div v-if="errorMessage" class="form-message error">
          <span>⚠</span> {{ errorMessage }}
        </div>
      </transition>
      <transition name="msg">
        <div v-if="successMessage" class="form-message success">
          <span>✓</span> {{ successMessage }}
        </div>
      </transition>

      <button type="submit" class="submit-btn" :disabled="loading">
        <span v-if="loading" class="spinner"></span>
        <span v-else>注 册</span>
      </button>
    </form>

    <div class="form-footer">
      已有账号？
      <router-link to="/login" class="form-link">立即登录</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = ref({ username: '', email: '', password: '', confirmPassword: '' })
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

async function handleRegister() {
  errorMessage.value = ''
  successMessage.value = ''

  if (form.value.password !== form.value.confirmPassword) {
    errorMessage.value = '两次密码输入不一致'
    return
  }
  if (form.value.password.length < 6) {
    errorMessage.value = '密码长度至少为 6 位'
    return
  }

  loading.value = true
  const result = await userStore.register(
    form.value.username,
    form.value.password,
    form.value.email
  )
  loading.value = false

  if (result.success) {
    successMessage.value = '注册成功，即将跳转到登录页...'
    setTimeout(() => { router.push('/login') }, 1500)
  } else {
    errorMessage.value = result.message
  }
}
</script>

<style scoped>
.register-view {
  display: flex;
  flex-direction: column;
}

.register-title {
  font-family: 'Playfair Display', serif;
  font-size: 26px;
  font-weight: 700;
  color: #2d2a27;
  margin: 0 0 4px 0;
  letter-spacing: -0.3px;
}

.register-desc {
  font-size: 13px;
  color: #9c9690;
  margin: 0 0 28px 0;
}

/* ═══ 表单 ═══ */
.register-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #6b6560;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.field-input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 14px;
  border: 1.5px solid #e8e3dc;
  border-radius: 12px;
  background: #faf8f5;
  transition: border-color 0.25s, box-shadow 0.25s;
}

.field-input-wrapper:focus-within {
  border-color: #d97706;
  box-shadow: 0 0 0 3px rgba(217, 119, 6, 0.1);
  background: #fff;
}

.field-icon {
  font-size: 15px;
  flex-shrink: 0;
  opacity: 0.6;
}

.field-input {
  flex: 1;
  padding: 12px 0;
  border: none;
  background: transparent;
  font-family: 'Crimson Pro', 'Noto Serif SC', serif;
  font-size: 15px;
  color: #2d2a27;
  outline: none;
}

.field-input::placeholder {
  color: #c4bdb6;
  font-style: italic;
}

/* ═══ 消息 ═══ */
.form-message {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 10px 14px;
  border-radius: 10px;
}

.form-message.error {
  background: #fef2f2;
  color: #dc2626;
  border: 1px solid #fecaca;
}

.form-message.success {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.msg-enter-active, .msg-leave-active {
  transition: all 0.25s ease;
}
.msg-enter-from, .msg-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ═══ 提交按钮 ═══ */
.submit-btn {
  width: 100%;
  padding: 14px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #d97706, #b45309);
  color: #fff;
  font-family: 'Noto Serif SC', serif;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 4px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s, opacity 0.2s;
  box-shadow: 0 4px 16px rgba(217, 119, 6, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  margin-top: 4px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(217, 119, 6, 0.35);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ═══ 底部链接 ═══ */
.form-footer {
  text-align: center;
  font-size: 13px;
  color: #9c9690;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f3efe8;
}

.form-link {
  color: #d97706;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s;
}

.form-link:hover {
  color: #b45309;
  text-decoration: underline;
}
</style>