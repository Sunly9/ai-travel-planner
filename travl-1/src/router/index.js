import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../utils/authStorage'

// 全部路由改成懒加载：首屏只下载首页那一个 chunk，
// 地图（leaflet）、截图（html2canvas）这类重依赖留到真正用到时再加载。
const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/chat', name: 'Chat', component: () => import('../views/Chat.vue') },
  { path: '/profile', name: 'Profile', component: () => import('../views/Profile.vue') },
  { path: '/profile/edit', name: 'ProfileEdit', component: () => import('../views/EditProfile.vue') },
  { path: '/profile/password', name: 'ChangePassword', component: () => import('../views/ChangePassword.vue') },
  { path: '/profile/stats', name: 'Stats', component: () => import('../views/Stats.vue') },
  { path: '/trips', name: 'TripsList', component: () => import('../views/TripsList.vue') },
  { path: '/favorites', name: 'Favorites', component: () => import('../views/Favorites.vue') },
  { path: '/detail', name: 'Detail', component: () => import('../views/Detail.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  // 兜底：不存在的路径回首页，避免白屏
  { path: '/:pathMatch(.*)*', name: 'NotFound', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = getToken()
  if (!token && to.path !== '/login') {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/')
  } else {
    next()
  }
})

export default router
