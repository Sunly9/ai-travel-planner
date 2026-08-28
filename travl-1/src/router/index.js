import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Chat from '../views/Chat.vue'
import Profile from '../views/Profile.vue'
import Detail from '../views/Detail.vue'
import Login from '../views/Login.vue'
import EditProfile from '../views/EditProfile.vue'
import TripsList from '../views/TripsList.vue'
import Favorites from '../views/Favorites.vue'
import ChangePassword from '../views/ChangePassword.vue'
import Stats from '../views/Stats.vue'

const routes = [
  { path: '/', name: 'Home', component: Home },
  { path: '/chat', name: 'Chat', component: Chat },
  { path: '/profile', name: 'Profile', component: Profile },
  { path: '/profile/edit', name: 'EditProfile', component: EditProfile },
  { path: '/profile/password', name: 'ChangePassword', component: ChangePassword },
  { path: '/profile/stats', name: 'Stats', component: Stats },
  { path: '/trips', name: 'TripsList', component: TripsList },
  { path: '/favorites', name: 'Favorites', component: Favorites },
  { path: '/detail', name: 'Detail', component: Detail },
  { path: '/login', name: 'Login', component: Login },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/')
  } else {
    next()
  }
})

export default router
