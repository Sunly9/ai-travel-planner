<template>
  <div class="login-page">
    <div class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="app-icon">
          <van-icon name="travel" size="32" color="#fff" />
        </div>
        <h1 class="app-name">智能旅游助手</h1>
        <p class="app-desc">探索世界，从心出发</p>
      </div>
    </div>

    <div class="form-section">
      <div class="tab-bar">
        <div
          class="tab-item"
          :class="{ active: activeTab === 'login' }"
          @click="switchTab('login')"
        >
          登录
        </div>
        <div
          class="tab-item"
          :class="{ active: activeTab === 'register' }"
          @click="switchTab('register')"
        >
          注册
        </div>
        <div class="tab-indicator" :class="{ right: activeTab === 'register' }"></div>
      </div>

      <transition name="fade-slide" mode="out-in">
        <!-- 登录表单 -->
        <div v-if="activeTab === 'login'" key="login" class="form-container">
          <van-form @submit="onLogin">
            <van-field
              v-model="loginForm.username"
              name="username"
              label="用户名"
              placeholder="请输入用户名"
              :rules="[{ required: true, message: '请输入用户名' }]"
              clearable
            >
              <template #left-icon>
                <div class="field-icon"><van-icon name="contact" /></div>
              </template>
            </van-field>
            <van-field
              v-model="loginForm.password"
              name="password"
              label="密码"
              placeholder="请输入密码"
              :type="showPwd ? 'text' : 'password'"
              :rules="pwdRules"
              clearable
            >
              <template #left-icon>
                <div class="field-icon"><van-icon name="lock-o" /></div>
              </template>
              <template #right-icon>
                <van-icon
                  :name="showPwd ? 'eye-o' : 'closed-eye'"
                  class="pwd-toggle"
                  @click="showPwd = !showPwd"
                />
              </template>
            </van-field>
            <div class="form-extras">
              <label class="remember-me">
                <van-checkbox v-model="rememberMe" shape="square" size="16" />
                <span>记住我</span>
              </label>
              <span class="forgot-pwd" @click="goForgotPwd">忘记密码？</span>
            </div>
            <div class="submit-area">
              <van-button
                round
                block
                type="primary"
                size="large"
                native-type="submit"
                class="submit-btn premium-btn"
              >
                登 录
              </van-button>
            </div>
          </van-form>
        </div>

        <!-- 注册表单 -->
        <div v-else key="register" class="form-container">
          <van-form @submit="onRegister">
            <van-field
              v-model="registerForm.username"
              name="username"
              label="用户名"
              placeholder="请设置用户名"
              :rules="[{ required: true, message: '请输入用户名' }]"
              clearable
            >
              <template #left-icon>
                <div class="field-icon"><van-icon name="contact" /></div>
              </template>
            </van-field>
            <van-field
              v-model="registerForm.nickname"
              name="nickname"
              label="昵称"
              placeholder="请输入昵称"
              :rules="[{ required: true, message: '请输入昵称' }]"
              clearable
            >
              <template #left-icon>
                <div class="field-icon"><van-icon name="smile-o" /></div>
              </template>
            </van-field>
            <van-field
              v-model="registerForm.password"
              name="password"
              label="密码"
              placeholder="请设置密码（至少6位）"
              :type="showRegPwd ? 'text' : 'password'"
              :rules="pwdRules"
              clearable
            >
              <template #left-icon>
                <div class="field-icon"><van-icon name="lock-o" /></div>
              </template>
              <template #right-icon>
                <van-icon
                  :name="showRegPwd ? 'eye-o' : 'closed-eye'"
                  class="pwd-toggle"
                  @click="showRegPwd = !showRegPwd"
                />
              </template>
            </van-field>
            <van-field
              v-model="registerForm.confirmPwd"
              name="confirmPwd"
              label="确认密码"
              placeholder="请再次输入密码"
              :type="showRegPwd ? 'text' : 'password'"
              :rules="confirmPwdRules"
              clearable
            >
              <template #left-icon>
                <div class="field-icon"><van-icon name="lock-o" /></div>
              </template>
            </van-field>
            <div class="agreement">
              <van-checkbox v-model="agree" shape="square" size="16">
                <span class="agree-text">我已阅读并同意</span>
              </van-checkbox>
              <span class="link" @click="goAgreement">《用户协议》</span>
              <span class="link" @click="goPrivacy">《隐私政策》</span>
            </div>
            <div class="submit-area">
              <van-button
                round
                block
                type="primary"
                size="large"
                native-type="submit"
                class="submit-btn premium-btn"
                :disabled="!agree"
              >
                注 册
              </van-button>
            </div>
          </van-form>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const showPwd = ref(false)
const showRegPwd = ref(false)
const rememberMe = ref(true)
const agree = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPwd: '',
})

const pwdRules = [
  { required: true, message: '请输入密码' },
  { pattern: /^.{6,}$/, message: '密码至少6位' },
]

const confirmPwdRules = [
  { required: true, message: '请再次输入密码' },
  {
    validator: (val) => val === registerForm.password,
    message: '两次密码输入不一致',
  },
]

const switchTab = (tab) => {
  activeTab.value = tab
}

