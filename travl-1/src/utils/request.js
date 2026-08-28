import axios from 'axios'
import { showLoadingToast, closeToast, showToast } from 'vant'

let loadingCount = 0

const showGlobalLoading = () => {
  loadingCount++
  if (loadingCount === 1) showLoadingToast({ forbidClick: true, duration: 0 })
}

const hideGlobalLoading = () => {
  loadingCount--
  if (loadingCount <= 0) {
    loadingCount = 0
    closeToast()
  }
}

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:3200'

// Token 自动续期：检查是否快过期
const shouldRefreshToken = () => {
  const token = localStorage.getItem('token')
  if (!token) return false
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000
    const remaining = exp - Date.now()
    // 剩余不到 2 小时就刷新
    return remaining < 2 * 60 * 60 * 1000
  } catch {
    return false
  }
}

const refreshToken = async () => {
  try {
    const res = await fetch(`${baseURL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    const data = await res.json()
    if (data.success && data.data?.token) {
      localStorage.setItem('token', data.data.token)
    }
  } catch {
    // 刷新失败忽略，等过期了再踢到登录页
  }
}

// 创建 axios 实例
const service = axios.create({
  baseURL,
  timeout: 180000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      // 静默续期，不阻塞当前请求
      if (shouldRefreshToken()) refreshToken()
    }
    // 默认显示 loading，可通过 config.noLoading 关闭
    if (!config.noLoading) showGlobalLoading()
    return config
  },
  (error) => {
    hideGlobalLoading()
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    if (!response.config.noLoading) hideGlobalLoading()
    return response.data
  },
  (error) => {
    hideGlobalLoading()
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
      return Promise.reject(error)
    }
    // 网络错误自动重试 1 次
    const { config, message } = error
    if (!window.navigator.onLine) {
      showToast('网络已断开，请检查网络连接')
      return Promise.reject(error)
    }
    if (config && !config.__retried && message.includes('Network Error')) {
      config.__retried = true
      return service(config)
    }
    return Promise.reject(error)
  }
)

//处理流式接口
export async function fetchStream(url, data, onChunk, onFinal, onError){
  const controller = new AbortController()
  
  const response = await fetch(`${baseURL}/api/travel/${url}`,{
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
    signal: controller.signal
  })

  if (!response.ok) {
    onError('请求失败: ' + response.status)
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while(true){
    const {done,value} = await reader.read()
    if (done){
      break;
    }
    const chunk = decoder.decode(value, {stream: true})
    buffer += chunk
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''
    
    for (const line of lines){
      const trimmed = line.trim()
      if (trimmed.startsWith('data:')){
        const jsonStr = trimmed.substring(5).trim()
        try{
          if (jsonStr){
            const jsonData = JSON.parse(jsonStr)
            if (jsonData.type === 'chunk'){
              onChunk(jsonData.content)
            }else if (jsonData.type === 'final'){
              onFinal(jsonData.content)
              return
            }else if (jsonData.done){
              onFinal(null)
              return
            }else if (jsonData.error){
              onError(jsonData.error)
              return
            }
          }
        }catch (err){
          // 忽略解析错误
        }
      }
    }
  }
  onFinal(null)
}


export default service
