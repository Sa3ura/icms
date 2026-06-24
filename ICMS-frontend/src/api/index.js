// ICMS 前端 API 核心对接模块 (支持 Mock 模式 与 真实后端 API 模式 切换)
import * as db from '../db/mockDb';
import axios from 'axios';

// ==================== 模式开关 ====================
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false';

// ==================== 内存 Token 管理 (防 XSS) ====================
let accessToken = null;
let refreshPromise = null;

export const setToken = (token) => { accessToken = token; };
export const hasToken = () => !!accessToken;

const clearAuth = () => {
  accessToken = null;
  localStorage.removeItem('icms_token');
  localStorage.removeItem('icms_user_info');
};

// ==================== Axios 真实请求实例配置 ====================
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 5000,
  withCredentials: true
});

request.interceptors.request.use(
  (config) => {
    if (accessToken) {
      config.headers['Authorization'] = accessToken;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const originalRequest = error.config;
    const status = error.response?.status;

    if (status === 401 && !originalRequest._retry && accessToken) {
      originalRequest._retry = true;

      if (!refreshPromise) {
        refreshPromise = request.post('/api/auth/refresh')
          .then(res => {
            if (res.code === 200 && res.data.token) {
              accessToken = res.data.token;
              localStorage.setItem('icms_user_info', JSON.stringify(res.data.userInfo));
              return res.data.token;
            }
            throw new Error('refresh_failed');
          })
          .catch(() => {
            clearAuth();
            window.location.hash = '#/login';
            throw new Error('refresh_failed');
          })
          .finally(() => {
            refreshPromise = null;
          });
      }

      try {
        const newToken = await refreshPromise;
        originalRequest.headers['Authorization'] = newToken;
        return request(originalRequest);
      } catch {
        return Promise.reject(new Error('认证已过期，请重新登录'));
      }
    }

    console.error('API Request Error:', error);
    const msg = error.response?.data?.message || '服务器连接异常，请检查后端服务是否启动';
    return Promise.reject(new Error(msg));
  }
);

export { request };

// 模拟延迟辅助函数
const mockRequest = async (action, dataCallback) => {
  await db.delay(200);
  try {
    const result = dataCallback();
    return {
      code: 200,
      message: '操作成功',
      data: result
    };
  } catch (error) {
    console.error(`Mock API Error (${action}):`, error);
    return {
      code: 500,
      message: error.message || '系统繁忙，请稍后再试'
    };
  }
};

// 模拟写入操作日志
const logOperation = (username, operation, method, url, params, status, errorMsg = null) => {
  db.insert('operation_logs', {
    username,
    operation,
    request_method: method,
    request_url: url,
    request_ip: '127.0.0.1',
    request_params: JSON.stringify(params),
    result_status: status,
    error_message: errorMsg
  });
};

// ==================== 验证码 ====================
export const apiGetCaptcha = async () => {
  if (!USE_MOCK) {
    const res = await axios.get(import.meta.env.VITE_API_BASE_URL + '/api/auth/captcha', {
      withCredentials: true,
      responseType: 'text'
    });
    const text = res.data;
    const match = text.match(/<!--([a-f0-9]+)-->/);
    return match ? { captchaId: match[1], svg: text } : null;
  }
  return null;
};

// ==================== 1. 登录认证 ====================
export const apiLogin = (username, password, captchaId, captchaCode) => {
  if (!USE_MOCK) {
    return request.post('/api/auth/login', { username, password, captchaId, captchaCode });
  }
  
  return mockRequest('login', () => {
    const users = db.getTable('users');
    const user = users.find(u => u.username === username);
    if (!user) {
      logOperation(username, '用户登录', 'POST', '/api/auth/login', { username }, 'fail', '用户不存在');
      throw new Error('用户名或密码错误');
    }
    if (user.password && user.password !== password) {
      logOperation(username, '用户登录', 'POST', '/api/auth/login', { username }, 'fail', '密码错误');
      throw new Error('用户名或密码错误');
    }
    if (user.status !== 1) {
      logOperation(username, '用户登录', 'POST', '/api/auth/login', { username }, 'fail', '账号被禁用');
      throw new Error('该账号已被禁用，请联系管理员');
    }
    const token = 'mock_jwt_token_' + user.username + '_' + Date.now();
    logOperation(username, '用户登录', 'POST', '/api/auth/login', { username }, 'success');
    return {
      token,
      userInfo: {
        id: user.id,
        username: user.username,
        real_name: user.real_name,
        user_type: user.user_type,
        phone: user.phone,
        email: user.email
      }
    };
  });
};

export const apiChangePassword = (userId, oldPassword, newPassword, username) => {
  if (!USE_MOCK) {
    return request.post(`/api/users/${userId}/change-pwd`, { oldPassword, newPassword });
  }

  return mockRequest('changePassword', () => {
    const user = db.getById('users', userId);
    if (!user) throw new Error('用户不存在');
    if (user.password && user.password !== oldPassword) throw new Error('当前密码错误');
    if (!newPassword || newPassword.length < 6) throw new Error('新密码长度不能少于6位');
    db.update('users', userId, { password: newPassword });
    logOperation(username, '修改登录密码', 'POST', `/api/users/${userId}/change-pwd`, {}, 'success');
    return true;
  });
};

// ==================== 2. 教室管理 ====================
export const apiGetClassrooms = (query = {}) => {
  if (!USE_MOCK) {
    return request.get('/api/classrooms', { params: query });
  }
  
  return mockRequest('getClassrooms', () => {
    let classrooms = db.getTable('classrooms').filter(c => !c.deleted);
    if (query.campus_id) classrooms = classrooms.filter(c => Number(c.campus_id) === Number(query.campus_id));
    if (query.building_id) classrooms = classrooms.filter(c => Number(c.building_id) === Number(query.building_id));
    if (query.room_type) classrooms = classrooms.filter(c => c.room_type === query.room_type);
    if (query.status) classrooms = classrooms.filter(c => c.status === query.status);
    if (query.min_capacity) classrooms = classrooms.filter(c => c.capacity >= Number(query.min_capacity));
    if (query.keyword) {
      const kw = query.keyword.toLowerCase();
      classrooms = classrooms.filter(c => c.name.toLowerCase().includes(kw) || c.room_no.toLowerCase().includes(kw));
    }
    return classrooms;
  });
};

export const apiSaveClassroom = (classroom, username) => {
  if (!USE_MOCK) {
    return request.post('/api/classrooms', classroom);
  }
  
  return mockRequest('saveClassroom', () => {
    const table = db.getTable('classrooms').filter(c => !c.deleted);
    const conflict = table.find(c => 
      Number(c.building_id) === Number(classroom.building_id) && 
      c.room_no === classroom.room_no && 
      Number(c.id) !== Number(classroom.id)
    );
    if (conflict) {
      throw new Error(`同楼栋下已存在编号为 ${classroom.room_no} 的教室`);
    }

    let result;
    if (classroom.id) {
      result = db.update('classrooms', classroom.id, classroom);
      logOperation(username, '修改教室', 'PUT', `/api/classrooms/${classroom.id}`, classroom, 'success');
    } else {
      result = db.insert('classrooms', classroom);
      logOperation(username, '新增教室', 'POST', '/api/classrooms', classroom, 'success');
    }
    return result;
  });
};

export const apiDeleteClassroom = (id, username) => {
  if (!USE_MOCK) {
    return request.delete(`/api/classrooms/${id}`);
  }
  
  return mockRequest('deleteClassroom', () => {
    const result = db.remove('classrooms', id, true);
    logOperation(username, '删除教室', 'DELETE', `/api/classrooms/${id}`, { id }, 'success');
    return result;
  });
};

export const apiGetCampuses = () => {
  if (!USE_MOCK) {
    return request.get('/api/campuses');
  }

  return mockRequest('getCampuses', () => {
    return db.getTable('campuses').filter(c => !c.deleted);
  });
};

export const apiGetBuildings = () => {
  if (!USE_MOCK) {
    return request.get('/api/buildings');
  }

  return mockRequest('getBuildings', () => {
    return db.getTable('buildings').filter(b => !b.deleted);
  });
};

// ==================== 3. 空闲教室与预约冲突检测 ====================
export const apiGetFreeClassrooms = (params = {}) => {
  if (!USE_MOCK) {
    return request.get('/api/classrooms/free', { params });
  }
  
  return mockRequest('getFreeClassrooms', () => {
    const { date, startSection, endSection, campus_id, minCapacity, devices } = params;
    if (!date || !startSection || !endSection) {
      throw new Error('预约日期和节次为必填项');
    }

    let classrooms = db.getTable('classrooms').filter(c => !c.deleted && c.status === 'available');
    if (campus_id) classrooms = classrooms.filter(c => Number(c.campus_id) === Number(campus_id));
    if (minCapacity) classrooms = classrooms.filter(c => c.capacity >= Number(minCapacity));

    if (devices && devices.length > 0) {
      const allDevices = db.getTable('devices').filter(d => !d.deleted && d.status === 'normal');
      classrooms = classrooms.filter(c => {
        const roomDevices = allDevices.filter(d => Number(d.classroom_id) === Number(c.id));
        return devices.every(devType => roomDevices.some(d => d.device_type === devType));
      });
    }

    const activeReservations = db.getTable('classroom_reservations').filter(r => 
      !r.deleted && r.reservation_date === date && r.status === 'approved'
    );
    
    const dayOfWeek = new Date(date).getDay() || 7;
    const activeSchedules = db.getTable('course_schedules').filter(s => 
      !s.deleted && s.weekday === dayOfWeek
    );

    return classrooms.filter(c => {
      const hasScheduleConflict = activeSchedules.some(s => 
        Number(s.classroom_id) === Number(c.id) &&
        Number(startSection) <= Number(s.end_section) && 
        Number(endSection) >= Number(s.start_section)
      );
      if (hasScheduleConflict) return false;

      const hasResConflict = activeReservations.some(r => 
        Number(r.classroom_id) === Number(c.id) &&
        Number(startSection) <= Number(r.end_section) && 
        Number(endSection) >= Number(r.start_section)
      );
      if (hasResConflict) return false;

      return true;
    });
  });
};

export const apiCheckConflict = (classroom_id, date, startSection, endSection) => {
  if (!USE_MOCK) {
    return request.get('/api/classrooms/check-conflict', {
      params: { classroom_id, date, startSection, endSection }
    });
  }
  
  return mockRequest('checkConflict', () => {
    const dayOfWeek = new Date(date).getDay() || 7;
    const schedules = db.getTable('course_schedules').filter(s => 
      !s.deleted && 
      Number(s.classroom_id) === Number(classroom_id) &&
      s.weekday === dayOfWeek &&
      Number(startSection) <= Number(s.end_section) && 
      Number(endSection) >= Number(s.start_section)
    );
    if (schedules.length > 0) {
      return { conflict: true, reason: `该时段已有课程：${schedules[0].course_name} (${schedules[0].class_name || ''})` };
    }

    const reservations = db.getTable('classroom_reservations').filter(r => 
      !r.deleted && 
      Number(r.classroom_id) === Number(classroom_id) &&
      r.reservation_date === date &&
      r.status === 'approved' &&
      Number(startSection) <= Number(r.end_section) && 
      Number(endSection) >= Number(r.start_section)
    );
    if (reservations.length > 0) {
      return { conflict: true, reason: `该时段已被预约通过：${reservations[0].purpose} (申请人: ${reservations[0].applicant_name})` };
    }

    return { conflict: false };
  });
};

// ==================== 4. 预约管理 ====================
export const apiGetReservations = (query = {}) => {
  if (!USE_MOCK) {
    return request.get('/api/reservations', { params: query });
  }
  
  return mockRequest('getReservations', () => {
    let list = db.getTable('classroom_reservations').filter(r => !r.deleted);
    if (query.applicant_id) list = list.filter(r => Number(r.applicant_id) === Number(query.applicant_id));
    if (query.status) list = list.filter(r => r.status === query.status);
    if (query.classroom_id) list = list.filter(r => Number(r.classroom_id) === Number(query.classroom_id));
    if (query.date_range && query.date_range.length === 2) {
      const [start, end] = query.date_range;
      list = list.filter(r => r.reservation_date >= start && r.reservation_date <= end);
    }
    return list.sort((a, b) => b.id - a.id);
  });
};

export const apiCreateReservation = (reservation, username) => {
  if (!USE_MOCK) {
    return request.post('/api/reservations', reservation);
  }
  
  return mockRequest('createReservation', () => {
    const { classroom_id, reservation_date, start_section, end_section, applicant_id } = reservation;
    const dayOfWeek = new Date(reservation_date).getDay() || 7;
    const scheduleConflict = db.getTable('course_schedules').some(s => 
      !s.deleted && Number(s.classroom_id) === Number(classroom_id) &&
      s.weekday === dayOfWeek && Number(start_section) <= Number(s.end_section) && Number(end_section) >= Number(s.start_section)
    );
    if (scheduleConflict) throw new Error('提交失败：该时段已有课程冲突');

    const resConflict = db.getTable('classroom_reservations').some(r => 
      !r.deleted && Number(r.classroom_id) === Number(classroom_id) &&
      r.reservation_date === reservation_date && r.status === 'approved' &&
      Number(start_section) <= Number(r.end_section) && Number(end_section) >= Number(r.start_section)
    );
    if (resConflict) throw new Error('提交失败：该时段已被其他预约通过');

    const user = db.getById('users', applicant_id);
    const classroom = db.getById('classrooms', classroom_id);

    const record = db.insert('classroom_reservations', {
      ...reservation,
      applicant_name: user ? user.real_name : '未知用户',
      classroom_name: classroom ? classroom.name : '未知教室',
      status: 'pending',
      approval_remark: null
    });
    logOperation(username, '申请预约教室', 'POST', '/api/reservations', record, 'success');
    return record;
  });
};

export const apiApproveReservation = (id, status, remark, approverId, username) => {
  if (!USE_MOCK) {
    return request.post(`/api/reservations/${id}/approve`, { status, remark, approverId });
  }
  
  return mockRequest('approveReservation', () => {
    const record = db.getById('classroom_reservations', id);
    if (!record) throw new Error('预约记录不存在');
    
    const updated = db.update('classroom_reservations', id, { status, approval_remark: remark });
    db.insert('approval_records', {
      business_type: 'reservation', business_id: id, approver_id: approverId,
      action: status === 'approved' ? 'approve' : 'reject', comment: remark
    });
    logOperation(username, `审批预约教室 (${status === 'approved' ? '同意' : '拒绝'})`, 'POST', `/api/reservations/${id}/approve`, { id, status, remark }, 'success');
    return updated;
  });
};

export const apiCancelReservation = (id, username) => {
  if (!USE_MOCK) {
    return request.post(`/api/reservations/${id}/cancel`);
  }
  
  return mockRequest('cancelReservation', () => {
    const updated = db.update('classroom_reservations', id, { status: 'cancelled' });
    logOperation(username, '取消教室预约', 'POST', `/api/reservations/${id}/cancel`, { id }, 'success');
    return updated;
  });
};

// ==================== 5. 设备管理 ====================
export const apiGetDevices = (query = {}) => {
  if (!USE_MOCK) {
    return request.get('/api/devices', { params: query });
  }
  
  return mockRequest('getDevices', () => {
    let list = db.getTable('devices').filter(d => !d.deleted);
    if (query.classroom_id) list = list.filter(d => Number(d.classroom_id) === Number(query.classroom_id));
    if (query.status) list = list.filter(d => d.status === query.status);
    if (query.device_type) list = list.filter(d => d.device_type === query.device_type);
    if (query.keyword) {
      const kw = query.keyword.toLowerCase();
      list = list.filter(d => d.device_name.toLowerCase().includes(kw) || d.device_code.toLowerCase().includes(kw));
    }
    const classrooms = db.getTable('classrooms');
    return list.map(device => {
      const room = classrooms.find(c => Number(c.id) === Number(device.classroom_id));
      return { ...device, classroom_name: room ? room.name : '未分配' };
    });
  });
};

export const apiSaveDevice = (device, username) => {
  if (!USE_MOCK) {
    return request.post('/api/devices', device);
  }
  
  return mockRequest('saveDevice', () => {
    const table = db.getTable('devices').filter(d => !d.deleted);
    const conflict = table.find(d => d.device_code === device.device_code && Number(d.id) !== Number(device.id));
    if (conflict) throw new Error(`设备编码 ${device.device_code} 已存在`);

    let result;
    if (device.id) {
      result = db.update('devices', device.id, device);
      logOperation(username, '修改设备档案', 'PUT', `/api/devices/${device.id}`, device, 'success');
    } else {
      result = db.insert('devices', device);
      logOperation(username, '新增设备档案', 'POST', '/api/devices', device, 'success');
    }
    return result;
  });
};

export const apiDeleteDevice = (id, username) => {
  if (!USE_MOCK) {
    return request.delete(`/api/devices/${id}`);
  }
  
  return mockRequest('deleteDevice', () => {
    const result = db.remove('devices', id, true);
    logOperation(username, '删除设备', 'DELETE', `/api/devices/${id}`, { id }, 'success');
    return result;
  });
};

// ==================== 6. 报修与工单 ====================
export const apiGetWorkOrders = (query = {}) => {
  if (!USE_MOCK) {
    return request.get('/api/work-orders', { params: query });
  }
  
  return mockRequest('getWorkOrders', () => {
    let list = db.getTable('work_orders').filter(w => !w.deleted);
    if (query.reporter_id) list = list.filter(w => Number(w.reporter_id) === Number(query.reporter_id));
    if (query.handler_id) list = list.filter(w => Number(w.handler_id) === Number(query.handler_id));
    if (query.status) list = list.filter(w => w.status === query.status);
    if (query.fault_level) list = list.filter(w => w.fault_level === query.fault_level);
    if (query.classroom_id) list = list.filter(w => Number(w.classroom_id) === Number(query.classroom_id));
    return list.sort((a, b) => b.id - a.id);
  });
};

export const apiCreateWorkOrder = (workOrder, username) => {
  if (!USE_MOCK) {
    return request.post('/api/work-orders', workOrder);
  }
  
  return mockRequest('createWorkOrder', () => {
    const { reporter_id, classroom_id, device_id, fault_title, fault_description, fault_level } = workOrder;
    const user = db.getById('users', reporter_id);
    const room = db.getById('classrooms', classroom_id);
    let deviceName = '整体环境/其他设备';
    
    if (device_id) {
      const device = db.getById('devices', device_id);
      if (device) {
        deviceName = device.device_name;
        db.update('devices', device_id, { status: 'fault' });
      }
    }
    const orderNo = `WO-${Date.now().toString().slice(0, 8)}-${Math.floor(1000 + Math.random() * 9000)}`;
    const record = db.insert('work_orders', {
      order_no: orderNo, reporter_id, reporter_name: user ? user.real_name : '未知报修人',
      classroom_id, classroom_name: room ? room.name : '未知教室', device_id, device_name: deviceName,
      fault_title, fault_description, fault_level, status: 'submitted'
    });
    logOperation(username, '提交故障报修', 'POST', '/api/work-orders', record, 'success');
    return record;
  });
};

export const apiAssignWorkOrder = (id, handlerId, username) => {
  if (!USE_MOCK) {
    return request.post(`/api/work-orders/${id}/assign`, { handlerId });
  }
  
  return mockRequest('assignWorkOrder', () => {
    const handler = db.getById('users', handlerId);
    if (!handler) throw new Error('选中的维修人员不存在');

    const updated = db.update('work_orders', id, { status: 'assigned', handler_id: handlerId, handler_name: handler.real_name });
    const record = db.getById('work_orders', id);
    if (record && record.device_id) db.update('devices', record.device_id, { status: 'repairing' });
    logOperation(username, '派发报修工单', 'POST', `/api/work-orders/${id}/assign`, { id, handlerId }, 'success');
    return updated;
  });
};

export const apiHandleWorkOrder = (id, handleResult, status, username) => {
  if (!USE_MOCK) {
    return request.post(`/api/work-orders/${id}/handle`, { handleResult, status });
  }
  
  return mockRequest('handleWorkOrder', () => {
    const updates = { status, handle_result: handleResult };
    if (status === 'finished') updates.finished_at = new Date().toLocaleString();
    
    const updated = db.update('work_orders', id, updates);
    const record = db.getById('work_orders', id);
    if (record && record.device_id) {
      db.update('devices', record.device_id, { status: status === 'finished' ? 'normal' : 'repairing', last_check_time: new Date().toLocaleString() });
    }
    logOperation(username, `维修人员处理工单 (${status === 'finished' ? '完成' : '处理中'})`, 'POST', `/api/work-orders/${id}/handle`, { id, status, handleResult }, 'success');
    return updated;
  });
};

export const apiCloseWorkOrder = (id, username) => {
  if (!USE_MOCK) {
    return request.post(`/api/work-orders/${id}/close`);
  }
  
  return mockRequest('closeWorkOrder', () => {
    const updated = db.update('work_orders', id, { status: 'closed' });
    logOperation(username, '关闭/归档报修工单', 'POST', `/api/work-orders/${id}/close`, { id }, 'success');
    return updated;
  });
};

// ==================== 7. 环境监测 ====================
export const apiGetEnvironmentData = (classroomId) => {
  if (!USE_MOCK) {
    return request.get('/api/environment/records', { params: { classroomId } });
  }
  
  return mockRequest('getEnvironmentData', () => {
    const cid = classroomId || 1;
    const room = db.getById('classrooms', cid);
    if (!room) throw new Error('未找到教室');

    const getRand = (min, max, decimal = 1) => {
      const rand = Math.random() * (max - min) + min;
      return Number(rand.toFixed(decimal));
    };

    const current = {
      classroom_id: cid, classroom_name: room.name,
      temperature: getRand(18, 28, 1), humidity: getRand(40, 75, 1),
      co2: Math.floor(getRand(400, 950, 0)), pm25: getRand(10, 65, 1),
      light: getRand(150, 600, 0), noise: getRand(30, 70, 1)
    };

    const trend = [];
    const now = new Date();
    for (let i = 11; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 60 * 60 * 1000);
      trend.push({
        time: time.toTimeString().slice(0, 5),
        temperature: Number((current.temperature + getRand(-2, 2, 1)).toFixed(1)),
        humidity: Number((current.humidity + getRand(-5, 5, 1)).toFixed(1)),
        co2: Math.floor(current.co2 + getRand(-80, 80, 0)),
        pm25: Number((current.pm25 + getRand(-10, 10, 1)).toFixed(1)),
        light: Math.max(0, Math.floor(current.light + getRand(-100, 100, 0))),
        noise: Number((current.noise + getRand(-8, 8, 1)).toFixed(1))
      });
    }
    return { current, trend };
  });
};