const onLogin = async () => {
  try {
    const res = await userStore.login(loginForm.username, loginForm.password, rememberMe.value)
    console.log('登录返回:', res)
    showToast({ message: '登录成功', icon: 'success', duration: 1500 })
    setTimeout(() => router.replace('/'), 1600)
  } catch (err) {
    console.error('登录失败详情:', err)
    const msg = err.response?.data?.message || err.message || '登录失败'
    showToast(msg)
  }
}

const onRegister = async () => {
  try {
    const res = await userStore.register(registerForm.username, registerForm.password, registerForm.nickname, rememberMe.value)
    console.log('注册返回:', res)
    showToast({ message: '注册成功', icon: 'success', duration: 1500 })
    setTimeout(() => router.replace('/'), 1600)
  } catch (err) {
    console.error('注册失败详情:', err)
    const msg = err.response?.data?.message || err.message || '注册失败'
    showToast(msg)
  }
}

const goForgotPwd = () => {
  showToast('功能开发中')
}

const goAgreement = () => {
  showToast('查看用户协议')
}

const goPrivacy = () => {
  showToast('查看隐私政策')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
}

/* ====== Hero 头部区域 ====== */
.hero-section {
  position: relative;
  height: 240px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a6bff 0%, #2b8cff 40%, #6eb6ff 100%);
  z-index: 0;
}

.hero-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 60% at 50% -20%, rgba(255,255,255,0.25) 0%, transparent 70%),
    radial-gradient(ellipse 100% 40% at 0% 100%, rgba(255,255,255,0.12) 0%, transparent 60%),
    radial-gradient(ellipse 100% 40% at 100% 100%, rgba(255,255,255,0.12) 0%, transparent 60%);
  z-index: 1;
}

.hero-content {
  position: relative;
  z-index: 2;
  text-align: center;
  transform: translateY(-10px);
}

.app-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  border: 1.5px solid rgba(255, 255, 255, 0.35);
  box-shadow: 0 8px 32px rgba(26, 107, 255, 0.3);
}

.app-name {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: 1px;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.app-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
  font-weight: 400;
  letter-spacing: 2px;
}

/* ====== 表单区域 ====== */
.form-section {
  flex: 1;
  background: #f7f8fa;
  border-radius: 24px 24px 0 0;
  margin-top: -24px;
  position: relative;
  z-index: 3;
  padding: 0 20px 32px;
}

/* 顶部 Tab */
.tab-bar {
  display: flex;
  position: relative;
  padding: 24px 0 20px;
  gap: 0;
  justify-content: center;
}

.tab-item {
  width: 120px;
  text-align: center;
  font-size: 17px;
  font-weight: 500;
  color: #969799;
  padding: 8px 0;
  cursor: pointer;
  transition: color 0.3s;
  position: relative;
  z-index: 1;
  user-select: none;
}

.tab-item.active {
  color: #2b8cff;
  font-weight: 600;
}

.tab-indicator {
  position: absolute;
  bottom: 16px;
  width: 48px;
  height: 3px;
  background: #2b8cff;
  border-radius: 4px;
  left: calc(50% - 120px + 36px);
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-indicator.right {
  left: calc(50% + 36px);
}

/* 表单容器 */
.form-container {
  padding-top: 4px;
}

/* Vant Field 重写 */
.form-container :deep(.van-cell) {
  background: #fff;
  border-radius: 12px;
  margin-bottom: 12px;
  padding: 12px 16px;
  align-items: center;
  border: 1.5px solid transparent;
  transition: border-color 0.25s, box-shadow 0.25s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.form-container :deep(.van-cell:focus-within) {
  border-color: #2b8cff;
  box-shadow: 0 2px 12px rgba(43, 140, 255, 0.12);
}

.form-container :deep(.van-field__label) {
  width: auto;
  min-width: 44px;
  margin-right: 8px;
  font-size: 14px;
  color: #323233;
  font-weight: 500;
}

.form-container :deep(.van-field__body) {
  font-size: 15px;
}

.form-container :deep(.van-field__control) {
  font-size: 15px;
  color: #323233;
}

.form-container :deep(.van-field__control::placeholder) {
  color: #c8c9cc;
}

.field-icon {
  width: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2b8cff;
  font-size: 18px;
  margin-right: 4px;
}

.pwd-toggle {
  color: #969799;
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
}

/* 表单扩展选项 */
.form-extras {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 4px 16px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #969799;
  cursor: pointer;
}

.remember-me :deep(.van-checkbox__label) {
  font-size: 13px;
  color: #969799;
}

.forgot-pwd {
  font-size: 13px;
  color: #2b8cff;
  cursor: pointer;
  transition: opacity 0.2s;
}

.forgot-pwd:active {
  opacity: 0.7;
}

/* 提交按钮 */
.submit-area {
  padding-top: 4px;
}

.premium-btn {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #1a6bff, #2b8cff);
  border: none;
  box-shadow: 0 4px 16px rgba(43, 140, 255, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;
}

.premium-btn:active {
  transform: scale(0.97);
  box-shadow: 0 2px 8px rgba(43, 140, 255, 0.25);
}

/* 协议 */
.agreement {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  padding: 4px 4px 16px;
  font-size: 13px;
  color: #969799;
}

.agree-text {
  margin-left: 6px;
  font-size: 13px;
  color: #969799;
}

.link {
  color: #2b8cff;
  cursor: pointer;
  font-size: 13px;
}

.link:active {
  opacity: 0.7;
}

/* ====== 过渡动画 ====== */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(24px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-24px);
}
</style>