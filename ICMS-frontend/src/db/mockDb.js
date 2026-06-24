// ICMS 前端 Mock 数据库 (LocalStorage)

const DB_KEY_PREFIX = 'icms_db_';

// 初始默认数据
const DEFAULT_DATA = {
  users: [
    { id: 1, username: 'admin', password: '123456', real_name: '系统管理员', user_type: 'admin', phone: '13800000001', email: 'admin@icms.edu.cn', status: 1, created_at: '2026-06-01 12:00:00' },
    { id: 2, username: 'jw_admin', password: '123456', real_name: '教务管理员', user_type: 'jw', phone: '13800000002', email: 'jw@icms.edu.cn', status: 1, created_at: '2026-06-01 12:00:00' },
    { id: 3, username: 'teacher_wang', password: '123456', real_name: '王教师', user_type: 'teacher', phone: '13800000003', email: 'wang@icms.edu.cn', status: 1, created_at: '2026-06-02 09:00:00' },
    { id: 4, username: 'student_li', password: '123456', real_name: '李同学', user_type: 'student', phone: '13800000004', email: 'li@icms.edu.cn', status: 1, created_at: '2026-06-02 10:00:00' },
    { id: 5, username: 'repairer_zhang', password: '123456', real_name: '张师傅', user_type: 'repairer', phone: '13800000005', email: 'zhang@icms.edu.cn', status: 1, created_at: '2026-06-03 08:30:00' }
  ],
  campuses: [
    { id: 1, campus_name: '东校区', address: '华盛大道100号', status: 1 },
    { id: 2, campus_name: '西校区', address: '科技路250号', status: 1 }
  ],
  buildings: [
    { id: 1, campus_id: 1, building_name: '第一教学楼', floor_count: 5, status: 1 },
    { id: 2, campus_id: 1, building_name: '实验大楼', floor_count: 6, status: 1 },
    { id: 3, campus_id: 2, building_name: '第二教学楼', floor_count: 4, status: 1 }
  ],
  classrooms: [
    { id: 1, campus_id: 1, building_id: 1, floor_no: 1, room_no: 'A101', name: '第一教学楼A101', capacity: 80, room_type: 'multimedia', status: 'available', open_time: '08:00-22:00', description: '配置高清投影与环绕声系统。' },
    { id: 2, campus_id: 1, building_id: 1, floor_no: 2, room_no: 'A203', name: '第一教学楼A203', capacity: 120, room_type: 'multimedia', status: 'available', open_time: '08:00-22:00', description: '大型多媒体阶梯教室。' },
    { id: 3, campus_id: 1, building_id: 2, floor_no: 3, room_no: 'B305', name: '实验大楼B305', capacity: 50, room_type: 'lab', status: 'available', open_time: '08:30-21:30', description: '计算机软件开发实验室。' },
    { id: 4, campus_id: 2, building_id: 3, floor_no: 1, room_no: 'C102', name: '第二教学楼C102', capacity: 60, room_type: 'normal', status: 'available', open_time: '08:00-21:30', description: '普通多功能教室。' },
    { id: 5, campus_id: 1, building_id: 1, floor_no: 3, room_no: 'A301', name: '第一教学楼A301', capacity: 45, room_type: 'normal', status: 'maintenance', open_time: '08:00-22:00', description: '内部设备升级中，暂停使用。' }
  ],
  devices: [
    { id: 1, classroom_id: 1, device_code: 'DEV-A101-001', device_name: '激光投影仪', device_type: 'projector', brand: 'Epson', model: 'CB-L530U', status: 'normal', purchase_date: '2025-01-10', last_check_time: '2026-06-15 10:00:00' },
    { id: 2, classroom_id: 1, device_code: 'DEV-A101-002', device_name: '壁挂空调', device_type: 'air_conditioner', brand: 'Gree', model: 'KFR-72LW', status: 'normal', purchase_date: '2025-03-12', last_check_time: '2026-06-15 10:30:00' },
    { id: 3, classroom_id: 2, device_code: 'DEV-A203-001', device_name: '交互电子白板', device_type: 'whiteboard', brand: 'MAXHUB', model: 'V6-Classic', status: 'normal', purchase_date: '2025-05-18', last_check_time: '2026-06-16 14:00:00' },
    { id: 4, classroom_id: 2, device_code: 'DEV-A203-002', device_name: '高清云台摄像头', device_type: 'camera', brand: 'Hikvision', model: 'DS-2CD2146G2', status: 'normal', purchase_date: '2025-06-20', last_check_time: '2026-06-16 14:15:00' },
    { id: 5, classroom_id: 3, device_code: 'DEV-B305-001', device_name: '中央控制台', device_type: 'controller', brand: 'Crestron', model: 'MC4', status: 'normal', purchase_date: '2024-09-05', last_check_time: '2026-06-10 11:00:00' },
    { id: 6, classroom_id: 3, device_code: 'DEV-B305-002', device_name: '红外防盗门禁', device_type: 'lock', brand: 'lock', status: 'fault', purchase_date: '2024-09-05', last_check_time: '2026-06-21 09:30:00' }
  ],
  course_schedules: [
    { id: 1, semester: '2025-2026-2', week_no: 1, weekday: 1, start_section: 1, end_section: 2, classroom_id: 1, course_code: 'MATH101', course_name: '高等数学(下)', teacher_id: 3, teacher_name: '王教师', class_name: '软件工程2401', student_count: 65, status: 'published' },
    { id: 2, semester: '2025-2026-2', week_no: 1, weekday: 1, start_section: 3, end_section: 4, classroom_id: 2, course_code: 'ENG201', course_name: '大学英语(II)', teacher_id: 3, teacher_name: '王教师', class_name: '物联网2402', student_count: 90, status: 'published' },
    { id: 3, semester: '2025-2026-2', week_no: 1, weekday: 2, start_section: 5, end_section: 6, classroom_id: 3, course_code: 'CS302', course_name: '操作系统实验', teacher_id: 3, teacher_name: '王教师', class_name: '计科2303', student_count: 42, status: 'published' },
    { id: 4, semester: '2025-2026-2', week_no: 1, weekday: 3, start_section: 7, end_section: 8, classroom_id: 4, course_code: 'HIS105', course_name: '中国近现代史纲要', teacher_id: 3, teacher_name: '王教师', class_name: '智能科学2501', student_count: 55, status: 'published' }
  ],
  classroom_reservations: [
    { id: 1, applicant_id: 3, applicant_name: '王教师', classroom_id: 1, classroom_name: '第一教学楼A101', reservation_date: '2026-06-23', start_section: 5, end_section: 6, purpose: '临时班会与选课指导', attendee_count: 45, contact_phone: '13800000003', status: 'approved', approval_remark: '同意使用，请用后关闭设备。', created_at: '2026-06-21 15:00:00' },
    { id: 2, applicant_id: 4, applicant_name: '李同学', classroom_id: 2, classroom_name: '第一教学楼A203', reservation_date: '2026-06-24', start_section: 7, end_section: 8, purpose: '考研自习与学习小组讨论', attendee_count: 10, contact_phone: '13800000004', status: 'pending', approval_remark: null, created_at: '2026-06-22 10:00:00' }
  ],
  work_orders: [
    { id: 1, order_no: 'WO-20260621-0001', reporter_id: 3, reporter_name: '王教师', classroom_id: 3, classroom_name: '实验大楼B305', device_id: 6, device_name: '红外防盗门禁', fault_title: '实验室门禁刷卡无反应', fault_description: '刷学生卡没有任何蜂鸣声，也无法开门，需手动复位。', fault_level: 'urgent', status: 'assigned', handler_id: 5, handler_name: '张师傅', handle_result: null, finished_at: null, created_at: '2026-06-21 09:45:00' }
  ],
  notices: [
    { id: 1, title: '关于2026年端午节期间教学楼开放时间调整的通知', content: '端午节放假期间（6月20日-6月22日），除第一教学楼正常开放（08:00-18:00）外，其他教学楼闭楼维护。特此通知。', notice_type: 'system', target_role: null, publisher_id: 1, publisher_name: '系统管理员', status: 'published', read_by: [], created_at: '2026-06-18 10:00:00' },
    { id: 2, title: '实验室大功率设备使用安全警告', content: '请各教研室、班级在实验大楼B区使用大功率设备前，务必检查空气开关及地线。下课时请务必断电。', notice_type: 'warning', target_role: 'teacher', publisher_id: 2, publisher_name: '教务管理员', status: 'published', read_by: [], created_at: '2026-06-20 14:30:00' },
    { id: 3, title: '2026秋季学期教室预分配方案已发布', content: '各学院教研室请于6月30日前登录ICMS系统查看教室分配草案并在线确认，逾期将视为无异议自动通过。', notice_type: 'info', target_role: 'jw', publisher_id: 1, publisher_name: '系统管理员', status: 'published', read_by: [], created_at: '2026-06-21 09:00:00' },
    { id: 4, title: '系统维护通知：6月25日凌晨停机升级', content: '服务器将于6月25日（周三）02:00-05:00闭关升级数据库，届时ICMS所有功能暂停服务，请提前保存操作。', notice_type: 'system', target_role: null, publisher_id: 1, publisher_name: '系统管理员', status: 'published', read_by: [], created_at: '2026-06-22 16:00:00' }
  ],
  operation_logs: [
    { id: 1, user_id: 1, username: 'admin', operation: '用户登录', request_method: 'POST', request_url: '/api/auth/login', request_ip: '127.0.0.1', request_params: '{}', result_status: 'success', error_message: null, created_at: '2026-06-22 20:00:00' }
  ]
};

