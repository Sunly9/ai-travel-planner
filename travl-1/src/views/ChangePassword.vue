<template>
  <div class="change-password">
    <van-nav-bar title="修改密码" left-text="返回" left-arrow @click-left="$router.back()" />

    <div class="form-wrap">
      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field
            v-model="form.oldPassword"
            type="password"
            label="旧密码"
            placeholder="请输入旧密码"
            :rules="[{ required: true, message: '请输入旧密码' }]"
          />
          <van-field
            v-model="form.newPassword"
            type="password"
            label="新密码"
            placeholder="至少6位"
            :rules="[{ required: true, message: '请输入新密码' }, { min: 6, message: '至少6位' }]"
          />
          <van-field
            v-model="form.confirmPassword"
            type="password"
            label="确认密码"
            placeholder="再输入一次新密码"
            :rules="[{ required: true, message: '请再次输入' }]"
          />
        </van-cell-group>
        <div class="submit-wrap">
          <van-button round block type="primary" native-type="submit" :loading="loading">
            确认修改
          </van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import request from '../utils/request'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const onSubmit = async () => {
  if (form.newPassword !== form.confirmPassword) {
    showToast('两次密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await request.put('/api/user/password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    if (res.success) {
      showToast('修改成功，请重新登录')
      userStore.logout()
      setTimeout(() => router.replace('/login'), 1000)
    } else {
      showToast(res.message || '修改失败')
    }
  } catch (err) {
    showToast(err?.message || '修改失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.change-password {
  background-color: #f7f8fa;
  min-height: 100vh;
}
.form-wrap {
  padding-top: 16px;
}
.submit-wrap {
  padding: 32px 16px;
}
</style>
