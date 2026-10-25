<template>
  <div class="skeleton-list">
    <div v-for="i in rows" :key="i" class="skeleton-card">
      <div class="skeleton-line w50"></div>
      <div class="skeleton-line w30"></div>
      <div class="skeleton-line w80"></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 3 },
})
</script>

<style scoped>
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.skeleton-line {
  height: 12px;
  border-radius: 6px;
  margin-bottom: 10px;
  background: linear-gradient(90deg, #f2f3f5 25%, #e8eaed 37%, #f2f3f5 63%);
  background-size: 400% 100%;
  animation: skeleton-shimmer 1.4s ease infinite;
}

.skeleton-line:last-child {
  margin-bottom: 0;
}

.w30 { width: 30%; }
.w50 { width: 50%; }
.w80 { width: 80%; }

@keyframes skeleton-shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}
</style>
