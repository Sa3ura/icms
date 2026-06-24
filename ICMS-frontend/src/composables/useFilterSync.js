import { watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';

export function useFilterSync(filters) {
  const route = useRoute();
  const router = useRouter();

  onMounted(() => {
    const q = route.query;
    if (Object.keys(q).length > 0) {
      Object.keys(filters.value).forEach(k => {
        if (q[k] !== undefined) {
          const val = q[k];
          if (val === 'true') filters.value[k] = true;
          else if (val === 'false') filters.value[k] = false;
          else if (!isNaN(val) && val !== '') filters.value[k] = Number(val);
          else filters.value[k] = val;
        }
      });
    }
  });

  watch(filters, (val) => {
    const query = {};
    Object.keys(val).forEach(k => {
      if (val[k] !== '' && val[k] !== null && val[k] !== undefined) {
        query[k] = val[k];
      }
    });
    router.replace({ query });
  }, { deep: true });
}
