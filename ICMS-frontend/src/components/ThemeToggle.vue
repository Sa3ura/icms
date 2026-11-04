<template>
  <button
    type="button"
    role="switch"
    class="theme-toggle"
    :class="{ 'is-dark': isDark }"
    aria-label="深色模式"
    :aria-checked="isDark"
    :title="isDark ? '切换到浅色模式' : '切换到深色模式'"
    @click="userStore.toggleTheme()"
  >
    <span
      class="theme-toggle__track"
      aria-hidden="true"
    >
      <Sunny class="theme-toggle__hint theme-toggle__hint--sun" />
      <Moon class="theme-toggle__hint theme-toggle__hint--moon" />
      <span class="theme-toggle__stars" />
      <span class="theme-toggle__thumb">
        <Sunny class="theme-toggle__icon theme-toggle__icon--sun" />
        <Moon class="theme-toggle__icon theme-toggle__icon--moon" />
      </span>
    </span>
  </button>
</template>

<script setup>
import { computed } from 'vue';
import { Moon, Sunny } from '@element-plus/icons-vue';
import { useUserStore } from '../store/user';

const userStore = useUserStore();
const isDark = computed(() => userStore.theme === 'dark');
</script>

<style scoped>
.theme-toggle {
  --switch-ease: cubic-bezier(0.22, 1, 0.36, 1);
  display: inline-block;
  flex-shrink: 0;
  width: 76px;
  height: 44px;
  padding: 4px 0;
  border: 0;
  border-radius: 999px;
  background: transparent;
  vertical-align: middle;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  touch-action: manipulation;
}

.theme-toggle__track {
  position: relative;
  display: block;
  width: 100%;
  height: 36px;
  overflow: hidden;
  border: 1px solid #d9dfed;
  border-radius: inherit;
  background-color: #edf1f9;
  box-shadow: inset 0 1px 3px rgb(30 41 59 / 5%);
  transition: background-color 400ms ease, border-color 400ms ease, box-shadow 250ms ease;
}

.theme-toggle__hint {
  position: absolute;
  top: 10px;
  width: 14px;
  height: 14px;
  transition: color 400ms ease, opacity 400ms ease;
}

.theme-toggle__hint--sun {
  left: 10px;
  color: #e5b560;
  opacity: 0;
}

.theme-toggle__hint--moon {
  right: 10px;
  color: #8d9ab8;
}

.theme-toggle__stars {
  position: absolute;
  top: 8px;
  left: 30px;
  width: 2px;
  height: 2px;
  border-radius: 50%;
  background: #c7d2fe;
  box-shadow: -5px 16px 0 #a5b4fc, 5px 9px 0 #c7d2fe;
  opacity: 0;
  transform: translateY(5px);
  transition: opacity 350ms ease, transform 500ms var(--switch-ease);
}

.theme-toggle__thumb {
  position: absolute;
  top: 3px;
  left: 3px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #fff;
  box-shadow: 0 2px 5px rgb(30 41 59 / 14%), 0 0 0 1px rgb(30 41 59 / 3%);
  transform: translateX(0);
  transition: transform 520ms var(--switch-ease), background-color 400ms ease, box-shadow 400ms ease, scale 180ms ease;
}

.theme-toggle__icon {
  position: absolute;
  inset: 6px;
  width: 16px;
  height: 16px;
  transition: transform 520ms var(--switch-ease), opacity 250ms ease;
}

.theme-toggle__icon--sun {
  color: #c18420;
  opacity: 1;
  transform: rotate(0deg) scale(1);
}

.theme-toggle__icon--moon {
  color: #35406f;
  opacity: 0;
  transform: rotate(-80deg) scale(0.45);
}

.is-dark .theme-toggle__track {
  border-color: #374462;
  background-color: #1b2541;
  box-shadow: inset 0 1px 4px rgb(0 0 0 / 20%);
}

.is-dark .theme-toggle__thumb {
  background-color: #c7d2fe;
  box-shadow: 0 2px 8px rgb(0 0 0 / 25%), 0 0 10px rgb(165 180 252 / 12%);
  transform: translateX(40px);
}

.is-dark .theme-toggle__icon--sun {
  opacity: 0;
  transform: rotate(90deg) scale(0.45);
}

.is-dark .theme-toggle__icon--moon {
  opacity: 1;
  transform: rotate(0deg) scale(1);
}

.is-dark .theme-toggle__hint--sun,
.is-dark .theme-toggle__stars {
  opacity: 1;
}

.is-dark .theme-toggle__stars {
  transform: translateY(0);
}

.is-dark .theme-toggle__hint--moon {
  opacity: 0;
}

@media (hover: hover) {
  .theme-toggle:hover .theme-toggle__track {
    border-color: #a5b4fc;
    box-shadow: 0 0 0 3px rgb(129 140 248 / 8%);
  }
}

.theme-toggle:active .theme-toggle__thumb {
  scale: 0.92;
}

.theme-toggle:focus-visible {
  outline: none;
}

.theme-toggle:focus-visible .theme-toggle__track {
  outline: 2px solid var(--accent-color);
  outline-offset: 3px;
}

@media (prefers-reduced-motion: reduce) {
  .theme-toggle__track,
  .theme-toggle__hint,
  .theme-toggle__stars,
  .theme-toggle__thumb,
  .theme-toggle__icon {
    transition: none;
  }
}
</style>
