# ICMS — 智慧教室管理系统

Intelligent Classroom Management System — 基于 Vue 3 + Spring Boot 的全栈智慧校园教室资源管理平台。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **前端** | Vue 3 (Composition API) | ^3.5 |
| 构建 | Vite | ^8.0 |
| 路由 | Vue Router | ^5.1 (Hash 模式) |
| 状态 | Pinia | ^3.0 |
| UI 框架 | Element Plus | ^2.14 |
| 图表 | ECharts (按需引入) | ^6.1 |
| HTTP | Axios | ^1.18 |
| 样式 | Sass + CSS 自定义属性 | — |
| **后端** | Spring Boot | 3.2.4 |
| ORM | MyBatis-Plus | 3.5.5 |
| 认证 | SaToken (JWT 无状态) | 1.38.0 |
| 安全 | BCrypt + SVG 验证码 + IP 限流 + SaMode.OR | — |
| 日志 | Log4j2 (彩色控制台 + SQL/App 分离滚动文件) | — |
| 数据库 | MySQL + Flyway 版本迁移 | 8.x |
| 构建 | Maven Wrapper | 3.9.6 |

## 项目结构

```
ICMS/
├── ICMS-frontend/              # Vue 3 前端
│   ├── src/
│   │   ├── api/index.js        # API 层（Mock + 真实后端双模式）
│   │   ├── db/mockDb.js        # localStorage Mock 数据库
│   │   ├── router/index.js     # 路由 + 角色权限守卫
│   │   ├── store/user.js       # Pinia 用户/主题状态
│   │   ├── layout/index.vue    # 主布局（侧边栏+顶栏+内容）
│   │   ├── composables/        # 组合式函数
│   │   ├── utils/mappings.js   # 枚举映射工具
│   │   └── views/              # 页面组件
│   │       ├── Login.vue
│   │       ├── dashboard/      # 首页仪表盘
│   │       ├── classroom/      # 教室管理 + 空闲查询
│   │       ├── reservation/    # 预约申请 + 审批
│   │       ├── device/         # 设备资产管理
│   │       ├── workorder/      # 报修工单
│   │       ├── environment/    # 环境实时监测
│   │       └── system/         # 用户管理 + 操作日志 + 通知管理
│   ├── .env.development        # 开发环境变量（Mock 模式开启）
│   ├── .env.production         # 生产环境变量
│   └── eslint.config.js
│
├── ICMS-backend/               # Spring Boot 后端
│   ├── src/main/java/com/icms/
│   │   ├── controller/         # REST 控制器 (11 个)
│   │   ├── service/            # 业务服务层
│   │   ├── mapper/             # MyBatis-Plus 数据访问
│   │   ├── entity/             # 数据实体 (13 个)
│   │   ├── config/             # Spring 配置 + DatabaseInitializer
│   │   ├── security/           # SaToken / 验证码 / 登录限流
│   │   └── common/             # 统一响应 / 异常处理 / AOP 日志 / 驼峰转蛇形
│   └── src/main/resources/
│       ├── db/migration/       # Flyway 版本化 SQL 迁移
│       └── application.yml
│
├── ICMS_database_schema_简体.sql  # 数据库建表脚本
├── ICMS_需求设计文档.md
├── ICMS_数据库表设计.md
├── ICMS_功能列表清单.md
├── ICMS_页面白板原型.md
└── ICMS_交付差距分析.md         # 上线前待修复清单
```

## 功能模块

