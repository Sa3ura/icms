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
          />
        </el-form-item>
        <el-form-item label="操作名称">
          <el-input
            v-model="filters.operation"
            placeholder="如：用户登录"
            clearable
            style="width: 180px"
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
      <el-table
        v-loading="loading"
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { apiGetLogs } from '../../api';

const loading = ref(false);
const tableData = ref([]);

const filters = ref({
  username: '',
  operation: ''
});

import { getMethodTag } from '../../utils/mappings';

const resetFilters = () => {
  filters.value = { username: '', operation: '' };
  loadData();
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await apiGetLogs();
    if (res.code === 200) {
      let list = res.data;
      if (filters.value.username) {
        list = list.filter(l => l.username.toLowerCase().includes(filters.value.username.toLowerCase()));
      }
      if (filters.value.operation) {
        list = list.filter(l => l.operation.toLowerCase().includes(filters.value.operation.toLowerCase()));
      }
      tableData.value = list;
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
</style>
