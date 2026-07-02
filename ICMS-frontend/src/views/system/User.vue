<template>
  <div class="user-management-container page-container">
    <!-- 查询 -->
    <el-card
      shadow="never"
      class="filter-card"
    >
      <el-form
        :inline="true"
        :model="filters"
        class="demo-form-inline"
      >
        <el-form-item label="角色类型">
          <el-select
            v-model="filters.user_type"
            placeholder="选择角色"
            clearable
            style="width: 150px"
          >
            <el-option
              label="系统管理员"
              value="admin"
            />
            <el-option
              label="教务管理员"
              value="jw"
            />
            <el-option
              label="教师"
              value="teacher"
            />
            <el-option
              label="学生"
              value="student"
            />
            <el-option
              label="维修人员"
              value="repairer"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="filters.keyword"
            placeholder="搜索用户名/真实姓名"
            clearable
            style="width: 200px"
            @input="debouncedKeyword"
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

    <!-- 列表数据 -->
    <el-card
      shadow="never"
      class="table-card"
    >
      <div class="table-actions">
        <el-button
          type="primary"
          icon="Plus"
          @click="handleCreate"
        >
          新增系统账号
        </el-button>
        <el-button
          icon="Download"
          @click="exportToExcel(tableData, userExportCols, '系统用户')"
        >
          导出
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        @sort-change="handleSortChange"
      >
        <el-table-column
          prop="id"
          label="ID"
          width="80"
        />
        <el-table-column
          prop="username"
          label="用户名"
          width="130"
          sortable="custom"
        />
        <el-table-column
          prop="real_name"
          label="真实姓名"
          width="130"
          sortable="custom"
        />
        <el-table-column
          prop="user_type"
          label="角色"
          width="130"
          sortable="custom"
        >
          <template #default="{ row }">
            <el-tag :type="getRoleTag(row.user_type)">
              {{ getRoleLabel(row.user_type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="phone"
          label="手机号码"
          width="130"
        />
        <el-table-column
          prop="email"
          label="电子邮箱"
          min-width="180"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-color="#13c2c2"
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="created_at"
          label="创建时间"
          width="165"
        />
        <el-table-column
          label="操作"
          min-width="90"
          fixed="right"
        >
          <template #default="{ row }">
            <el-dropdown
              trigger="click"
              @command="(cmd) => handleUserDropdown(cmd, row)"
            >
              <el-button
                size="small"
                link
              >
                操作 <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    command="edit"
                    :icon="Edit"
                  >
                    编辑账号
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="resetPwd"
                    :icon="Key"
                  >
                    重置密码
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="delete"
                    :icon="Delete"
                    divided
                  >
                    <span class="text-danger">删除账号</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty>
          <el-skeleton
            v-if="loading"
            :rows="5"
            animated
          />
        </template>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogType === 'create' ? '新增账号' : '编辑账号'" 
      width="480px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-form-item
          label="用户名"
          prop="username"
        >
          <el-input
            v-model="form.username"
            placeholder="登录账号"
            :disabled="dialogType === 'edit'"
          />
        </el-form-item>
        <el-form-item
          label="真实姓名"
          prop="real_name"
        >
          <el-input
            v-model="form.real_name"
            placeholder="请输入姓名"
          />
        </el-form-item>
        <el-form-item
          label="角色类型"
          prop="user_type"
        >
          <el-select
            v-model="form.user_type"
            placeholder="请分配系统角色"
            class="w-100"
          >
            <el-option
              label="系统管理员"
              value="admin"
            />
            <el-option
              label="教务管理员"
              value="jw"
            />
            <el-option
              label="教师"
              value="teacher"
            />
            <el-option
              label="学生"
              value="student"
            />
            <el-option
              label="维修人员"
              value="repairer"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="手机号码"
          prop="phone"
        >
          <el-input
            v-model="form.phone"
            placeholder="输入手机号"
          />
        </el-form-item>
        <el-form-item
          label="电子邮箱"
          prop="email"
        >
          <el-input
            v-model="form.email"
            placeholder="输入邮箱"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="submitForm"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog
      v-model="resetPwdVisible"
      title="重置用户密码"
      width="380px"
      destroy-on-close
    >
      <el-form
        ref="resetFormRef"
        :model="resetForm"
        :rules="resetRules"
        label-width="80px"
      >
        <el-form-item
          label="新密码"
          prop="newPassword"
        >
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="resetSubmitLoading"
          @click="submitResetPwd"
        >
          重置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useUserStore } from '../../store/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import { apiGetUsers, apiSaveUser, apiDeleteUser, apiResetPassword } from '../../api';

const userStore = useUserStore();

const { exportToExcel } = useExport();

const userExportCols = [
  { prop: 'id', label: 'ID' },
  { prop: 'username', label: '用户名' },
  { prop: 'real_name', label: '真实姓名' },
  { prop: 'user_type', label: '角色' },
  { prop: 'phone', label: '手机号' },
  { prop: 'email', label: '邮箱' },
  { prop: 'created_at', label: '创建时间' }
];

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogType = ref('create'); // create / edit
const tableData = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

// 重置密码
const resetPwdVisible = ref(false);
const resetSubmitLoading = ref(false);
const activeUser = ref(null);

const filters = ref({
  user_type: '',
  keyword: ''
});

useFilterSync(filters);

const form = ref({
  id: null,
  username: '',
  real_name: '',
  user_type: 'teacher',
  phone: '',
  email: '',
  status: 1
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  real_name: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  user_type: [{ required: true, message: '请分配角色', trigger: 'change' }],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
};

const resetForm = ref({
  newPassword: ''
});

const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '长度不能低于6位', trigger: 'blur' }
  ]
};

