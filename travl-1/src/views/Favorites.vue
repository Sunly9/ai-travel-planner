<template>
  <div class="favorites-page">
    <van-nav-bar title="我的收藏" left-arrow @click-left="goBack" />

    <div class="page-content">
      <van-loading v-if="loading" size="48px" class="loading-container">加载中...</van-loading>

      <van-empty v-else-if="favorites.length === 0" description="暂无收藏，去收藏一些景点吧～">
        <van-button type="primary" @click="goHome">去看看</van-button>
      </van-empty>

      <div v-else class="fav-list">
        <div v-for="fav in favorites" :key="fav.id" class="fav-card">
          <div class="fav-main">
            <div class="fav-name">⭐ {{ fav.spotName }}</div>
            <div class="fav-time">{{ formatTime(fav.createTime) }}</div>
          </div>
          <van-button size="small" type="danger" plain @click="onDelete(fav)">取消收藏</van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const favorites = ref([])

const goBack = () => router.back()
const goHome = () => router.replace('/')

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const loadFavorites = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/favorites')
    if (res.success && res.data) {
      favorites.value = res.data
    }
  } catch (err) {
    showToast(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onDelete = async (fav) => {
  try {
    await showConfirmDialog({ title: '确认取消', message: `确定要取消收藏"${fav.spotName}"吗？` })
  } catch { return }
  try {
    const res = await request.delete('/api/favorites', { data: { spotName: fav.spotName } })
    if (res.success) {
      showToast('已取消收藏')
      loadFavorites()
    } else {
      showToast(res.message || '操作失败')
    }
  } catch (err) {
    showToast(err.message || '操作失败')
  }
}

onMounted(loadFavorites)
</script>

<style scoped>
.favorites-page {
  min-height: 100vh;
  background-color: #f7f8fa;
}

.page-content {
  padding: 16px;
}

.loading-container {
  padding-top: 60px;
  display: flex;
  justify-content: center;
}

.fav-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.fav-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.fav-main {
  flex: 1;
}

.fav-name {
  font-size: 16px;
  font-weight: 600;
  color: #323233;
  margin-bottom: 4px;
}

.fav-time {
  font-size: 12px;
  color: #969799;
}
</style>
