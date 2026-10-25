// 登录态存储
// 勾选「记住我」→ localStorage：关掉浏览器再打开仍然是登录状态
// 不勾选       → sessionStorage：关掉浏览器就自动登出（同一标签页刷新仍有效）
const TOKEN_KEY = 'token'
const USER_KEY = 'userInfo'
const REMEMBER_KEY = 'rememberMe'

function allStores() {
  return [localStorage, sessionStorage]
}

/** 是否勾选了「记住我」（默认记住，兼容老数据） */
export function isRemembered() {
  return localStorage.getItem(REMEMBER_KEY) !== '0'
}

function activeStore() {
  return isRemembered() ? localStorage : sessionStorage
}

export function getToken() {
  for (const s of allStores()) {
    const t = s.getItem(TOKEN_KEY)
    if (t) return t
  }
  return ''
}

export function getUserInfo() {
  for (const s of allStores()) {
    const raw = s.getItem(USER_KEY)
    if (raw) {
      try {
        return JSON.parse(raw)
      } catch {
        return null
      }
    }
  }
  return null
}

/** 登录/注册成功后写入登录态 */
export function setAuth(token, userInfo, remember = true) {
  localStorage.setItem(REMEMBER_KEY, remember ? '1' : '0')
  clearAuth()
  const store = remember ? localStorage : sessionStorage
  store.setItem(TOKEN_KEY, token)
  store.setItem(USER_KEY, JSON.stringify(userInfo ?? null))
}

/** 静默续期时只更新 token，不影响「记住我」的选择 */
export function setToken(token) {
  activeStore().setItem(TOKEN_KEY, token)
}

/** 资料变更后更新缓存的用户信息 */
export function setUserInfo(userInfo) {
  if (!getToken()) return
  activeStore().setItem(USER_KEY, JSON.stringify(userInfo ?? null))
}

/** 退出登录 / token 失效时清理 */
export function clearAuth() {
  for (const s of allStores()) {
    s.removeItem(TOKEN_KEY)
    s.removeItem(USER_KEY)
  }
}
