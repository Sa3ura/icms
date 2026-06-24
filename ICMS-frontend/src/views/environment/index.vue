<template>
  <div class="environment-container page-container">
    <!-- 过滤选择 -->
    <el-card
      shadow="never"
      class="filter-card"
    >
      <el-form
        :inline="true"
        class="demo-form-inline"
      >
        <el-form-item label="监控教室">
          <el-select
            v-model="selectedClassroomId"
            placeholder="选择监测教室"
            style="width: 200px"
            @change="loadEnvData"
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
            icon="Refresh"
            @click="loadEnvData"
          >
            刷新数据
          </el-button>
        </el-form-item>
        <el-form-item class="right-alert">
          <el-tag
            type="success"
            effect="dark"
            class="realtime-tag"
          >
            <el-icon class="is-loading">
              <Loading />
            </el-icon> 实时传输中
          </el-tag>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 实时数值卡片 -->
    <el-row
      v-loading="loading"
      :gutter="20"
      class="gauge-row"
    >
      <el-col
        v-for="metric in environmentMetrics"
        :key="metric.label"
        :xs="12"
        :sm="8"
        :md="4"
      >
        <el-card
          shadow="never"
          class="gauge-card"
        >
          <div class="gauge-content">
            <span class="gauge-label">{{ metric.label }}</span>
            <div
              class="gauge-value"
              :style="{ color: metric.color }"
            >
              {{ metric.value }} <span class="gauge-unit">{{ metric.unit }}</span>
            </div>
            <div class="gauge-status-row">
              <el-tag
                size="small"
                :type="metric.statusType"
              >
                {{ metric.status }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图表分析 -->
    <el-row
      :gutter="20"
      class="chart-row"
    >
      <el-col
        :xs="24"
        :md="12"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <div class="card-header-flex">
              <span>温湿度变化曲线 (近12小时)</span>
            </div>
          </template>
          <div
            ref="tempHumChartRef"
            class="chart-box"
          />
        </el-card>
      </el-col>

      <el-col
        :xs="24"
        :md="12"
      >
        <el-card
          shadow="never"
          class="chart-card"
        >
          <template #header>
            <div class="card-header-flex">
              <span>空气质量走势 (CO₂ / PM2.5)</span>
            </div>
          </template>
          <div
            ref="airChartRef"
            class="chart-box"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史记录表格 -->
    <el-card
      shadow="never"
      class="table-card"
    >
      <template #header>
        <div class="card-header-flex">
          <span>历史监测点数据归档记录</span>
        </div>
      </template>
      <el-table
        :data="historyData"
        style="width: 100%"
      >
        <el-table-column
          prop="time"
          label="采样时间"
          width="180"
        />
        <el-table-column
          prop="temperature"
          label="温度 (℃)"
        >
          <template #default="{ row }">
            {{ row.temperature }} ℃
          </template>
        </el-table-column>
        <el-table-column
          prop="humidity"
          label="湿度 (%RH)"
        >
          <template #default="{ row }">
            {{ row.humidity }} %
          </template>
        </el-table-column>
        <el-table-column
          prop="co2"
          label="CO₂ (ppm)"
        >
          <template #default="{ row }">
            {{ row.co2 }} ppm
          </template>
        </el-table-column>
        <el-table-column
          prop="pm25"
          label="PM2.5 (μg/m³)"
        >
          <template #default="{ row }">
            {{ row.pm25 }} μg
          </template>
        </el-table-column>
        <el-table-column
          prop="light"
          label="光照强度 (Lux)"
        >
          <template #default="{ row }">
            {{ row.light }} Lux
          </template>
        </el-table-column>
        <el-table-column
          prop="noise"
          label="环境噪音 (dB)"
        >
          <template #default="{ row }">
            {{ row.noise }} dB
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useUserStore } from '../../store/user';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts/core';
import { LineChart, BarChart } from 'echarts/charts';
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components';
import { SVGRenderer } from 'echarts/renderers';