const formRef = ref(null);
const resetFormRef = ref(null);

let keywordTimer = null;
const debouncedKeyword = () => {
  clearTimeout(keywordTimer);
  keywordTimer = setTimeout(() => {
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

import { getRoleLabel, getRoleTag } from '../../utils/mappings';
import { Key, Delete, MoreFilled } from '@element-plus/icons-vue';
import { useFilterSync } from '../../composables/useFilterSync';
import { useExport } from '../../composables/useExport';
import { useHmrData } from '../../composables/useHmrData';
import { useTableSort } from '../../composables/useTableSort';

const resetFilters = () => {
  filters.value = { user_type: '', keyword: '' };
  loadData();
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await apiGetUsers({ page: currentPage.value, pageSize: pageSize.value, ...filters.value });
    if (res.code === 200) {
      tableData.value = res.data.records;
      total.value = res.data.total;
    }
  } catch (error) {
    ElMessage.error('读取用户列表错误');
  } finally {
    loading.value = false;
  }
};

const handleCreate = () => {
  form.value = {
    id: null,
    username: '',
    real_name: '',
    user_type: 'teacher',
    phone: '',
    email: '',
    status: 1
  };
  dialogType.value = 'create';
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  form.value = { ...row };
  dialogType.value = 'edit';
  dialogVisible.value = true;
};

const submitForm = () => {
  formRef.value?.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const payload = { ...form.value };
        if (dialogType.value === 'create') {
          payload.password = '123456'; // 默认初始密码
        }
        const res = await apiSaveUser(payload, userStore.userInfo.username);
        if (res.code === 200) {
          ElMessage.success(dialogType.value === 'create' ? '账号创建成功（默认口令: 123456）' : '编辑成功');
          dialogVisible.value = false;
          loadData();
        } else {
          ElMessage.error(res.message);
        }
      } catch (error) {
        ElMessage.error(error.message);
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

const toggleStatus = async (row) => {
  try {
    await apiSaveUser(row, userStore.userInfo.username);
    ElMessage.success(`已${row.status === 1 ? '启用' : '禁用'}该账号`);
  } catch (e) {
    ElMessage.error('更新账号状态失败');
    row.status = row.status === 1 ? 0 : 1; // 还原状态
  }
};

const handleDelete = async (row) => {
  if (row.username === 'admin') {
    ElMessage.error('安全警告：超级管理员账号 admin 不允许删除！');
    return;
  }
  try {
    await apiDeleteUser(row.id, userStore.userInfo.username);
    ElMessage.success('成功删除用户');
    loadData();
  } catch (e) {
    ElMessage.error('操作失败');
  }
};

const handleUserDropdown = async (command, row) => {
  if (command === 'edit') {
    handleEdit(row);
  } else if (command === 'resetPwd') {
    openResetPwd(row);
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定要删除这个用户账号吗？', '删除确认', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      });
      handleDelete(row);
    } catch (_) {
      // user cancelled
    }
  }
};

const openResetPwd = (row) => {
  activeUser.value = row;
  resetForm.value.newPassword = '';
  resetPwdVisible.value = true;
};

const submitResetPwd = () => {
  resetFormRef.value?.validate(async (valid) => {
    if (valid) {
      resetSubmitLoading.value = true;
      try {
        const res = await apiResetPassword(
          activeUser.value.id,
          resetForm.value.newPassword,
          userStore.userInfo.username
        );
        if (res.code === 200) {
          ElMessage.success('用户密码重置成功！');
          resetPwdVisible.value = false;
        }
      } catch (error) {
        ElMessage.error('重置密码错误');
      } finally {
        resetSubmitLoading.value = false;
      }
    }
  });
};

onMounted(() => {
  loadData();
});

useHmrData(loadData);

const { handleSortChange } = useTableSort(tableData, loadData);

</script>

<style scoped>
.filter-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.table-actions {
  margin-bottom: 16px;
}

.w-100 {
  width: 100%;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