// ==================== 8. 通知公告 ====================
export const apiGetNotices = (userId) => {
  if (!USE_MOCK) {
    return request.get('/api/notices');
  }

  return mockRequest('getNotices', () => {
    const all = db.getTable('notices').sort((a, b) => b.id - a.id);
    return all.map(n => ({
      ...n,
      is_read: Array.isArray(n.read_by) && n.read_by.includes(Number(userId))
    }));
  });
};

export const apiSaveNotice = (notice, username) => {
  if (!USE_MOCK) {
    return request.post('/api/notices', notice);
  }

  return mockRequest('saveNotice', () => {
    let result;
    if (notice.id) {
      const existing = db.getById('notices', notice.id);
      result = db.update('notices', notice.id, { ...existing, ...notice });
      logOperation(username, '编辑通知公告', 'PUT', `/api/notices/${notice.id}`, notice, 'success');
    } else {
      result = db.insert('notices', {
        ...notice,
        read_by: [],
        publisher_name: username,
        status: 'published'
      });
      logOperation(username, '发布新通知公告', 'POST', '/api/notices', notice, 'success');
    }
    return result;
  });
};

export const apiDeleteNotice = (id, username) => {
  if (!USE_MOCK) {
    return request.delete(`/api/notices/${id}`);
  }

  return mockRequest('deleteNotice', () => {
    const result = db.remove('notices', id, false);
    logOperation(username, '删除通知公告', 'DELETE', `/api/notices/${id}`, { id }, 'success');
    return result;
  });
};

