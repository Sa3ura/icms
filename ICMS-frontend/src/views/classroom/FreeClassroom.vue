<template>
  <div class="free-classroom-container page-container">
    <!-- 查询过滤卡片 -->
    <el-card
      shadow="never"
      class="filter-card"
    >
      <template #header>
        <div class="card-header-flex">
          <span>空闲教室条件筛选</span>
        </div>
      </template>
      <el-form
        ref="queryFormRef"
        :model="queryForm"
        :rules="queryRules"
        label-width="85px"
        label-position="left"
      >
        <el-row :gutter="20">
          <el-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <el-form-item
              label="查询日期"
              prop="date"
            >
              <el-date-picker 
                v-model="queryForm.date" 
                type="date" 
                placeholder="选择日期" 
                value-format="YYYY-MM-DD"
                class="w-100"
                :disabled-date="disabledPastDates"
              />
            </el-form-item>
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
            :md="8"
          >
            <el-form-item
              label="节次范围"
              prop="sections"
            >
              <div class="section-picker-row">
                <el-select
                  v-model="queryForm.startSection"
                  placeholder="开始"
                  style="width: 110px"
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
                  v-model="queryForm.endSection"
                  placeholder="结束"
                  style="width: 110px"
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
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
            :md="5"
          >
            <el-form-item label="校区选择">
              <el-select
                v-model="queryForm.campus_id"
                placeholder="全部校区"
                clearable
                class="w-100"
              >
                <el-option
                  v-for="c in campuses"
                  :key="c.id"
                  :label="c.campus_name"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col
            :xs="24"
            :sm="12"
            :md="5"
          >
            <el-form-item label="容纳人数">
              <el-input-number
                v-model="queryForm.minCapacity"
                :min="10"
                :max="300"
                :step="10"
                class="w-100"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row
          :gutter="20"
          class="mt-10"
        >
          <el-col :span="24">
            <el-form-item label="配套设备">
              <el-checkbox-group v-model="queryForm.devices">
                <el-checkbox label="projector">
                  投影仪
                </el-checkbox>
                <el-checkbox label="whiteboard">
                  电子白板
                </el-checkbox>
                <el-checkbox label="camera">
                  云台摄像头
                </el-checkbox>
                <el-checkbox label="air_conditioner">
                  空调
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="filter-buttons">
          <el-button
            type="primary"
            icon="Search"
            @click="handleSearch"
          >
            查询空闲教室
          </el-button>
          <el-button
            icon="Refresh"
            @click="resetQuery"
          >
            重置
          </el-button>
        </div>
      </el-form>
    </el-card>

    <!-- 查询结果卡片区 -->
    <div
      v-loading="loading"
      class="result-area"
    >
      <div
        v-if="searched && classrooms.length === 0"
        class="empty-placeholder"
      >
        <el-empty description="在所选时段内，没有找到满足条件的空闲教室" />
      </div>
      
      <div
        v-else-if="!searched"
        class="empty-placeholder"
      >
        <el-empty description="请选择日期与节次，开始匹配空闲的智慧教室资源" />
      </div>

      <div v-else>
        <div class="result-header-tip">
          <el-alert 
            :title="`为您找到以下 ${classrooms.length} 间在 ${queryForm.date} (第 ${queryForm.startSection}-${queryForm.endSection} 节) 空闲且满足要求的教室：`" 
            type="success" 
            show-icon 
            :closable="false"
            class="mb-20"
          />
        </div>

        <el-row :gutter="20">
          <el-col
            v-for="room in classrooms"
            :key="room.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
            class="mb-20"
          >
            <el-card
              shadow="never"
              class="classroom-card hover-scale"
            >
              <div class="room-header">
                <div class="room-type-tag">
                  <el-tag :type="getRoomTypeTag(room.room_type)">
                    {{ getRoomTypeLabel(room.room_type) }}
                  </el-tag>
                </div>
                <h3>{{ room.room_no }}</h3>
              </div>
              
              <div class="room-body">
                <p class="room-name">
                  {{ room.name }}
                </p>
                <div class="room-meta">
                  <span class="meta-item"><el-icon><User /></el-icon> 容纳: {{ room.capacity }}人</span>
                  <span class="meta-item"><el-icon><Location /></el-icon> {{ getCampusLabel(room.campus_id) }}</span>
                </div>
                <p
                  class="room-desc"
                  :title="room.description"
                >
                  {{ room.description || '暂无描述' }}
                </p>
              </div>

              <div class="room-footer">
                <el-button
                  type="info"
                  size="small"
                  link
                  icon="InfoFilled"
                  @click="viewDetail(room)"
                >
                  详情
                </el-button>
                <el-button
                  type="primary"
                  size="small"
                  icon="Calendar"
                  @click="quickBook(room)"
                >
                  立即预约
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- 快速预约弹窗 -->
    <el-dialog
      v-model="bookDialogVisible"
      title="申请教室预约"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="bookFormRef"
        :model="bookForm"
        :rules="bookRules"
        label-width="90px"
      >
        <el-form-item label="选定教室">
          <el-input
            :value="selectedRoom?.name"
            disabled
          />
        </el-form-item>
        <el-form-item label="预约日期">
          <el-input
            :value="bookForm.reservation_date"
            disabled
          />
        </el-form-item>
        <el-form-item label="节次范围">
          <el-input
            :value="`第 ${bookForm.start_section} 节 至 第 ${bookForm.end_section} 节`"
            disabled
          />
        </el-form-item>
        <el-form-item
          label="使用人数"
          prop="attendee_count"
        >
          <el-input-number
            v-model="bookForm.attendee_count"
            :min="1"
            :max="selectedRoom?.capacity"
            class="w-100"
          />
          <span class="input-tip">使用人数不得超过教室最大容量：{{ selectedRoom?.capacity }} 人</span>
        </el-form-item>
        <el-form-item
          label="联系电话"
          prop="contact_phone"
        >
          <el-input
            v-model="bookForm.contact_phone"
            placeholder="请输入联系电话"
          />
        </el-form-item>
        <el-form-item
          label="预约用途"
          prop="purpose"
        >
          <el-input
            v-model="bookForm.purpose"
            type="textarea"
            rows="4"
            placeholder="请详细描述该教室的预约用途（如：专业讲座、社团会议等）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="bookSubmitLoading"
          @click="submitBook"
        >
          提交预约申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 课表详情弹窗 -->
    <el-dialog
      v-model="timetableVisible"
      :title="selectedRoom?.name + ' - 排课课表大沙盘'"
      width="900px"
    >
      <el-table
        :data="timetableGrid"
        style="width: 100%"
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
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useUserStore } from '../../store/user';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { 
  apiGetFreeClassrooms, 
  apiCreateReservation,
  apiGetSchedules,
  apiGetReservations,
  apiGetCampuses
} from '../../api';

