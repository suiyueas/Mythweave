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

          <!-- ═══ 视图二：充值中心 ═══ -->
          <template v-else>
            <!-- 支付成功动画 -->
            <Transition name="success-pop">
              <div v-if="paySuccess" class="pay-success-overlay">
                <div class="pay-success-box">
                  <div class="success-icon">✓</div>
                  <h3>开通成功！</h3>
                  <p>感谢您的支持，祝创作愉快 🎉</p>
                </div>
              </div>
            </Transition>

            <div class="vip-recharge" :class="{ blurred: paySuccess }">
              <div class="recharge-header">
                <div>
                  <h3 class="vip-title">👑 升级 VIP</h3>
                  <p class="vip-slogan">"解锁 AI 无限可能，让每一次创作都惊艳"</p>
                </div>
                <button class="recharge-close" @click="handleClose">✕</button>
              </div>

              <div v-if="paying" class="paying-status">
                <span class="paying-spinner"></span>
                <span>支付处理中…</span>
              </div>

              <template v-else>
                <!-- 套餐选择 - 双卡片布局 -->
                <div class="plan-cards">
                  <button
                    v-for="plan in plans"
                    :key="plan.id"
                    class="plan-card"
                    :class="{ active: selectedPlan === plan.id, popular: plan.id === 'permanent' }"
                    @click="selectedPlan = plan.id"
                  >
                    <div v-if="plan.id === 'permanent'" class="plan-hot-tag">🔥 性价比之王</div>
                    <div class="plan-card-inner">
                      <div class="plan-name-row">
                        <span class="plan-name">{{ plan.name }}</span>
                      </div>
                      <div class="plan-price-row">
                        <span class="plan-price">¥{{ plan.price }}</span>
                        <span class="plan-unit">/{{ plan.isPermanent ? '永久' : '月' }}</span>
                      </div>
                      <div v-if="plan.id === 'monthly'" class="plan-original">
                        <span class="original-label">原价 ¥15/月</span>
                      </div>
                      <div v-if="plan.id === 'permanent'" class="plan-original">
                        <span class="original-label">原价 ¥299</span>
                      </div>
                      <p class="plan-desc">{{ plan.description }}</p>
                      <ul class="plan-features">
                        <li v-for="(f, idx) in plan.features" :key="idx" :class="{ highlight: f.includes('🎁') }">
                          {{ f }}
                        </li>
                      </ul>
                      <div class="plan-selector">
                        <span class="selector-check">{{ selectedPlan === plan.id ? '✓' : '' }}</span>
                        <span class="selector-text">{{ selectedPlan === plan.id ? '已选择' : '选择' }}</span>
                      </div>
                    </div>
                  </button>
                </div>

                <!-- 价值计算器 -->
                <div v-if="selectedPlan === 'permanent'" class="value-calculator">
                  <span class="calc-icon">💡</span>
                  <span class="calc-text">月度 ¥9 × 12月 = ¥108，永久仅 ¥99，立省 ¥9，且享终身权益！</span>
                </div>

                <!-- 支付方式 -->
                <div class="pay-section">
                  <span class="pay-label">选择支付方式</span>
                  <div class="pay-cards">
                    <button
                      v-for="m in payMethods"
                      :key="m.key"
                      class="pay-card"
                      :class="{ active: payMethod === m.key, recommended: m.key === 'alipay' }"
                      @click="payMethod = m.key"
                    >
                      <span v-if="m.key === 'alipay'" class="recommended-tag">推荐</span>
                      <span class="pay-icon">{{ m.icon }}</span>
                      <span class="pay-name">{{ m.label }}</span>
                    </button>
                  </div>
                </div>

                <!-- 支付按钮 -->
                <div class="recharge-actions">
                  <button class="btn-pay" :disabled="!selectedPlan" @click="handlePay">
                    <span class="btn-pay-icon">🔒</span>
                    {{ payMethod === 'alipay' ? '支付宝' : '微信' }}支付 ¥{{ currentPlan?.price || 0 }}
                  </button>
                </div>

                <!-- 信任信号 -->
                <div class="trust-signals">
                  <span class="trust-item">🔒 加密传输</span>
                  <span class="trust-divider">·</span>
                  <span class="trust-item">7天无理由退款</span>
                  <span class="trust-divider">·</span>
                  <span class="trust-item">24小时客服</span>
                </div>
                <p class="social-proof">已有 <strong>1,285</strong> 位用户升级</p>
              </template>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
/**
 * VIP 升级弹窗组件
 * 支持两种模式：
 * 1. guard - 权限拦截引导（用户尝试使用VIP功能时显示）
 * 2. recharge - 充值中心（用户主动充值时显示）
 */
