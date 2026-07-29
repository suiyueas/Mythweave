import { get, post, del, streamPost } from './request'

const REQUEST_TIMEOUT = 30000
const MAX_RETRIES = 2

function withTimeout(promise, timeoutMs = REQUEST_TIMEOUT) {
  return Promise.race([
    promise,
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error('请求超时，请检查网络后重试')), timeoutMs)
    )
  ])
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

async function withRetry(fn, maxRetries = MAX_RETRIES) {
  let lastError
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await fn()
    } catch (e) {
      lastError = e
      if (attempt < maxRetries) {
        const delay = Math.min(1000 * Math.pow(2, attempt), 5000)
        console.warn(`请求失败，第 ${attempt + 1} 次重试（${delay}ms）...`)
        await sleep(delay)
      }
    }
  }
  throw lastError
}

export const chatApi = {
  // ─── 会话管理 ───
  listSessions: (projectId) => get(`/api/projects/${projectId}/ai/sessions`),

  createSession: (projectId, data) => post(`/api/projects/${projectId}/ai/sessions`, data),

  getSessionMessages: (projectId, sessionId) => get(`/api/projects/${projectId}/ai/sessions/${sessionId}/messages`),

  deleteSession: (projectId, sessionId) => del(`/api/projects/${projectId}/ai/sessions/${sessionId}`),

  // ─── 健康检查 ───
  checkHealth: (projectId) => get(`/api/projects/${projectId}/ai/health`).catch(() => null),

  // ─── 多Agent协作分析 ───
  orchestrate: (projectId, data) => post(`/api/projects/${projectId}/agent/orchestrate`, data),

  // ─── 综合分析历史 ───
  getAnalysisHistory: (projectId) => get(`/api/projects/${projectId}/analysis/history`),
  getAnalysis: (projectId, analysisId) => get(`/api/projects/${projectId}/analysis/${analysisId}`),
  deleteAnalysis: (projectId, analysisId) => del(`/api/projects/${projectId}/analysis/${analysisId}`),

  // ─── 流式对话（增强版：超时 + 重试） ───
  streamChat(projectId, params, onToken, onDone, onError) {
    const { userMessage, agent, sessionId, novelTitle, genre, currentChapter, context } = params

    const doRequest = () => new Promise((resolve, reject) => {
      const timeoutId = setTimeout(() => {
        reject(new Error('请求超时，请重试'))
      }, REQUEST_TIMEOUT)

      const stream = streamPost(
        `/api/projects/${projectId}/ai/stream/chat`,
        { userMessage, agent, sessionId, novelTitle, genre, currentChapter, context },
        (token) => {
          clearTimeout(timeoutId)
          onToken && onToken(token)
        },
        (...args) => {
          clearTimeout(timeoutId)
          resolve(...args)
        },
        (err) => {
          clearTimeout(timeoutId)
          reject(err)
        }
      )
    })

    withRetry(doRequest).then(onDone).catch(onError)
  }
}

export default chatApi