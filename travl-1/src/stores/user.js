import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'
import { getToken, getUserInfo, setAuth, setUserInfo, clearAuth } from '../utils/authStorage'

export const useUserStore = defineStore('user', () => {
  // 从存储恢复登录态：记住我 → localStorage，否则本次会话 → sessionStorage
  const token = ref(getToken())
  const userInfo = ref(getUserInfo())

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value)

  // 登录
  async function login(username, password, remember = true) {
    const res = await request.post('/api/auth/login', { username, password })
    if (!res.success || !res.data) {
      throw new Error(res.message || '登录失败')
    }
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    setAuth(token.value, userInfo.value, remember)
    return res
  }

  // 注册
  async function register(username, password, nickname, remember = true) {
    const res = await request.post('/api/auth/register', { username, password, nickname })
    if (!res.success || !res.data) {
      throw new Error(res.message || '注册失败')
    }
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    setAuth(token.value, userInfo.value, remember)
    return res
  }

  // 获取个人信息（从后端刷新）
  async function fetchProfile() {
    const res = await request.get('/api/user/profile')
    if (!res.success || !res.data) {
      throw new Error(res.message || '获取用户信息失败')
    }
    userInfo.value = res.data
    setUserInfo(userInfo.value)
    return res
  }

  // 退出登录
  function logout() {
    token.value = ''
    userInfo.value = null
    clearAuth()
  }

  return { token, userInfo, isLoggedIn, login, register, fetchProfile, logout }
})
