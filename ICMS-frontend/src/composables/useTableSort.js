import { ref } from 'vue';

export function useTableSort(tableData, reloadFn) {
  const sortProp = ref('');
  const sortOrder = ref('');

  const handleSortChange = ({ prop, order }) => {
    sortProp.value = prop;
    sortOrder.value = order;

    if (!order) {
      reloadFn?.();
      return;
    }

    const direction = order === 'ascending' ? 1 : -1;
    tableData.value = [...tableData.value].sort((a, b) => {
      const av = a[prop] ?? '';
      const bv = b[prop] ?? '';

      if (typeof av === 'number' && typeof bv === 'number') {
        return (av - bv) * direction;
      }

      return String(av).localeCompare(String(bv), 'zh-CN', { numeric: true }) * direction;
    });
  };

  return { sortProp, sortOrder, handleSortChange };
}
