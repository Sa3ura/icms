# 文档中心

[← 返回项目首页](../README.md)

## 使用与开发

| 文档 | 内容 |
| :--- | :--- |
| [开发指南](DEVELOPMENT.md) | 项目结构、环境配置、本地启动与常用命令 |
| [部署指南](DEPLOYMENT.md) | 构建、服务器配置、反向代理与排障 |
| [测试说明](TEST_CASES.md) | 自动化验证范围与业务用例 |
| [视觉素材](assets/README.md) | 项目标志、横幅与截图说明 |

## 设计资料

| 文档 | 内容 |
| :--- | :--- |
| [需求设计](design/REQUIREMENTS.md) | 业务背景与需求设计 |
| [功能清单](design/FEATURES.md) | 各业务模块的功能列表 |
| [数据库设计](design/DATABASE.md) | 数据表与字段设计 |
| [页面原型](design/WIREFRAMES.md) | 页面结构与交互原型 |

设计资料用于说明设计意图，实际行为与数据库结构以当前源码和 [Flyway 迁移](../ICMS-backend/src/main/resources/db/migration/) 为准。

## 历史资料

- [交付差距分析](archive/DELIVERY_GAP_ANALYSIS.md)：保留原始日期与当时的评估结论，不作为当前验证结果。
- [旧版数据库脚本](archive/database-schema-v1.sql)：仅作历史参考，新环境使用 Flyway 建表。

## 仓库文件约定

- 版本控制保留源码、依赖清单与锁文件、配置模板、测试及可维护的文档。
- `node_modules/`、`target/`、`dist/`、Maven 下载缓存、日志和测试报告由工具生成并忽略。
- 真实的 `application-local.yml` 与本地环境覆盖文件保持 Git 忽略。
- Word / PPT 导出、演示文稿工程、旧部署压缩包与诊断记录归入 `local/`，按需在本机使用。
- 本地代理配置与 CodeGraph 索引保持忽略；仓库级协作约定使用根目录 `AGENTS.md`。