export const apiMarkNoticeRead = (noticeId, userId) => {
  if (!USE_MOCK) {
    return request.put(`/api/notices/${noticeId}/read`, { userId });
  }

  return mockRequest('markNoticeRead', () => {
    const notice = db.getById('notices', noticeId);
    if (!notice) throw new Error('通知不存在');
    const readBy = Array.isArray(notice.read_by) ? [...notice.read_by] : [];
    if (!readBy.includes(Number(userId))) {
      readBy.push(Number(userId));
      db.update('notices', noticeId, { read_by: readBy });
    }
    return true;
  });
};

export const apiMarkAllNoticesRead = (role, userId) => {
  if (!USE_MOCK) {
    return request.put('/api/notices/read-all', { role, userId });
  }

  return mockRequest('markAllNoticesRead', () => {
    const all = db.getTable('notices');
    let count = 0;
    all.forEach(n => {
      if (!n.target_role || n.target_role === role) {
        const readBy = Array.isArray(n.read_by) ? [...n.read_by] : [];
        if (!readBy.includes(Number(userId))) {
          readBy.push(Number(userId));
          db.update('notices', n.id, { read_by: readBy });
          count++;
        }
      }
    });
    return { marked: count };
  });
};

// ==================== 9. 用户与系统管理 ====================
export const apiGetUsers = (query = {}) => {
  if (!USE_MOCK) {
    return request.get('/api/users', { params: query });
  }
  
  return mockRequest('getUsers', () => {
    let list = db.getTable('users').filter(u => !u.deleted);
    if (query.user_type) list = list.filter(u => u.user_type === query.user_type);
    if (query.keyword) {
      const kw = query.keyword.toLowerCase();
      list = list.filter(u => u.username.toLowerCase().includes(kw) || u.real_name.toLowerCase().includes(kw));
    }
    return list;
  });
};

