<template>
  <div class="notice-manage-container page-container">
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
          发布通知
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
      >
        <el-table-column
          prop="title"
          label="通知标题"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column
          prop="notice_type"
          label="类型"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              :type="getNoticeTypeTag(row.notice_type)"
              size="small"
            >
              {{ getNoticeTypeLabel(row.notice_type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="target_role"
          label="目标角色"
          width="110"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.target_role"
              type="info"
              size="small"
            >
              {{ getRoleLabel(row.target_role) }}
            </el-tag>
            <el-tag
              v-else
              type="success"
              size="small"
            >
              全员
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="read_count"
          label="已读/总计"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <span :class="{ 'text-success': row.read_count > 0 }">
              {{ row.read_count }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="publisher_name"
          label="发布人"
          width="110"
        />
        <el-table-column
          prop="created_at"
          label="发布时间"
          width="165"
        />
        <el-table-column
          label="操作"
          min-width="140"
          fixed="right"
        >
          <template #default="{ row }">
            <el-dropdown
              trigger="click"
              @command="(cmd) => handleDropdown(cmd, row)"
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
                    编辑通知
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="detail"
                    :icon="InfoFilled"
                  >
                    查看详情
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="delete"
                    :icon="Delete"
                    divided
                  >
                    <span class="text-danger">删除通知</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
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

    <!-- 发布/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'create' ? '发布新通知' : '编辑通知'"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
      >
        <el-form-item
          label="通知类型"
          prop="notice_type"
        >
          <el-radio-group v-model="form.notice_type">
            <el-radio-button value="info">
              通知
            </el-radio-button>
            <el-radio-button value="warning">
              警告
            </el-radio-button>
            <el-radio-button value="system">
              系统
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="目标角色"
          prop="target_role"
        >
          <el-select
            v-model="form.target_role"
            placeholder="选择推送角色（不选为全员）"
            clearable
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
          label="通知标题"
          prop="title"
        >
          <el-input
            v-model="form.title"
            placeholder="请输入通知标题"
          />
        </el-form-item>
        <el-form-item
          label="通知内容"
          prop="content"
        >
          <el-input
            v-model="form.content"
            type="textarea"
            rows="6"
            placeholder="请输入通知正文内容..."
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
          {{ dialogType === 'create' ? '立即发布' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 通知详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="通知详情"
      width="520px"
      destroy-on-close
    >
      <div
        v-if="currentNotice"
        class="detail-body"
      >
        <div class="detail-meta-row">
          <el-tag
            :type="getNoticeTypeTag(currentNotice.notice_type)"
            size="small"
          >
            {{ getNoticeTypeLabel(currentNotice.notice_type) }}
          </el-tag>
          <span>目标：{{ currentNotice.target_role ? getRoleLabel(currentNotice.target_role) : '全体用户' }}</span>
          <span>发布人：{{ currentNotice.publisher_name }}</span>
          <span>{{ currentNotice.created_at }}</span>
        </div>
        <el-divider border-style="dashed" />
        <h3 class="detail-title">
          {{ currentNotice.title }}
        </h3>
        <div class="detail-content">
          {{ currentNotice.content }}
        </div>
        <el-divider border-style="dashed" />
        <div class="detail-stats">
          <span>已读 {{ currentNotice.read_count || 0 }} 人</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useUserStore } from '../../store/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import { apiGetNotices, apiSaveNotice, apiDeleteNotice } from '../../api';
import { getRoleLabel } from '../../utils/mappings';
import { Edit, Delete, InfoFilled } from '@element-plus/icons-vue';

const userStore = useUserStore();

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogType = ref('create');
const detailVisible = ref(false);
const currentNotice = ref(null);
const tableData = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const formRef = ref(null);

const form = ref({
  id: null,
  title: '',
  content: '',
  notice_type: 'info',
  target_role: ''
});

const rules = {
  title: [{ required: true, message: '请输入通知标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入通知内容', trigger: 'blur' }]
};

const getNoticeTypeLabel = (type) => {
  const mapping = { info: '通知', warning: '警告', system: '系统' };
  return mapping[type] || type;
};

const getNoticeTypeTag = (type) => {
  const mapping = { info: 'primary', warning: 'warning', system: 'danger' };
  return mapping[type] || 'info';
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await apiGetNotices({ page: currentPage.value, pageSize: pageSize.value, userId: userStore.userInfo?.id });
    if (res.code === 200) {
      tableData.value = res.data.records.map(n => ({
        ...n,
        read_count: Array.isArray(n.read_by) ? n.read_by.length : 0
      }));
      total.value = res.data.total;
    }
  } catch (error) {
    ElMessage.error('加载通知列表失败');
  } finally {
    loading.value = false;
  }
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

const handleCreate = () => {
  form.value = { id: null, title: '', content: '', notice_type: 'info', target_role: '' };
  dialogType.value = 'create';
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  form.value = { ...row, target_role: row.target_role || '' };
  dialogType.value = 'edit';
  dialogVisible.value = true;
};

const submitForm = () => {
  formRef.value?.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const payload = { ...form.value, target_role: form.value.target_role || null };
        const res = await apiSaveNotice(payload, userStore.userInfo.username);
        if (res.code === 200) {
          ElMessage.success(dialogType.value === 'create' ? '通知发布成功' : '通知修改成功');
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

const handleDropdown = async (command, row) => {
  if (command === 'edit') {
    handleEdit(row);
  } else if (command === 'detail') {
    currentNotice.value = { ...row, read_count: Array.isArray(row.read_by) ? row.read_by.length : 0 };
    detailVisible.value = true;
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定删除此通知吗？所有用户将无法再看到该通知。', '删除确认', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      });
      const res = await apiDeleteNotice(row.id, userStore.userInfo.username);
      if (res.code === 200) {
        ElMessage.success('通知已删除');
        loadData();
      }
    } catch (_) {
      // cancelled
    }
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.table-card {
  margin-bottom: 20px;
}
.table-actions {
  margin-bottom: 16px;
}
.w-100 {
  width: 100%;
}
.text-success {
  color: var(--success-color);
  font-weight: 600;
}
.text-danger {
  color: var(--danger-color);
  font-weight: 500;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-body {
  padding: 10px 0;
}
.detail-meta-row {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
  flex-wrap: wrap;
}
.detail-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
  margin-bottom: 16px;
  line-height: 1.4;
}
.detail-content {
  font-size: 15px;
  color: var(--text-regular);
  line-height: 1.8;
  white-space: pre-wrap;
  padding: 10px 0;
}
.detail-stats {
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
