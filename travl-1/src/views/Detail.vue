<script setup>
//首次启动触发
import {onMounted,reactive,ref,nextTick,watch} from 'vue'
//本页面接收参数
import {useRoute,useRouter} from 'vue-router'
import { showToast } from 'vant'
import html2canvas from 'html2canvas'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
//引入接口
import request from '../utils/request'
import { fetchStream } from '../utils/request'
import SpotItem from '../components/SpotItem.vue'
import BudgetTable from '../components/BudgetTable.vue'

const router = useRouter()
const route = useRoute()

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.replace('/')
  }
}

const forData = reactive({
  "city":'',
  "budget":'',
  "days":'',
})

// 推荐结果
const tripData = ref(null)
// 加载状态
const loading = ref(true)
const error = ref(false)
const isStreaming = ref(false)
const activeDays = ref([])

// 天气数据
const weatherData = ref(null)
// 地图实例
let mapInstance = null
// 所有景点标记 { day: [{ marker, spot }] }
let spotMarkers = {}
// 地图当前显示的天数（0=全部）
const mapActiveDay = ref(0)
// 地图当前显示的时段（null=全部, 'morning'/'afternoon'/'evening'）
const mapActivePeriod = ref(null)
// 缓存景点坐标，供点击行程卡片时 flyTo 用
const spotCoords = ref({})  // key: "day-period" → {lat, lng, name}
// 当前高亮的行程区域（给 CSS 加 glow 效果）
const highlightedSection = ref('')  // "day-period"

const fetchWeather = async (city) => {
  if (!city) return
  try {
    const res = await request.get('/api/weather', { params: { city }, noLoading: true })
    console.log('=== 天气接口响应 ===', res)
    if (res.success && res.data) {
      weatherData.value = res.data
    } else {
      console.warn('=== 天气接口返回失败 ===', res)
    }
  } catch (e) {
    console.error('=== 天气接口异常 ===', e)
  }
}

