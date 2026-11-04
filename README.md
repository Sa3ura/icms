<div align="center">

<h1>ICMS</h1>
<p><strong>智慧教室管理系统</strong></p>
<p>Intelligent Classroom Management System</p>
<p>让教室预约、设备报修与校园空间管理更有序。</p>

<p>
  <img alt="Vue 3" src="https://img.shields.io/badge/Vue-3-42b883?style=flat-square&logo=vuedotjs&logoColor=white" />
  <img alt="Spring Boot 3" src="https://img.shields.io/badge/Spring_Boot-3-6db33f?style=flat-square&logo=springboot&logoColor=white" />
  <img alt="Java 17" src="https://img.shields.io/badge/Java-17-2563eb?style=flat-square" />
  <a href="https://github.com/Sa3ura/icms/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/Sa3ura/icms/actions/workflows/ci.yml/badge.svg" /></a>
</p>

<p>
  <a href="https://icms.unjust.cn/">在线体验</a> ·
  <a href="#快速开始">快速开始</a> ·
  <a href="docs/DEVELOPMENT.md">开发指南</a> ·
  <a href="docs/DEPLOYMENT.md">部署指南</a>
</p>

</div>

---

## 功能概览

面向管理员、教务、教师、学生和维修人员，连接教室资源的查询、使用与维护流程。

| 模块 | 主要能力 |
| :--- | :--- |
| 教室与课表 | 管理校区、楼栋和教室，按日期、节次、容量与设备查询空闲教室 |
| 预约与审批 | 提交预约、检查时间冲突、审批与取消申请 |
| 设备与工单 | 维护设备档案，完成报修、指派、处理与关闭，并联动设备状态 |
| 统计与监测 | 查看空间使用情况、设备状态及环境趋势；环境监测目前使用模拟数据 |
| 系统管理 | 用户与角色管理、通知公告、操作日志 |

支持浅色 / 深色主题、主题记忆，以及 Mock 与真实后端两种运行方式。

## 快速开始

先体验界面和业务流程，无需启动后端或数据库。

准备 Node.js 22.12+（22.x）或 24.x，在仓库根目录执行：

```bash
cd ICMS-frontend
npm ci
npm run dev:e2e
```

打开 **<http://localhost:5173>**。此命令启用 Mock 模式，数据保存在浏览器本地；演示账号见 [开发指南](docs/DEVELOPMENT.md#本地演示账号)。

需要连接 MySQL 和 Spring Boot？请查看 [开发指南](docs/DEVELOPMENT.md)。

## 技术栈

| 层级 | 技术 |
| :--- | :--- |
| 前端 | Vue 3 · Vite · Element Plus · Pinia · ECharts |
| 后端 | Java 17 · Spring Boot 3 · MyBatis-Plus · Sa-Token |
| 数据与工程 | MySQL 8 · Flyway · Vitest · JUnit · Playwright · GitHub Actions |

## 文档导航

| 文档 | 内容 |
| :--- | :--- |
| [开发指南](docs/DEVELOPMENT.md) | 项目结构、完整启动、本地账号与常用命令 |
| [部署指南](docs/DEPLOYMENT.md) | 构建产物、外部配置、OpenResty / Nginx 代理与常见问题 |
| [测试说明](docs/TEST_CASES.md) | 自动化测试范围、CI 流程与业务用例 |
| [需求设计](ICMS_需求设计文档.md) | 业务背景与需求设计 |
| [数据库设计](ICMS_数据库表设计.md) | 数据表与字段说明 |
