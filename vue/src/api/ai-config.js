import { get, put, post, del } from './request'

export const aiConfigApi = {
  getConfig: (projectId) => get(`/api/projects/${projectId}/ai/config`),

  updateConfig: (projectId, config) =>
    put(`/api/projects/${projectId}/ai/config`, config),

  getUsage: (projectId) => get(`/api/projects/${projectId}/ai/usage`),

  getPresets: (projectId) => get(`/api/projects/${projectId}/ai/presets`),

  createPreset: (projectId, preset) =>
    post(`/api/projects/${projectId}/ai/presets`, preset),

  updatePreset: (projectId, presetId, preset) =>
    put(`/api/projects/${projectId}/ai/presets/${presetId}`, preset),

  deletePreset: (projectId, presetId) =>
    del(`/api/projects/${projectId}/ai/presets/${presetId}`)
}

export default aiConfigApi