export const apiSaveUser = (user, username) => {
  if (!USE_MOCK) {
    return request.post('/api/users', user);
  }
  
  return mockRequest('saveUser', () => {
    const table = db.getTable('users').filter(u => !u.deleted);
    const conflict = table.find(u => u.username === user.username && Number(u.id) !== Number(user.id));
    if (conflict) throw new Error(`用户名 ${user.username} 已被占用`);

    let result;
    if (user.id) {
      result = db.update('users', user.id, user);
      logOperation(username, '修改用户资料', 'PUT', `/api/users/${user.id}`, user, 'success');
    } else {
      result = db.insert('users', { ...user, status: 1 });
      logOperation(username, '新建系统用户', 'POST', '/api/users', user, 'success');
    }
    return result;
  });
};

export const apiResetPassword = (id, newPassword, username) => {
  if (!USE_MOCK) {
    return request.post(`/api/users/${id}/reset-pwd`, { newPassword });
  }
  
  return mockRequest('resetPassword', () => {
    const result = db.update('users', id, { password: newPassword });
    logOperation(username, '重置用户密码', 'POST', `/api/users/${id}/reset-pwd`, { id }, 'success');
    return result;
  });
};

export const apiGetLogs = () => {
  if (!USE_MOCK) {
    return request.get('/api/users/logs');
  }
  
  return mockRequest('getLogs', () => {
    return db.getTable('operation_logs').sort((a, b) => b.id - a.id);
  });
};

