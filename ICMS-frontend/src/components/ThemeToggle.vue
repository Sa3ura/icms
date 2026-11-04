<template>
  <button
    type="button"
    class="theme-toggle"
    :aria-label="userStore.theme === 'dark' ? '切换到浅色模式' : '切换到深色模式'"
    :aria-pressed="userStore.theme === 'dark'"
    @click="userStore.toggleTheme()"
  >
    <el-icon
      :size="16"
      aria-hidden="true"
    >
      <Sunny v-if="userStore.theme === 'dark'" />
      <Moon v-else />
    </el-icon>
    <span>{{ userStore.theme === 'dark' ? '浅色' : '深色' }}</span>
  </button>
</template>

<script setup>
import { Moon, Sunny } from '@element-plus/icons-vue';
import { useUserStore } from '../store/user';

const userStore = useUserStore();
</script>

<style scoped>
.theme-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid var(--border-medium);
  border-radius: 8px;
  background-color: var(--bg-card);
  color: var(--text-regular);
  font: inherit;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s, color 0.2s;
}

.theme-toggle:hover {
  background-color: var(--primary-light);
  color: var(--text-main);
}

.theme-toggle:focus-visible {
  outline: 2px solid var(--accent-color);
  outline-offset: 3px;
}
</style>