const initMap = async (cityName) => {
  const container = document.getElementById('travel-map')
  if (!container) {
    console.warn('=== 地图容器不存在 ===')
    return
  }
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
  spotMarkers = {}
  spotCoords.value = {}

  // 1. 先用高德地理编码拿城市中心坐标（GCJ-02，和瓦片一致）
  let centerLat, centerLng
  try {
    const geoRes = await request.get('/api/geo/search', {
      params: { address: cityName },
      noLoading: true
    })
    if (geoRes.success && geoRes.data) {
      centerLat = geoRes.data.latitude
      centerLng = geoRes.data.longitude
      console.log('=== 高德城市中心 ===', cityName, centerLat, centerLng)
    }
  } catch (e) {
    console.warn('高德地理编码城市失败，用天气坐标兜底', e)
  }
  // 兜底：用 Open-Meteo 的坐标（WGS-84，会有几百米偏移但总比没有好）
  if (!centerLat && weatherData.value) {
    centerLat = weatherData.value.latitude
    centerLng = weatherData.value.longitude
  }
  if (!centerLat) return

  mapInstance = L.map('travel-map').setView([centerLat, centerLng], 12)

  // 高德地图瓦片（中文，国内秒加载）
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    subdomains: ['1', '2', '3', '4'],
    attribution: '© 高德地图',
    maxZoom: 18,
  }).addTo(mapInstance)

  // 城市中心标记
  L.marker([centerLat, centerLng]).addTo(mapInstance).bindPopup(cityName).openPopup()

  setTimeout(() => mapInstance?.invalidateSize(), 300)

  // 2. 收集所有景点
  const allSpots = []
  if (tripData.value?.dailyItinerary) {
    for (const day of tripData.value.dailyItinerary) {
      for (const key of ['morning', 'afternoon', 'evening']) {
        const spot = day[key]
        if (spot && (spot.spot || spot.name)) {
          allSpots.push({ name: spot.spot || spot.name, day: day.day, period: key })
        }
      }
    }
  }

  // 3. 逐个地理编码并标注（不限数量了）
  const colors = { morning: '#fa8c16', afternoon: '#1890ff', evening: '#52c41a' }
  for (const spot of allSpots) {
    try {
      const res = await request.get('/api/geo/search', {
        params: { address: spot.name, city: cityName },
        noLoading: true
      })
      if (res.success && res.data && res.data.latitude && mapInstance) {
        const sLat = res.data.latitude
        const sLng = res.data.longitude
        // 缓存坐标
        spotCoords.value[`${spot.day}-${spot.period}`] = { lat: sLat, lng: sLng, name: spot.name }

        const icon = L.divIcon({
          html: `<div style="background:${colors[spot.period]};width:26px;height:26px;border-radius:50%;display:flex;align-items:center;justify-content:center;color:#fff;font-size:12px;border:2px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.4);">${spot.day}</div>`,
          className: 'custom-marker',
          iconSize: [26, 26],
          iconAnchor: [13, 13]
        })
        const periodZh = spot.period === 'morning' ? '上午' : spot.period === 'afternoon' ? '下午' : '晚上'
        // 高德导航 URI（GCJ-02 坐标系，手机装了高德 APP 直接唤起，没装打开网页版）
        const amapNavUrl = `https://uri.amap.com/navigation?to=${sLng},${sLat},${encodeURIComponent(spot.name)}&mode=car&policy=1&src=travel-project&coordinate=gaode&callnative=1`
        const amapWalkUrl = `https://uri.amap.com/navigation?to=${sLng},${sLat},${encodeURIComponent(spot.name)}&mode=walk&policy=1&src=travel-project&coordinate=gaode&callnative=1`
        const marker = L.marker([sLat, sLng], { icon })
          .bindPopup(`
            <div style="min-width:180px;font-size:14px;">
              <b style="font-size:15px;">${spot.name}</b><br>
              <span style="color:#969799;font-size:12px;">第${spot.day}天 · ${periodZh}</span><br>
              <div style="display:flex;gap:6px;margin-top:10px;">
                <a href="${amapNavUrl}" target="_blank" style="flex:1;text-align:center;padding:5px 0;background:#1989fa;color:#fff;border-radius:4px;text-decoration:none;font-size:12px;">🚗 驾车导航</a>
                <a href="${amapWalkUrl}" target="_blank" style="flex:1;text-align:center;padding:5px 0;background:#52c41a;color:#fff;border-radius:4px;text-decoration:none;font-size:12px;">🚶 步行导航</a>
              </div>
            </div>
          `)
          .on('click', () => {
            // 点击地图标记 → 行程面板联动
            onMarkerClick(spot.day, spot.period)
          })

        // 按天分组存起来
        if (!spotMarkers[spot.day]) spotMarkers[spot.day] = []
        spotMarkers[spot.day].push({ marker, spot })

        // 默认全部加到地图上
        marker.addTo(mapInstance)
      }
    } catch (e) {
      // 单个景点地理编码失败不影响其他
    }
  }

  // 地图加完后，按当前选中的天数过滤显示
  filterMapByDay(mapActiveDay.value)
}

// 地图按天数 + 时段过滤显示
const filterMapByDay = (day) => {
  if (!mapInstance) return
  mapActiveDay.value = day

  // 先把所有景点标记都从地图上移除
  Object.values(spotMarkers).forEach(markers => {
    markers.forEach(({ marker }) => mapInstance?.removeLayer(marker))
  })

  // 再按选中的天数 + 时段加回来
  const daysToShow = day === 0 ? Object.keys(spotMarkers) : [String(day)]
  const period = mapActivePeriod.value
  const bounds = []
  daysToShow.forEach(d => {
    if (spotMarkers[d]) {
      spotMarkers[d].forEach(({ marker, spot }) => {
        if (period && spot.period !== period) return  // 时段不匹配跳过
        marker.addTo(mapInstance)
        bounds.push(marker.getLatLng())
      })
    }
  })

  // 如果有标记，自动缩放到合适视野
  if (bounds.length > 0) {
    mapInstance.fitBounds(bounds, { padding: [50, 50], maxZoom: 14 })
  }
}

