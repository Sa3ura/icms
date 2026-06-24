# ICMS 智慧教室管理系统数据库表设计

## 1. 数据库设计说明

数据库采用 MySQL 8.x，字符集建议使用 `utf8mb4`，排序规则建议使用 `utf8mb4_unicode_ci`。系统采用关系型数据库存储核心业务数据，主要包括系统权限、教室资源、设备资产、课程排课、预约审批、维修工单、环境监测、通知公告和操作日志。

数据库命名规范：

- 表名使用小写英文和下划线，例如 `classroom_reservation`。
- 主键统一命名为 `id`，类型为 `BIGINT AUTO_INCREMENT`。
- 创建时间字段统一命名为 `created_at`。
- 更新时间字段统一命名为 `updated_at`。
- 逻辑删除字段统一命名为 `deleted`，0表示未删除，1表示已删除。
- 状态字段可使用 `status`，具体枚举值由业务约定。

## 2. 数据库逻辑关系概览

```text
sys_user ──< sys_user_role >── sys_role ──< sys_role_permission >── sys_permission

campus ──< building ──< classroom ──< device
                         │
                         ├──< course_schedule
                         ├──< classroom_reservation ──< approval_record
                         ├──< work_order
                         └──< environment_record

sys_user ──< classroom_reservation
sys_user ──< work_order
sys_user ──< notice
sys_user ──< operation_log
```

## 3. 核心数据表清单

| 表名 | 中文名称 | 说明 |
|---|---|---|
| `sys_user` | 用户表 | 存储系统用户账号信息 |
| `sys_role` | 角色表 | 存储角色定义 |
| `sys_permission` | 权限表 | 存储菜单、按钮、接口权限 |
| `sys_user_role` | 用户角色关联表 | 用户与角色多对多关系 |
| `sys_role_permission` | 角色权限关联表 | 角色与权限多对多关系 |
| `campus` | 校区表 | 存储校区信息 |
| `building` | 楼栋表 | 存储教学楼等建筑信息 |
| `classroom` | 教室表 | 存储教室基础信息 |
| `device` | 设备表 | 存储教室设备资产信息 |
| `course_schedule` | 课程排课表 | 存储课程占用教室信息 |
| `classroom_reservation` | 教室预约表 | 存储预约申请和审批状态 |
| `approval_record` | 审批记录表 | 存储审批流转记录 |
| `work_order` | 维修工单表 | 存储报修和维修处理信息 |
| `environment_record` | 环境监测记录表 | 存储教室环境传感数据 |
| `notice` | 通知公告表 | 存储系统公告 |
| `operation_log` | 操作日志表 | 存储用户操作审计信息 |

## 4. 表结构设计

### 4.1 用户表 `sys_user`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | 用户名 |
| password | VARCHAR(100) | NOT NULL | 加密后的密码 |
| real_name | VARCHAR(50) | NOT NULL | 真实姓名 |
| email | VARCHAR(100) | NULL | 邮箱 |
| phone | VARCHAR(30) | NULL | 手机号 |
| user_type | VARCHAR(30) | DEFAULT 'teacher' | 用户类型 |
| status | TINYINT | DEFAULT 1 | 状态：1启用，0禁用 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 4.2 角色表 `sys_role`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 角色ID |
| role_code | VARCHAR(50) | NOT NULL, UNIQUE | 角色编码 |
| role_name | VARCHAR(50) | NOT NULL | 角色名称 |
| description | VARCHAR(255) | NULL | 角色描述 |
| status | TINYINT | DEFAULT 1 | 状态 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 4.3 权限表 `sys_permission`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 权限ID |
| parent_id | BIGINT | DEFAULT 0 | 父级权限ID |
| perm_code | VARCHAR(100) | NOT NULL, UNIQUE | 权限编码 |
| perm_name | VARCHAR(100) | NOT NULL | 权限名称 |
| perm_type | VARCHAR(20) | NOT NULL | 类型：menu/button/api |
| route_path | VARCHAR(200) | NULL | 前端路由路径 |
| component | VARCHAR(200) | NULL | Vue组件路径 |
| icon | VARCHAR(50) | NULL | 菜单图标 |
| sort_no | INT | DEFAULT 0 | 排序号 |
| status | TINYINT | DEFAULT 1 | 状态 |

