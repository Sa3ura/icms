<template>
  <div class="log-container page-container">
    <!-- 过滤查询 -->
    <el-card
      shadow="never"
      class="filter-card"
    >
      <el-form
        :inline="true"
        :model="filters"
        class="demo-form-inline"
      >
        <el-form-item label="操作用户">
          <el-input
            v-model="filters.username"
            placeholder="搜索用户名"
            clearable
            style="width: 180px"
            @input="debouncedSearch"
            @clear="loadData"
          />
        </el-form-item>
        <el-form-item label="操作名称">
          <el-input
            v-model="filters.operation"
            placeholder="如：用户登录"
            clearable
            style="width: 180px"
            @input="debouncedSearch"
            @clear="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            icon="Search"
            @click="loadData"
          >
            查询
          </el-button>
          <el-button
            icon="Refresh"
            @click="resetFilters"
          >
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志列表 -->
    <el-card
      shadow="never"
      class="table-card"
    >
      <div class="table-actions">
        <el-button
          icon="Download"
          @click="exportToExcel(tableData, logExportCols, '操作日志')"
        >
          导出
        </el-button>
      </div>

      <el-skeleton
        v-if="loading"
        :rows="8"
        animated
      />

      <el-table
        v-else
        :data="tableData"
        style="width: 100%"
      >
        <el-table-column
          prop="id"
          label="日志ID"
          width="90"
        />
        <el-table-column
          prop="username"
          label="操作用户"
          width="130"
        />
        <el-table-column
          prop="operation"
          label="操作内容"
          min-width="150"
        />
        <el-table-column
          prop="request_method"
          label="请求方式"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="getMethodTag(row.request_method)"
              size="small"
            >
              {{ row.request_method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="request_url"
          label="请求路径"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="request_ip"
          label="操作IP"
          width="130"
        />
        <el-table-column
          prop="request_params"
          label="请求参数"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="result_status"
          label="结果状态"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.result_status === 'success' ? 'success' : 'danger'"
              size="small"
            >
              {{ row.result_status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="error_message"
          label="失败详情"
          min-width="150"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span
              v-if="row.error_message"
              class="text-danger"
            >{{ row.error_message }}</span>
            <span
              v-else
              class="text-secondary"
            >-</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="created_at"
          label="操作时间"
          width="165"
        />
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        class="pagination-bar"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { apiGetLogs } from '../../api';

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const { exportToExcel } = useExport();

const logExportCols = [
  { prop: 'id', label: 'ID' },
  { prop: 'username', label: '操作用户' },
  { prop: 'operation', label: '操作内容' },
  { prop: 'request_method', label: '请求方式' },
  { prop: 'request_url', label: '请求路径' },
  { prop: 'request_ip', label: '操作IP' },
  { prop: 'result_status', label: '结果状态' },
  { prop: 'created_at', label: '操作时间' }
];

const filters = ref({
  username: '',
  operation: ''
});

useFilterSync(filters);

import { getMethodTag } from '../../utils/mappings';
import { useFilterSync } from '../../composables/useFilterSync';
import { useExport } from '../../composables/useExport';

let searchTimer = null;
const debouncedSearch = () => {
  clearTimeout(searchTimer);
  searchTimer = setTimeout(() => {
    currentPage.value = 1;
    loadData();
  }, 300);
};

const handlePageChange = (page) => {
  currentPage.value = page;
  loadData();
};

const handleSizeChange = (size) => {
  pageSize.value = size;
  currentPage.value = 1;
  loadData();
};

const resetFilters = () => {
  filters.value = { username: '', operation: '' };
  loadData();
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await apiGetLogs({ page: currentPage.value, pageSize: pageSize.value });
    if (res.code === 200) {
      let list = res.data.records;
      if (filters.value.username) {
        list = list.filter(l => l.username.toLowerCase().includes(filters.value.username.toLowerCase()));
      }
      if (filters.value.operation) {
        list = list.filter(l => l.operation.toLowerCase().includes(filters.value.operation.toLowerCase()));
      }
      tableData.value = list;
      total.value = res.data.total;
    }
  } catch (error) {
    ElMessage.error('无法读取操作审计日志');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.text-danger {
  color: var(--danger-color);
  font-weight: 500;
}

.text-secondary {
  color: var(--text-secondary);
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
