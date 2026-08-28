<template>
  <div class="stats-page">
    <van-nav-bar title="数据统计" left-text="返回" left-arrow @click-left="$router.back()" />

    <div class="stats-list" v-if="loaded">
      <div class="stat-card">
        <div class="stat-icon plan-icon">🗺️</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.planCount }}</div>
          <div class="stat-label">规划行程次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon fav-icon">⭐</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.favoriteCount }}</div>
          <div class="stat-label">收藏景点数量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon chat-icon">💬</div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.chatCount }}</div>
          <div class="stat-label">与 AI 对话轮次</div>
        </div>
      </div>
    </div>

    <van-empty v-else description="加载中..." />
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '../utils/request'

const stats = reactive({ planCount: 0, favoriteCount: 0, chatCount: 0 })
const loaded = ref(false)

onMounted(async () => {
  try {
    const res = await request.get('/api/user/stats')
    if (res.success && res.data) {
      stats.planCount = res.data.planCount || 0
      stats.favoriteCount = res.data.favoriteCount || 0
      stats.chatCount = res.data.chatCount || 0
    }
  } catch (err) {
    //
  } finally {
    loaded.value = true
  }
})
</script>

<style scoped>
.stats-page {
  background-color: #f7f8fa;
  min-height: 100vh;
}
.stats-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.stat-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.plan-icon { background: #e8f3ff; }
.fav-icon { background: #fff4e5; }
.chat-icon { background: #e6f7ec; }
.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #323233;
}
.stat-label {
  font-size: 13px;
  color: #969799;
  margin-top: 2px;
}
</style>
