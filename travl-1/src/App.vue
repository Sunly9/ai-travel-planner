<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const active = computed(() => {
  const map = { '/': 0, '/chat': 1, '/profile': 2 }
  return map[route.path] ?? 0
})
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="fade-slide" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
  <van-tabbar v-if="['Home', 'Chat', 'Profile'].includes(route.name)" v-model="active">
  <van-tabbar-item to="/" icon="home-o">首页</van-tabbar-item>
  <van-tabbar-item to="/chat" icon="chat-o">对话</van-tabbar-item>
  <van-tabbar-item to="/profile" icon="friends-o">我的</van-tabbar-item>
</van-tabbar>
</template>

<style>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(12px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-12px);
}
</style>
