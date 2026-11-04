# 开发指南

[← 返回项目首页](../README.md)

## 项目结构

```text
ICMS-frontend/                 Vue 前端
  src/api/                    Mock / 真实 API 适配
  src/components/             公共组件
  src/composables/             可复用逻辑
  src/router/                 页面路由与角色配置
  src/store/                  登录状态与主题
  src/views/                  业务页面
ICMS-backend/                  Spring Boot 后端
  src/main/java/com/icms/      Controller / Service / Mapper 等分层
  src/main/resources/         应用配置与 Flyway 迁移
  src/test/                   后端测试
docs/                         开发、部署与测试说明
```

## 完整启动

准备 Java 17、MySQL 8，以及 Node.js 22.12+（22.x）或 24.x。

### 1. 创建数据库

```sql
CREATE DATABASE icms DEFAULT CHARACTER SET utf8mb4;
```

为项目配置数据库用户。新建空库由 Flyway 自动建表，无需再导入仓库根目录的建表 SQL。

### 2. 配置后端

参考 [配置模板](../ICMS-backend/src/main/resources/application-local.yml.example)，在 **`ICMS-backend/` 根目录**创建 `application-local.yml`，填写数据库连接、用户名、密码和随机 JWT 密钥。

当前配置从启动工作目录加载该文件；请在后端根目录执行启动命令。真实配置已被 Git 忽略，并从 JAR 资源中排除。

```powershell
cd ICMS-backend
.\mvnw.cmd spring-boot:run
```

Linux / macOS 使用 `sh mvnw spring-boot:run`。首次执行时脚本会下载 Maven，需要网络；Linux / macOS 还需要 `curl` 或 `wget`，以及 `unzip`。

默认端口为 `8080`，开发环境 API 文档入口为 <http://localhost:8080/doc.html>。

### 3. 启动前端

在另一个终端执行：

```bash
cd ICMS-frontend
npm ci
npm run dev
```

当前 `.env.development` 已配置为真实后端模式：

```dotenv
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080
```

打开 <http://localhost:5173>。如需仅运行前端 Mock 演示，改用 `npm run dev:e2e`；该模式不访问真实后端。

## 本地演示账号

仅在用户表为空时，后端才会初始化以下账号，初始密码均为 `123456`。Mock 模式也提供对应角色的演示数据，登录时验证码可填写任意非空内容。

| 用户名 | 角色 |
| :--- | :--- |
| `admin` | 系统管理员 |
| `jw_admin` | 教务管理员 |
| `teacher_wang` | 教师 |
| `student_li` | 学生 |
| `repairer_zhang` | 维修人员 |

正式开放访问前修改初始密码，禁用不需要的演示账号。已部署环境的账号以实际配置为准。

## 常用命令

在对应子项目目录执行：

| 用途 | 前端 | 后端（Windows） |
| :--- | :--- | :--- |
| 开发启动 | `npm run dev` | `.\mvnw.cmd spring-boot:run` |
| 单元测试 | `npm test` | `.\mvnw.cmd test` |
| 生产构建 | `npm run build` | `.\mvnw.cmd clean package` |

- `npx eslint src --ext .vue,.js`：只检查前端代码；`npm run lint` 会自动修复。
- `npm run test:e2e`：Playwright 流程测试，首次运行需要安装浏览器依赖。
- `.\mvnw.cmd verify`：包含后端集成测试，需要可用的 Docker 环境。

Linux / macOS 将 `.\mvnw.cmd` 替换为 `sh mvnw`。更完整的测试范围见 [测试说明](TEST_CASES.md)。
