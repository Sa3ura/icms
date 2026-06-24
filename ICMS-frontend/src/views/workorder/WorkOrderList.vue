<template>
  <div class="workorder-container page-container">
    <!-- 过滤筛选 -->
    <el-card
      shadow="never"
      class="filter-card"
    >
      <el-form
        :inline="true"
        :model="filters"
        class="demo-form-inline"
      >
        <el-form-item label="工单状态">
          <el-select
            v-model="filters.status"
            placeholder="选择状态"
            clearable
            style="width: 130px"
          >
            <el-option
              label="已提交"
              value="submitted"
            />
            <el-option
              label="已派单"
              value="assigned"
            />
            <el-option
              label="处理中"
              value="processing"
            />
            <el-option
              label="已修复"
              value="finished"
            />
            <el-option
              label="已关闭"
              value="closed"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="故障等级">
          <el-select
            v-model="filters.fault_level"
            placeholder="选择等级"
            clearable
            style="width: 120px"
          >
            <el-option
              label="普通"
              value="normal"
            />
            <el-option
              label="紧急"
              value="urgent"
            />
            <el-option
              label="严重"
              value="severe"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择教室">
          <el-select
            v-model="filters.classroom_id"
            placeholder="选择教室"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="c in classrooms"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
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

    <!-- 数据列表 -->
    <el-card
      shadow="never"
      class="table-card"
    >
      <div
        v-if="canReport"
        class="table-actions"
      >
        <el-button
          type="primary"
          icon="Plus"
          @click="handleCreate"
        >
          提交故障报修
        </el-button>
        <el-button
          icon="Download"
          @click="exportToExcel(tableData, woExportCols, '报修工单')"
        >
          导出
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
      >
        <el-table-column
          prop="order_no"
          label="工单编号"
          width="165"
        />
        <el-table-column
          prop="classroom_name"
          label="报修教室"
          min-width="150"
        />
        <el-table-column
          prop="device_name"
          label="故障设备"
          min-width="140"
        />
        <el-table-column
          prop="fault_title"
          label="故障标题"
          min-width="160"
          show-overflow-tooltip
        />
        <el-table-column
          prop="fault_level"
          label="等级"
          width="90"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="getFaultLevelTag(row.fault_level)">
              {{ getFaultLevelLabel(row.fault_level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="工单状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="getWorkOrderStatusTag(row.status)">
              {{ getWorkOrderStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="reporter_name"
          label="报修人"
          width="100"
        />
        <el-table-column
          prop="handler_name"
          label="维修工"
          width="100"
        >
          <template #default="{ row }">
            <span :class="row.handler_name ? '' : 'text-secondary'">
              {{ row.handler_name || '未指派' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="created_at"
          label="报修时间"
          width="160"
        />
        <el-table-column
          prop="finished_at"
          label="修复时间"
          width="160"
        >
          <template #default="{ row }">
            {{ row.finished_at || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          min-width="90"
          fixed="right"
        >
          <template #default="{ row }">
            <el-dropdown
              trigger="click"
              @command="(cmd) => handleOrderDropdown(cmd, row)"
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
                    command="detail"
                    :icon="InfoFilled"
                  >
                    查看详情
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="canAssign && row.status === 'submitted'"
                    command="assign"
                    :icon="UserFilled"
                    divided
                  >
                    指派维修
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="isMyJob(row) && ['assigned','processing'].includes(row.status)"
                    command="handle"
                    :icon="Tools"
                  >
                    处理工单
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="canClose(row) && row.status === 'finished'"
                    command="close"
                    :icon="CircleCloseFilled"
                  >
                    <span class="text-danger">关闭工单</span>
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

    <!-- 1. 新增报修弹窗 -->
    <el-dialog
      v-model="createDialogVisible"
      title="提交故障报修"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        label-width="95px"
      >
        <el-form-item
          label="报修教室"
          prop="classroom_id"
        >
          <el-select
            v-model="createForm.classroom_id"
            placeholder="请选择发生故障的教室"
            class="w-100"
            @change="handleClassroomChange"
          >
            <el-option
              v-for="c in classrooms"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="故障设备"
          prop="device_id"
        >
          <el-select
            v-model="createForm.device_id"
            placeholder="可选择具体故障设备"
            clearable
            class="w-100"
          >
            <el-option
              label="整体环境/其他设备"
              :value="null"
            />
            <el-option
              v-for="d in formDevices"
              :key="d.id"
              :label="d.device_name + ' (' + d.device_code + ')'"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="故障简述"
          prop="fault_title"
        >
          <el-input
            v-model="createForm.fault_title"
            placeholder="一句话简单描述（如：多媒体讲台无显示）"
          />
        </el-form-item>
        <el-form-item
          label="严重级别"
          prop="fault_level"
        >
          <el-radio-group v-model="createForm.fault_level">
            <el-radio-button value="normal">
              普通
            </el-radio-button>
            <el-radio-button value="urgent">
              紧急
            </el-radio-button>
            <el-radio-button value="severe">
              严重
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="细节详述"
          prop="fault_description"
        >
          <el-input 
            v-model="createForm.fault_description" 
            type="textarea" 
            rows="4" 
            placeholder="请尽量描述清晰故障现象，如：显示器开机指示灯闪烁但黑屏，连结线已确认插紧..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="submitCreate"
        >
          提交报修
        </el-button>
      </template>
    </el-dialog>

    <!-- 2. 工单派发弹窗 -->
    <el-dialog
      v-model="assignDialogVisible"
      title="指派维修人员"
      width="400px"
      destroy-on-close
    >
      <el-form
        ref="assignFormRef"
        :model="assignForm"
        :rules="assignRules"
        label-width="90px"
      >
        <el-form-item label="指派工单">
          <el-input
            :value="activeRow?.order_no"
            disabled
          />
        </el-form-item>
        <el-form-item label="故障描述">
          <p class="desc-text">
            {{ activeRow?.fault_title }}
          </p>
        </el-form-item>
        <el-form-item
          label="维修师傅"
          prop="handler_id"
        >
          <el-select
            v-model="assignForm.handler_id"
            placeholder="选择指派维修人员"
            class="w-100"
          >
            <el-option
              v-for="w in repairers"
              :key="w.id"
              :label="w.real_name + ' (' + w.phone + ')'"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="submitAssign"
        >
          确认派单
        </el-button>
      </template>
    </el-dialog>

    <!-- 3. 工单处理弹窗 -->
    <el-dialog
      v-model="handleDialogVisible"
      title="维修工单处理"
      width="460px"
      destroy-on-close
    >
      <el-form
        ref="handleFormRef"
        :model="handleForm"
        :rules="handleRules"
        label-width="90px"
      >
        <el-form-item
          label="更新状态"
          prop="status"
        >
          <el-radio-group v-model="handleForm.status">
            <el-radio-button value="processing">
              维修中
            </el-radio-button>
            <el-radio-button value="finished">
              修复完成
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="处理反馈"
          prop="handle_result"
        >
          <el-input 
            v-model="handleForm.handle_result" 
            type="textarea" 
            rows="4" 
            placeholder="请详细录入本次维修处理的过程、备件消耗、处理结果等。" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="submitHandle"
        >
          提交处理
        </el-button>
      </template>
    </el-dialog>

    <!-- 4. 详情查看弹窗 -->
    <el-dialog
      v-model="detailsVisible"
      title="工单详情与记录追溯"
      width="550px"
    >
      <div class="details-box">
        <el-descriptions
          title="工单基础信息"
          :column="2"
          border
        >
          <el-descriptions-item
            label="工单编号"
            span="2"
          >
            {{ activeRow?.order_no }}
          </el-descriptions-item>
          <el-descriptions-item label="报修教室">
            {{ activeRow?.classroom_name }}
          </el-descriptions-item>
          <el-descriptions-item label="设备名称">
            {{ activeRow?.device_name }}
          </el-descriptions-item>
          <el-descriptions-item label="报修人员">
            {{ activeRow?.reporter_name }}
          </el-descriptions-item>
          <el-descriptions-item label="故障级别">
            <el-tag
              :type="getFaultLevelTag(activeRow?.fault_level)"
              size="small"
            >
              {{ getFaultLevelLabel(activeRow?.fault_level) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item
            label="报修时间"
            span="2"
          >
            {{ activeRow?.created_at }}
          </el-descriptions-item>
          <el-descriptions-item
            label="故障描述"
            span="2"
          >
            {{ activeRow?.fault_description || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-descriptions
          title="处理状态跟踪"
          :column="2"
          border
          class="mt-20"
        >
          <el-descriptions-item label="当前状态">
            <el-tag
              :type="getWorkOrderStatusTag(activeRow?.status)"
              size="small"
            >
              {{ getWorkOrderStatusLabel(activeRow?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="负责维修工">
            {{ activeRow?.handler_name || '未指派' }}
          </el-descriptions-item>
          <el-descriptions-item
            v-if="activeRow?.handle_result"
            label="处理结果"
            span="2"
          >
            <div class="result-box">
              {{ activeRow.handle_result }}
            </div>
          </el-descriptions-item>
          <el-descriptions-item
            v-if="activeRow?.finished_at"
            label="完工修复时间"
            span="2"
          >
            {{ activeRow.finished_at }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useUserStore } from '../../store/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  apiGetWorkOrders, 
  apiCreateWorkOrder, 
  apiAssignWorkOrder, 
  apiHandleWorkOrder, 
  apiCloseWorkOrder,
  apiGetClassrooms,
  apiGetDevices,
  apiGetUsers
} from '../../api';

const userStore = useUserStore();

const { exportToExcel } = useExport();

const woExportCols = [
  { prop: 'order_no', label: '工单编号' },
  { prop: 'classroom_name', label: '报修教室' },
  { prop: 'device_name', label: '故障设备' },
  { prop: 'fault_title', label: '故障标题' },
  { prop: 'fault_level', label: '故障等级' },
  { prop: 'status', label: '工单状态' },
  { prop: 'reporter_name', label: '报修人' },
  { prop: 'handler_name', label: '维修工' },
  { prop: 'created_at', label: '报修时间' }
];

const loading = ref(false);
const submitLoading = ref(false);
const activeRow = ref(null);

// 弹窗状态
const createDialogVisible = ref(false);
const assignDialogVisible = ref(false);
const handleDialogVisible = ref(false);
const detailsVisible = ref(false);

const tableData = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const classrooms = ref([]);
const formDevices = ref([]); // 动态绑定的故障设备下拉
const repairers = ref([]); // 系统里所有的维修工用户

const filters = ref({
  status: '',
  fault_level: '',
  classroom_id: ''
});

useFilterSync(filters);

// 新增工单
const createForm = ref({
  classroom_id: null,
  device_id: null,
  fault_title: '',
  fault_level: 'normal',
  fault_description: ''
});

const createRules = {
  classroom_id: [{ required: true, message: '请选择教室', trigger: 'change' }],
  fault_title: [{ required: true, message: '请输入故障简述', trigger: 'blur' }],
  fault_description: [{ required: true, message: '请输入故障描述', trigger: 'blur' }]
};

// 派单表单
const assignForm = ref({
  handler_id: null
});

const assignRules = {
  handler_id: [{ required: true, message: '请指派维修人员', trigger: 'change' }]
};

// 维修处理表单
const handleForm = ref({
  status: 'processing',
  handle_result: ''
});

const handleRules = {
  handle_result: [{ required: true, message: '请输入维修处理反馈', trigger: 'blur' }]
};

const createFormRef = ref(null);
const assignFormRef = ref(null);
const handleFormRef = ref(null);

// 权限判定
const canReport = computed(() => ['admin', 'jw', 'teacher', 'student'].includes(userStore.role));
const canAssign = computed(() => ['admin', 'jw'].includes(userStore.role));
const isMyJob = (row) => Number(row.handler_id) === Number(userStore.userInfo.id) && userStore.role === 'repairer';
const canClose = (row) => ['admin', 'jw'].includes(userStore.role) || Number(row.reporter_id) === Number(userStore.userInfo.id);

import { getFaultLevelLabel, getFaultLevelTag, getWorkOrderStatusLabel, getWorkOrderStatusTag } from '../../utils/mappings';
import { UserFilled, Tools, CircleCloseFilled, MoreFilled } from '@element-plus/icons-vue';
import { useFilterSync } from '../../composables/useFilterSync';
import { useExport } from '../../composables/useExport';

const resetFilters = () => {
  filters.value = { status: '', fault_level: '', classroom_id: '' };
  loadData();
};

const loadData = async () => {
  loading.value = true;
  try {
    const query = { ...filters.value };
    // 限制：普通学生/教师只能看自己报修的工单；维修工只能看分配给自己的工单
    if (!['admin', 'jw'].includes(userStore.role)) {
      if (userStore.role === 'repairer') {
        query.handler_id = userStore.userInfo.id;
      } else {
        query.reporter_id = userStore.userInfo.id;
      }
    }

    const res = await apiGetWorkOrders({ page: currentPage.value, pageSize: pageSize.value, ...query });
    if (res.code === 200) {
      tableData.value = res.data.records;
      total.value = res.data.total;
    }
  } catch (error) {
    ElMessage.error('加载故障工单列表失败');
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
  createForm.value = {
    classroom_id: classrooms.value[0]?.id || null,
    device_id: null,
    fault_title: '',
    fault_level: 'normal',
    fault_description: ''
  };
  handleClassroomChange(createForm.value.classroom_id);
  createDialogVisible.value = true;
};

// 级联拉取设备
const handleClassroomChange = async (cid) => {
  createForm.value.device_id = null;
  formDevices.value = [];
  if (!cid) return;
  const res = await apiGetDevices({ pageSize: 10000, classroom_id: cid, status: 'normal' });
  if (res.code === 200) {
    formDevices.value = res.data.records;
  }
};

const submitCreate = () => {
  createFormRef.value?.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const payload = {
          reporter_id: userStore.userInfo.id,
          ...createForm.value
        };
        const res = await apiCreateWorkOrder(payload, userStore.userInfo.username);
        if (res.code === 200) {
          ElMessage.success('工单上报提交成功！');
          createDialogVisible.value = false;
          loadData();
        }
      } catch (error) {
        ElMessage.error(error.message);
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

const openAssignDialog = (row) => {
  activeRow.value = row;
  assignForm.value.handler_id = repairers.value[0]?.id || null;
  assignDialogVisible.value = true;
};

const submitAssign = () => {
  assignFormRef.value?.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const res = await apiAssignWorkOrder(
          activeRow.value.id,
          assignForm.value.handler_id,
          userStore.userInfo.username
        );
        if (res.code === 200) {
          ElMessage.success('指派维修师傅成功！');
          assignDialogVisible.value = false;
          loadData();
        }
      } catch (error) {
        ElMessage.error(error.message);
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

const openHandleDialog = (row) => {
  activeRow.value = row;
  handleForm.value = {
    status: row.status === 'assigned' ? 'processing' : row.status,
    handle_result: row.handle_result || ''
  };
  handleDialogVisible.value = true;
};

const submitHandle = () => {
  handleFormRef.value?.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        const res = await apiHandleWorkOrder(
          activeRow.value.id,
          handleForm.value.handle_result,
          handleForm.value.status,
          userStore.userInfo.username
        );
        if (res.code === 200) {
          ElMessage.success('工单状态反馈更新成功！');
          handleDialogVisible.value = false;
          loadData();
        }
      } catch (error) {
        ElMessage.error(error.message);
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

const handleClose = async (row) => {
  try {
    const res = await apiCloseWorkOrder(row.id, userStore.userInfo.username);
    if (res.code === 200) {
      ElMessage.success('工单已成功关闭归档');
      loadData();
    }
  } catch (error) {
    ElMessage.error('无法关闭工单');
  }
};

const handleOrderDropdown = async (command, row) => {
  if (command === 'detail') {
    viewDetails(row);
  } else if (command === 'assign') {
    openAssignDialog(row);
  } else if (command === 'handle') {
    openHandleDialog(row);
  } else if (command === 'close') {
    try {
      await ElMessageBox.confirm('确定关闭此工单吗？关闭后工单将归档，不可再操作。', '关闭确认', {
        confirmButtonText: '确定关闭',
        cancelButtonText: '取消',
        type: 'warning'
      });
      handleClose(row);
    } catch (_) {
      // user cancelled
    }
  }
};

const viewDetails = (row) => {
  activeRow.value = row;
  detailsVisible.value = true;
};

onMounted(async () => {
  loadData();
  const roomRes = await apiGetClassrooms({ pageSize: 10000 });
  if (roomRes.code === 200) {
    classrooms.value = roomRes.data.records;
  }
  
  // 拉取所有维修工人
  const userRes = await apiGetUsers({ pageSize: 10000, user_type: 'repairer' });
  if (userRes.code === 200) {
    repairers.value = userRes.data.records;
  }

  // UX Shortcut: 检查是否有快捷报修跳转过来
  const shortcutDevice = localStorage.getItem('icms_shortcut_report_device');
  if (shortcutDevice) {
    localStorage.removeItem('icms_shortcut_report_device');
    const device = JSON.parse(shortcutDevice);
    
    // 打开新增弹窗，并自动填充
    createForm.value = {
      classroom_id: device.classroom_id,
      device_id: device.id,
      fault_title: `${device.device_name} 发生异常故障`,
      fault_level: 'normal',
      fault_description: '在教室资源巡视中发现该设备运转异常，请维修老师前去排查。'
    };
    
    // 加载对应的设备列表
    await handleClassroomChange(device.classroom_id);
    createForm.value.device_id = device.id; // 重置设备ID
    createDialogVisible.value = true;
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

.text-secondary {
  color: var(--text-secondary);
  font-size: 13px;
}

.w-100 {
  width: 100%;
}

.desc-text {
  margin: 0;
  color: var(--text-main);
  background-color: var(--bg-app);
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid var(--warning-color);
  font-size: 13px;
}

.mt-20 {
  margin-top: 20px;
}

.result-box {
  background-color: var(--bg-app);
  padding: 10px 14px;
  border-radius: 8px;
  color: var(--text-main);
  font-family: monospace;
  font-size: 13px;
  white-space: pre-wrap;
  border: 1px solid var(--border-light);
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
