<template>
  <Teleport to="body">
    <Transition name="vip-fade">
      <div v-if="vipState.visible" class="vip-overlay" @click.self="handleClose">
        <div class="vip-modal" :class="{ 'vip-modal-recharge': vipState.mode === 'recharge' }">
          <!-- ═══ 视图一：权限拦截引导 ═══ -->
          <template v-if="vipState.mode === 'guard'">
            <div class="vip-guard">
              <div class="vip-badge">👑</div>
              <h3 class="vip-title">{{ vipState.expired ? '您的 VIP 已过期' : '该功能仅限 VIP 用户使用' }}</h3>
              <p v-if="vipState.featureName" class="vip-feature">
                「{{ vipState.featureName }}」{{ vipState.expired ? '需续费后继续使用' : '为 VIP 专属功能' }}
              </p>
              <p v-else class="vip-feature">{{ vipState.expired ? '续费后即可继续使用全部 AI 能力' : '升级后即可解锁全部 AI 能力' }}</p>

              <ul class="vip-benefits">
                <li v-for="b in benefits" :key="b">
                  <span class="benefit-dot">✦</span>{{ b }}
                </li>
              </ul>

              <div class="vip-actions">
                <button class="btn-upgrade" @click="goRecharge">
                  {{ vipState.expired ? '立即续费' : '立即升级' }}
                </button>
                <button class="btn-later" @click="handleClose">暂不需要</button>
              </div>
              <button class="vip-all-link" @click="goRecharge">查看全部权益 →</button>
            </div>
          </template>

          <!-- ═══ 视图二：充值中心（支付占位） ═══ -->
          <template v-else>
            <div class="vip-recharge">
              <div class="recharge-header">
                <h3 class="vip-title">👑 升级 VIP</h3>
                <button class="recharge-close" @click="handleClose">✕</button>
              </div>
              <p class="recharge-sub">解锁无限 AI 生成 · 专属模板 · 优先支持</p>

              <div v-if="paying" class="paying-status">
                <span class="paying-spinner"></span>
                <span>支付处理中…</span>
              </div>

              <template v-else>
                <!-- 套餐选择 -->
                <div class="plan-grid">
                  <button
                    v-for="plan in plans"
                    :key="plan.id"
                    class="plan-card"
                    :class="{ active: selectedPlan === plan.id, popular: plan.id === 'yearly' }"
                    @click="selectedPlan = plan.id"
                  >
                    <span v-if="plan.id === 'yearly'" class="plan-tag">最划算</span>
                    <span class="plan-name">{{ plan.name }}</span>
                    <span class="plan-price">¥{{ plan.price }}</span>
                    <span class="plan-unit">{{ plan.unitPrice }}</span>
                    <span class="plan-badge">{{ plan.badge }}</span>
                  </button>
                </div>

                <!-- 支付方式 -->
                <div class="pay-methods">
                  <span class="pay-label">支付方式</span>
                  <div class="pay-options">
                    <button
                      v-for="m in payMethods"
                      :key="m.key"
                      class="pay-option"
                      :class="{ active: payMethod === m.key }"
                      @click="payMethod = m.key"
                    >
                      <span class="pay-icon">{{ m.icon }}</span>{{ m.label }}
                    </button>
                  </div>
                </div>

                <div class="recharge-actions">
                  <button class="btn-upgrade" :disabled="!selectedPlan" @click="handlePay">
                    {{ payMethod === 'wechat' ? '微信支付' : '支付宝支付' }} ¥{{ currentPlan?.price || 0 }}
                  </button>
                  <button class="btn-later" @click="handleClose">取消</button>
                </div>
                <p class="pay-tip">🔒 模拟支付环境：点击后直接开通，后续可对接真实支付</p>
              </template>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useVipModalState, closeVipModal } from '@/services/vipService'

const vipState = useVipModalState()
const userStore = useUserStore()

const benefits = [
  '无限次 AI 生成',
  '专属写作模板',
  '智能续写与润色',
  '优先技术支持'
]

// ─── 充值中心 ───
const plans = ref([])
const selectedPlan = ref('')
const payMethod = ref('wechat')
const paying = ref(false)
const payMethods = [
  { key: 'wechat', icon: '💚', label: '微信支付' },
  { key: 'alipay', icon: '🔵', label: '支付宝' }
]

const currentPlan = computed(() => plans.value.find(p => p.id === selectedPlan.value))

function goRecharge() {
  vipState.mode = 'recharge'
}

function handleClose() {
  if (paying.value) return
  closeVipModal()
}

async function loadPlans() {
  const list = await userStore.getVipPlans()
  if (list && list.length) {
    plans.value = list
    if (!selectedPlan.value) selectedPlan.value = list.find(p => p.id === 'yearly')?.id || list[0].id
  }
}

async function handlePay() {
  if (!selectedPlan.value || paying.value) return
  paying.value = true
  const res = await userStore.activateVip(selectedPlan.value)
  paying.value = false
  if (res.success) {
    await userStore.refreshVip()
    closeVipModal()
  } else {
    // 支付失败保留弹窗，由用户重试
    window.alert(res.message || '开通失败，请重试')
  }
}

onMounted(() => { loadPlans() })
watch(() => vipState.visible, (v) => {
  if (v) {
    selectedPlan.value = ''
    payMethod.value = 'wechat'
    loadPlans()
  }
})
</script>

<style scoped>
/* ═══ 弹窗遮罩与容器 ═══ */
.vip-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 12, 8, 0.55);
  backdrop-filter: blur(4px);
  padding: 20px;
}

