import { ref } from 'vue';

export function useTableSort(reloadFn) {
  const sortProp = ref('');
  const sortOrder = ref('');

  const handleSortChange = ({ prop, order }) => {
    sortProp.value = prop;
    sortOrder.value = order;
    if (!order) {
      reloadFn();
    }
  };

  return { sortProp, sortOrder, handleSortChange };
}
