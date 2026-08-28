<template>
  <div class="edit-page">
    <van-nav-bar title="编辑资料" left-arrow @click-left="goBack" />

    <div class="form-section">
      <van-form @submit="onSave">
        <van-cell-group inset>
          <van-field
            v-model="form.nickname"
            name="nickname"
            label="昵称"
            placeholder="请输入昵称"
            :rules="[{ required: true, message: '请输入昵称' }]"
            maxlength="30"
            show-word-limit
          />
          <van-field
            v-model="form.phone"
            name="phone"
            label="手机号"
            placeholder="请输入手机号"
            type="tel"
            :rules="[{ pattern: /^1\d{10}$/, message: '手机号格式不正确' }]"
            maxlength="11"
          />
        </van-cell-group>

        <div class="submit-bar">
          <van-button round block type="primary" native-type="submit" :loading="saving">
            保存
          </van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()
const saving = ref(false)

const form = reactive({
  nickname: '',
  phone: '',
})

onMounted(() => {
  if (userStore.userInfo) {
    form.nickname = userStore.userInfo.nickname || ''
    form.phone = userStore.userInfo.phone || ''
  }
})

const goBack = () => {
  router.back()
}

const onSave = async () => {
  saving.value = true
  try {
    const res = await request.put('/api/user/profile', {
      nickname: form.nickname,
      phone: form.phone,
    })
    if (!res.success) {
      showToast(res.message || '保存失败')
      return
    }
    // 刷新本地用户信息
    userStore.userInfo = res.data
    localStorage.setItem('userInfo', JSON.stringify(res.data))
    showToast({ message: '保存成功', icon: 'success' })
    setTimeout(() => router.back(), 800)
  } catch (err) {
    console.error('保存失败:', err)
    showToast(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.edit-page {
  min-height: 100vh;
  background-color: #f7f8fa;
}

.form-section {
  padding: 16px;
}

.submit-bar {
  padding: 32px 16px;
}
</style>
