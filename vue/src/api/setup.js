import { get, post } from './request'

export const setupApi = {
  /** 触发全套设定生成 */
  generate: (projectId, params) =>
    post(`/api/projects/${projectId}/ai/setup/generate`, params),

  /** 查询生成进度 */
  getStatus: (projectId, taskId) =>
    get(`/api/projects/${projectId}/ai/setup/status`, { taskId }),

  /** 获取完整设定（带重试） */
  async getSetup(projectId, retries = 2) {
    const sid = projectId
    for (let i = 0; i <= retries; i++) {
      try {
        const data = await get(`/api/projects/${sid}/ai/setup`)
        if (hasAnyData(data)) return data
        if (i < retries) {
          console.warn(`⏳ 数据为空，${(i + 1) * 1.5}s 后重试 (${i + 1}/${retries})...`)
          await new Promise(r => setTimeout(r, (i + 1) * 1500))
        }
      } catch (e) {
        if (i === retries) throw e
        console.warn(`❌ 请求失败，重试中 (${i + 1}/${retries}):`, e.message)
      }
    }
    return get(`/api/projects/${sid}/ai/setup`) // 最终返回，可能是空数据
  },

  /** 单模块重新生成 */
  regenerate: (projectId, data) =>
    post(`/api/projects/${projectId}/ai/setup/regenerate`, data),

  // ═══ 分步引导式 API（新增） ═══

  /** 步骤1：生成世界观 */
  generateWorld: (projectId, params) =>
    post(`/api/projects/${projectId}/ai/setup/step/world`, params),

  /** 步骤2：生成人物群像 */
  generateCharacters: (projectId, params) =>
    post(`/api/projects/${projectId}/ai/setup/step/characters`, params),

  /** 步骤3：生成大纲结构 */
  generateOutline: (projectId, params) =>
    post(`/api/projects/${projectId}/ai/setup/step/outline`, params),

  /** 步骤4：生成情节引擎 */
  generatePlot: (projectId, params) =>
    post(`/api/projects/${projectId}/ai/setup/step/plot`, params),

  /** 步骤5：生成灵感素材 */
  generateInspirations: (projectId, params) =>
    post(`/api/projects/${projectId}/ai/setup/step/inspirations`, params),

  /** 生成人物关系（基于已有角色） */
  generateRelations: (projectId, params) =>
    post(`/api/projects/${projectId}/ai/setup/step/relations`, params)
}

/** 检测设定数据是否含有任何有效内容 */
function hasAnyData(data) {
  if (!data) return false
  const keys = ['worldSettings', 'characters', 'outlines', 'plotThreads', 'inspirations', 'foreshadowings']
  for (const key of keys) {
    if (data[key] && Array.isArray(data[key]) && data[key].length > 0) return true
  }
  return false
}
