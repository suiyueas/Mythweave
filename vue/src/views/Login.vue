<template>
  <div class="login-view">
    <h2 class="page-title">欢迎回来</h2>
    <p class="page-subtitle">登录以继续你的创作之旅</p>

    <form @submit.prevent="handleLogin" class="login-form">
      <div class="field-group">
        <input
          v-model="form.username"
          type="text"
          placeholder="输入用户名或邮箱"
          required
          class="field-input"
        />
      </div>

      <div class="field-group">
        <div class="password-wrapper">
          <input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="输入密码"
            required
            class="field-input"
          />
          <button type="button" class="password-toggle" @click="showPassword = !showPassword">
            {{ showPassword ? '👁' : '👁‍🗨' }}
          </button>
        </div>
      </div>

      <div class="form-options">
        <router-link to="/forgot-password" class="forgot-link">忘记密码？</router-link>
      </div>

      <transition name="msg">
        <div v-if="errorMessage" class="form-message error">
          <span>⚠</span> {{ errorMessage }}
        </div>
      </transition>

      <button type="submit" class="submit-btn" :disabled="loading" ref="btnRef">
        <span v-if="loading" class="spinner"></span>
        <span v-else>登 录</span>
      </button>
    </form>

    <p class="form-footer">
      还没有账号？
      <router-link to="/register" class="footer-link">创建新账号</router-link>
    </p>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const authExiting = inject('authExiting')

const form = ref({ username: '', password: '' })
const loading = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const btnRef = ref(null)

async function handleLogin() {
  if (loading.value) return

  if (btnRef.value) {
    btnRef.value.classList.add('clicked')
    setTimeout(() => btnRef.value?.classList.remove('clicked'), 600)
  }

  loading.value = true
  errorMessage.value = ''

  const result = await userStore.login(form.value.username, form.value.password)
  loading.value = false

  if (result.success) {
    authExiting(() => {
      router.push('/my-works')
    })
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

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #5C3D1E;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 14px;
  color: #A08060;
  margin: 0 0 32px;
}

/* ═══ 表单 ═══ */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-group {
  position: relative;
}

.field-input {
  width: 100%;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.8);
  border: 1.5px solid rgba(200, 160, 120, 0.3);
  border-radius: 14px;
  color: #5C3D1E;
  font-size: 14px;
  transition: border-color 0.25s, box-shadow 0.25s;
  box-sizing: border-box;
}

.field-input::placeholder {
  color: #B8A090;
}

.field-input:focus {
  outline: none;
  border-color: #E87A3E;
  box-shadow: 0 0 0 0 rgba(232, 122, 62, 0.4);
  animation: inputGlow 1.2s ease-in-out infinite;
  background: rgba(255, 255, 255, 0.95);
}

@keyframes inputGlow {
  0% {
    border-color: rgba(232, 122, 62, 0.4);
    box-shadow: 0 0 0 0 rgba(232, 122, 62, 0.1);
  }
  50% {
    border-color: #E87A3E;
    box-shadow: 0 0 20px 8px rgba(232, 122, 62, 0.15);
  }
  100% {
    border-color: rgba(232, 122, 62, 0.4);
    box-shadow: 0 0 0 0 rgba(232, 122, 62, 0.1);
  }
}

.password-wrapper {
  position: relative;
}

.password-wrapper .field-input {
  padding-right: 48px;
}

.password-toggle {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  opacity: 0.4;
  transition: opacity 0.2s;
}

.password-toggle:hover {
  opacity: 0.8;
}

.form-options {
  display: flex;
  justify-content: flex-end;
  margin-top: -4px;
}

.forgot-link {
  font-size: 13px;
  color: #E87A3E;
  text-decoration: none;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: #D4692E;
}

.form-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 10px;
  font-size: 13px;
}

.form-message.error {
  background: rgba(220, 80, 60, 0.1);
  color: #C05040;
  border: 1px solid rgba(220, 80, 60, 0.2);
}

.form-message span {
  font-size: 14px;
}

.submit-btn {
  position: relative;
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #E87A3E 0%, #D4692E 100%);
  border: none;
  border-radius: 14px;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 4px 16px rgba(232, 122, 62, 0.3);
  margin-top: 8px;
  overflow: hidden;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(232, 122, 62, 0.4);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.97);
}

.submit-btn::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  transition: width 0.6s ease-out, height 0.6s ease-out, opacity 0.6s ease-out;
  opacity: 0;
}

.submit-btn.clicked::after {
  width: 300px;
  height: 300px;
  opacity: 0;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ═══ 底部链接 ═══ */
.form-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: #A08060;
}

.footer-link {
  color: #E87A3E;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.footer-link:hover {
  color: #D4692E;
}

/* ═══ 消息动画 ═══ */
.msg-enter-active,
.msg-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.msg-enter-from,
.msg-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>