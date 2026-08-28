<template>
    <div class="page-container">
      <van-nav-bar title="智能旅游助手" />
      <div class="page-content">
        <van-notice-bar
          left-icon="info-o"
          text="无论我们能活多久，我们能够享受的只有无法分割的此刻，此外别无其他。"
        />
        <div class="card" style="margin-top: 16px;">
          <div class="section-title">
            规划你的旅程
          </div>
          <van-field 
          @click="showCityPick = true"
          v-model="forData.city" 
          label="目的地" 
          is-link
          readonly
          placeholder="请选择目的地" 
          style="background-color: #f7f7f7; border-radius: 8px; margin-bottom: 8px;"
          />
          <van-field 
          v-model="forData.budget" 
          label="预算（元）" 
          placeholder="请输入预算" 
          style="background-color: #f7f7f7; border-radius: 8px; margin-bottom: 8px;"
          />
          <van-field 
          v-model="forData.days" 
          label="天数" 
          placeholder="请输入天数" 
          style="background-color: #f7f7f7; border-radius: 8px; margin-bottom: 8px;"
          />
          <van-button round type="primary" size="large" @click="handleSubmit">确认规划</van-button>
        </div>
          <div class="card">
            <div class="section-title">
              快捷入口
            </div>
            <div class="quick-entry">
              <div class="entry-item" @click="goChat('/chat')">
                <div class="entry-icon">
                  <van-icon name="chat-o" size="30" />
                </div>
                <span class="entry-label">AI 对话</span>
              </div>
              <div class="entry-item" @click="goProfile('/profile')">
                <div class="entry-icon">
                  <van-icon name="user-o" size="30" />
                </div>
                <span class="entry-label">我的</span>
              </div>
            </div>
          </div>
          <div class="card">
            <div class="section-title">
              热门目的地
            </div>
            <van-grid :gutter="12" :column-num="4">
              <van-grid-item @click="selectedCity(city)" v-for="city in hotCities" :key="city">
                <div class="city-tag">
                  {{city}}
                </div>
              </van-grid-item>
            </van-grid>
          </div>
          <div class="card" v-if="isLoggedIn">
            <div class="section-title">
              为你推荐
            </div>
            <van-grid v-if="recommendCities.length" :gutter="12" :column-num="4">
              <van-grid-item @click="selectedCity(city)" v-for="city in recommendCities" :key="city">
                <div class="city-tag">
                  {{city}}
                </div>
              </van-grid-item>
            </van-grid>
            <div v-else class="empty-tip">已探索全部热门城市</div>
          </div>
          <van-popup
          round="bottom"
          v-model:show="showCityPick" 
          position="bottom">
            <van-picker
            title="请选择目的地"
            :columns="columns"
            @confirm="onConfirm"
            @cancel="onCancel"
          />
          </van-popup>
        </div>
      </div>

</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const showCityPick = ref(false)
const isLoggedIn = !!localStorage.getItem('token')
const recommendCities = ref([])

const loadRecommendCities = async () => {
  if (!isLoggedIn) return
  try {
    const res = await request.get('/api/plans')
    const visited = new Set()
    if (res.success && Array.isArray(res.data)) {
      res.data.forEach(plan => {
        if (plan.city) visited.add(plan.city)
      })
    }
    if (visited.size === 0) {
      recommendCities.value = [...hotCities]
    } else {
      recommendCities.value = hotCities.filter(city => !visited.has(city))
    }
  } catch (err) {
    recommendCities.value = [...hotCities]
  }
}

onMounted(() => {
  loadRecommendCities()
})

const forData = reactive(
  {
    "city": "",
    "budget": null,
    "days": null
  }
)

const onConfirm = ({selectedOptions}) => {
  console.log(selectedOptions[0])
  forData.city = selectedOptions[0].value
  showCityPick.value = false
}
const onCancel = () => {
  showCityPick.value = false
}

const allCities = [ '北京','上海','成都','重庆','西安','杭州','苏州','南京','昆明','大理','丽江',
  '桂林','厦门','青岛','长沙','武汉','三亚','西宁','拉萨','广州']
const columns = allCities.map(city =>({text: city, value: city}))

const handleSubmit = () => {
  console.log(forData)
  //校验
  if (!forData.city){
    showToast('请选择目的地')
    return
  }
  if (!forData.budget || forData.budget <= 100){
    showToast('请输入预算且预算不能低于100元')
    return
  }
  if (!forData.days || forData.days <= 1 || forData.days > 30){
    showToast('请输入天数且天数不能低于1天且不能超过30天')
    return
  }
  router.push(
    {
      path:'/detail',
      query:{
        city:forData.city,
        budget:forData.budget,
        days:forData.days
      }
    }
  )

}

const hotCities = ['北京', '上海', '广州', '深圳', '成都', '杭州', '西安', '重庆']

const goChat = (path) => {
  router.push(path)
}

const goProfile = (path) => {
  router.push(path)
}

const selectedCity = (city) => {
  forData.city = city
}

</script>
<style scoped>
.page-container {
  background-color: #f7f8fa;
  padding-bottom: 70px;
  min-height: 100vh;
}
.van-nav-bar {
  height: 50px;
  --van-nav-bar-title-font-size: 20px;
  --van-nav-bar-title-font-weight:700;
  --van-nav-bar-title-font-family: '黑体';
}
.page-content {
  padding: 16px;
}
.card {
  padding: 16px;
  background-color: #fff;
  border-radius: 18px;
  margin-bottom: 12px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #323233;
  margin-bottom: 12px;
  text-align: left;
}

.quick-entry {
  display: flex;
  gap: 24px;
}
.entry-item {
  border-radius: 8px;
  margin-bottom: 8px;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 0;
  cursor: pointer;
  transition: transform 0.2s;
  border: 0.5px solid #dedee5;

}
.entry-item:active {
  transform: scale(0.96);
}
.entry-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}
.entry-label {
  font-size: 14px;
  color: #323233;
  font-weight: 500;
}
.city-tag{
  padding: 8px 12px;
  border-radius: 16px;
  background: #f7f8fa;
  font-size: 14px;
  color: #666;
  transition: all 0.3s;
}
.city-tag:hover{
  background-color: #007AFF;
  color: #fff;
}
.empty-tip{
  padding: 16px 0;
  text-align: center;
  font-size: 14px;
  color: #969799;
}

</style>