// ==================== 10. 排课管理 ====================
export const apiGetSchedules = (query = {}) => {
  if (!USE_MOCK) {
    return request.get('/api/schedules', { params: query });
  }
  
  return mockRequest('getSchedules', () => {
    let list = db.getTable('course_schedules').filter(s => !s.deleted);
    if (query.classroom_id) list = list.filter(s => Number(s.classroom_id) === Number(query.classroom_id));
    if (query.weekday) list = list.filter(s => Number(s.weekday) === Number(query.weekday));
    if (query.teacher_name) list = list.filter(s => s.teacher_name.includes(query.teacher_name));
    return list;
  });
};

export const apiSaveSchedule = (schedule, username) => {
  if (!USE_MOCK) {
    return request.post('/api/schedules', schedule);
  }
  
  return mockRequest('saveSchedule', () => {
    const { id, classroom_id, weekday, start_section, end_section } = schedule;
    const list = db.getTable('course_schedules').filter(s => !s.deleted && Number(s.classroom_id) === Number(classroom_id) && Number(s.weekday) === Number(weekday) && Number(s.id) !== Number(id));
    const conflict = list.some(s => Number(start_section) <= Number(s.end_section) && Number(end_section) >= Number(s.start_section));
    if (conflict) throw new Error('排课冲突：该教室在此时间段已有排课课程');

    let result;
    if (id) {
      result = db.update('course_schedules', id, schedule);
      logOperation(username, '修改课程排课', 'PUT', `/api/schedules/${id}`, schedule, 'success');
    } else {
      result = db.insert('course_schedules', schedule);
      logOperation(username, '录入新排课', 'POST', '/api/schedules', schedule, 'success');
    }
    return result;
  });
};

