import { get } from './request'

export const searchApi = {
  /**
   * 全局搜索
   * @param {number} projectId - 项目ID
   * @param {string} keyword - 搜索关键词（支持空格分隔的多关键词）
   * @returns {Promise<Array>} 搜索结果列表
   */
  globalSearch: (projectId, keyword) => {
    return get(`/api/projects/${projectId}/search`, { keyword })
  },

  /**
   * 快速搜索（返回前10条结果，用于搜索框下拉建议）
   * @param {number} projectId - 项目ID
   * @param {string} keyword - 搜索关键词
   * @returns {Promise<Array>} 搜索结果列表（最多10条）
   */
  quickSearch: (projectId, keyword) => {
    return get(`/api/projects/${projectId}/search/quick`, { keyword })
  }
}