import { ref, computed, onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useVipModalState, closeVipModal } from '@/services/vipService'

// ─── 状态 ───
const vipState = useVipModalState()
const userStore = useUserStore()

// VIP 权益列表（引导视图使用）
const benefits = [
  '无限次 AI 生成',
  '专属写作模板',
  '智能续写与润色',
  '优先技术支持'
]

// ─── 充值中心状态 ───
const plans = ref([])                   // 套餐列表
const selectedPlan = ref('')             // 当前选中的套餐ID
const payMethod = ref('alipay')         // 支付方式：alipay-支付宝 / wechat-微信
const paying = ref(false)               // 是否正在支付中
const paySuccess = ref(false)           // 支付是否成功（显示成功动画）

// 支付方式选项
const payMethods = [
  { key: 'alipay', icon: '💳', label: '支付宝' },
  { key: 'wechat', icon: '💚', label: '微信支付' }
]

// 当前选中的套餐详情
const currentPlan = computed(() => plans.value.find(p => p.id === selectedPlan.value))

// ─── 方法 ───

/**
 * 切换到充值中心视图
 */
function goRecharge() {
  vipState.mode = 'recharge'
}

/**
 * 关闭弹窗
 * 支付中不允许关闭
 */
function handleClose() {
  if (paying.value) return
  paySuccess.value = false
  closeVipModal()
}

/**
 * 加载VIP套餐列表
 * 默认选中永久会员
 */
async function loadPlans() {
  const list = await userStore.getVipPlans()
  if (list && list.length) {
    plans.value = list
    if (!selectedPlan.value) selectedPlan.value = list.find(p => p.id === 'permanent')?.id || list[0].id
  }
}

/**
 * 处理支付
 * 1. 创建订单（带去重机制，相同套餐有未支付订单则复用）
 * 2. 调用模拟支付回调
 * 3. 成功后显示动画并关闭弹窗
 */
async function handlePay() {
  if (!selectedPlan.value || paying.value) return
  paying.value = true

  try {
    // 1. 创建订单（带去重机制）
    const orderRes = await userStore.createVipOrder(selectedPlan.value, payMethod.value)
    if (!orderRes.success) {
      window.alert(orderRes.message || '创建订单失败')
      paying.value = false
      return
    }

    // 2. 模拟支付回调（mockPayCallback 内部已更新 profile）
    const payRes = await userStore.mockPayCallback(orderRes.orderNo)
    paying.value = false

    if (payRes.success) {
      paySuccess.value = true
      setTimeout(() => {
        paySuccess.value = false
        closeVipModal()
      }, 2000)
    } else {
      window.alert(payRes.message || '支付失败，请重试')
    }
  } catch (e) {
    paying.value = false
    window.alert('支付异常：' + e.message)
  }
}

