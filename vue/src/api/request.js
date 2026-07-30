// 开发环境走 Vite 代理（同源无 CORS），生产环境直连后端
const BASE_URL = import.meta.env.DEV ? '' : 'http://localhost:8080'

async function request(url, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  const token = localStorage.getItem('token')
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const config = {
    headers,
    ...options
  }
  if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
    config.body = JSON.stringify(config.body)
  } else if (config.body instanceof FormData) {
    delete config.headers['Content-Type']
  }

  let response
  try {
    response = await fetch(`${BASE_URL}${url}`, config)
  } catch (e) {
    const err = new Error('服务器连接失败，请确认后端服务已启动')
    err.code = 0
    err.originalError = e
    throw err
  }

  const json = await response.json()

  if (json.code === 401) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    window.location.href = '/login'
    throw new Error(json.message || '请先登录')
  }

  if (json.code != 200) {
    const err = new Error(json.message || `请求失败 (${json.code})`)
    err.code = json.code
    throw err
  }
  return json.data
}

function get(url, params) {
  if (params) {
    const qs = new URLSearchParams(Object.entries(params).filter(([_, v]) => v != null)).toString()
    url = `${url}?${qs}`
  }
  return request(url)
}

function post(url, body) { return request(url, { method: 'POST', body }) }
function put(url, body) { return request(url, { method: 'PUT', body }) }
function del(url, body) { return request(url, { method: 'DELETE', body }) }

function streamGet(url, params, onToken, onDone, onError) {
  if (params) {
    const qs = new URLSearchParams(Object.entries(params).filter(([_, v]) => v != null)).toString()
    url = `${url}?${qs}`
  }
  return streamFetch(url, null, 'GET', onToken, onDone, onError)
}

function streamPost(url, body, onToken, onDone, onError) {
  return streamFetch(url, body, 'POST', onToken, onDone, onError)
}

const HEARTBEAT_TIMEOUT_MS = 20000
const MAX_RECONNECT_ATTEMPTS = 3
const RECONNECT_DELAY_MS = 2000

function streamFetch(url, body, method, onToken, onDone, onError) {
  let reconnectAttempts = 0
  let heartbeatTimer = null
  let isClosed = false

  const getHeaders = () => {
    const token = localStorage.getItem('token')
    const headers = { 'Accept': 'text/event-stream', 'Content-Type': 'application/json' }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    return headers
  }

  const resetHeartbeatTimer = () => {
    if (heartbeatTimer) {
      clearTimeout(heartbeatTimer)
    }
    heartbeatTimer = setTimeout(() => {
      if (!isClosed) {
        console.warn('SSE heartbeat timeout, connection may be dead')
        isClosed = true
        if (heartbeatTimer) {
          clearTimeout(heartbeatTimer)
        }
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
          reconnectAttempts++
          console.warn(`SSE reconnecting... attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}`)
          setTimeout(() => {
            streamConnect()
          }, RECONNECT_DELAY_MS)
        } else {
          onError && onError(new Error('SSE connection lost after max reconnect attempts'))
        }
      }
    }, HEARTBEAT_TIMEOUT_MS)
  }

  const streamConnect = () => {
    if (isClosed) return

    let hasToken = false
    const config = {
      method,
      headers: getHeaders()
    }
    if (body && method === 'POST') {
      config.body = JSON.stringify(body)
    }

    fetch(`${BASE_URL}${url}`, config)
      .then(async (response) => {
        if (!response.ok) {
          let errMsg = `HTTP ${response.status}`
          try {
            const text = await response.text()
            try {
              const json = JSON.parse(text)
              errMsg = json.message || json.error || text
            } catch { errMsg = text || errMsg }
          } catch { /* ignore */ }
          throw new Error(errMsg)
        }
        if (!response.body) {
          throw new Error('响应体为空')
        }
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        resetHeartbeatTimer()

        while (true) {
          const { done, value } = await reader.read()
          if (done || isClosed) {
            if (!hasToken && !isClosed && onToken) {
              onToken('⚠️ 服务器未返回有效响应，请检查后端 AI 配置（DeepSeek API Key）')
            }
            if (!isClosed) {
              onDone && onDone()
            }
            break
          }
          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''
          for (const line of lines) {
            if (isClosed) break
            if (line.startsWith('event:heartbeat') || line.startsWith('data:ping')) {
              resetHeartbeatTimer()
            } else if (line.startsWith('data:')) {
              const raw = line.slice(5)
              let data
              try {
                data = JSON.parse(raw)
              } catch {
                data = raw
              }
              if (data !== undefined && data !== null) {
                hasToken = true
                resetHeartbeatTimer()
                onToken(data)
              }
            }
          }
        }
      })
      .catch((e) => {
        if (!isClosed) {
          if (heartbeatTimer) {
            clearTimeout(heartbeatTimer)
          }
          if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            console.warn(`SSE error, reconnecting... attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}`, e.message)
            setTimeout(() => {
              streamConnect()
            }, RECONNECT_DELAY_MS)
          } else {
            onError && onError(e)
          }
        }
      })
  }

  streamConnect()

  return {
    close: () => {
      isClosed = true
      if (heartbeatTimer) {
        clearTimeout(heartbeatTimer)
      }
    }
  }
}

export { get, post, put, del, streamGet, streamPost, request }