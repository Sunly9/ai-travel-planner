<template>
  <div class="profile-page">
    <div class="page-header">
      <h1 class="page-title">我的</h1>
    </div>

    <div class="page-content">
      <!-- 用户信息卡片 -->
      <div class="user-card">
        <div class="user-info">
          <div class="avatar-wrap" @click="triggerAvatarUpload">
            <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" />
            <div v-else class="avatar">{{ userStore.userInfo?.nickname?.charAt(0) || '游' }}</div>
            <input ref="avatarInput" type="file" accept="image/*" style="display:none" @change="onAvatarChange" />
          </div>
          <div class="user-meta">
            <div class="user-name">{{ userStore.userInfo?.nickname || '未登录' }}</div>
            <div class="user-phone">{{ userStore.userInfo?.phone || '未绑定手机号' }}</div>
            <div class="edit-link" @click="goEditProfile">编辑资料</div>
          </div>
        </div>
        <div class="quick-stats">
          <div class="stat-item" @click="goTrips">
            <div class="stat-value">{{ tripCount }}</div>
            <div class="stat-label">我的行程</div>
          </div>
          <div class="stat-item" @click="goFavorites">
            <div class="stat-value">{{ favoriteCount }}</div>
            <div class="stat-label">收藏</div>
          </div>
        </div>
      </div>

      <!-- 功能菜单 -->
      <div class="menu-card">
        <van-cell title="我的行程" is-link @click="goTrips">
          <template #icon>
            <div class="menu-icon primary"><van-icon name="location-o" /></div>
          </template>
        </van-cell>
        <van-cell title="清除聊天记录" is-link @click="onClearChat">
          <template #icon>
            <div class="menu-icon danger"><van-icon name="delete-o" /></div>
          </template>
        </van-cell>
        <van-cell title="修改密码" is-link @click="goChangePassword">
          <template #icon>
            <div class="menu-icon primary"><van-icon name="lock" /></div>
          </template>
        </van-cell>
        <van-cell title="数据统计" is-link :border="false" @click="goStats">
          <template #icon>
            <div class="menu-icon primary"><van-icon name="chart-trending-o" /></div>
          </template>
        </van-cell>
      </div>

      <van-button plain block class="logout-button" @click="onLogout">
        退出登录
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()

const tripCount = ref(0)
const favoriteCount = ref(0)
const avatarInput = ref(null)

const avatarUrl = computed(() => userStore.userInfo?.avatar)

const triggerAvatarUpload = () => avatarInput.value?.click()

// 把图片压到 512px 以内的 JPEG 再传，避免 base64 撑爆请求体和数据库列
const compressImage = (file, maxSide = 512, quality = 0.82) =>
  new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      try {
        const scale = Math.min(1, maxSide / Math.max(img.width, img.height))
        const width = Math.max(1, Math.round(img.width * scale))
        const height = Math.max(1, Math.round(img.height * scale))
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        // 透明 PNG 转 JPEG 会变黑，先铺白底
        ctx.fillStyle = '#ffffff'
        ctx.fillRect(0, 0, width, height)
        ctx.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', quality))
      } catch (err) {
        reject(err)
      } finally {
        URL.revokeObjectURL(url)
      }
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片读取失败'))
    }
    img.src = url
  })

const onAvatarChange = async (e) => {
  const file = e.target.files?.[0]
  e.target.value = '' // 允许连续选同一张图
  if (!file) return
  if (!file.type.startsWith('image/')) {
    showToast('请选择图片文件')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    showToast('原图请不要超过 10MB')
    return
  }
  try {
    const base64 = await compressImage(file)
    const res = await request.put('/api/user/profile', { avatar: base64 })
    if (res.success) {
      await userStore.fetchProfile()
      showToast('头像已更新')
    } else {
      showToast(res.message || '上传失败')
    }
  } catch (err) {
    console.error('头像上传失败:', err)
    showToast('上传失败，请换一张图')
  }
}

onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      await userStore.fetchProfile()
      const [tripRes, favRes] = await Promise.all([
        request.get('/api/plans'),
        request.get('/api/favorites')
      ])
      if (tripRes.success) tripCount.value = tripRes.data?.length || 0
      if (favRes.success) favoriteCount.value = favRes.data?.length || 0
    } catch (err) {}
  }
})

const goEditProfile = () => router.push('/profile/edit')
const goTrips = () => router.push('/trips')
const goFavorites = () => router.push('/favorites')
const goChangePassword = () => router.push('/profile/password')
const goStats = () => router.push('/profile/stats')

const onClearChat = () => {
  showConfirmDialog({
    title: '清除聊天记录',
    message: '确定要清空所有 AI 聊天记录吗？此操作不可恢复。',
    confirmButtonText: '清除',
    confirmButtonColor: '#ee0a24'
  })
    .then(async () => {
      try {
        await request.delete('/api/chat-messages')
        showToast('已清除')
      } catch (err) {
        showToast('清除失败')
      }
    })
    .catch(() => {})
}

const onLogout = () => {
  showConfirmDialog({ title: '确认退出', message: '确定要退出登录吗？' })
    .then(() => {
      userStore.logout()
      router.replace('/login')
    })
    .catch(() => {})
}
</script>

<style scoped>
.profile-page {
  background-color: #f7f8fa;
  padding-bottom: 70px;
  min-height: 100vh;
}
.page-header {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #323233;
}
.page-content {
  padding: 0 16px 16px;
}
.user-card {
  background-color: #fff;
  border-radius: 18px;
  padding: 20px 16px 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}
.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}
.avatar-wrap {
  flex-shrink: 0;
  cursor: pointer;
  position: relative;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background-color: #2b8cff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
}
.avatar-img {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}
.user-meta {
  min-width: 0;
  flex: 1;
}
.user-name {
  font-size: 18px;
  font-weight: 600;
  color: #323233;
}
.user-phone {
  font-size: 14px;
  color: #969799;
  margin-top: 4px;
}
.edit-link {
  display: inline-block;
  margin-top: 6px;
  font-size: 13px;
  color: #2b8cff;
  cursor: pointer;
}
.quick-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebedf0;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  border-radius: 12px;
  cursor: pointer;
}
.stat-item:active {
  background-color: #f2f3f5;
}
.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: #323233;
}
.stat-label {
  font-size: 12px;
  color: #969799;
  margin-top: 4px;
}
.menu-card {
  background-color: #fff;
  border-radius: 18px;
  overflow: hidden;
  margin-bottom: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}
.menu-card :deep(.van-cell) {
  padding: 14px 16px;
}
.menu-card :deep(.van-cell__title) {
  font-size: 15px;
  color: #323233;
}
.menu-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  font-size: 18px;
}
.menu-icon.primary { color: #2b8cff; }
.menu-icon.danger { color: #ee0a24; }
.logout-button {
  --van-button-default-color: #ee0a24;
  --van-button-default-border-color: transparent;
  --van-button-default-background: transparent;
  font-size: 15px;
  font-weight: 500;
}
.logout-button :deep(.van-button__text) {
  color: #ee0a24;
}
</style>
