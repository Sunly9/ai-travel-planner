<template>
  <div class="stats-page">
    <van-nav-bar title="数据统计" left-text="返回" left-arrow @click-left="$router.back()" />

    <div class="stats-body">
      <SkeletonList v-if="!loaded" :rows="3" />

      <template v-else>
        <div class="stat-card">
          <div class="stat-icon plan-icon">🗺️</div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.planCount }}</div>
            <div class="stat-label">保存的行程</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon fav-icon">⭐</div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.favoriteCount }}</div>
            <div class="stat-label">收藏的景点</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon chat-icon">💬</div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.chatCount }}</div>
            <div class="stat-label">与 AI 对话轮次</div>
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-title">最近 7 天规划次数</div>
          <div class="chart-bars" v-if="totalTrend > 0">
            <div class="bar-item" v-for="item in stats.planTrend" :key="item.date">
              <div class="bar-count">{{ item.count || '' }}</div>
              <div class="bar" :style="{ height: barHeight(item.count) + 'px' }"></div>
              <div class="bar-label">{{ item.label }}</div>
            </div>
          </div>
          <div class="chart-empty" v-else>最近 7 天还没有规划记录</div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import request from '../utils/request'
import SkeletonList from '../components/SkeletonList.vue'

const stats = reactive({ planCount: 0, favoriteCount: 0, chatCount: 0, planTrend: [] })
const loaded = ref(false)

const maxTrend = computed(() => Math.max(1, ...stats.planTrend.map((i) => i.count || 0)))
const totalTrend = computed(() => stats.planTrend.reduce((sum, i) => sum + (i.count || 0), 0))

const barHeight = (count) => {
  if (!count) return 4
  return Math.max(8, Math.round((count / maxTrend.value) * 72))
}

onMounted(async () => {
  try {
    const res = await request.get('/api/user/stats')
    if (res.success && res.data) {
      stats.planCount = res.data.planCount || 0
      stats.favoriteCount = res.data.favoriteCount || 0
      stats.chatCount = res.data.chatCount || 0
      stats.planTrend = Array.isArray(res.data.planTrend) ? res.data.planTrend : []
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
.stats-body {
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

.chart-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}
.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #323233;
  margin-bottom: 14px;
}
.chart-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 6px;
  height: 104px;
}
.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}
.bar-count {
  font-size: 11px;
  color: #969799;
  height: 14px;
}
.bar {
  width: 60%;
  max-width: 26px;
  border-radius: 6px 6px 2px 2px;
  background: linear-gradient(180deg, #6eb6ff, #2b8cff);
  min-height: 4px;
  transition: height 0.3s ease;
}
.bar-label {
  font-size: 11px;
  color: #c8c9cc;
}
.chart-empty {
  font-size: 13px;
  color: #969799;
  text-align: center;
  padding: 24px 0 12px;
}
</style>
