<template>
  <div class="register-view">
    <h2 class="page-title">创建账号</h2>
    <p class="page-subtitle">加入 Mythweave，开启你的创作之旅</p>

    <form @submit.prevent="handleRegister" class="register-form">
      <div class="field-group">
        <input
          v-model="form.username"
          type="text"
          placeholder="给自己取个笔名"
          required
          class="field-input"
        />
      </div>

      <div class="field-group">
        <input
          v-model="form.email"
          type="email"
          placeholder="用于找回密码（选填）"
          class="field-input"
        />
      </div>

      <div class="field-group">
        <div class="password-wrapper">
          <input
            v-model="form.password"
            :type="showPassword ? 'text' : 'password'"
            placeholder="至少 6 位字符"
            required
            class="field-input"
          />
          <button type="button" class="password-toggle" @click="showPassword = !showPassword">
            {{ showPassword ? '👁' : '👁‍🗨' }}
          </button>
        </div>
      </div>

      <div class="field-group">
        <div class="password-wrapper">
          <input
            v-model="form.confirmPassword"
            :type="showConfirm ? 'text' : 'password'"
            placeholder="再次输入密码"
            required
            class="field-input"
          />
          <button type="button" class="password-toggle" @click="showConfirm = !showConfirm">
            {{ showConfirm ? '👁' : '👁‍🗨' }}
          </button>
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

      <div class="agreement-row">
        <label class="agreement-label">
          <input v-model="agreed" type="checkbox" class="agreement-checkbox" />
          <span class="agreement-text">
            我已阅读并同意
            <button type="button" class="agreement-link" @click.prevent="showAgreement = 'user'">《用户协议》</button>
            和
            <button type="button" class="agreement-link" @click.prevent="showAgreement = 'privacy'">《隐私政策》</button>
          </span>
        </label>
      </div>

      <!-- 协议弹窗 -->
      <Teleport to="body">
        <Transition name="modal">
          <div v-if="showAgreement" class="agreement-modal" @click.self="showAgreement = ''">
            <div class="agreement-content">
              <div class="agreement-header">
                <h3>{{ showAgreement === 'user' ? '用户协议' : '隐私政策' }}</h3>
                <button class="agreement-close" @click="showAgreement = ''">✕</button>
              </div>
              <div class="agreement-body">
                <template v-if="showAgreement === 'user'">
                  <h4>一、接受条款</h4>
                  <p>欢迎使用 Mythweave 智能小说创作平台。当您注册或使用本服务时，表示您已同意遵守本协议的所有条款。</p>
                  <h4>二、服务描述</h4>
                  <p>Mythweave 提供智能小说创作辅助工具，包括但不限于AI写作建议、情节生成、人物设定等功能。</p>
                  <h4>三、用户责任</h4>
                  <p>用户需保证上传和生成的内容不侵犯他人知识产权，不包含违法、有害或不当信息。</p>
                  <h4>四、知识产权</h4>
                  <p>用户在使用本平台生成的内容，知识产权归用户所有。平台保留优化和改进服务的权利。</p>
                  <h4>五、服务变更</h4>
                  <p>平台保留随时修改或中断服务的权利，并会提前通知用户。</p>
                </template>
                <template v-else>
                  <h4>一、信息收集</h4>
                  <p>我们收集您提供的注册信息（用户名、邮箱）以及使用过程中产生的数据，以用于改善服务质量。</p>
                  <h4>二、信息使用</h4>
                  <p>您的信息将用于：提供个性化服务、保障账号安全、分析使用情况。我们不会将您的个人信息出售给第三方。</p>
                  <h4>三、信息保护</h4>
                  <p>我们采用行业标准的安全措施保护您的数据，防止未经授权的访问、使用或泄露。</p>
                  <h4>四、Cookie 使用</h4>
                  <p>为提升用户体验，我们使用 Cookie 技术来记住您的偏好设置和登录状态。</p>
                  <h4>五、用户权利</h4>
                  <p>您有权查看、修改或删除您的个人信息。如需帮助，请联系我们的客服团队。</p>
                </template>
              </div>
              <div class="agreement-footer">
                <button class="agreement-confirm" @click="showAgreement = ''">我已知晓</button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

      <button type="submit" class="submit-btn" :disabled="loading || !agreed" ref="btnRef">
        <span v-if="loading" class="spinner"></span>
        <span v-else>立即加入</span>
      </button>
    </form>

    <p class="form-footer">
      已有账号？
      <router-link to="/login" class="footer-link">去登录</router-link>
    </p>
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
const agreed = ref(false)
const showPassword = ref(false)
const showConfirm = ref(false)
const showAgreement = ref('')
const btnRef = ref(null)

async function handleRegister() {
  if (loading.value) return

  if (btnRef.value) {
    btnRef.value.classList.add('clicked')
    setTimeout(() => btnRef.value?.classList.remove('clicked'), 600)
  }

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
.register-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
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

.form-message.success {
  background: rgba(80, 160, 100, 0.1);
  color: #408050;
  border: 1px solid rgba(80, 160, 100, 0.2);
}

.form-message span {
  font-size: 14px;
}

/* ═══ 协议勾选 ═══ */
.agreement-row {
  margin: 4px 0;
}

.agreement-label {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  cursor: pointer;
}

.agreement-checkbox {
  width: 18px;
  height: 18px;
  margin-top: 2px;
  accent-color: #E87A3E;
  cursor: pointer;
  flex-shrink: 0;
}

.agreement-text {
  font-size: 13px;
  color: #A08060;
  line-height: 1.5;
}

.agreement-link {
  color: #E87A3E;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
  background: none;
  border: none;
  padding: 0;
  font-size: inherit;
  cursor: pointer;
  font-family: inherit;
}

.agreement-link:hover {
  color: #D4692E;
  text-decoration: underline;
}

/* ═══ 提交按钮 ═══ */
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
  opacity: 0.5;
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

/* ═══ 协议弹窗 ═══ */
.agreement-modal {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(92, 61, 30, 0.5);
  backdrop-filter: blur(4px);
  padding: 20px;
}

.agreement-content {
  width: 100%;
  max-width: 480px;
  max-height: 80vh;
  background: #FFFDF8;
  border-radius: 20px;
  box-shadow: 0 16px 48px rgba(92, 61, 30, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.agreement-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(200, 160, 120, 0.2);
}

.agreement-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #5C3D1E;
  margin: 0;
}

.agreement-close {
  width: 32px;
  height: 32px;
  border: none;
  background: #F5F0EB;
  border-radius: 8px;
  color: #A08060;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.agreement-close:hover {
  background: #EDE6DE;
}

.agreement-body {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.agreement-body h4 {
  font-size: 14px;
  font-weight: 600;
  color: #5C3D1E;
  margin: 0 0 8px;
}

.agreement-body p {
  font-size: 13px;
  color: #8B7355;
  line-height: 1.7;
  margin: 0 0 16px;
}

.agreement-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(200, 160, 120, 0.2);
}

.agreement-confirm {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #E87A3E 0%, #D4692E 100%);
  border: none;
  border-radius: 12px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.agreement-confirm:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(232, 122, 62, 0.3);
}

/* ═══ 弹窗动画 ═══ */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s;
}

.modal-enter-active .agreement-content,
.modal-leave-active .agreement-content {
  transition: transform 0.3s, opacity 0.3s;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .agreement-content,
.modal-leave-to .agreement-content {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
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