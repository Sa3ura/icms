<template>
  <div class="device-list-container page-container">
    <!-- 过滤搜索 -->
    <el-card
      shadow="never"
      class="filter-card"
    >
      <el-form
        :inline="true"
        :model="filters"
        class="demo-form-inline"
      >
        <el-form-item label="所属教室">
          <el-select
            v-model="filters.classroom_id"
            placeholder="选择教室"
            clearable
            style="width: 160px"
          >
            <el-option
              v-for="c in classrooms"
              :key="c.id"
              :label="c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select
            v-model="filters.device_type"
            placeholder="选择类型"
            clearable
            style="width: 140px"
          >
            <el-option
              label="投影仪"
              value="projector"
            />
            <el-option
              label="电子白板"
              value="whiteboard"
            />
            <el-option
              label="空调"
              value="air_conditioner"
            />
            <el-option
              label="摄像头"
              value="camera"
            />
            <el-option
              label="中控主机"
              value="controller"
            />
            <el-option
              label="智能门禁"
              value="lock"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="运行状态">
          <el-select
            v-model="filters.status"
            placeholder="选择状态"
            clearable
            style="width: 120px"
          >
            <el-option
              label="正常运行"
              value="normal"
            />
            <el-option
              label="出现故障"
              value="fault"
            />
            <el-option
              label="正在维修"
              value="repairing"
            />
            <el-option
              label="已报废"
              value="scrapped"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="filters.keyword"
            placeholder="搜索设备编码/名称"
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

    <!-- 设备列表表格 -->
    <el-card
      shadow="never"
      class="table-card"
    >
      <div
        v-if="canEdit"
        class="table-actions"
      >
        <el-button
          type="primary"
          icon="Plus"
          @click="handleCreate"
        >
          新增设备档案
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
      >
        <el-table-column
          prop="device_code"
          label="设备编码"
          width="130"
        />
        <el-table-column
          prop="device_name"
          label="设备名称"
          min-width="150"
        />
        <el-table-column
          prop="device_type"
          label="类型"
          width="110"
        >
          <template #default="{ row }">
            <el-tag :type="getDeviceTypeTag(row.device_type)">
              {{ getDeviceTypeLabel(row.device_type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="classroom_name"
          label="所属教室"
          min-width="150"
        />
        <el-table-column
          prop="brand"
          label="品牌"
          width="100"
        />
        <el-table-column
          prop="model"
          label="规格型号"
          width="120"
          show-overflow-tooltip
        />
        <el-table-column
          prop="purchase_date"
          label="采购日期"
          width="110"
        />
        <el-table-column
          prop="last_check_time"
          label="最近巡检时间"
          width="160"
        />
        <el-table-column
          prop="status"
          label="运行状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="getDeviceStatusTag(row.status)">
              {{ getDeviceStatusLabel(row.status) }}
            </el-tag>
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
              @command="(cmd) => handleDeviceDropdown(cmd, row)"
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
                    v-if="row.status === 'normal'"
                    command="report"
                    :icon="Warning"
                  >
                    故障报修
                  </el-dropdown-item>
                  <template v-if="canEdit">
                    <el-dropdown-item
                      command="edit"
                      :icon="Edit"
                      :divided="row.status === 'normal'"
                    >
                      编辑设备
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="delete"
                      :icon="Delete"
                    >
                      <span class="text-danger">删除设备</span>
                    </el-dropdown-item>
                  </template>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogType === 'create' ? '新增设备档案' : '编辑设备'" 
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-form-item
          label="设备编码"
          prop="device_code"
        >
          <el-input
            v-model="form.device_code"
            placeholder="如 DEV-A101-001"
            :disabled="dialogType === 'edit'"
          />
        </el-form-item>
        <el-form-item
          label="设备名称"
          prop="device_name"
        >
          <el-input
            v-model="form.device_name"
            placeholder="如 激光投影仪"
          />
        </el-form-item>
        <el-form-item
          label="设备类别"
          prop="device_type"
        >
          <el-select
            v-model="form.device_type"
            placeholder="请选择类别"
            class="w-100"
          >
            <el-option
              label="投影仪"
              value="projector"
            />
            <el-option
              label="电子白板"
              value="whiteboard"
            />
            <el-option
              label="空调"
              value="air_conditioner"
            />
            <el-option
              label="摄像头"
              value="camera"
            />
            <el-option
              label="中控主机"
              value="controller"
            />
            <el-option
              label="智能门禁"
              value="lock"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="绑定教室"
          prop="classroom_id"
        >
          <el-select
            v-model="form.classroom_id"
            placeholder="请选择教室"
            class="w-100"
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
          label="品牌名称"
          prop="brand"
        >
          <el-input
            v-model="form.brand"
            placeholder="如 Epson"
          />
        </el-form-item>
        <el-form-item
          label="规格型号"
          prop="model"
        >
          <el-input
            v-model="form.model"
            placeholder="如 CB-L530U"
          />
        </el-form-item>
        <el-form-item
          label="采购日期"
          prop="purchase_date"
        >
          <el-date-picker
            v-model="form.purchase_date"
            type="date"
            placeholder="选择采购日期"
            value-format="YYYY-MM-DD"
            class="w-100"
          />
        </el-form-item>
        <el-form-item
          label="运行状态"
          prop="status"
        >
          <el-select
            v-model="form.status"
            class="w-100"
          >
            <el-option
              label="正常运行"
              value="normal"
            />
            <el-option
              label="发生故障"
              value="fault"
            />
            <el-option
              label="正在维修"
              value="repairing"
            />
            <el-option
              label="已报废"
              value="scrapped"
            />
          </el-select>
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

    <!-- 故障报修弹窗 -->
    <el-dialog
      v-model="faultDialogVisible"
      title="故障快速报修"
      width="480px"
      destroy-on-close
    >
      <el-form
        ref="faultFormRef"
        :model="faultForm"
        :rules="faultRules"
        label-width="90px"
      >
        <el-form-item label="报修设备">
          <el-input
            :value="activeDevice?.device_name + ' (' + activeDevice?.device_code + ')'"
            disabled
          />
        </el-form-item>
        <el-form-item label="所属教室">
          <el-input
            :value="activeDevice?.classroom_name"
            disabled
          />
        </el-form-item>
        <el-form-item
          label="故障标题"
          prop="fault_title"
        >
          <el-input
            v-model="faultForm.fault_title"
            placeholder="一句话简单描述故障（如：投影仪无法开机）"
          />
        </el-form-item>
        <el-form-item
          label="故障等级"
          prop="fault_level"
        >
          <el-radio-group v-model="faultForm.fault_level">
            <el-radio-button value="normal">
              普通故障
            </el-radio-button>
            <el-radio-button value="urgent">
              紧急故障
            </el-radio-button>
            <el-radio-button value="severe">
              严重故障
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="详情描述"
          prop="fault_description"
        >
          <el-input 
            v-model="faultForm.fault_description" 
            type="textarea" 
            rows="3" 
            placeholder="请叙述故障的现象与细节..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="faultDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="faultSubmitLoading"
          @click="submitFault"
        >
          提交报修
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useUserStore } from '../../store/user';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  apiGetDevices, 
  apiSaveDevice, 
  apiDeleteDevice, 
  apiGetClassrooms,
  apiCreateWorkOrder 
} from '../../api';

const userStore = useUserStore();
const router = useRouter();

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogType = ref('create'); // create / edit
const tableData = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);