const userStore = useUserStore();
const router = useRouter();

const loading = ref(false);
const searched = ref(false);
const bookSubmitLoading = ref(false);
const bookDialogVisible = ref(false);
const timetableVisible = ref(false);

const campuses = ref([]);
const classrooms = ref([]);

const selectedRoom = ref(null);
const activeRoomSchedules = ref([]);
const activeRoomReservations = ref([]);

// 初始查询表单：默认查询明天第1-2节
const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
const tomorrowStr = tomorrow.toISOString().slice(0, 10);

const queryForm = ref({
  date: tomorrowStr,
  startSection: 1,
  endSection: 2,
  campus_id: '',
  minCapacity: 10,
  devices: []
});

const queryRules = {
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
  sections: [
    {
      validator: (rule, value, callback) => {
        if (queryForm.value.startSection > queryForm.value.endSection) {
          callback(new Error('开始节次不能大于结束节次'));
        } else {
          callback();
        }
      },
      trigger: 'change'
    }
  ]
};

// 预约表单对象
const bookForm = ref({
  classroom_id: null,
  reservation_date: '',
  start_section: null,
  end_section: null,
  applicant_id: null,
  attendee_count: 10,
  contact_phone: '',
  purpose: ''
});

const bookRules = {
  attendee_count: [{ required: true, message: '请输入预约使用人数', trigger: 'blur' }],
  contact_phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号码', trigger: 'blur' }
  ],
  purpose: [
    { required: true, message: '请输入预约具体用途', trigger: 'blur' },
    { min: 5, message: '具体用途描述不能少于5个字', trigger: 'blur' }
  ]
};

const disabledPastDates = (time) => {
  return time.getTime() < Date.now() - 3600 * 1000 * 24; // 只能选今天和以后的日期
};

