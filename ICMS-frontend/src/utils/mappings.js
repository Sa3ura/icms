export const getRoomTypeLabel = (type) => {
  const mapping = { normal: '普通教室', multimedia: '多媒体教室', lab: '实验室' };
  return mapping[type] || type;
};

export const getRoomTypeTag = (type) => {
  const mapping = { normal: 'info', multimedia: 'primary', lab: 'warning' };
  return mapping[type] || 'info';
};

export const getClassroomStatusLabel = (status) => {
  const mapping = { available: '可用', busy: '使用中', maintenance: '维护中', deactivated: '已停用' };
  return mapping[status] || status;
};

export const getClassroomStatusTag = (status) => {
  const mapping = { available: 'success', busy: 'primary', maintenance: 'warning', deactivated: 'danger' };
  return mapping[status] || 'info';
};

export const getDeviceStatusLabel = (status) => {
  const mapping = { normal: '正常运转', fault: '发生故障', repairing: '正在维修', scrapped: '已报废' };
  return mapping[status] || status;
};

export const getDeviceStatusTag = (status) => {
  const mapping = { normal: 'success', fault: 'danger', repairing: 'warning', scrapped: 'info' };
  return mapping[status] || 'info';
};

export const getDeviceTypeLabel = (type) => {
  const mapping = {
    projector: '投影仪',
    whiteboard: '电子白板',
    air_conditioner: '空调',
    camera: '摄像头',
    controller: '中控主机',
    lock: '智能门禁'
  };
  return mapping[type] || type;
};

export const getDeviceTypeTag = (type) => {
  const mapping = {
    projector: 'primary',
    whiteboard: 'success',
    air_conditioner: 'info',
    camera: 'warning',
    controller: 'danger',
    lock: 'warning'
  };
  return mapping[type] || 'info';
};

export const getReservationStatusLabel = (status) => {
  const mapping = { pending: '待审批', approved: '已通过', rejected: '已驳回', cancelled: '已取消' };
  return mapping[status] || status;
};

export const getReservationStatusTag = (status) => {
  const mapping = { pending: 'warning', approved: 'success', rejected: 'danger', cancelled: 'info' };
  return mapping[status] || 'info';
};

export const getWorkOrderStatusLabel = (status) => {
  const mapping = {
    submitted: '已提交',
    assigned: '已派单',
    processing: '处理中',
    finished: '已修复',
    closed: '已关闭'
  };
  return mapping[status] || status;
};

export const getWorkOrderStatusTag = (status) => {
  const mapping = {
    submitted: 'info',
    assigned: 'primary',
    processing: 'warning',
    finished: 'success',
    closed: 'info'
  };
  return mapping[status] || 'info';
};

export const getFaultLevelLabel = (level) => {
  const mapping = { normal: '普通', urgent: '紧急', severe: '严重' };
  return mapping[level] || level;
};

export const getFaultLevelTag = (level) => {
  const mapping = { normal: 'info', urgent: 'warning', severe: 'danger' };
  return mapping[level] || 'info';
};

export const getRoleLabel = (type) => {
  const mapping = {
    admin: '系统管理员',
    jw: '教务管理员',
    teacher: '教师',
    student: '学生',
    repairer: '维修人员'
  };
  return mapping[type] || type;
};

export const getRoleTag = (type) => {
  const mapping = {
    admin: 'danger',
    jw: 'warning',
    teacher: 'primary',
    student: 'success',
    repairer: 'info'
  };
  return mapping[type] || 'info';
};

export const getMethodTag = (method) => {
  const mapping = { GET: 'info', POST: 'success', PUT: 'warning', DELETE: 'danger' };
  return mapping[method] || 'info';
};

export const getWeekdayLabel = (day) => {
  const mapping = { 1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日' };
  return mapping[day] || '';
};

export const getSectionTime = (start, end) => {
  const times = {
    1: '08:00', 2: '09:35',
    3: '10:05', 4: '11:40',
    5: '14:00', 6: '15:35',
    7: '16:05', 8: '17:40',
    9: '19:00', 10: '20:35',
    11: '21:05', 12: '22:00'
  };
  return `${times[start] || ''}-${times[end] || ''}`;
};
