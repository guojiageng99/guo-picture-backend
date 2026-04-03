# Guo Picture（图多多云图库）

全栈云图库项目：**后端**为 Spring Boot API，**前端**为 Vue 3 + Vite 单页应用。前后端仓库分离，本地联调需同时启动两端。

| 部分 | 说明 |
|------|------|
| **后端（本仓库）** | 当前目录 `guo-picture-backend` |
| **前端** |  sibling 目录 `guo-picture-frontend`（例如 `E:\code\guo-picture-frontend`） |

---

## 后端技术栈

| 类别 | 技术 |
|------|------|
| 运行时 | Java 11 |
| 框架 | Spring Boot 2.7.6 |
| 数据访问 | MyBatis-Plus 3.5.x |
| 分库分表 | ShardingSphere JDBC 5.2（`picture` 按 `spaceId` 分表） |
| 缓存 / 会话 | Redis、Spring Session、Caffeine（图片列表多级缓存等） |
| 认证 | Sa-Token + Session |
| 对象存储 | 腾讯云 COS |
| 检索（可选） | Spring Data Elasticsearch 7.x |
| 消息（可选） | Spring AMQP / RabbitMQ（AI 扩图异步任务） |
| 其他 | WebSocket、Knife4j（OpenAPI2）、Hutool、混元（可选）、阿里云百炼扩图 API 等 |

---

## 默认服务地址

| 项 | 值 |
|----|-----|
| 端口 | **9123** |
| 上下文路径 | **`/api`**（例：`http://localhost:9123/api/...`） |
| 接口文档（Knife4j） | **http://localhost:9123/api/doc.html** |
| OpenAPI JSON（供前端 codegen） | **http://localhost:9123/api/v2/api-docs** |

---

## 环境要求

- JDK 11、Maven 3.6+
- MySQL（初始化脚本：`sql/create_table.sql`，库名以脚本及 `application-local.yml` 为准）
- Redis（Session、缓存、限流等）
- 腾讯云 COS（`cos.client`）
- **可选**：RabbitMQ（`outpainting.mq-enabled=true` 时必填）
- **可选**：Elasticsearch（`elasticsearch.enabled=true` 时需可连 ES，并视情况执行全量重建索引接口）
- **可选**：阿里云 DashScope（扩图）、腾讯混元（`tencent.hunyuan`，看图填元数据 / AI 审核）
- 若使用以图搜图等能力，需满足 Selenium 与浏览器环境

---

## 配置说明

- 公共配置：`src/main/resources/application.yml`（含上传大小、`picture`、`outpainting`、`elasticsearch` 等默认值）。
- **本地/生产密钥**：`application-local.yml` / `application-prod.yml`（`application-local.yml` 应在 `.gitignore` 中，勿提交密钥）。

请在私密配置中至少包含：数据源与 ShardingSphere、Redis、`cos.client`。启用扩图时还需配置阿里云相关项（见代码中 `OutpaintingProperties` 或部署文档）；启用 MQ 时配置 `spring.rabbitmq`。

### 与分表相关的业务约定

- **`spaceId = 0`（或 null 入库时归一为 0）表示公共图库**，`space` 表中不存在 `id = 0` 的记录；接口传 `0` 时不应调用 `spaceService.getById(0)`。
- 更新 `picture` 时 **WHERE 必须带分片键 `spaceId`**，否则 ShardingSphere 会广播更新所有分表，极慢且易触发前端超时。

### 常用自定义项（`application.yml` 可覆盖）

| 配置键 | 含义 |
|--------|------|
| `picture.url-download-timeout-ms` | 通过 URL 下载图片（如扩图结果写回 COS）时 Hutool HTTP 超时（毫秒），默认 **60000** |
| `outpainting.*` | 扩图额度、限流、对账调度、MQ 开关等 |
| `elasticsearch.enabled` | 是否启用 ES 同步与检索 |
| `tencent.hunyuan.*` | 混元看图与 AI 审核开关 |

---

## 初始化数据库

```bash
mysql -u root -p < sql/create_table.sql
```

按实际库名、账号修改 JDBC 与 ShardingSphere 配置。

---

## 构建与运行

```bash
mvn clean package -DskipTests
java -jar target/guo-picture-backend-0.0.1-SNAPSHOT.jar
```

开发：

```bash
mvn spring-boot:run
```

或运行主类：`com.guo.guopicturebackend.GuoPictureBackendApplication`

---

## 主要接口模块（Controller）

包路径：`com.guo.guopicturebackend.controller`

| 类 | 职责摘要 |
|----|----------|
| `UserController` | 注册、登录、会话、扩图额度查询等 |
| `PictureController` | 图片 CRUD、URL/文件上传、审核、**AI 扩图任务**、ES 重建索引等 |
| `SpaceController` / `SpaceUserController` / `SpaceAnalyzeController` | 空间、成员、分析 |
| `FileController` / `CosMultipartController` | 文件与分片上传 |
| `PictureCategoryAdminController` / `PictureTagAdminController` | 分类、标签管理 |
| `UserMessageController` | 站内消息 |
| `MainController` | 其他入口 |

具体路径与参数以 **Knife4j** 为准。

---

## 前端（guo-picture-frontend）

- 技术栈：Vue 3、TypeScript、Vite 5、Ant Design Vue 4、Pinia、Axios 等。
- 仓库路径：与后端同级目录 `guo-picture-frontend`。
- 开发：`npm install` → `npm run dev`（默认 **5173**），Vite 将 **`/api`** 代理到 **`http://localhost:9123`**。
- 前端 **Axios 默认超时 60s**，与后端 `picture.url-download-timeout-ms` 对齐；大图或弱网场景可按需再调大。

详见前端仓库 **`README.md`**。

---

## 联调步骤

1. 配置后端私密 YAML，初始化 MySQL，按需启动 Redis / RabbitMQ / ES。  
2. 启动后端，确认 **http://localhost:9123/api/doc.html** 可访问。  
3. 在前端目录执行 `npm install`、`npm run dev`，浏览器访问控制台给出的本地地址。  
4. 更新 API 类型时：后端启动后在前端执行 `npm run openapi`。

---

## 许可证

若对外开源，请补充 `LICENSE` 并在本 README 中注明。