// 地图按时段过滤（图例点击）
const filterMapByPeriod = (period) => {
  mapActivePeriod.value = mapActivePeriod.value === period ? null : period  // 再点一次取消
  filterMapByDay(mapActiveDay.value)
}

// 点击地图上的 marker → 行程面板联动
const onMarkerClick = (day, period) => {
  // 展开对应天数的折叠面板
  if (!activeDays.value.includes(day)) {
    activeDays.value.push(day)
  }
  // 高亮该景点区域
  highlightedSection.value = `${day}-${period}`
  setTimeout(() => { highlightedSection.value = '' }, 2000)  // 2 秒后取消高亮

  // 滚动到对应天数
  nextTick(() => {
    const el = document.querySelector(`.day-${day}`)
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

// 点击行程卡片里的景点 → 地图飞过去
const flyToSpot = (day, period) => {
  if (!mapInstance) return
  const coord = spotCoords.value[`${day}-${period}`]
  if (!coord) return
  // 先切到对应天数
  if (mapActiveDay.value !== 0 && mapActiveDay.value !== day) {
    filterMapByDay(day)
  }
  mapInstance.flyTo([coord.lat, coord.lng], 15, { duration: 0.8 })
  // 找到对应 marker 打开 popup
  const markers = spotMarkers[day] || []
  const found = markers.find(m => m.spot.period === period)
  if (found) found.marker.openPopup()
}

// 点击导航按钮 → 跳转高德地图导航
const navigateTo = (day, period) => {
  const coord = spotCoords.value[`${day}-${period}`]
  if (!coord) {
    showToast('该景点位置尚未解析，请稍等')
    return
  }
  const spot = tripData.value?.dailyItinerary?.find(d => d.day === day)?.[period]
  const name = spot?.spot || spot?.name || '目的地'
  // 高德 URI：coordinate=gaode 表示 GCJ-02 坐标系，callnative=1 唤起 APP
  const url = `https://uri.amap.com/navigation?to=${coord.longitude},${coord.latitude},${encodeURIComponent(name)}&mode=car&policy=1&src=travel-project&coordinate=gaode&callnative=1`
  window.open(url, '_blank')
}

// 点击折叠面板的天数 → 地图也切过去
const onDayExpand = (day) => {
  filterMapByDay(day)
}



onMounted(() =>{
  console.log(route.query)
  forData.city = route.query.city
  forData.budget = route.query.budget
  forData.days = route.query.days
  // 如果有保存的行程数据，直接用，不调 AI
  if (route.query.savedPlan) {
    try {
      tripData.value = JSON.parse(decodeURIComponent(route.query.savedPlan))
      loading.value = false
      error.value = false
      return
    } catch (e) {
      console.error('解析保存的行程数据失败', e)
    }
  }
  fetchRecommend()
  fetchWeather(route.query.city)
})

// 等 tripData 和 weatherData 都就绪后，DOM 渲染完再初始化地图
watch([tripData, weatherData], ([trip, weather]) => {
  if (trip && trip.success === true && !mapInstance) {
    nextTick(() => {
      console.log('=== watch 触发 initMap ===')
      initMap(weather?.city || trip.city || route.query.city)
    })
  }
})

// 跳转到对话页面
  const goToChat = () => {
    router.push({ name: 'Chat' })
  }

  const savingPlan = ref(false)

  const onSavePlan = async () => {
    if (!tripData.value) return
    savingPlan.value = true
    try {
      const res = await request.post('/api/plans', {
        city: tripData.value.city,
        days: tripData.value.days,
        budget: forData.budget,
        planData: JSON.stringify(tripData.value),
      })
      if (res.success) {
        showToast({ message: '保存成功', icon: 'success' })
      } else {
        showToast(res.message || '保存失败')
      }
    } catch (err) {
      showToast(err.message || '保存失败')
    } finally {
      savingPlan.value = false
    }
  }

const fetchRecommend = async () => {
  loading.value = true
  error.value = false
  isStreaming.value = true
  tripData.value = null

  try {
    const params = {
      city: route.query.city,
      budget: parseInt(route.query.budget),
      days: parseInt(route.query.days) || 2,
    }

    await fetchStream('recommend/stream', params,
      () => { /* chunk 接收中，保持 loading */ },
      (finalJson) => {
        isStreaming.value = false
        loading.value = false
        if (finalJson) {
          try {
            tripData.value = JSON.parse(finalJson)
          } catch (e) {
            error.value = true
            showToast('解析推荐数据失败')
          }
        } else {
          error.value = true
          showToast('未收到有效数据')
        }
      },
      (err) => {
        error.value = true
        isStreaming.value = false
        loading.value = false
        showToast(err || '获取推荐失败')
      }
    )
  } catch (err) {
    error.value = true
    isStreaming.value = false
    loading.value = false
    console.error('获取推荐失败：', err)
  }
}

// 分享功能
const shareVisible = ref(false)
const shareImage = ref('')
const sharing = ref(false)

const onShare = async () => {
  if (sharing.value) return
  sharing.value = true
  try {
    const target = document.querySelector('.page-content')
    if (!target) {
      showToast('未找到可分享的内容')
      return
    }
    const canvas = await html2canvas(target, {
      useCORS: true,
      backgroundColor: '#f5f5f5',
      scale: window.devicePixelRatio || 1,
    })
    shareImage.value = canvas.toDataURL('image/png')
    shareVisible.value = true
  } catch (e) {
    console.error('生成分享图片失败：', e)
    showToast('生成图片失败')
  } finally {
    sharing.value = false
  }
}

const onCloseShare = () => {
  shareVisible.value = false
}
</script>


<template>
    <div class="page-container">
      <van-nav-bar title="旅游推荐" left-arrow @click="goBack"/>
      <div class="page-content">  
        <div v-if="loading && !tripData" class="loading-container">
          <van-loading size="48px" type="spinner">
            {{ isStreaming ? 'AI 正在生成行程...' : '加载中...' }}
          </van-loading>
        </div>
        <div v-else-if="error">
          <van-empty description="描述文字">
            <van-button @click="fetchRecommend" type="primary" class="bottom-button">按钮</van-button>
          </van-empty>
        </div>
        <template v-else-if="tripData && tripData.success === true">
          <div class="card overview-card">
              <h2>{{tripData.city}} · {{ tripData.days }}天的行程</h2>
              <span class="trip-budget">预算：{{ forData.budget }}元</span>
          </div>
          <!-- 天气卡片 -->
          <div class="card weather-card" v-if="weatherData && weatherData.forecast">
            <div class="section-title">未来 3 天天气</div>
            <div class="weather-list">
              <div class="weather-item" v-for="item in weatherData.forecast" :key="item.date">
                <div class="weather-date">{{ item.date.substring(5) }}</div>
                <div class="weather-desc">{{ item.weatherDescription }}</div>
                <div class="weather-temp">{{ item.minTemperature }}° ~ {{ item.maxTemperature }}°</div>
                <div class="weather-rain" v-if="item.precipitationProbability != null">降水 {{ item.precipitationProbability }}%</div>
              </div>
            </div>
          </div>
          <!-- 地图卡片 -->
          <div class="card map-card" v-if="weatherData">
            <div class="section-title">景点地图</div>
            <!-- 天数切换 Tab -->
            <div class="map-day-tabs" v-if="tripData.dailyItinerary?.length">
              <div class="day-tab" :class="{ active: mapActiveDay === 0 }" @click="filterMapByDay(0)">全部</div>
              <div class="day-tab" 
                   v-for="day in tripData.dailyItinerary" 
                   :key="day.day"
                   :class="{ active: mapActiveDay === day.day }"
                   @click="filterMapByDay(day.day)">第{{ day.day }}天</div>
            </div>
            <div id="travel-map"></div>
            <div class="map-legend">
              <span class="legend-item" :class="{ active: !mapActivePeriod }" @click="filterMapByPeriod(null)">
                <span class="legend-dot" style="background:#ccc"></span>全部
              </span>
              <span class="legend-item" :class="{ active: mapActivePeriod === 'morning' }" @click="filterMapByPeriod('morning')">
                <span class="legend-dot" style="background:#fa8c16"></span>上午
              </span>
              <span class="legend-item" :class="{ active: mapActivePeriod === 'afternoon' }" @click="filterMapByPeriod('afternoon')">
                <span class="legend-dot" style="background:#1890ff"></span>下午
              </span>
              <span class="legend-item" :class="{ active: mapActivePeriod === 'evening' }" @click="filterMapByPeriod('evening')">
                <span class="legend-dot" style="background:#52c41a"></span>晚上
              </span>
            </div>
          </div>
              <van-collapse v-model="activeDays" @change="onDayExpand">
                <van-collapse-item v-for="day in tripData.dailyItinerary" :key="day.day" :title="'第' + day.day + '天'" :name="day.day">
                  <div :class="['day-schedule', `day-${day.day}`]">
                      <div :class="['schedule-section', 'morning', { highlighted: highlightedSection === `${day.day}-morning` }]" @click="flyToSpot(day.day, 'morning')">
                          <div class="section-label morning">上午</div>
                          <SpotItem :data="day.morning" @navigate.stop="navigateTo(day.day, 'morning')" />
                      </div>
                      <div :class="['schedule-section', 'afternoon', { highlighted: highlightedSection === `${day.day}-afternoon` }]" @click="flyToSpot(day.day, 'afternoon')">
                          <div class="section-label afternoon">下午</div>
                          <SpotItem :data="day.afternoon" @navigate.stop="navigateTo(day.day, 'afternoon')" />
                      </div>
                      <div :class="['schedule-section', 'evening', { highlighted: highlightedSection === `${day.day}-evening` }]" @click="flyToSpot(day.day, 'evening')">
                          <div class="section-label evening">晚上</div>
                          <SpotItem :data="day.evening" @navigate.stop="navigateTo(day.day, 'evening')" />
                      </div>
                  </div>
                </van-collapse-item>
              </van-collapse>
              <div class="card budget-card">
                <div class="section-title">预算明细</div>
                <BudgetTable :data="tripData.budgetBreakdown" :total="tripData.totalBudget" />
              </div>
              <div class="card tips-card">
                <div class="section-title">温馨提示</div>
                <ul class="tips-list">
                  <li v-for="tip in tripData.tips" :key="tip">{{ tip }}</li>
                </ul>
              </div>
              <div class="card warnings-card">
                <div class="section-title">注意事项</div>
                <ul class="warnings-list">
                  <li v-for="warning in tripData.warnings" :key="warning">{{ warning }}</li>
                </ul>
              </div>
          <!-- 推荐结果 --> 
        </template>
      </div>
      <div v-if="tripData && tripData.success !== false" class="detail-footer">
        <van-button round type="default" size="large" @click="onSavePlan" :loading="savingPlan">保存行程</van-button>
        <van-button round type="default" size="large" @click="onShare" :loading="sharing">分享</van-button>
        <van-button round type="primary" size="large" class="primary-button" @click="goToChat()">咨询 AI 助手</van-button>
      </div>
      <van-popup v-model:show="shareVisible" round closeable position="center" class="share-popup">
        <div class="share-popup-content">
          <img v-if="shareImage" :src="shareImage" class="share-image" alt="行程分享图" />
          <div class="share-tip">长按图片可保存到相册</div>
          <div class="share-actions">
            <a :href="shareImage" :download="`行程分享_${tripData?.city || ''}.png`" class="download-link">
              <van-button round type="primary" size="large">下载图片</van-button>
            </a>
            <van-button round type="default" size="large" @click="onCloseShare">关闭</van-button>
          </div>
        </div>
      </van-popup>
    </div>
</template>

<style scoped>
.page-container {
    min-height: 100vh;
    background-color: #f5f5f5;
    padding-bottom: 70px;
}
.card {
    background-color: #fff;
    border-radius: 8px;
    padding:16px;
    margin-bottom: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}
.loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 16px;
    gap: 16px;
}
.overview-card h2 {
    margin:10px 0;
    font-size: 20px;
}
.section-title {
    font-size: 18px;
    font-weight: 600;
    color: #323233;
    margin-bottom: 12px;
}
.page-content {
    padding: 16px;
}
.overview-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.weather-card {
  margin-bottom: 16px;
}

.weather-list {
  display: flex;
  gap: 12px;
  overflow-x: auto;
}

.weather-item {
  flex-shrink: 0;
  min-width: 80px;
  text-align: center;
  padding: 12px 8px;
  background: #f7f8fa;
  border-radius: 10px;
}

.weather-date {
  font-size: 13px;
  font-weight: 600;
  color: #323233;
  margin-bottom: 6px;
}

.weather-desc {
  font-size: 14px;
  color: #1989fa;
  margin-bottom: 4px;
}

.weather-temp {
  font-size: 13px;
  color: #646566;
}

.weather-rain {
  font-size: 12px;
  color: #969799;
  margin-top: 4px;
}

.map-card {
  margin-bottom: 16px;
}

.map-day-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.day-tab {
  padding: 6px 14px;
  background: #f7f8fa;
  border-radius: 16px;
  font-size: 13px;
  color: #646566;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.day-tab.active {
  background: #1989fa;
  color: #fff;
  font-weight: 600;
}

#travel-map {
  height: 280px;
  width: 100%;
  border-radius: 10px;
  overflow: hidden;
  z-index: 1;
  background: #e8e8e8;
}

.map-legend {
  display: flex;
  gap: 16px;
  margin-top: 10px;
  font-size: 13px;
  color: #666;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.legend-item.active {
  background: #f2f3f5;
  font-weight: 600;
  color: #323233;
}

.legend-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.schedule-section {
  margin-bottom: 16px;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  transition: background 0.15s;
}

.schedule-section:active {
  background: #f2f3f5;
}

.schedule-section.highlighted {
  animation: sectionGlow 2s ease-out;
}

@keyframes sectionGlow {
  0% { box-shadow: 0 0 0 3px #1989fa; background: #ecf5ff; }
  100% { box-shadow: none; background: transparent; }
}


.trip-header h2 {
  font-size: 20px;
  color: #323233;
  margin: 0;
}

.trip-budget {
  font-size: 16px;
  color: #ee0a24;
  font-weight: 600;
}

.trip-collapse {
  margin-bottom: 16px;
}

.day-schedule {
  padding: 8px 0;
}

.schedule-section:last-child {
  margin-bottom: 0;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 8px;
}

.section-label.morning {
  background: #fff7e6;
  color: #fa8c16;
}

.section-label.afternoon {
  background: #e6f7ff;
  color: #1890ff;
}

.section-label.evening {
  background: #f6ffed;
  color: #52c41a;
}

.budget-card,
.tips-card,
.warnings-card {
  margin-bottom: 16px;
}

.tips-list,
.warnings-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-list li,
.warnings-list li {
  padding: 8px 0;
  color: #666;
  font-size: 14px;
  border-bottom: 1px solid #f5f5f5;
}

.tips-list li:last-child,
.warnings-list li:last-child {
  border-bottom: none;
}

.detail-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background: #fff;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
  max-width: 750px;
  margin: 0 auto;
  display: flex;
  gap: 12px;
}
.detail-footer .van-button {
  flex: 1;
}

.error-card {
  text-align: center;
  padding: 40px 16px;
}

.share-popup {
  width: 86%;
  max-width: 360px;
}

.share-popup-content {
  padding: 20px 16px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.share-image {
  width: 100%;
  max-height: 60vh;
  object-fit: contain;
  border-radius: 8px;
  -webkit-user-select: none;
  user-select: none;
}

.share-tip {
  font-size: 12px;
  color: #969799;
  margin: 10px 0 14px;
  text-align: center;
}

.share-actions {
  display: flex;
  gap: 12px;
  width: 100%;
}

.share-actions .van-button {
  flex: 1;
}

.download-link {
  flex: 1;
  display: block;
  text-decoration: none;
}

.download-link .van-button {
  width: 100%;
}
</style>