export const apiDeleteSchedule = (id, username) => {
  if (!USE_MOCK) {
    return request.delete(`/api/schedules/${id}`);
  }
  
  return mockRequest('deleteSchedule', () => {
    const result = db.remove('course_schedules', id, true);
    logOperation(username, '删除排课记录', 'DELETE', `/api/schedules/${id}`, { id }, 'success');
    return result;
  });
};

// ==================== 11. 仪表盘统计数据 ====================
export const apiGetDashboardStats = () => {
  if (!USE_MOCK) {
    return request.get('/api/stats/dashboard');
  }
  
  return mockRequest('getDashboardStats', () => {
    const classrooms = db.getTable('classrooms').filter(c => !c.deleted);
    const reservations = db.getTable('classroom_reservations').filter(r => !r.deleted);
    const workOrders = db.getTable('work_orders').filter(w => !w.deleted);
    const devices = db.getTable('devices').filter(d => !d.deleted);

    const classroomTotal = classrooms.length;
    const todayStr = new Date().toISOString().slice(0, 10);
    const todayReservations = reservations.filter(r => r.reservation_date === todayStr).length;
    const pendingApprovals = reservations.filter(r => r.status === 'pending').length;
    const pendingWorkOrders = workOrders.filter(w => ['submitted', 'assigned', 'processing'].includes(w.status)).length;
    const faultyDevices = devices.filter(d => d.status === 'fault').length;

    return { classroomTotal, todayReservations, pendingApprovals, pendingWorkOrders, faultyDevices };
  });
};
