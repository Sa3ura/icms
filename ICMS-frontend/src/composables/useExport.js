import * as XLSX from 'xlsx';
import { ElMessage } from 'element-plus';

export function useExport() {
  const exportToExcel = (data, columns, filename = 'export') => {
    if (!data || data.length === 0) {
      ElMessage.warning('没有数据可导出');
      return;
    }

    const sheetData = data.map(row => {
      const obj = {};
      columns.forEach(col => {
        const val = typeof col.formatter === 'function'
          ? col.formatter(row)
          : row[col.prop];
        obj[col.label] = val !== undefined && val !== null ? val : '';
      });
      return obj;
    });

    const worksheet = XLSX.utils.json_to_sheet(sheetData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1');
    XLSX.writeFile(workbook, `${filename}.xlsx`);
    ElMessage.success('导出成功');
  };

  return { exportToExcel };
}