### 4.4 校区表 `campus`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 校区ID |
| campus_name | VARCHAR(100) | NOT NULL | 校区名称 |
| address | VARCHAR(255) | NULL | 校区地址 |
| status | TINYINT | DEFAULT 1 | 状态 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 4.5 楼栋表 `building`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 楼栋ID |
| campus_id | BIGINT | NOT NULL, INDEX | 所属校区ID |
| building_name | VARCHAR(100) | NOT NULL | 楼栋名称 |
| floor_count | INT | DEFAULT 0 | 楼层数 |
| status | TINYINT | DEFAULT 1 | 状态 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 4.6 教室表 `classroom`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 教室ID |
| campus_id | BIGINT | NOT NULL, INDEX | 所属校区ID |
| building_id | BIGINT | NOT NULL, INDEX | 所属楼栋ID |
| floor_no | INT | NULL | 楼层 |
| room_no | VARCHAR(30) | NOT NULL | 教室编号 |
| name | VARCHAR(100) | NOT NULL | 教室名称 |
| capacity | INT | NOT NULL | 容纳人数 |
| room_type | VARCHAR(30) | DEFAULT 'normal' | 教室类型 |
| status | VARCHAR(20) | DEFAULT 'available' | 状态 |
| open_time | VARCHAR(50) | NULL | 开放时间 |
| description | VARCHAR(255) | NULL | 描述 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

索引与约束：

- `UNIQUE KEY uk_room (building_id, room_no)`：同一楼栋下教室编号唯一。
- `INDEX idx_classroom_location (campus_id, building_id, room_no)`：加速位置查询。
- `INDEX idx_classroom_capacity (capacity)`：加速按容量筛选。

### 4.7 设备表 `device`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 设备ID |
| classroom_id | BIGINT | NOT NULL, INDEX | 所属教室ID |
| device_code | VARCHAR(50) | NOT NULL, UNIQUE | 设备编码 |
| device_name | VARCHAR(100) | NOT NULL | 设备名称 |
| device_type | VARCHAR(30) | NOT NULL | 设备类型 |
| brand | VARCHAR(50) | NULL | 品牌 |
| model | VARCHAR(50) | NULL | 型号 |
| status | VARCHAR(20) | DEFAULT 'normal' | 状态 |
| purchase_date | DATE | NULL | 采购日期 |
| last_check_time | DATETIME | NULL | 最近巡检时间 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 4.8 课程排课表 `course_schedule`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 排课ID |
| semester | VARCHAR(30) | NOT NULL | 学期，如 2025-2026-1 |
| week_no | INT | NOT NULL | 周次 |
| weekday | TINYINT | NOT NULL | 星期，1-7 |
| start_section | INT | NOT NULL | 开始节次 |
| end_section | INT | NOT NULL | 结束节次 |
| classroom_id | BIGINT | NOT NULL, INDEX | 教室ID |
| course_code | VARCHAR(50) | NULL | 课程编码 |
| course_name | VARCHAR(100) | NOT NULL | 课程名称 |
| teacher_id | BIGINT | NULL, INDEX | 教师ID |
| class_name | VARCHAR(100) | NULL | 班级名称 |
| student_count | INT | NULL | 学生人数 |
| status | VARCHAR(20) | DEFAULT 'published' | 状态 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

### 4.9 教室预约表 `classroom_reservation`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 预约ID |
| applicant_id | BIGINT | NOT NULL, INDEX | 申请人ID |
| classroom_id | BIGINT | NOT NULL, INDEX | 教室ID |
| reservation_date | DATE | NOT NULL | 预约日期 |
| start_section | INT | NOT NULL | 开始节次 |
| end_section | INT | NOT NULL | 结束节次 |
| purpose | VARCHAR(255) | NOT NULL | 预约用途 |
| attendee_count | INT | NULL | 参与人数 |
| contact_phone | VARCHAR(30) | NULL | 联系电话 |
| status | VARCHAR(20) | DEFAULT 'pending' | 状态：pending/approved/rejected/cancelled |
| approval_remark | VARCHAR(255) | NULL | 审批意见 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

关键索引：

- `idx_reservation_room_date (classroom_id, reservation_date, start_section, end_section, status)`：用于预约冲突检测。
- `idx_reservation_applicant (applicant_id, created_at)`：用于查询个人预约记录。

