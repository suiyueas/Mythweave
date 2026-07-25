<template>
  <div class="login-view">
    <h2 class="login-title">欢迎回来</h2>
    <p class="login-desc">登录以继续你的创作</p>

    <form @submit.prevent="handleLogin" class="login-form">
      <div class="field-group">
        <label class="field-label">用户名</label>
        <div class="field-input-wrapper">
          <span class="field-icon">👤</span>
          <input
            v-model="form.username"
            type="text"
            placeholder="输入用户名或邮箱"
            required
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
            placeholder="输入密码"
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

      <button type="submit" class="submit-btn" :disabled="loading">
        <span v-if="loading" class="spinner"></span>
        <span v-else>登 录</span>
      </button>
    </form>

    <div class="form-footer">
      还没有账号？
      <router-link to="/register" class="form-link">创建一个</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = ref({ username: '', password: '' })
const loading = ref(false)
const errorMessage = ref('')

async function handleLogin() {
  loading.value = true
  errorMessage.value = ''

  const result = await userStore.login(form.value.username, form.value.password)
  loading.value = false

  if (result.success) {
    router.push('/my-works')
  } else {
    errorMessage.value = result.message
  }
}
</script>

<style scoped>
.login-view {
  display: flex;
  flex-direction: column;
}

.login-title {
  font-family: 'Playfair Display', serif;
  font-size: 26px;
  font-weight: 700;
  color: #2d2a27;
  margin: 0 0 4px 0;
  letter-spacing: -0.3px;
}

.login-desc {
  font-size: 13px;
  color: #9c9690;
  margin: 0 0 28px 0;
}

/* ═══ 表单 ═══ */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  padding: 13px 0;
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