import { getRoomTypeLabel, getRoomTypeTag, getWeekdayLabel } from '../../utils/mappings';
import { useTimetable } from '../../composables/useTimetable';

const getCampusLabel = (id) => campuses.value.find(c => c.id === id)?.campus_name || '';

const handleSearch = () => {
  queryFormRef.value?.validate(async (valid) => {
    if (valid) {
      if (queryForm.value.startSection > queryForm.value.endSection) {
        ElMessage.error('开始节次必须小于等于结束节次');
        return;
      }
      
      loading.value = true;
      try {
        const res = await apiGetFreeClassrooms(queryForm.value);
        if (res.code === 200) {
          classrooms.value = res.data;
          searched.value = true;
        }
      } catch (error) {
        ElMessage.error(error.message);
      } finally {
        loading.value = false;
      }
    }
  });
};

const resetQuery = () => {
  queryForm.value = {
    date: tomorrowStr,
    startSection: 1,
    endSection: 2,
    campus_id: '',
    minCapacity: 10,
    devices: []
  };
  classrooms.value = [];
  searched.value = false;
};

const viewDetail = async (room) => {
  selectedRoom.value = room;
  try {
    const schedRes = await apiGetSchedules({ classroom_id: room.id });
    const resRes = await apiGetReservations({ classroom_id: room.id, status: 'approved' });

    if (schedRes.code === 200) activeRoomSchedules.value = schedRes.data;
    if (resRes.code === 200) activeRoomReservations.value = resRes.data;

    timetableVisible.value = true;
  } catch (error) {
    ElMessage.error('读取课表详情出错');
  }
};

const quickBook = (room) => {
  // 仅限特定角色可预约 (教师、学生、教务、系统管理员)
  if (['repairer'].includes(userStore.role)) {
    ElMessage.warning('维修工人员账号不能申请教室预约。');
    return;
  }

  selectedRoom.value = room;
  bookForm.value = {
    classroom_id: room.id,
    reservation_date: queryForm.value.date,
    start_section: queryForm.value.startSection,
    end_section: queryForm.value.endSection,
    applicant_id: userStore.userInfo.id,
    attendee_count: Math.min(20, room.capacity),
    contact_phone: userStore.userInfo.phone || '',
    purpose: ''
  };
  bookDialogVisible.value = true;
};

const submitBook = () => {
  bookFormRef.value?.validate(async (valid) => {
    if (valid) {
      bookSubmitLoading.value = true;
      try {
        const res = await apiCreateReservation(bookForm.value, userStore.userInfo.username);
        if (res.code === 200) {
          ElMessage.success('教室预约申请提交成功，请等待管理员审批！');
          bookDialogVisible.value = false;
          // 跳转到我的预约页查看审批进度
          router.push('/reservation/my');
        } else {
          ElMessage.error(res.message);
        }
      } catch (error) {
        ElMessage.error(error.message);
      } finally {
        bookSubmitLoading.value = false;
      }
    }
  });
};

const { timetableGrid } = useTimetable(activeRoomSchedules, activeRoomReservations);

const queryFormRef = ref(null);
const bookFormRef = ref(null);

onMounted(async () => {
  const res = await apiGetCampuses();
  if (res.code === 200) {
    campuses.value = res.data;
  }
});
</script>

<style scoped>
.filter-card {
  margin-bottom: 20px;
}

.section-picker-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.range-separator {
  color: var(--text-secondary);
}

.mt-10 {
  margin-top: 10px;
}

.filter-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 15px;
  border-top: 1px solid var(--border-light);
  padding-top: 15px;
}

.result-area {
  min-height: 250px;
}

.empty-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px 0;
  background-color: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-light);
}

.mb-20 {
  margin-bottom: 20px;
}

.classroom-card {
  display: flex;
  flex-direction: column;
  height: 210px;
  border: 1px solid var(--border-light);
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px dashed var(--border-light);
  padding-bottom: 8px;
  margin-bottom: 10px;
}

.room-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}

.room-body {
  flex-grow: 1;
}

.room-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.room-desc {
  font-size: 12px;
  color: var(--text-regular);
  margin-top: 8px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.room-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
  border-top: 1px solid var(--border-light);
  padding-top: 10px;
}

.w-100 {
  width: 100%;
}

.input-tip {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 4px;
  display: block;
}

/* 课程排课课表样式 */
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
