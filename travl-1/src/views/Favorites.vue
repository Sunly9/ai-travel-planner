<template>
  <div class="favorites-page">
    <van-nav-bar title="我的收藏" left-arrow @click-left="goBack" />

    <div class="page-content">
      <SkeletonList v-if="loading" :rows="3" />

      <EmptyState
        v-else-if="favorites.length === 0"
        icon="⭐"
        title="还没有收藏的景点"
        description="在行程详情里点景点右侧的星星，就能收藏到这里"
      >
        <van-button type="primary" round @click="goHome">去看看</van-button>
      </EmptyState>

      <div v-else class="fav-list">
        <div v-for="fav in favorites" :key="fav.id" class="fav-card">
          <div class="fav-main">
            <div class="fav-name">⭐ {{ fav.spotName }}</div>
            <div class="fav-tags" v-if="fav.meta">
              <span v-if="fav.meta.duration">{{ fav.meta.duration }}</span>
              <span v-if="fav.meta.ticket">门票 {{ fav.meta.ticket }}</span>
              <span v-if="fav.meta.transportation">{{ fav.meta.transportation }}</span>
            </div>
            <div class="fav-desc" v-if="fav.meta?.description">{{ fav.meta.description }}</div>
            <div class="fav-time">收藏于 {{ formatTime(fav.createTime) }}</div>
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
import SkeletonList from '../components/SkeletonList.vue'
import EmptyState from '../components/EmptyState.vue'

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

// 收藏时把景点对象存进了 spotData，这里解析出来展示（以前存了但没用上）
const parseMeta = (spotData) => {
  if (!spotData) return null
  try {
    const obj = typeof spotData === 'string' ? JSON.parse(spotData) : spotData
    if (!obj || typeof obj !== 'object') return null
    return {
      duration: obj.duration || '',
      ticket: obj.ticket || '',
      transportation: obj.transportation || '',
      description: obj.description || '',
    }
  } catch (e) {
    return null
  }
}

const loadFavorites = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/favorites')
    if (res.success && res.data) {
      favorites.value = res.data.map((f) => ({ ...f, meta: parseMeta(f.spotData) }))
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
  align-items: flex-start;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.fav-main {
  flex: 1;
  min-width: 0;
}

.fav-name {
  font-size: 16px;
  font-weight: 600;
  color: #323233;
  margin-bottom: 6px;
}

.fav-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #646566;
}

.fav-tags span {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 2px 8px;
}

.fav-desc {
  font-size: 13px;
  color: #969799;
  line-height: 1.5;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.fav-time {
  font-size: 12px;
  color: #c8c9cc;
}
</style>
