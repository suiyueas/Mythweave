import { get } from './request'

export const systemApi = {
  /** 获取系统真实状态（AI服务/数据库/存储） */
  getStatus: () => get('/api/system/status')
}
