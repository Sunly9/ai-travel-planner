<template>
  <div class="empty-state">
    <div class="empty-icon">{{ icon }}</div>
    <div class="empty-title">{{ title }}</div>
    <div v-if="description" class="empty-desc">{{ description }}</div>
    <div v-if="$slots.default" class="empty-action">
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  icon: { type: String, default: '🧭' },
  title: { type: String, required: true },
  description: { type: String, default: '' },
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 24px;
  text-align: center;
}

.empty-icon {
  font-size: 44px;
  line-height: 1;
  margin-bottom: 14px;
  opacity: 0.9;
}

.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #323233;
}

.empty-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #969799;
  line-height: 1.6;
  max-width: 260px;
}

.empty-action {
  margin-top: 18px;
}
</style>