### 4.10 审批记录表 `approval_record`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 审批记录ID |
| business_type | VARCHAR(30) | NOT NULL | 业务类型，如 reservation |
| business_id | BIGINT | NOT NULL | 业务ID |
| approver_id | BIGINT | NOT NULL | 审批人ID |
| action | VARCHAR(20) | NOT NULL | approve/reject/cancel |
| comment | VARCHAR(255) | NULL | 审批意见 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 审批时间 |

### 4.11 维修工单表 `work_order`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 工单ID |
| order_no | VARCHAR(50) | NOT NULL, UNIQUE | 工单编号 |
| reporter_id | BIGINT | NOT NULL | 报修人ID |
| classroom_id | BIGINT | NOT NULL, INDEX | 教室ID |
| device_id | BIGINT | NULL | 设备ID |
| fault_title | VARCHAR(100) | NOT NULL | 故障标题 |
| fault_description | TEXT | NULL | 故障描述 |
| fault_level | VARCHAR(20) | DEFAULT 'normal' | 故障等级 |
| status | VARCHAR(20) | DEFAULT 'submitted' | 状态 |
| handler_id | BIGINT | NULL | 处理人ID |
| handle_result | TEXT | NULL | 处理结果 |
| finished_at | DATETIME | NULL | 完成时间 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |

工单状态建议：

| 状态值 | 说明 |
|---|---|
| submitted | 已提交 |
| assigned | 已派单 |
| processing | 处理中 |
| finished | 已完成 |
| closed | 已关闭 |
| cancelled | 已取消 |

### 4.12 环境监测记录表 `environment_record`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| classroom_id | BIGINT | NOT NULL, INDEX | 教室ID |
| temperature | DECIMAL(5,2) | NULL | 温度 |
| humidity | DECIMAL(5,2) | NULL | 湿度 |
| co2 | INT | NULL | 二氧化碳浓度 |
| pm25 | DECIMAL(6,2) | NULL | PM2.5 |
| light | DECIMAL(8,2) | NULL | 光照 |
| noise | DECIMAL(6,2) | NULL | 噪音 |
| record_time | DATETIME | NOT NULL, INDEX | 记录时间 |

### 4.13 通知公告表 `notice`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 公告ID |
| title | VARCHAR(100) | NOT NULL | 标题 |
| content | TEXT | NOT NULL | 内容 |
| notice_type | VARCHAR(30) | DEFAULT 'system' | 公告类型 |
| target_role | VARCHAR(50) | NULL | 目标角色 |
| publisher_id | BIGINT | NULL | 发布人ID |
| status | VARCHAR(20) | DEFAULT 'published' | 状态 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 发布时间 |

### 4.14 操作日志表 `operation_log`

| 字段名 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 日志ID |
| user_id | BIGINT | NULL, INDEX | 用户ID |
| username | VARCHAR(50) | NULL | 用户名 |
| operation | VARCHAR(100) | NULL | 操作名称 |
| request_method | VARCHAR(10) | NULL | 请求方式 |
| request_url | VARCHAR(255) | NULL | 请求地址 |
| request_ip | VARCHAR(50) | NULL | 请求IP |
| request_params | TEXT | NULL | 请求参数 |
| result_status | VARCHAR(20) | NULL | 结果状态 |
| error_message | TEXT | NULL | 异常信息 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

## 5. 关键业务约束

### 5.1 教室预约冲突判断

一个教室在同一天、同一节次范围内，不能同时存在：

- 已发布课程排课记录。
- 已审批通过的预约记录。
- 未取消且未删除的有效占用记录。

节次区间冲突判断条件：

```sql
new_start <= existing_end AND new_end >= existing_start
```

### 5.2 教室容量约束

预约人数原则上不得超过教室容量：

```text
attendee_count <= classroom.capacity
```

### 5.3 数据删除策略

核心业务表建议使用逻辑删除，避免误删影响审计和统计：

```text
deleted = 0 表示有效数据
deleted = 1 表示已删除数据
```

### 5.4 审计策略

以下操作建议写入 `operation_log`：

- 登录、退出、修改密码。
- 新增、编辑、删除用户。
- 新增、编辑、删除教室和设备。
- 提交、审批、取消预约。
- 提交、派单、处理、关闭工单。

## 6. 建表 SQL

完整 MySQL 建表脚本已单独提供：`ICMS_database_schema_简体.sql`。
