<template>
  <div class="spot-item" v-if="data">
    <div class="spot-header">
      <div class="spot-name">{{ data.spot || data.name || '待定' }}</div>
      <div class="spot-actions">
        <van-icon
          name="arrow"
          size="20"
          color="#1989fa"
          class="nav-icon"
          @click="$emit('navigate')"
        />
        <van-icon
          :name="favorited ? 'star' : 'star-o'"
          :color="favorited ? '#ff976a' : '#2b8cff'"
          size="22"
          class="fav-icon"
          @click="onToggleFav"
        />
      </div>
    </div>
    <div class="spot-details" v-if="data.duration || data.ticket || data.transportation">
      <div class="detail-row" v-if="data.duration">
        <van-icon name="clock-o" size="14" />
        <span>{{ data.duration }}</span>
      </div>
      <div class="detail-row" v-if="data.ticket">
        <van-icon name="ticket-o" size="14" />
        <span>{{ data.ticket }}</span>
      </div>
      <div class="detail-row" v-if="data.transportation">
        <van-icon name="logistics" size="14" />
        <span>{{ data.transportation }}</span>
      </div>
    </div>
    <div class="spot-desc" v-if="data.description">{{ data.description }}</div>
  </div>
  <div class="spot-item empty" v-else>
    <van-empty description="暂无安排" :image-size="40" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import request from '../utils/request'

const props = defineProps({
  data: {
    type: Object,
    default: () => ({})
  }
})

defineEmits(['navigate'])

const spotName = () => props.data?.spot || props.data?.name || ''

const favorited = ref(false)

const checkFav = async () => {
  const name = spotName()
  if (!name) return
  try {
    const res = await request.get('/api/favorites/check', { params: { spotName: name } })
    favorited.value = res.success && res.data === true
  } catch {}
}

const onToggleFav = async () => {
  const name = spotName()
  if (!name) return
  try {
    if (favorited.value) {
      const res = await request.delete('/api/favorites', { data: { spotName: name } })
      if (res.success) {
        favorited.value = false
        showToast('已取消收藏')
      }
    } else {
      const res = await request.post('/api/favorites', {
        spotName: name,
        spotData: JSON.stringify(props.data),
      })
      if (res.success) {
        favorited.value = true
        showToast({ message: '收藏成功', icon: 'success' })
      } else {
        showToast(res.message || '收藏失败')
      }
    }
  } catch (err) {
    showToast(err.message || '操作失败')
  }
}

onMounted(checkFav)
</script>

<style scoped>
.spot-item {
  padding: 8px 0;
}

.spot-item.empty {
  padding: 16px 0;
}

.spot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.spot-name {
  font-size: 16px;
  font-weight: 600;
  color: #323233;
}

.fav-icon {
  cursor: pointer;
}

.spot-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.nav-icon {
  cursor: pointer;
}

.spot-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.spot-desc {
  font-size: 14px;
  color: #969799;
  line-height: 1.5;
}
</style>