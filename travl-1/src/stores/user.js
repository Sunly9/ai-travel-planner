import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

export const useUserStore = defineStore('user', () => {
  // 从 localStorage 恢复 token
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  // 是否已登录
  const isLoggedIn = computed(() => !!token.value)

  // 登录
  async function login(username, password) {
    const res = await request.post('/api/auth/login', { username, password })
    if (!res.success || !res.data) {
      throw new Error(res.message || '登录失败')
    }
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return res
  }

  // 注册
  async function register(username, password, nickname) {
    const res = await request.post('/api/auth/register', { username, password, nickname })
    if (!res.success || !res.data) {
      throw new Error(res.message || '注册失败')
    }
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return res
  }

  // 获取个人信息（从后端刷新）
  async function fetchProfile() {
    const res = await request.get('/api/user/profile')
    if (!res.success || !res.data) {
      throw new Error(res.message || '获取用户信息失败')
    }
    userInfo.value = res.data
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    return res
  }

  // 退出登录
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return { token, userInfo, isLoggedIn, login, register, fetchProfile, logout }
})