onMounted(() => { loadPlans() })
watch(() => vipState.visible, (v) => {
  if (v) {
    selectedPlan.value = ''
    payMethod.value = 'alipay'
    paySuccess.value = false
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
  overflow-y: auto;
}

.vip-modal {
  width: 480px;
  max-width: 100%;
  max-height: calc(100vh - 40px);
  background: #fffdf8;
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(90, 60, 10, 0.28);
  border: 1px solid rgba(217, 119, 6, 0.25);
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  margin: auto;
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

/* ═══ 弹窗内容区可滚动 ═══ */
.vip-modal > * {
  flex-shrink: 0;
}
.vip-modal > .vip-guard,
.vip-modal > .vip-recharge {
  flex-shrink: 1;
  min-height: 0;
}

/* ═══ 视图一：权限拦截 ═══ */
.vip-guard {
  padding: 36px 32px 28px;
  text-align: center;
  overflow-y: auto;
  flex: 1;
}

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
  margin: 0 0 6px;
  letter-spacing: 0.2px;
}

.vip-slogan {
  font-size: 12px;
  color: #a16207;
  margin: 0;
  font-style: italic;
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
.vip-recharge {
  padding: 28px 28px 24px;
  overflow-y: auto;
  flex: 1;
}
.vip-recharge.blurred { filter: blur(4px); pointer-events: none; }

.recharge-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
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
  flex-shrink: 0;
}
.recharge-close:hover { background: #e7e5e4; }

/* ═══ 套餐卡片布局 ═══ */
.plan-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 14px;
}

.plan-card {
  position: relative;
  border: 2px solid #e7e5e4;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
  padding: 0;
  text-align: left;
}
.plan-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}
.plan-card.active {
  border-color: #d97706;
  background: linear-gradient(180deg, #fffbeb 0%, #fff 100%);
}
.plan-card.popular {
  border-color: #f59e0b;
}
.plan-card.popular.active {
  background: linear-gradient(180deg, #fef3c7 0%, #fff 100%);
}

.plan-hot-tag {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 6px 10px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ef4444, #f97316);
  text-align: center;
  letter-spacing: 0.5px;
}

.plan-card-inner {
  padding: 16px 14px 14px;
}

.plan-card.popular .plan-card-inner {
  padding-top: 28px;
}

.plan-name-row {
  margin-bottom: 8px;
}

.plan-name {
  font-size: 14px;
  font-weight: 700;
  color: #1c1917;
}

.plan-price-row {
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin-bottom: 2px;
}

.plan-price {
  font-size: 28px;
  font-weight: 800;
  color: #b45309;
  line-height: 1;
}

.plan-unit {
  font-size: 13px;
  color: #a8a29e;
}

.plan-original {
  margin-bottom: 6px;
}

.original-label {
  font-size: 11px;
  color: #a8a29e;
  text-decoration: line-through;
}

.plan-desc {
  font-size: 11px;
  color: #78716c;
  margin: 0 0 10px;
  line-height: 1.5;
}

.plan-features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.plan-features li {
  font-size: 11px;
  color: #57534e;
  display: flex;
  align-items: center;
  gap: 5px;
}

.plan-features li.highlight {
  color: #d97706;
  font-weight: 600;
}

.plan-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: 10px;
  padding: 6px;
  border-radius: 8px;
  background: #f5f5f4;
  font-size: 11px;
  font-weight: 600;
  color: #78716c;
  transition: all 0.15s;
}
.plan-card.active .plan-selector {
  background: linear-gradient(135deg, #d97706, #f59e0b);
  color: #fff;
}
.selector-check { font-size: 12px; }

/* ═══ 价值计算器 ═══ */
.value-calculator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
  border: 1px solid #fde68a;
  border-radius: 10px;
  margin-bottom: 16px;
}

.calc-icon { font-size: 14px; }
.calc-text { font-size: 12px; color: #92400e; line-height: 1.4; }

/* ═══ 支付方式 ═══ */
.pay-section { margin-bottom: 16px; }

.pay-label {
  font-size: 12px;
  font-weight: 600;
  color: #44403c;
  display: block;
  margin-bottom: 8px;
}

.pay-cards { display: flex; gap: 10px; }

.pay-card {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border: 2px solid #e7e5e4;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.15s;
}
.pay-card:hover { border-color: #d4a574; }
.pay-card.active {
  border-color: #d97706;
  background: linear-gradient(180deg, #fffbeb, #fff);
}
.pay-card.recommended { border-color: #fcd34d; }

.recommended-tag {
  position: absolute;
  top: -8px;
  right: 8px;
  font-size: 9px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #ef4444, #f97316);
  padding: 2px 6px;
  border-radius: 999px;
}

.pay-icon { font-size: 20px; }
.pay-name { font-size: 12px; font-weight: 600; color: #44403c; }

/* ═══ 支付按钮 ═══ */
.recharge-actions { margin-bottom: 12px; }

.btn-pay {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 20px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #d97706, #f59e0b);
  box-shadow: 0 6px 20px rgba(217, 119, 6, 0.4);
  cursor: pointer;
  transition: all 0.2s;
}
.btn-pay:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(217, 119, 6, 0.5);
}
.btn-pay:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.btn-pay-icon { font-size: 14px; }

/* ═══ 信任信号 ═══ */
.trust-signals {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.trust-item { font-size: 11px; color: #78716c; }
.trust-divider { color: #d4d4d4; font-size: 11px; }

.social-proof {
  text-align: center;
  font-size: 12px;
  color: #78716c;
  margin: 0;
}
.social-proof strong { color: #d97706; }

/* ═══ 支付中状态 ═══ */
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

/* ═══ 支付成功动画 ═══ */
.pay-success-overlay {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 253, 248, 0.95);
  backdrop-filter: blur(2px);
}

.pay-success-box {
  text-align: center;
  padding: 40px;
}

.success-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: #fff;
  background: linear-gradient(135deg, #16a34a, #22c55e);
  border-radius: 50%;
  box-shadow: 0 8px 32px rgba(22, 163, 74, 0.4);
  animation: successPulse 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes successPulse {
  from { transform: scale(0); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.pay-success-box h3 {
  font-size: 22px;
  font-weight: 700;
  color: #1c1917;
  margin: 0 0 8px;
}

.pay-success-box p {
  font-size: 14px;
  color: #78716c;
  margin: 0;
}

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

.success-pop-enter-active { transition: opacity 0.3s; }
.success-pop-leave-active { transition: opacity 0.3s; }
.success-pop-enter-from, .success-pop-leave-to { opacity: 0; }
</style>