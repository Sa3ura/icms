import { describe, it, expect } from 'vitest';
import { useTableSort } from '../composables/useTableSort';
import { ref } from 'vue';

describe('useTableSort', () => {
  it('sorts numbers ascending', () => {
    const tableData = ref([
      { id: 3, name: 'c' },
      { id: 1, name: 'a' },
      { id: 2, name: 'b' }
    ]);
    const { handleSortChange } = useTableSort(tableData, () => {});
    handleSortChange({ prop: 'id', order: 'ascending' });
    expect(tableData.value.map(r => r.id)).toEqual([1, 2, 3]);
  });
  it('sorts numbers descending', () => {
    const tableData = ref([
      { id: 1, name: 'a' },
      { id: 3, name: 'c' },
      { id: 2, name: 'b' }
    ]);
    const { handleSortChange } = useTableSort(tableData, () => {});
    handleSortChange({ prop: 'id', order: 'descending' });
    expect(tableData.value.map(r => r.id)).toEqual([3, 2, 1]);
  });
  it('sorts strings with localeCompare', () => {
    const tableData = ref([
      { id: 1, name: 'charlie' },
      { id: 2, name: 'alice' },
      { id: 3, name: 'bob' }
    ]);
    const { handleSortChange } = useTableSort(tableData, () => {});
    handleSortChange({ prop: 'name', order: 'ascending' });
    expect(tableData.value.map(r => r.name)).toEqual(['alice', 'bob', 'charlie']);
  });
  it('calls reloadFn when order is cleared', () => {
    let reloadCalled = false;
    const tableData = ref([{ id: 1 }]);
    const { handleSortChange } = useTableSort(tableData, () => { reloadCalled = true; });
    handleSortChange({ prop: 'id', order: null });
    expect(reloadCalled).toBe(true);
  });
  it('does not throw when reloadFn is null on clear', () => {
    const tableData = ref([{ id: 1 }]);
    const { handleSortChange } = useTableSort(tableData, null);
    expect(() => handleSortChange({ prop: 'id', order: null })).not.toThrow();
  });
});