| 模块 | 路由 | 功能 | 角色 |
|------|------|------|------|
| 仪表盘 | `/dashboard` | 统计卡片 + 空间活跃度图表 + 设备状态饼图 + 今日占用 | 全部 |
| 教室管理 | `/classroom/list` | CRUD + 校区/楼栋/楼层级联 + 课表沙盘 + 设备快查 | admin/jw/teacher |
| 空闲查询 | `/classroom/free` | 按日期/节次/容量/设备过滤 + 一键预约 | 全部 |
| 我的预约 | `/reservation/my` | 新建申请 + 冲突实时检测 + 取消 | 全部 |
| 预约审批 | `/reservation/approval` | 通过/驳回 + 审批意见 | admin/jw |
| 设备管理 | `/device/list` | CRUD + 快速报修跳转工单 | admin/jw/teacher |
| 工单管理 | `/work-order/list` | 提交→指派→处理→关闭全生命周期 | 全部 |
| 环境监测 | `/environment/index` | 6 项指标实时卡片 + 温湿度/空气质量双图表 | admin/jw/teacher |
| 用户管理 | `/system/user` | CRUD + 启用/禁用 + 重置密码 | admin/jw |
| 通知管理 | `/system/notices` | 发布/编辑/删除通知 + 已读统计 | admin/jw |
| 操作日志 | `/system/log` | 审计日志检索 | admin |

## 快速开始（前端 Mock 模式）

无需后端和数据库，纯前端运行：

```bash
cd ICMS-frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`，默认使用 Mock 数据（localStorage）。

## 切换真实后端

修改 `.env.development`：

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080
```

## 后端启动

```bash
cd ICMS-backend

# 1. 创建数据库（仅需一次）
# mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS icms DEFAULT CHARACTER SET utf8mb4;"

# 2. 修改 application.yml 中的数据库连接信息

# 3. 启动（Flyway 自动建表 + DatabaseInitializer 播种数据）
./mvnw spring-boot:run   # Linux/Mac
mvnw.cmd spring-boot:run # Windows
```

启动时 Flyway 自动执行 `db/migration/` 下的版本化 SQL 脚本，无需手动导入建表文件。

## 默认账户

所有种子用户密码统一为 `123456`：

| 用户名 | 角色 | 说明 |
|--------|------|------|
| `admin` | 系统管理员 | 全部权限 |
| `jw_admin` | 教务管理员 | 教室/预约/审批 |
| `teacher_wang` | 教师 | 预约/报修 |
| `student_li` | 学生 | 查询/预约 |
| `repairer_zhang` | 维修人员 | 工单处理 |

## 开发命令

```bash
# 前端
npm run dev        # 开发服务器
npm run build      # 生产构建
npm run preview    # 预览生产构建
npm run lint       # ESLint 检查
npm run format     # ESLint 自动修复
```

## 用户角色

| 角色 | 代号 | 权限范围 |
|------|------|----------|
| 系统管理员 | `admin` | 全部功能 |
| 教务管理员 | `jw` | 教室管理、预约审批、设备管理、通知管理 |
| 教师 | `teacher` | 查询教室、申请预约、报修设备 |
| 学生 | `student` | 查询空闲教室、申请预约 |
| 维修人员 | `repairer` | 工单接收与处理 |

## 特性

- **双模式运行**：Mock 模式（纯前端 localStorage）与真实 API 模式一键切换
- **安全认证**：SaToken JWT 无状态 + `@SaCheckRole(value = {...}, mode = SaMode.OR)` 注解鉴权 + 双 Token (Access 内存/Refresh httpOnly Cookie) 防 XSS 窃取
- **登录安全**：后端 SVG 干扰验证码 + 同 IP 5次/5分钟失败封锁 15 分钟
- **暗黑模式**：CSS 自定义属性 + 平滑主题过渡动画 (`theme-transitioning`)
- **通知系统**：发布→推送→已读追踪（`read_by[]` 独立状态）+ 30s 自动轮询
- **ECharts 按需引入**：仅打包使用的 Line/Bar/Pie 图表 + CanvasRenderer
- **操作栏优化**：≥3 按钮自动收进 `el-dropdown`，统一"操作 ▾"样式
- **滚动体验**：8px 滚动条 + Firefox 兼容 + 平滑滚动 + 表格独立横向滚动
- **Hash 路由**：无服务端 fallback 配置需求，刷新不 404
- **数据库版本管理**：Flyway `V1__init_schema.sql` → `V2__add_refresh_token.sql` 自动增量迁移

## 交付状态

已修复 **41 项**缺陷，致命安全漏洞 **4/4 全部清零**，编译/Lint 零错误。S1 全部完成。  
可作为安全可靠的原型/Demo 演示。
