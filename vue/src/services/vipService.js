/**
 * VIP 会员服务 — 全局弹窗状态管理与 AI 功能权限拦截
 *
 * 用法：
 * - requireVip('智能大纲生成')：在各 AI 生成入口调用，VIP 放行返回 true，
 *   非 VIP 弹出升级引导弹窗并返回 false
 * - openVipModal({ mode: 'recharge' })：直接打开充值中心
 */
import { reactive } from 'vue'
import { useUserStore } from '@/stores/user'

// 全局 VIP 弹窗状态（单一实例，任意组件均可读取）
const state = reactive({
  visible: false,
  // 弹窗视图：guard-权限拦截引导 / recharge-充值中心
  mode: 'guard',
  // 是否为 VIP 过期场景（文案区分）
  expired: false,
  // 触发拦截的 AI 功能名称
  featureName: ''
})

/** 打开 VIP 弹窗 */
export function openVipModal(options = {}) {
  state.mode = options.mode || 'guard'
  state.expired = !!options.expired
  state.featureName = options.featureName || ''
  state.visible = true
}

/** 关闭 VIP 弹窗 */
export function closeVipModal() {
  state.visible = false
}

/** 获取全局弹窗状态（供组件绑定） */
export function useVipModalState() {
  return state
}

/**
 * AI 功能权限拦截：VIP 生效中放行；普通用户/已过期用户弹出升级引导
 * @param {string} featureName - 触发的功能名称（如"智能大纲生成"）
 * @returns {boolean} true-可继续执行，false-已拦截需升级
 */
export function requireVip(featureName = '') {
  const userStore = useUserStore()
  if (userStore.isVip) return true
  openVipModal({ mode: 'guard', expired: userStore.vipExpired, featureName })
  return false
}