const classrooms = ref([]);

// 快速报修相关
const faultDialogVisible = ref(false);
const faultSubmitLoading = ref(false);
const activeDevice = ref(null);

const filters = ref({
  classroom_id: '',
  device_type: '',
  status: '',
  keyword: ''
});

const form = ref({
  id: null,
  classroom_id: null,
  device_code: '',
  device_name: '',
  device_type: 'projector',
  brand: '',
  model: '',
  purchase_date: '',
  status: 'normal'
});

const rules = {
  device_code: [{ required: true, message: '请输入设备编码', trigger: 'blur' }],
  device_name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  device_type: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
  classroom_id: [{ required: true, message: '请选择绑定教室', trigger: 'change' }]
};

const faultForm = ref({
  fault_title: '',
  fault_level: 'normal',
  fault_description: ''
});

const faultRules = {
  fault_title: [{ required: true, message: '请输入故障简述', trigger: 'blur' }],
  fault_description: [{ required: true, message: '请输入详情描述', trigger: 'blur' }]
};

const formRef = ref(null);
const faultFormRef = ref(null);

const canEdit = computed(() => ['admin', 'jw'].includes(userStore.role));

let keywordTimer = null;
const debouncedKeyword = () => {
  clearTimeout(keywordTimer);
  keywordTimer = setTimeout(() => {
    currentPage.value = 1;
    loadData();
  }, 300);
};

import { getDeviceTypeLabel, getDeviceTypeTag, getDeviceStatusLabel, getDeviceStatusTag } from '../../utils/mappings';
import { Edit, Delete, MoreFilled } from '@element-plus/icons-vue';

const resetFilters = () => {
  filters.value = { classroom_id: '', device_type: '', status: '', keyword: '' };
  loadData();
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await apiGetDevices({ page: currentPage.value, pageSize: pageSize.value, ...filters.value });
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
  form.value = {
    id: null,
    classroom_id: classrooms.value[0]?.id || null,
    device_code: '',
    device_name: '',
    device_type: 'projector',
    brand: '',
    model: '',
    purchase_date: new Date().toISOString().slice(0, 10),
    status: 'normal'
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
        const res = await apiSaveDevice(form.value, userStore.userInfo.username);
        if (res.code === 200) {
          ElMessage.success(dialogType.value === 'create' ? '录入设备成功' : '保存成功');
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

const handleDelete = async (row) => {
  try {
    const res = await apiDeleteDevice(row.id, userStore.userInfo.username);
    if (res.code === 200) {
      ElMessage.success('成功删除该设备档案');
      loadData();
    }
  } catch (e) {
    ElMessage.error('删除设备出错');
  }
};

const handleDeviceDropdown = async (command, row) => {
  if (command === 'report') {
    reportFault(row);
  } else if (command === 'edit') {
    handleEdit(row);
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确认删除此设备档案？删除后将无法恢复。', '删除确认', {
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

// 触发快速报修
const reportFault = (row) => {
  activeDevice.value = row;
  faultForm.value = {
    fault_title: '',
    fault_level: 'normal',
    fault_description: ''
  };
  faultDialogVisible.value = true;
};

const submitFault = () => {
  faultFormRef.value?.validate(async (valid) => {
    if (valid) {
      faultSubmitLoading.value = true;
      try {
        const payload = {
          reporter_id: userStore.userInfo.id,
          classroom_id: activeDevice.value.classroom_id,
          device_id: activeDevice.value.id,
          ...faultForm.value
        };
        const res = await apiCreateWorkOrder(payload, userStore.userInfo.username);
        if (res.code === 200) {
          ElMessage.success('故障报修成功，已自动生成维修工单！');
          faultDialogVisible.value = false;
          // 跳转到工单列表查看
          router.push('/work-order/list');
        }
      } catch (error) {
        ElMessage.error(error.message);
      } finally {
        faultSubmitLoading.value = false;
      }
    }
  });
};

onMounted(async () => {
  loadData();
  const res = await apiGetClassrooms({ pageSize: 10000 });
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

.w-100 {
  width: 100%;
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