echarts.use([LineChart, BarChart, TooltipComponent, GridComponent, LegendComponent, SVGRenderer]);
import { apiGetClassrooms, apiGetEnvironmentData } from '../../api';

const userStore = useUserStore();

const loading = ref(false);
const classrooms = ref([]);
const selectedClassroomId = ref(1);

const currentEnv = ref({
  temperature: 24.2,
  humidity: 55.4,
  co2: 620,
  pm25: 18.5,
  light: 450,
  noise: 45.2
});

const trendList = ref([]);

// 历史数据表 ( trend 加上具体日期拼接即可 )
const historyData = computed(() => {
  const todayStr = new Date().toISOString().slice(0, 10);
  return trendList.value.map(t => ({
    time: todayStr + ' ' + t.time + ':00',
    ...t
  })).reverse(); // 倒序排列，最新在最前
});

// 图表 Ref 引用
const tempHumChartRef = ref(null);
const airChartRef = ref(null);
let tempHumChart = null;
let airChart = null;

// 指标状态判定
const environmentMetrics = computed(() => {
  const data = currentEnv.value;
  return [
    { 
      label: '室内温度', 
      value: data.temperature, 
      unit: '℃', 
      color: '#3b82f6',
      status: data.temperature >= 18 && data.temperature <= 26 ? '舒适' : '偏高/偏低',
      statusType: data.temperature >= 18 && data.temperature <= 26 ? 'success' : 'warning'
    },
    { 
      label: '空气湿度', 
      value: data.humidity, 
      unit: '%RH', 
      color: '#10b981',
      status: data.humidity >= 40 && data.humidity <= 70 ? '舒适' : '干燥/潮湿',
      statusType: data.humidity >= 40 && data.humidity <= 70 ? 'success' : 'warning'
    },
    { 
      label: '二氧化碳', 
      value: data.co2, 
      unit: 'ppm', 
      color: '#f59e0b',
      status: data.co2 <= 800 ? '优良' : (data.co2 <= 1000 ? '温和' : '超标'),
      statusType: data.co2 <= 800 ? 'success' : (data.co2 <= 1000 ? 'warning' : 'danger')
    },
    { 
      label: 'PM 2.5', 
      value: data.pm25, 
      unit: 'μg/m³', 
      color: '#ef4444',
      status: data.pm25 <= 35 ? '优良' : (data.pm25 <= 75 ? '轻度' : '重度'),
      statusType: data.pm25 <= 35 ? 'success' : (data.pm25 <= 75 ? 'warning' : 'danger')
    },
    { 
      label: '光照强度', 
      value: data.light, 
      unit: 'Lux', 
      color: '#a855f7',
      status: data.light >= 300 ? '明亮' : '较暗',
      statusType: data.light >= 300 ? 'success' : 'info'
    },
    { 
      label: '环境噪音', 
      value: data.noise, 
      unit: 'dB', 
      color: '#64748b',
      status: data.noise <= 55 ? '安静' : '嘈杂',
      statusType: data.noise <= 55 ? 'success' : 'warning'
    }
  ];
});

