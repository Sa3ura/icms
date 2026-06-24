import { onMounted } from 'vue';

const store = new Map();

export function useHmrData(reloadFn) {
  onMounted(reloadFn);
  if (import.meta.env.DEV && store.get(reloadFn)) {
    setTimeout(reloadFn, 0);
  }
  store.set(reloadFn, true);
}
