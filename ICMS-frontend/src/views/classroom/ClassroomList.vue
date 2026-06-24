<template>
  <div class="classroom-list-container page-container">
    <!-- 顶栏查询区域 -->
    <el-card
      shadow="never"
      class="filter-card"
    >
      <el-form
        :inline="true"
        :model="filters"
        class="demo-form-inline"
      >
        <el-form-item label="校区">
          <el-select
            v-model="filters.campus_id"
            placeholder="选择校区"
            clearable
            style="width: 130px"
            @change="handleCampusChange"
          >
            <el-option
              v-for="c in campuses"
              :key="c.id"
              :label="c.campus_name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select
            v-model="filters.building_id"
            placeholder="选择楼栋"
            clearable
            style="width: 140px"
          >
            <el-option
              v-for="b in filteredBuildings"
              :key="b.id"
              :label="b.building_name"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select
            v-model="filters.room_type"
            placeholder="选择类别"
            clearable
            style="width: 130px"
          >
            <el-option
              label="普通教室"
              value="normal"
            />
            <el-option
              label="多媒体教室"
              value="multimedia"
            />
            <el-option
              label="实验室"
              value="lab"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="filters.status"
            placeholder="选择状态"
            clearable
            style="width: 120px"
          >
            <el-option
              label="可用"
              value="available"
            />
            <el-option
              label="使用中"
              value="busy"
            />
            <el-option
              label="维护中"
              value="maintenance"
            />
            <el-option
              label="已停用"
              value="deactivated"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="filters.keyword"
            placeholder="搜索教室名称/编号"
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

    <!-- 操作与表格列表 -->
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
          新增教室
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
      >
        <el-table-column
          prop="room_no"
          label="教室编号"
          width="100"
        />
        <el-table-column
          prop="name"
          label="教室名称"
          min-width="150"
        />
        <el-table-column
          label="校区/楼栋/楼层"
          min-width="200"
        >
          <template #default="{ row }">
            {{ getCampusName(row.campus_id) }} / {{ getBuildingName(row.building_id) }} / {{ row.floor_no }}层
          </template>
        </el-table-column>
        <el-table-column
          prop="capacity"
          label="容纳人数"
          width="100"
        />
        <el-table-column
          prop="room_type"
          label="类型"
          width="110"
        >
          <template #default="{ row }">
            <el-tag :type="getRoomTypeTag(row.room_type)">
              {{ getRoomTypeLabel(row.room_type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="100"
        >
          <template #default="{ row }">
            <el-tag :type="getClassroomStatusTag(row.status)">
              {{ getClassroomStatusLabel(row.status) }}
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
                    command="timetable"
                    :icon="Calendar"
                  >
                    查看课表
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="devices"
                    :icon="Cpu"
                  >
                    查看设备
                  </el-dropdown-item>
                  <template v-if="canEdit">
                    <el-dropdown-item
                      command="edit"
                      :icon="Edit"
                      divided
                    >
                      编辑教室
                    </el-dropdown-item>
                    <el-dropdown-item
                      command="delete"
                      :icon="Delete"
                    >
                      <span class="text-danger">删除教室</span>
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
      :title="dialogType === 'create' ? '新增教室' : '编辑教室'" 
      width="540px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >
        <el-form-item
          label="所属校区"
          prop="campus_id"
        >
          <el-select
            v-model="form.campus_id"
            placeholder="请选择校区"
            class="w-100"
            @change="handleFormCampusChange"
          >
            <el-option
              v-for="c in campuses"
              :key="c.id"
              :label="c.campus_name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="所属楼栋"
          prop="building_id"
        >
          <el-select
            v-model="form.building_id"
            placeholder="请选择楼栋"
            class="w-100"
          >
            <el-option
              v-for="b in formBuildings"
              :key="b.id"
              :label="b.building_name"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="所属楼层"
          prop="floor_no"
        >
          <el-input-number
            v-model="form.floor_no"
            :min="1"
            :max="30"
            class="w-100"
          />
        </el-form-item>
        <el-form-item
          label="教室编号"
          prop="room_no"
        >
          <el-input
            v-model="form.room_no"
            placeholder="如 A101"
          />
        </el-form-item>
        <el-form-item
          label="教室名称"
          prop="name"
        >
          <el-input
            v-model="form.name"
            placeholder="如 第一教学楼A101"
          />
        </el-form-item>
        <el-form-item
          label="容纳人数"
          prop="capacity"
        >
          <el-input-number
            v-model="form.capacity"
            :min="10"
            :max="300"
            :step="10"
            class="w-100"
          />
        </el-form-item>
        <el-form-item
          label="教室类型"
          prop="room_type"
        >
          <el-radio-group v-model="form.room_type">
            <el-radio-button value="normal">
              普通
            </el-radio-button>
            <el-radio-button value="multimedia">
              多媒体
            </el-radio-button>
            <el-radio-button value="lab">
              实验室
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="教室状态"
          prop="status"
        >
          <el-select
            v-model="form.status"
            class="w-100"
          >
            <el-option
              label="可用"
              value="available"
            />
            <el-option
              label="维护中"
              value="maintenance"
            />
            <el-option
              label="已停用"
              value="deactivated"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="开放时间"
          prop="open_time"
        >
          <el-input
            v-model="form.open_time"
            placeholder="如 08:00-22:00"
          />
        </el-form-item>
        <el-form-item
          label="备注描述"
          prop="description"
        >
          <el-input
            v-model="form.description"
            type="textarea"
            rows="3"
            placeholder="教室详细描述..."
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

    <!-- 设备弹窗 -->
    <el-dialog
      v-model="deviceDialogVisible"
      :title="activeRoomName + ' - 设备列表'"
      width="650px"
    >
      <el-table
        :data="classroomDevices"
        style="width: 100%"
      >
        <el-table-column
          prop="device_code"
          label="设备编号"
          width="130"
        />
        <el-table-column
          prop="device_name"
          label="设备名称"
        />
        <el-table-column
          prop="brand"
          label="品牌"
          width="100"
        />
        <el-table-column
          prop="model"
          label="型号"
          width="100"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="90"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 'normal' ? 'success' : (row.status === 'fault' ? 'danger' : 'warning')">
              {{ row.status === 'normal' ? '正常' : (row.status === 'fault' ? '故障' : '维修中') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          min-width="100"
          fixed="right"
        >
          <template #default="{ row }">
            <template v-if="row.status === 'normal'">
              <el-tooltip
                content="对此设备发起故障报修"
                placement="top"
              >
                <el-button 
                  size="small" 
                  type="danger" 
                  link 
                  @click="reportDeviceFault(row)"
                >
                  报修
                </el-button>
              </el-tooltip>
            </template>
            <span
              v-else
              class="text-secondary"
            >-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 课表弹窗 -->
    <el-dialog
      v-model="timetableVisible"
      :title="activeRoomName + ' - 本周排课与占用表'"
      width="900px"
      custom-class="timetable-dialog"
    >
      <div class="timetable-wrapper">
        <el-table
          :data="timetableGrid"
          style="width: 100%"
          cell-class-name="timetable-cell"
        >
          <el-table-column
            prop="section_label"
            label="节次/时间"
            width="110"
            align="center"
          />
          <el-table-column
            v-for="day in 7"
            :key="day"
            :label="getWeekdayLabel(day)"
            align="center"
          >
            <template #default="{ row }">
              <div
                v-if="row[day]"
                :class="['schedule-block', row[day].type]"
              >
                <div class="block-title">
                  {{ row[day].name }}
                </div>
                <div class="block-teacher">
                  {{ row[day].teacher }}
                </div>
                <div
                  v-if="row[day].details"
                  class="block-details"
                >
                  {{ row[day].details }}
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useUserStore } from '../../store/user';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  apiGetClassrooms, 
  apiSaveClassroom, 
  apiDeleteClassroom, 
  apiGetDevices, 
  apiGetSchedules,
  apiGetReservations,
  apiGetCampuses,
  apiGetBuildings
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

const deviceDialogVisible = ref(false);
const activeRoomName = ref('');
const classroomDevices = ref([]);

const timetableVisible = ref(false);
const activeRoomSchedules = ref([]);
const activeRoomReservations = ref([]);

// 基础数据
const campuses = ref([]);
const buildings = ref([]);

// 筛选条件
const filters = ref({
  campus_id: '',
  building_id: '',
  room_type: '',
  status: '',
  keyword: ''
});

// 根据当前登录角色判断是否有编辑权限
const canEdit = computed(() => ['admin', 'jw'].includes(userStore.role));

// 过滤联动楼栋列表
const filteredBuildings = computed(() => {
  if (!filters.value.campus_id) return [];
  return buildings.value.filter(b => Number(b.campus_id) === Number(filters.value.campus_id));
});

// 新增/编辑表单联动楼栋列表
const formBuildings = ref([]);

// 表单对象
const form = ref({
  id: null,
  campus_id: null,
  building_id: null,
  floor_no: 1,
  room_no: '',
  name: '',
  capacity: 60,
  room_type: 'normal',
  status: 'available',
  open_time: '08:00-22:00',
  description: ''
});

const rules = {
  campus_id: [{ required: true, message: '请选择校区', trigger: 'change' }],
  building_id: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  room_no: [{ required: true, message: '请输入教室编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入教室名称', trigger: 'blur' }]
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

const handleCampusChange = () => {
  filters.value.building_id = '';
};

let keywordTimer = null;
const debouncedKeyword = () => {
  clearTimeout(keywordTimer);
  keywordTimer = setTimeout(() => {
    currentPage.value = 1;
    loadData();
  }, 300);
};

const handleFormCampusChange = (val) => {
  form.value.building_id = null;
  formBuildings.value = buildings.value.filter(b => Number(b.campus_id) === Number(val));
};

// 获取校区/楼栋的名称
const getCampusName = (id) => campuses.value.find(c => c.id === id)?.campus_name || '';
const getBuildingName = (id) => buildings.value.find(b => b.id === id)?.building_name || '';

import { 
  getRoomTypeLabel, 
  getRoomTypeTag, 
  getClassroomStatusLabel, 
  getClassroomStatusTag, 
  getWeekdayLabel 
} from '../../utils/mappings';
import { useTimetable } from '../../composables/useTimetable';
import { Cpu, Edit, Delete, MoreFilled } from '@element-plus/icons-vue';

// 重置查询条件
const resetFilters = () => {
  filters.value = { campus_id: '', building_id: '', room_type: '', status: '', keyword: '' };
  loadData();
};

// 加载基础数据与列表
const loadData = async () => {
  loading.value = true;
  try {
    const [campusesRes, buildingsRes] = await Promise.all([
      apiGetCampuses(),
      apiGetBuildings()
    ]);

    if (campusesRes.code === 200) campuses.value = campusesRes.data;
    if (buildingsRes.code === 200) buildings.value = buildingsRes.data;

    const res = await apiGetClassrooms({ page: currentPage.value, pageSize: pageSize.value, ...filters.value });
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

const handleCreate = () => {
  form.value = {
    id: null,
    campus_id: campuses.value[0]?.id || null,
    building_id: null,
    floor_no: 1,
    room_no: '',
    name: '',
    capacity: 60,
    room_type: 'normal',
    status: 'available',
    open_time: '08:00-22:00',
    description: ''
  };
  formBuildings.value = form.value.campus_id ? buildings.value.filter(b => Number(b.campus_id) === Number(form.value.campus_id)) : [];
  dialogType.value = 'create';
  dialogVisible.value = true;
};

const handleEdit = (row) => {
  form.value = { ...row };
  formBuildings.value = buildings.value.filter(b => Number(b.campus_id) === Number(row.campus_id));
  dialogType.value = 'edit';
  dialogVisible.value = true;
};

const submitForm = () => {
  submitLoading.value = true;
  apiSaveClassroom(form.value, userStore.userInfo.username)
    .then(res => {
      if (res.code === 200) {
        ElMessage.success(dialogType.value === 'create' ? '新增教室成功' : '编辑教室成功');
        dialogVisible.value = false;
        loadData();
      } else {
        ElMessage.error(res.message);
      }
    })
    .catch(err => ElMessage.error(err.message))
    .finally(() => submitLoading.value = false);
};

const handleDelete = (row) => {
  apiDeleteClassroom(row.id, userStore.userInfo.username)
    .then(res => {
      if (res.code === 200) {
        ElMessage.success('删除成功');
        loadData();
      } else {
        ElMessage.error(res.message);
      }
    })
    .catch(err => ElMessage.error(err.message));
};

const handleDropdown = async (command, row) => {
  if (command === 'timetable') {
    viewTimetable(row);
  } else if (command === 'devices') {
    viewDevices(row);
  } else if (command === 'edit') {
    handleEdit(row);
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定要删除这个教室吗？删除后数据将无法恢复。', '删除确认', {
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

// 设备详情弹窗
const viewDevices = async (row) => {
  activeRoomName.value = row.name;
  try {
    const res = await apiGetDevices({ pageSize: 10000, classroom_id: row.id });
    if (res.code === 200) {
      classroomDevices.value = res.data.records;
      deviceDialogVisible.value = true;
    }
  } catch (error) {
    ElMessage.error('无法读取设备信息');
  }
};

const reportDeviceFault = (device) => {
  // 保存目标设备和教室信息，跳转到工单列表，自动触发新增工单弹窗
  localStorage.setItem('icms_shortcut_report_device', JSON.stringify(device));
  deviceDialogVisible.value = false;
  router.push('/work-order/list');
};

// 课表详情弹窗
const viewTimetable = async (row) => {
  activeRoomName.value = row.name;
  try {
    const schedRes = await apiGetSchedules({ pageSize: 10000, classroom_id: row.id });
    const resRes = await apiGetReservations({ pageSize: 10000, classroom_id: row.id, status: 'approved' });

    if (schedRes.code === 200) activeRoomSchedules.value = schedRes.data;
    if (resRes.code === 200) activeRoomReservations.value = resRes.data;

    timetableVisible.value = true;
  } catch (error) {
    ElMessage.error('无法读取排课课表');
  }
};

// 将课表数据构造成节次为行，星期为列的网络格
const { timetableGrid } = useTimetable(activeRoomSchedules, activeRoomReservations);

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

.table-actions {
  margin-bottom: 16px;
}

.w-100 {
  width: 100%;
}

.text-secondary {
  color: var(--text-secondary);
}

.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 课程排课课表样式 */
.timetable-wrapper {
  margin: 10px 0;
  overflow-x: auto;
}

:deep(.timetable-cell) {
  padding: 0 !important;
}

.schedule-block {
  padding: 8px;
  border-radius: 6px;
  min-height: 70px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 2px;
  font-size: 12px;
}

.schedule-block.course {
  background-color: var(--primary-light);
  color: var(--primary-color);
  border: 1px solid rgba(99, 102, 241, 0.3);
}

.schedule-block.reservation {
  background-color: #d1fae5;
  color: #065f46;
  border: 1px solid rgba(16, 185, 129, 0.3);
}

html.dark .schedule-block.course {
  background-color: rgba(99, 102, 241, 0.15);
  color: #a5b4fc;
}

html.dark .schedule-block.reservation {
  background-color: rgba(16, 185, 129, 0.15);
  color: #34d399;
}

.block-title {
  font-weight: 600;
  text-align: center;
  line-height: 1.2;
}

.block-teacher {
  opacity: 0.85;
}

.block-details {
  font-size: 11px;
  opacity: 0.75;
  margin-top: 2px;
  text-align: center;
}
</style>
