<template>
  <div class="approval-container page-container">
    <!-- 查询过滤 -->
    <el-card
      shadow="never"
      class="filter-card"
    >
      <el-form
        :inline="true"
        :model="filters"
        class="demo-form-inline"
      >
        <el-form-item label="申请状态">
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
        <el-form-item label="申请人">
          <el-input
            v-model="filters.keyword"
            placeholder="搜索申请人姓名"
            clearable
            style="width: 180px"
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

    <!-- 审批列表数据 -->
    <el-card
      shadow="never"
      class="table-card"
    >
      <div class="table-actions">
        <el-button
          icon="Download"
          @click="exportToExcel(tableData, apprExportCols, '预约审批')"
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
          label="单号"
          width="80"
        />
        <el-table-column
          prop="applicant_name"
          label="申请人"
          width="110"
          sortable="custom"
        />
        <el-table-column
          prop="classroom_name"
          label="申请教室"
          min-width="150"
        />
        <el-table-column
          prop="reservation_date"
          label="预约日期"
          width="110"
          sortable="custom"
        />
        <el-table-column
          label="节次时段"
          width="170"
        >
          <template #default="{ row }">
            第 {{ row.start_section }}-{{ row.end_section }} 节
            <span class="time-tip">({{ getSectionTime(row.start_section, row.end_section) }})</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="purpose"
          label="申请原因/用途"
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
          prop="contact_phone"
          label="联系电话"
          width="120"
        />
        <el-table-column
          prop="created_at"
          label="提交时间"
          width="160"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="100"
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
          label="操作"
          min-width="140"
          fixed="right"
        >
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-tooltip
                content="同意此预约申请"
                placement="top"
              >
                <el-button
                  size="small"
                  type="success"
                  link
                  icon="Check"
                  @click="openApprovalDialog(row, 'approved')"
                >
                  通过
                </el-button>
              </el-tooltip>
              <el-tooltip
                content="驳回此预约申请"
                placement="top"
              >
                <el-button
                  size="small"
                  type="danger"
                  link
                  icon="Close"
                  @click="openApprovalDialog(row, 'rejected')"
                >
                  驳回
                </el-button>
              </el-tooltip>
            </template>
            <el-tag
              v-else
              type="info"
              size="small"
            >
              已审批
            </el-tag>
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

    <!-- 审批弹出框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="actionType === 'approved' ? '审批通过' : '驳回申请'" 
      width="440px"
      destroy-on-close
    >
      <div class="summary-box">
        <p><strong>申请人：</strong>{{ activeRow?.applicant_name }}</p>
        <p><strong>预约教室：</strong>{{ activeRow?.classroom_name }}</p>
        <p><strong>预约时段：</strong>{{ activeRow?.reservation_date }} (第 {{ activeRow?.start_section }}-{{ activeRow?.end_section }} 节)</p>
        <p><strong>使用用途：</strong>{{ activeRow?.purpose }}</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        label-position="top"
      >
        <el-form-item 
          label="审批意见/原因" 
          prop="remark"
          :rules="actionType === 'rejected' ? [{ required: true, message: '请填写驳回原因', trigger: 'blur' }] : []"
        >
          <el-input 
            v-model="form.remark" 
            type="textarea" 
            rows="3" 
            :placeholder="actionType === 'approved' ? '可填写同意批注（选填）' : '请说明驳回的具体理由（必填）'" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button 
          :type="actionType === 'approved' ? 'success' : 'danger'" 
          :loading="submitLoading" 
          @click="submitApproval"
        >
          确认{{ actionType === 'approved' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useUserStore } from '../../store/user';
import { ElMessage } from 'element-plus';
import { apiGetReservations, apiApproveReservation } from '../../api';

const userStore = useUserStore();

const { exportToExcel } = useExport();

const apprExportCols = [
  { prop: 'id', label: '单号' },
  { prop: 'applicant_name', label: '申请人' },
  { prop: 'classroom_name', label: '教室' },
  { prop: 'reservation_date', label: '日期' },
  { prop: 'start_section', label: '开始节' },
  { prop: 'end_section', label: '结束节' },
  { prop: 'purpose', label: '用途' },
  { prop: 'attendee_count', label: '人数' },
  { prop: 'status', label: '状态' },
  { prop: 'created_at', label: '提交时间' }
];

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);

const tableData = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const activeRow = ref(null);
const actionType = ref('approved'); // approved / rejected

const filters = ref({
  status: 'pending', // 默认筛选待审批
  keyword: ''
});

useFilterSync(filters);

const form = ref({
  remark: ''
});

const formRef = ref(null);

let keywordTimer = null;
const debouncedKeyword = () => {
  clearTimeout(keywordTimer);
  keywordTimer = setTimeout(() => {
    currentPage.value = 1;
    loadData();
  }, 300);
};

import { getSectionTime, getReservationStatusLabel, getReservationStatusTag } from '../../utils/mappings';
import { useFilterSync } from '../../composables/useFilterSync';
import { useExport } from '../../composables/useExport';
import { useHmrData } from '../../composables/useHmrData';
import { useTableSort } from '../../composables/useTableSort';

const resetFilters = () => {
  filters.value = { status: 'pending', keyword: '' };
  loadData();
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

const loadData = async () => {
  loading.value = true;
  try {
    const params = { page: currentPage.value, pageSize: pageSize.value };
    if (filters.value.status) params.status = filters.value.status;
    const res = await apiGetReservations(params);
    if (res.code === 200) {
      let list = res.data.records;
      if (filters.value.keyword) {
        const kw = filters.value.keyword.toLowerCase();
        list = list.filter(r => r.applicant_name.toLowerCase().includes(kw));
      }
      tableData.value = list;
      total.value = res.data.total;
    }
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
};

useHmrData(loadData);

const { handleSortChange } = useTableSort(loadData);

const openApprovalDialog = (row, type) => {
  activeRow.value = row;
  actionType.value = type;
  form.value.remark = type === 'approved' ? '同意申请。' : '';
  dialogVisible.value = true;
};

const submitApproval = () => {
  formRef.value?.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const res = await apiApproveReservation(
          activeRow.value.id,
          actionType.value,
          form.value.remark,
          userStore.userInfo.id,
          userStore.userInfo.username
        );
        if (res.code === 200) {
          ElMessage.success('审批处理完毕！');
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

.time-tip {
  font-size: 11px;
  color: var(--text-secondary);
  margin-left: 4px;
}

.text-secondary {
  color: var(--text-secondary);
  font-size: 13px;
}

.summary-box {
  background-color: var(--bg-app);
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 20px;
  border-left: 4px solid var(--primary-color);
  line-height: 1.6;
}

.summary-box p {
  margin: 4px 0;
  color: var(--text-main);
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
