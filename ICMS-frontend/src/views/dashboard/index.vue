<template>
  <div class="dashboard-container page-container">
    <div class="welcome-header">
      <h2>你好，{{ userStore.realName }}。</h2>
      <p class="subtitle">
        这里是 ICMS 智慧教室运行概览，所有空间与设备状态一目了然。
      </p>
    </div>

    <!-- 数据指标统计 -->
    <el-row
      :gutter="24"
      class="metric-row"
    >
      <el-col
        v-for="card in statCards"
        :key="card.title"
        :xs="24"
        :sm="12"
        :md="6"
      >
        <el-card
          shadow="never"
          class="metric-card hover-scale"
          @click="handleCardClick(card.action)"
        >
          <div class="metric-content">
            <div class="metric-left">
              <span class="metric-title">{{ card.title }}</span>
              <div class="metric-value">
                {{ card.value }}
              </div>
            </div>
            <div
              class="metric-icon"
              :style="{ backgroundColor: card.iconBg, color: card.iconColor }"
            >
              <el-icon size="22">
                <component :is="card.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表分析区 -->
    <el-row
      :gutter="24"
      class="chart-row"
    >
      <!-- 教室使用率折线图 -->
      <el-col
        :xs="24"
        :md="15"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <div class="card-header-flex">
              <span>空间活跃度 (近7天)</span>
              <el-radio-group
                v-model="campusFilter"
                size="small"
                @change="updateCharts"
              >
                <el-radio-button label="all">
                  全校
                </el-radio-button>
                <el-radio-button label="1">
                  东区
                </el-radio-button>
                <el-radio-button label="2">
                  西区
                </el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div
            ref="lineChartRef"
            class="chart-box"
          />
        </el-card>
      </el-col>

      <!-- 设备状态分布饼图 -->
      <el-col
        :xs="24"
        :md="9"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <div class="card-header-flex">
              <span>终端健康监测</span>
            </div>
          </template>
          <div
            ref="pieChartRef"
            class="chart-box"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 今日课程与预约 -->
    <el-card
      shadow="never"
      class="table-card"
    >
      <template #header>
        <div class="card-header-flex">
          <span>今日空间资源占用</span>
          <el-button
            type="primary"
            class="plain-btn"
            size="small"
            @click="goToReservations"
          >
            管理预约
          </el-button>
        </div>
      </template>
      <el-table
        v-loading="tableLoading"
        :data="todaySchedules"
        style="width: 100%"
        :show-header="true"
      >
        <el-table-column
          prop="classroom_name"
          label="空间名称"
          min-width="120"
        >
          <template #default="{ row }">
            <span class="room-name">{{ row.classroom_name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          prop="type_label"
          label="活动类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.type === 'course' ? 'info' : 'success'"
              size="small"
            >
              {{ row.type === 'course' ? '排课' : '预约' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="时间段"
          width="180"
        >
          <template #default="{ row }">
            <div class="time-block">
              <span class="section-text">{{ row.start_section }}-{{ row.end_section }} 节</span>
              <span class="time-tip">{{ getSectionTime(row.start_section, row.end_section) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="title"
          label="任务/课程内容"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="user_name"
          label="负责人"
          width="120"
        />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '../../store/user';
import * as echarts from 'echarts/core';
import { LineChart, PieChart } from 'echarts/charts';
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([LineChart, PieChart, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer]);
import { 
  apiGetDashboardStats, 
  apiGetClassrooms, 
  apiGetReservations, 
  apiGetDevices, 
  apiGetSchedules 
} from '../../api';

const router = useRouter();
const userStore = useUserStore();

const campusFilter = ref('all');
const tableLoading = ref(false);

const stats = ref({
  classroomTotal: 0,
  todayReservations: 0,
  pendingApprovals: 0,
  pendingWorkOrders: 0,
  faultyDevices: 0
});

const todaySchedules = ref([]);

const lineChartRef = ref(null);
const pieChartRef = ref(null);
let lineChart = null;
let pieChart = null;

const statCards = computed(() => [
  { 
    title: '纳管空间总数', 
    value: stats.value.classroomTotal, 
    icon: 'School', 
    iconColor: 'var(--text-main)', 
    iconBg: 'var(--primary-light)',
    action: '/classroom/list'
  },
  { 
    title: '今日预约流转', 
    value: stats.value.todayReservations, 
    icon: 'Calendar', 
    iconColor: 'var(--success-color)', 
    iconBg: 'rgba(16, 185, 129, 0.1)',
    action: '/reservation/my'
  },
  { 
    title: '待审批预约', 
    value: stats.value.pendingApprovals, 
    icon: 'Stamp', 
    iconColor: 'var(--warning-color)', 
    iconBg: 'rgba(245, 158, 11, 0.1)',
    action: '/reservation/approval'
  },
  { 
    title: '未完结工单', 
    value: stats.value.pendingWorkOrders, 
    icon: 'Tools', 
    iconColor: 'var(--danger-color)', 
    iconBg: 'rgba(239, 68, 68, 0.1)',
    action: '/work-order/list'
  }
]);

const handleCardClick = (path) => {
  const rootRoute = router.options.routes.find(r => r.path === '/');
  if (!rootRoute || !rootRoute.children) return;
  const targetRoute = rootRoute.children.find(c => '/' + c.path === path);
  if (targetRoute && targetRoute.meta.roles && targetRoute.meta.roles.includes(userStore.role)) {
    router.push(path);
  }
};

const goToReservations = () => {
  router.push('/reservation/my');
};

import { getSectionTime } from '../../utils/mappings';

const loadStats = async () => {
  const res = await apiGetDashboardStats();
  if (res.code === 200) {
    stats.value = res.data;
  }
};

const loadTodayTable = async () => {
  tableLoading.value = true;
  try {
    const today = new Date();
    const todayStr = today.toISOString().slice(0, 10);
    const dayOfWeek = today.getDay() || 7;

    const schedRes = await apiGetSchedules({ pageSize: 10000, weekday: dayOfWeek });
    const resRes = await apiGetReservations({ pageSize: 10000, date_range: [todayStr, todayStr], status: 'approved' });
    const classroomsRes = await apiGetClassrooms({ pageSize: 10000 });
    const classrooms = classroomsRes.code === 200 ? classroomsRes.data.records : [];

    const list = [];
    if (schedRes.code === 200) {
      schedRes.data.records.forEach(s => {
        const room = classrooms.find(c => c.id === s.classroom_id);
        list.push({
          id: 'course_' + s.id,
          classroom_name: room ? room.name : '未知空间',
          type: 'course',
          start_section: s.start_section,
          end_section: s.end_section,
          title: s.course_name,
          user_name: s.teacher_name
        });
      });
    }

    if (resRes.code === 200) {
      resRes.data.records.forEach(r => {
        list.push({
          id: 'res_' + r.id,
          classroom_name: r.classroom_name,
          type: 'res',
          start_section: r.start_section,
          end_section: r.end_section,
          title: r.purpose,
          user_name: r.applicant_name
        });
      });
    }

    todaySchedules.value = list.sort((a, b) => {
      if (a.classroom_name === b.classroom_name) return a.start_section - b.start_section;
      return a.classroom_name.localeCompare(b.classroom_name);
    });
  } catch (err) {
    console.error(err);
  } finally {
    tableLoading.value = false;
  }
};

const updateCharts = async () => {
  const isDark = userStore.theme === 'dark';
  const textColor = isDark ? '#9ca3af' : '#6b7280';
  const axisColor = isDark ? '#374151' : '#f3f4f6';

  if (lineChartRef.value) {
    if (!lineChart) lineChart = echarts.init(lineChartRef.value);
    
    const baseRates = [35, 42, 58, 62, 45, 68, 72];
    const modifier = campusFilter.value === '1' ? 5 : (campusFilter.value === '2' ? -8 : 0);
    const chartRates = baseRates.map(v => Math.min(100, Math.max(0, v + modifier + Math.floor(Math.random() * 6))));

    const lineOption = {
      tooltip: {
        trigger: 'axis',
        backgroundColor: isDark ? '#1f2937' : '#ffffff',
        borderColor: axisColor,
        textStyle: { color: isDark ? '#f9fafb' : '#111827' },
        formatter: '{b} <br/>使用率 : {c}%'
      },
      grid: { left: '2%', right: '2%', bottom: '0%', top: '15%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        axisLine: { lineStyle: { color: axisColor } },
        axisLabel: { color: textColor, margin: 16 }
      },
      yAxis: {
        type: 'value',
        min: 0, max: 100,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: textColor, formatter: '{value}%' },
        splitLine: { lineStyle: { color: axisColor, type: 'dashed' } }
      },
      series: [
        {
          name: '空间活跃度',
          type: 'line',
          smooth: 0.4,
          data: chartRates,
          symbol: 'none',
          itemStyle: { color: '#111827' },
          lineStyle: { width: 4, color: isDark ? '#f9fafb' : '#111827' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: isDark ? 'rgba(249, 250, 251, 0.15)' : 'rgba(17, 24, 39, 0.1)' },
              { offset: 1, color: isDark ? 'rgba(249, 250, 251, 0)' : 'rgba(17, 24, 39, 0)' }
            ])
          }
        }
      ]
    };
    lineChart.setOption(lineOption);
  }

  if (pieChartRef.value) {
    if (!pieChart) pieChart = echarts.init(pieChartRef.value);

    const devRes = await apiGetDevices({ pageSize: 10000 });
    let normal = 0, fault = 0, repairing = 0, scrapped = 0;
    if (devRes.code === 200) {
      devRes.data.records.forEach(d => {
        if (d.status === 'normal') normal++;
        else if (d.status === 'fault') fault++;
        else if (d.status === 'repairing') repairing++;
        else if (d.status === 'scrapped') scrapped++;
      });
    } else {
      normal = 20; fault = 2; repairing = 1; scrapped = 0;
    }

    const pieOption = {
      tooltip: {
        trigger: 'item',
        backgroundColor: isDark ? '#1f2937' : '#ffffff',
        borderColor: axisColor,
        textStyle: { color: isDark ? '#f9fafb' : '#111827' },
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        bottom: '0%',
        left: 'center',
        icon: 'circle',
        itemWidth: 8,
        textStyle: { color: textColor }
      },
      series: [
        {
          name: '终端状态',
          type: 'pie',
          radius: ['55%', '80%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderColor: isDark ? '#121212' : '#ffffff',
            borderWidth: 4
          },
          label: { show: false },
          data: [
            { value: normal, name: '正常', itemStyle: { color: '#111827' } },
            { value: repairing, name: '维修', itemStyle: { color: '#f59e0b' } },
            { value: fault, name: '故障', itemStyle: { color: '#ef4444' } },
            { value: scrapped, name: '报废', itemStyle: { color: '#e5e7eb' } }
          ]
        }
      ]
    };
    pieChart.setOption(pieOption);
  }
};

const handleResize = () => {
  lineChart?.resize();
  pieChart?.resize();
};

watch(() => userStore.theme, () => { updateCharts(); });

onMounted(() => {
  loadStats();
  loadTodayTable();
  updateCharts();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  lineChart?.dispose();
  lineChart = null;
  pieChart?.dispose();
  pieChart = null;
});
</script>

<style scoped>
.welcome-header {
  margin-bottom: 40px;
}
.welcome-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-main);
  letter-spacing: -0.5px;
  margin-bottom: 8px;
}
.subtitle {
  font-size: 15px;
  color: var(--text-secondary);
}

.metric-row {
  margin-bottom: 24px;
}
.metric-card {
  cursor: pointer;
  padding: 8px;
}
.metric-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.metric-title {
  font-size: 13px;
  color: var(--text-regular);
  font-weight: 500;
  margin-bottom: 8px;
  display: block;
}
.metric-value {
  font-size: 32px;
}
.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.chart-row {
  margin-bottom: 24px;
}
.chart-box {
  height: 340px;
  width: 100%;
}

.room-name {
  font-weight: 600;
  color: var(--text-main);
}
.time-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.section-text {
  font-weight: 500;
  color: var(--text-main);
}
.time-tip {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
