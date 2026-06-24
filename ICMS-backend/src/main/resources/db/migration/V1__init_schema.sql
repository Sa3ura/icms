-- ============================================================
-- ICMS 智慧教室管理系统 — Flyway 数据库迁移 V1
-- 建表脚本：15 张核心业务表
-- ============================================================

-- 1. 系统用户
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  email VARCHAR(100),
  phone VARCHAR(30),
  user_type VARCHAR(30) DEFAULT 'teacher',
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 系统角色
CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL UNIQUE,
  role_name VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. 系统权限
CREATE TABLE sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0,
  perm_code VARCHAR(100) NOT NULL UNIQUE,
  perm_name VARCHAR(100) NOT NULL,
  perm_type VARCHAR(20) NOT NULL COMMENT 'menu/button/api',
  route_path VARCHAR(200),
  component VARCHAR(200),
  icon VARCHAR(50),
  sort_no INT DEFAULT 0,
  status TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. 用户-角色关联
CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. 角色-权限关联
CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. 校区
CREATE TABLE campus (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  campus_name VARCHAR(100) NOT NULL,
  address VARCHAR(255),
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. 楼栋
CREATE TABLE building (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  campus_id BIGINT NOT NULL,
  building_name VARCHAR(100) NOT NULL,
  floor_count INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_building_campus (campus_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 教室资源
CREATE TABLE classroom (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  campus_id BIGINT NOT NULL,
  building_id BIGINT NOT NULL,
  floor_no INT,
  room_no VARCHAR(30) NOT NULL,
  name VARCHAR(100) NOT NULL,
  capacity INT NOT NULL,
  room_type VARCHAR(30) DEFAULT 'normal',
  status VARCHAR(20) DEFAULT 'available',
  open_time VARCHAR(50),
  description VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_room (building_id, room_no),
  INDEX idx_classroom_location (campus_id, building_id, room_no),
  INDEX idx_classroom_capacity (capacity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. 设备档案
CREATE TABLE device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  device_code VARCHAR(50) NOT NULL UNIQUE,
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(30) NOT NULL,
  brand VARCHAR(50),
  model VARCHAR(50),
  status VARCHAR(20) DEFAULT 'normal',
  purchase_date DATE,
  last_check_time DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_device_room (classroom_id),
  INDEX idx_device_status (device_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. 课程排课
CREATE TABLE course_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  semester VARCHAR(30) NOT NULL,
  week_no INT NOT NULL,
  weekday TINYINT NOT NULL,
  start_section INT NOT NULL,
  end_section INT NOT NULL,
  classroom_id BIGINT NOT NULL,
  course_code VARCHAR(50),
  course_name VARCHAR(100) NOT NULL,
  teacher_id BIGINT,
  class_name VARCHAR(100),
  student_count INT,
  status VARCHAR(20) DEFAULT 'published',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_schedule_room_time (classroom_id, semester, week_no, weekday, start_section, end_section),
  INDEX idx_schedule_teacher (teacher_id, semester, week_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. 教室预约
CREATE TABLE classroom_reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  applicant_id BIGINT NOT NULL,
  classroom_id BIGINT NOT NULL,
  reservation_date DATE NOT NULL,
  start_section INT NOT NULL,
  end_section INT NOT NULL,
  purpose VARCHAR(255) NOT NULL,
  attendee_count INT,
  contact_phone VARCHAR(30),
  status VARCHAR(20) DEFAULT 'pending',
  approval_remark VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_reservation_room_date (classroom_id, reservation_date, start_section, end_section, status),
  INDEX idx_reservation_applicant (applicant_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. 审批记录
CREATE TABLE approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(30) NOT NULL,
  business_id BIGINT NOT NULL,
  approver_id BIGINT NOT NULL,
  action VARCHAR(20) NOT NULL,
  comment VARCHAR(255),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_approval_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. 报修工单
CREATE TABLE work_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) NOT NULL UNIQUE,
  reporter_id BIGINT NOT NULL,
  classroom_id BIGINT NOT NULL,
  device_id BIGINT,
  fault_title VARCHAR(100) NOT NULL,
  fault_description TEXT,
  fault_level VARCHAR(20) DEFAULT 'normal',
  status VARCHAR(20) DEFAULT 'submitted',
  handler_id BIGINT,
  handle_result TEXT,
  finished_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_work_order_status (status, fault_level, created_at),
  INDEX idx_work_order_room (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. 环境监测记录
CREATE TABLE environment_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  classroom_id BIGINT NOT NULL,
  temperature DECIMAL(5,2),
  humidity DECIMAL(5,2),
  co2 INT,
  pm25 DECIMAL(6,2),
  light DECIMAL(8,2),
  noise DECIMAL(6,2),
  record_time DATETIME NOT NULL,
  INDEX idx_env_room_time (classroom_id, record_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. 通知公告
CREATE TABLE notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  notice_type VARCHAR(30) DEFAULT 'system',
  target_role VARCHAR(50),
  publisher_id BIGINT,
  status VARCHAR(20) DEFAULT 'published',
  read_by JSON,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 16. 操作日志
CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  operation VARCHAR(100),
  request_method VARCHAR(10),
  request_url VARCHAR(255),
  request_ip VARCHAR(50),
  request_params TEXT,
  result_status VARCHAR(20),
  error_message TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_operation_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
