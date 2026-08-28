<template>
  <div class="trips-page">
    <van-nav-bar title="我的行程" left-arrow @click-left="goBack" />

    <div class="page-content">
      <van-loading v-if="loading" size="48px" class="loading-container">加载中...</van-loading>

      <van-empty v-else-if="plans.length === 0" description="暂无行程，去首页规划一个吧～">
        <van-button type="primary" @click="goHome">去规划</van-button>
      </van-empty>

      <div v-else class="plan-list">
        <div v-for="plan in plans" :key="plan.id" class="plan-card" @click="viewDetail(plan)">
          <div class="plan-header">
            <span class="plan-city">🏖️ {{ plan.city }}</span>
            <span class="plan-days">{{ plan.days }}天</span>
          </div>
          <div class="plan-meta">
            <span>预算：{{ plan.budget }}元</span>
            <span>{{ formatTime(plan.createTime) }}</span>
          </div>
          <div class="plan-actions" @click.stop>
            <van-button size="small" type="danger" plain @click="onDelete(plan)">删除</van-button>
          </div>
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
const plans = ref([])

const goBack = () => router.back()
const goHome = () => router.replace('/')

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const loadPlans = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/plans')
    if (res.success && res.data) {
      plans.value = res.data
    }
  } catch (err) {
    showToast(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (plan) => {
  if (plan.planData) {
    const planStr = typeof plan.planData === 'string' ? plan.planData : JSON.stringify(plan.planData)
    router.push({
      path: '/detail',
      query: {
        city: plan.city,
        budget: plan.budget,
        days: plan.days,
        savedPlan: encodeURIComponent(planStr),
      },
    })
  }
}

const onDelete = async (plan) => {
  try {
    await showConfirmDialog({ title: '确认删除', message: `确定要删除"${plan.city}"的行程吗？` })
  } catch {
    return
  }
  try {
    const res = await request.delete(`/api/plans/${plan.id}`)
    if (res.success) {
      showToast('删除成功')
      loadPlans()
    } else {
      showToast(res.message || '删除失败')
    }
  } catch (err) {
    showToast(err.message || '删除失败')
  }
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped>
.trips-page {
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

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.plan-city {
  font-size: 17px;
  font-weight: 600;
  color: #323233;
}

.plan-days {
  font-size: 13px;
  color: #2b8cff;
  background: #e6f0ff;
  padding: 2px 10px;
  border-radius: 10px;
}

.plan-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #969799;
  margin-bottom: 12px;
}

.plan-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