// 模拟延迟辅助函数
export const delay = (ms = 150) => new Promise(resolve => setTimeout(resolve, ms));

// 初始化数据库
export const initDb = () => {
  for (const [key, val] of Object.entries(DEFAULT_DATA)) {
    const storageKey = DB_KEY_PREFIX + key;
    if (!localStorage.getItem(storageKey)) {
      localStorage.setItem(storageKey, JSON.stringify(val));
    }
  }
};

// 获取某张表的所有数据
export const getTable = (tableName) => {
  initDb();
  const raw = localStorage.getItem(DB_KEY_PREFIX + tableName);
  return raw ? JSON.parse(raw) : [];
};

// 保存某张表的所有数据
export const saveTable = (tableName, data) => {
  localStorage.setItem(DB_KEY_PREFIX + tableName, JSON.stringify(data));
};

// 获取单条数据
export const getById = (tableName, id) => {
  const table = getTable(tableName);
  return table.find(item => Number(item.id) === Number(id));
};

// 插入单条数据
export const insert = (tableName, record) => {
  const table = getTable(tableName);
  const nextId = table.length > 0 ? Math.max(...table.map(item => item.id)) + 1 : 1;
  const newRecord = {
    id: nextId,
    created_at: new Date().toLocaleString(),
    ...record
  };
  table.push(newRecord);
  saveTable(tableName, table);
  return newRecord;
};

// 更新单条数据
export const update = (tableName, id, updates) => {
  const table = getTable(tableName);
  const idx = table.findIndex(item => Number(item.id) === Number(id));
  if (idx !== -1) {
    table[idx] = {
      ...table[idx],
      ...updates,
      updated_at: new Date().toLocaleString()
    };
    saveTable(tableName, table);
    return table[idx];
  }
  return null;
};

// 删除单条数据 (物理或逻辑删除)
export const remove = (tableName, id, logical = true) => {
  const table = getTable(tableName);
  if (logical) {
    return update(tableName, id, { deleted: 1 });
  } else {
    const filtered = table.filter(item => Number(item.id) !== Number(id));
    saveTable(tableName, filtered);
    return true;
  }
};

// 查询辅助函数：模拟 SQL where 子句
export const queryTable = (tableName, filterFn) => {
  const table = getTable(tableName);
  return table.filter(item => !item.deleted && filterFn(item));
};