.vip-modal {
  width: 420px;
  max-width: 100%;
  background: #fffdf8;
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(90, 60, 10, 0.28);
  border: 1px solid rgba(217, 119, 6, 0.25);
  overflow: hidden;
  position: relative;
}

.vip-modal::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 4px;
  background: linear-gradient(90deg, #f59e0b, #fbbf24, #d97706, #f59e0b);
  background-size: 200% 100%;
  animation: vipShimmer 3s linear infinite;
}

@keyframes vipShimmer {
  from { background-position: 0% 0; }
  to { background-position: 200% 0; }
}

.vip-modal-recharge { width: 520px; }

/* ═══ 视图一：权限拦截 ═══ */
.vip-guard { padding: 36px 32px 28px; text-align: center; }

.vip-badge {
  width: 64px;
  height: 64px;
  margin: 0 auto 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border-radius: 50%;
  box-shadow: 0 8px 24px rgba(217, 119, 6, 0.35);
}

.vip-title {
  font-size: 19px;
  font-weight: 700;
  color: #1c1917;
  margin: 0 0 8px;
  letter-spacing: 0.2px;
}

.vip-feature {
  font-size: 13px;
  color: #78716c;
  margin: 0 0 18px;
  line-height: 1.6;
}

.vip-benefits {
  list-style: none;
  margin: 0 0 22px;
  padding: 16px 18px;
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
  border: 1px solid #fde68a;
  border-radius: 12px;
  text-align: left;
  display: grid;
  gap: 9px;
}

.vip-benefits li {
  font-size: 13px;
  color: #78350f;
  display: flex;
  align-items: center;
  gap: 8px;
}

.benefit-dot { color: #d97706; font-size: 14px; }

.vip-actions { display: flex; gap: 10px; justify-content: center; }

.btn-upgrade {
  flex: 1;
  padding: 11px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #d97706, #f59e0b);
  box-shadow: 0 6px 16px rgba(217, 119, 6, 0.35);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.btn-upgrade:hover { transform: translateY(-1px); box-shadow: 0 8px 20px rgba(217, 119, 6, 0.45); }
.btn-upgrade:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.btn-later {
  padding: 11px 22px;
  border: 1px solid #e7e5e4;
  border-radius: 10px;
  background: #fff;
  color: #57534e;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-later:hover { background: #f5f5f4; }

.vip-all-link {
  margin-top: 14px;
  background: none;
  border: none;
  font-size: 12px;
  color: #d97706;
  cursor: pointer;
}
.vip-all-link:hover { text-decoration: underline; }

/* ═══ 视图二：充值中心 ═══ */
.vip-recharge { padding: 28px 28px 24px; }

.recharge-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.recharge-header .vip-title { margin: 0; font-size: 20px; }

.recharge-close {
  width: 30px;
  height: 30px;
  border: none;
  background: #f5f5f4;
  border-radius: 8px;
  color: #78716c;
  font-size: 13px;
  cursor: pointer;
}
.recharge-close:hover { background: #e7e5e4; }

.recharge-sub {
  font-size: 13px;
  color: #a16207;
  margin: 6px 0 18px;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 18px;
}

.plan-card {
  position: relative;
  padding: 14px 8px 12px;
  border: 1.5px solid #e7e5e4;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s;
}
.plan-card:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,0.06); }
.plan-card.active {
  border-color: #d97706;
  background: linear-gradient(180deg, #fffbeb, #fff);
  box-shadow: 0 4px 14px rgba(217, 119, 6, 0.18);
}

.plan-tag {
  position: absolute;
  top: -9px;
  right: 8px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #d97706, #f59e0b);
  padding: 2px 8px;
  border-radius: 999px;
}

.plan-name { font-size: 13px; font-weight: 600; color: #1c1917; }
.plan-price { font-size: 22px; font-weight: 700; color: #b45309; line-height: 1.2; }
.plan-unit { font-size: 11px; color: #a8a29e; }
.plan-badge { font-size: 11px; color: #d97706; }

.pay-methods { margin-bottom: 20px; }

.pay-label { font-size: 13px; font-weight: 600; color: #44403c; display: block; margin-bottom: 8px; }

.pay-options { display: flex; gap: 10px; }

.pay-option {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  border: 1.5px solid #e7e5e4;
  border-radius: 10px;
  background: #fff;
  font-size: 13px;
  color: #44403c;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}
.pay-option.active { border-color: #d97706; background: #fffbeb; }
.pay-icon { font-size: 15px; }

.recharge-actions { display: flex; gap: 10px; }

.pay-tip {
  margin: 14px 0 0;
  font-size: 11px;
  color: #a8a29e;
  text-align: center;
}

/* ═══ 支付中 ═══ */
.paying-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 56px 0;
  font-size: 14px;
  color: #57534e;
}

.paying-spinner {
  width: 26px;
  height: 26px;
  border: 3px solid #fde68a;
  border-top-color: #d97706;
  border-radius: 50%;
  animation: vipSpin 0.8s linear infinite;
}

@keyframes vipSpin { to { transform: rotate(360deg); } }

/* ═══ 动画 ═══ */
.vip-fade-enter-active { transition: opacity 0.25s; }
.vip-fade-leave-active { transition: opacity 0.2s; }
.vip-fade-enter-from, .vip-fade-leave-to { opacity: 0; }

.vip-fade-enter-active .vip-modal { animation: vipModalIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1); }
.vip-fade-leave-active .vip-modal { animation: vipModalOut 0.2s ease-in; }

@keyframes vipModalIn {
  from { opacity: 0; transform: scale(0.92) translateY(14px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
@keyframes vipModalOut {
  from { opacity: 1; transform: scale(1); }
  to { opacity: 0; transform: scale(0.95); }
}
</style>