// 加载温湿度与空气质量图表
const drawCharts = () => {
  const isDark = userStore.theme === 'dark';
  const textColor = isDark ? '#cbd5e1' : '#475569';
  const borderColor = isDark ? '#334155' : '#e2e8f0';
  const tooltipBg = isDark ? '#1f2937' : '#ffffff';

  const timeData = trendList.value.map(t => t.time);

  // 1. 温湿度图表
  if (tempHumChartRef.value) {
    if (!tempHumChart) {
      tempHumChart = echarts.init(tempHumChartRef.value);
    }
    const option = {
      tooltip: {
        trigger: 'axis',
        backgroundColor: tooltipBg,
        borderColor: borderColor
      },
      legend: { textStyle: { color: textColor } },
      grid: { left: '3%', right: '4%', bottom: '3%', outerBounds: false },
      xAxis: {
        type: 'category',
        data: timeData,
        axisLine: { lineStyle: { color: borderColor } },
        axisLabel: { color: textColor }
      },
      yAxis: [
        {
          type: 'value',
          name: '温度 (℃)',
          axisLabel: { color: textColor },
          splitLine: { lineStyle: { color: borderColor } }
        },
        {
          type: 'value',
          name: '湿度 (%RH)',
          axisLabel: { color: textColor },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '温度',
          type: 'line',
          data: trendList.value.map(t => t.temperature),
          itemStyle: { color: isDark ? '#60a5fa' : '#3b82f6' },
          smooth: true
        },
        {
          name: '湿度',
          type: 'line',
          yAxisIndex: 1,
          data: trendList.value.map(t => t.humidity),
          itemStyle: { color: isDark ? '#34d399' : '#10b981' },
          smooth: true
        }
      ]
    };
    tempHumChart.setOption(option);
  }

  // 2. 空气质量走势
  if (airChartRef.value) {
    if (!airChart) {
      airChart = echarts.init(airChartRef.value);
    }
    const option = {
      tooltip: {
        trigger: 'axis',
        backgroundColor: tooltipBg,
        borderColor: borderColor
      },
      legend: { textStyle: { color: textColor } },
      grid: { left: '3%', right: '4%', bottom: '3%', outerBounds: false },
      xAxis: {
        type: 'category',
        data: timeData,
        axisLine: { lineStyle: { color: borderColor } },
        axisLabel: { color: textColor }
      },
      yAxis: [
        {
          type: 'value',
          name: 'CO₂ (ppm)',
          axisLabel: { color: textColor },
          splitLine: { lineStyle: { color: borderColor } }
        },
        {
          type: 'value',
          name: 'PM2.5 (μg)',
          axisLabel: { color: textColor },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: 'CO₂ 浓度',
          type: 'bar',
          data: trendList.value.map(t => t.co2),
          itemStyle: { color: isDark ? '#fcd34d' : '#f59e0b' }
        },
        {
          name: 'PM 2.5',
          type: 'line',
          yAxisIndex: 1,
          data: trendList.value.map(t => t.pm25),
          itemStyle: { color: isDark ? '#f87171' : '#ef4444' },
          smooth: true
        }
      ]
    };
    airChart.setOption(option);
  }
};

const loadEnvData = async () => {
  loading.value = true;
  try {
    const res = await apiGetEnvironmentData(selectedClassroomId.value);
    if (res.code === 200) {
      currentEnv.value = res.data.current;
      trendList.value = res.data.trend;
      drawCharts();
    }
  } catch (error) {
    ElMessage.error('获取教室环境状态异常');
  } finally {
    loading.value = false;
  }
};

const handleResize = () => {
  tempHumChart?.resize();
  airChart?.resize();
};

watch(() => userStore.theme, () => {
  drawCharts();
});

onMounted(async () => {
  const roomRes = await apiGetClassrooms({ pageSize: 10000 });
  if (roomRes.code === 200 && roomRes.data.records.length > 0) {
    classrooms.value = roomRes.data.records;
    selectedClassroomId.value = roomRes.data.records[0].id;
    await loadEnvData();
  }
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  tempHumChart?.dispose();
  tempHumChart = null;
  airChart?.dispose();
  airChart = null;
});
</script>

<style scoped>
.filter-card {
  margin-bottom: 20px;
}

.right-alert {
  float: right;
  margin-right: 0 !important;
}

.realtime-tag {
  border-radius: 20px !important;
  padding: 0 16px;
  font-weight: bold;
}

.gauge-row {
  margin-bottom: 20px;
}

.gauge-card {
  text-align: center;
}

.gauge-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.gauge-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

.gauge-value {
  font-size: 24px;
  font-weight: 700;
  margin-top: 6px;
}

.gauge-unit {
  font-size: 13px;
  font-weight: 500;
}

.gauge-status-row {
  margin-top: 6px;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-box {
  height: 300px;
  width: 100%;
}

.table-card {
  margin-bottom: 20px;
}
</style>
