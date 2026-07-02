<template>
  <div class="my-reservation-container page-container">
    <!-- 查询过滤卡片 -->
    <el-card
      shadow="never"
      class="filter-card"
    >
      <el-form
        :inline="true"
        :model="filters"
        class="demo-form-inline"
      >
        <el-form-item label="审批状态">
          <el-select
            v-model="filters.status"
            placeholder="选择状态"
            clearable
            style="width: 140px"
          >
            <el-option
              label="待审批"
              value="pending"
            />
            <el-option
              label="已通过"
              value="approved"
            />
            <el-option
              label="已驳回"
              value="rejected"
            />
            <el-option
              label="已取消"
              value="cancelled"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="filters.date_range"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 250px"
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
          发起预约申请
        </el-button>
        <el-button
          icon="Download"
          @click="exportToExcel(tableData, resExportCols, '我的预约')"
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
          label="预约单号"
          width="90"
        />
        <el-table-column
          prop="classroom_name"
          label="预约教室"
          min-width="150"
          sortable="custom"
        />
        <el-table-column
          prop="reservation_date"
          label="预约日期"
          width="120"
          sortable="custom"
        />
        <el-table-column
          label="节次时段"
          width="180"
        >
          <template #default="{ row }">
            第 {{ row.start_section }}-{{ row.end_section }} 节
            <span class="time-tip">({{ getSectionTime(row.start_section, row.end_section) }})</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="purpose"
          label="用途"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="attendee_count"
          label="使用人数"
          width="90"
          align="center"
        />
        <el-table-column
          prop="status"
          label="审批状态"
          width="110"
          align="center"
          sortable="custom"
        >
          <template #default="{ row }">
            <el-tag :type="getReservationStatusTag(row.status)">
              {{ getReservationStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="approval_remark"
          label="审批意见/驳回原因"
          min-width="150"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.status === 'rejected', 'text-secondary': !row.approval_remark }">
              {{ row.approval_remark || '暂无意见' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          min-width="90"
          fixed="right"
        >
          <template #default="{ row }">
            <el-tooltip
              content="取消此预约申请"
              placement="top"
            >
              <el-popconfirm
                title="确定要取消此项预约申请吗？"
                @confirm="handleCancel(row)"
              >
                <template #reference>
                  <el-button 
                    size="small" 
                    type="danger" 
                    link 
                    icon="Close"
                    :disabled="row.status !== 'pending' && row.status !== 'approved'"
                  >
                    取消
                  </el-button>
                </template>
              </el-popconfirm>
            </el-tooltip>
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

    <!-- 发起预约弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新建教室预约申请"
      width="550px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-form-item
          label="选择教室"
          prop="classroom_id"
        >
          <el-select
            v-model="form.classroom_id"
            placeholder="请选择教室"
            class="w-100"
            @change="triggerConflictCheck"
          >
            <el-option
              v-for="r in classrooms"
              :key="r.id"
              :label="r.name"
              :value="r.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          label="使用日期"
          prop="reservation_date"
        >
          <el-date-picker
            v-model="form.reservation_date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            class="w-100"
            :disabled-date="disabledPastDates"
            @change="triggerConflictCheck"
          />
        </el-form-item>

        <el-form-item
          label="节次时段"
          prop="sections"
        >
          <div class="section-picker-row">
            <el-select
              v-model="form.start_section"
              placeholder="开始"
              style="width: 130px"
              @change="triggerConflictCheck"
            >
              <el-option
                v-for="i in 12"
                :key="i"
                :label="'第 ' + i + ' 节'"
                :value="i"
              />
            </el-select>
            <span class="range-separator">至</span>
            <el-select
              v-model="form.end_section"
              placeholder="结束"
              style="width: 130px"
              @change="triggerConflictCheck"
            >
              <el-option
                v-for="i in 12"
                :key="i"
                :label="'第 ' + i + ' 节'"
                :value="i"
              />
            </el-select>
          </div>
        </el-form-item>

        <!-- 冲突检测栏 -->
        <div
          v-if="conflictCheckResult"
          class="conflict-check-alert"
        >
          <el-alert
            :title="conflictCheckResult.title"
            :type="conflictCheckResult.type"
            :description="conflictCheckResult.description"
            show-icon
            :closable="false"
          />
        </div>

        <el-form-item
          label="使用人数"
          prop="attendee_count"
        >
          <el-input-number
            v-model="form.attendee_count"
            :min="1"
            :max="selectedRoomMaxCapacity"
            class="w-100"
          />
          <span
            v-if="selectedRoomMaxCapacity"
            class="input-tip"
          >该教室最大能容纳 {{ selectedRoomMaxCapacity }} 人</span>
        </el-form-item>

        <el-form-item
          label="联系电话"
          prop="contact_phone"
        >
          <el-input
            v-model="form.contact_phone"
            placeholder="请输入联系人电话"
          />
        </el-form-item>

        <el-form-item
          label="预约用途"
          prop="purpose"
        >
          <el-input 
            v-model="form.purpose" 
            type="textarea" 
            rows="4" 
            placeholder="请详细叙述教室的使用用途，方便管理员尽快审批。" 
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
          :disabled="hasConflict"
          @click="submitForm"
        >
          提交申请
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useUserStore } from '../../store/user';
import { ElMessage } from 'element-plus';
import { 
  apiGetReservations, 
  apiCreateReservation, 
  apiCancelReservation,
  apiGetClassrooms,
  apiCheckConflict
} from '../../api';

const userStore = useUserStore();

const { exportToExcel } = useExport();

const resExportCols = [
  { prop: 'id', label: '预约单号' },
  { prop: 'classroom_name', label: '教室' },
  { prop: 'reservation_date', label: '日期' },
  { prop: 'start_section', label: '开始节' },
  { prop: 'end_section', label: '结束节' },
  { prop: 'purpose', label: '用途' },
  { prop: 'attendee_count', label: '人数' },
  { prop: 'status', label: '状态' }
];

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const tableData = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const classrooms = ref([]);

// 冲突状态
const conflictStatus = ref('none'); // none / checking / free / conflict
const conflictReason = ref('');

const filters = ref({
  status: '',
  date_range: []
});

const form = ref({
  classroom_id: null,
  reservation_date: '',
  start_section: null,
  end_section: null,
  applicant_id: null,
  attendee_count: 10,
  contact_phone: '',
  purpose: ''
});

const rules = {
  classroom_id: [{ required: true, message: '请选择教室', trigger: 'change' }],
  reservation_date: [{ required: true, message: '请选择使用日期', trigger: 'change' }],
  sections: [
    {
      validator: (rule, value, callback) => {
        if (!form.value.start_section || !form.value.end_section) {
          callback(new Error('请完整选择开始和结束节次'));
        } else if (form.value.start_section > form.value.end_section) {
          callback(new Error('开始节次不能大于结束节次'));
        } else {
          callback();
        }
      },
      trigger: 'change'
    }
  ],
  attendee_count: [{ required: true, message: '请输入预约使用人数', trigger: 'blur' }],
  contact_phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' }
  ],
  purpose: [
    { required: true, message: '请输入预约用途', trigger: 'blur' },
    { min: 5, message: '具体用途描述不能少于5个字', trigger: 'blur' }
  ]
};

// 选中的教室最大容量
const selectedRoomMaxCapacity = computed(() => {
  if (!form.value.classroom_id) return 300;
  return classrooms.value.find(c => c.id === form.value.classroom_id)?.capacity || 300;
});

// 禁止选择过去的日期
const disabledPastDates = (time) => {
  return time.getTime() < Date.now() - 3600 * 1000 * 24;
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

// 节次时间显示
import { getSectionTime, getReservationStatusLabel, getReservationStatusTag } from '../../utils/mappings';
import { useExport } from '../../composables/useExport';
import { useHmrData } from '../../composables/useHmrData';
import { useTableSort } from '../../composables/useTableSort';

const resetFilters = () => {
  filters.value = { status: '', date_range: [] };
  loadData();
};

const loadData = async () => {
  loading.value = true;
  try {
    // 默认传入 applicant_id 限制查询当前用户自己的预约 (除了管理员和教务老师可看全部)
    const query = { ...filters.value };
    if (!['admin', 'jw'].includes(userStore.role)) {
      query.applicant_id = userStore.userInfo.id;
    }
    const res = await apiGetReservations({ page: currentPage.value, pageSize: pageSize.value, ...query });
    if (res.code === 200) {
      tableData.value = res.data.records;
      total.value = res.data.total;
    }
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
};

useHmrData(loadData);

const { handleSortChange } = useTableSort(tableData, loadData);

const handleCreate = () => {
  form.value = {
    classroom_id: null,
    reservation_date: '',
    start_section: null,
    end_section: null,
    applicant_id: userStore.userInfo.id,
    attendee_count: 10,
    contact_phone: userStore.userInfo.phone || '',
    purpose: ''
  };
  conflictStatus.value = 'none';
  conflictReason.value = '';
  dialogVisible.value = true;
};

// 触发冲突检查
const triggerConflictCheck = async () => {
  const { classroom_id, reservation_date, start_section, end_section } = form.value;
  if (!classroom_id || !reservation_date || !start_section || !end_section) {
    conflictStatus.value = 'none';
    return;
  }

  if (start_section > end_section) {
    conflictStatus.value = 'none';
    return;
  }

  conflictStatus.value = 'checking';
  try {
    const res = await apiCheckConflict(classroom_id, reservation_date, start_section, end_section);
    if (res.code === 200) {
      if (res.data.conflict) {
        conflictStatus.value = 'conflict';
        conflictReason.value = res.data.reason;
      } else {
        conflictStatus.value = 'free';
        conflictReason.value = '';
      }
    }
  } catch (e) {
    conflictStatus.value = 'none';
  }
};

// 冲突警告框属性
const conflictCheckResult = computed(() => {
  if (conflictStatus.value === 'checking') {
    return { title: '冲突检测中...', type: 'info', description: '正在极速检测时段占用情况' };
  }
  if (conflictStatus.value === 'free') {
    return { title: '时段空闲', type: 'success', description: '🟢 恭喜！当前时段没有冲突排课或已审批通过的预约。' };
  }
  if (conflictStatus.value === 'conflict') {
    return { title: '发生冲突占用', type: 'error', description: '🔴 ' + conflictReason.value };
  }
  return null;
});

const hasConflict = computed(() => conflictStatus.value === 'conflict' || conflictStatus.value === 'checking');

const submitForm = () => {
  formRef.value?.validate(async (valid) => {
    if (valid) {
      if (hasConflict.value) {
        ElMessage.error('当前选定时段有冲突占用，请重新选择时间或教室。');
        return;
      }

      submitLoading.value = true;
      try {
        const res = await apiCreateReservation(form.value, userStore.userInfo.username);
        if (res.code === 200) {
          ElMessage.success('预约申请提交成功，已进入待审批队列');
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

const handleCancel = async (row) => {
  try {
    const res = await apiCancelReservation(row.id, userStore.userInfo.username);
    if (res.code === 200) {
      ElMessage.success('预约已成功取消');
      loadData();
    }
  } catch (e) {
    ElMessage.error('取消预约失败');
  }
};

const formRef = ref(null);

onMounted(async () => {
  loadData();
  // 加载仅限于可用状态的教室，供下拉菜单选用
  const res = await apiGetClassrooms({ pageSize: 10000, status: 'available' });
  if (res.code === 200) {
    classrooms.value = res.data.records;
  }
});
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

.time-tip {
  font-size: 11px;
  color: var(--text-secondary);
  margin-left: 4px;
}

.text-danger {
  color: var(--danger-color);
  font-weight: 500;
}

.text-secondary {
  color: var(--text-secondary);
}

.w-100 {
  width: 100%;
}

.section-picker-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.range-separator {
  color: var(--text-secondary);
}

.input-tip {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 4px;
  display: block;
}

.conflict-check-alert {
  margin-left: 90px;
  margin-bottom: 18px;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
