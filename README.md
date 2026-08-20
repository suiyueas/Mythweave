<p align="center">
  <h1 align="center">Mythweave AI - AI智能小说创作平台</h1>
</p>

<p align="center">
  <strong>让 AI 成为作者的副脑 — 全能型 AI 写作工作台</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License">
  <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D.svg" alt="Vue">
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED.svg" alt="Docker">
</p>

---

## ✨ 功能特性

### 📚 作品管理
- 多作品并行管理（创建/编辑/删除/归档）
- 写作目标设定与进度追踪
- 作品模板（玄幻/都市/科幻/悬疑等类型）

### 🗺️ 智能大纲
- 树形/思维导图式大纲编辑器
- AI 一键生成完整大纲
- 经典叙事结构模板（三幕式/英雄之旅/二十四章经）
- 大纲与正文双向联动

### 👥 人物工坊
- 人物角色管理（主角/配角/反派）
- AI 人物弧线设计
- 人物关系图谱

### 🌍 世界观构建
- 世界观设定编辑器
- 地理/政治/魔法体系管理
- 势力与派系设定

### ✍️ 智能写作
- AI 续写与润色（SSE 流式输出，30s 心跳保活 + 断线重连）
- 多章节并发写作
- 章节版本管理
- 上下文记忆（RAG 混合检索：kNN 向量召回 + BM25 关键词融合）

### 🛡️ 智能哨兵
- 伏笔遗漏巡查
- 逻辑矛盾检测
- 写作节奏分析
- 人物出场统计
- WebSocket 实时告警推送

### 👑 VIP 会员系统
- AI 功能权限拦截（免费用户限次，VIP 无限畅用）
- 后端二次校验（防止绕过前端直接调接口）
- 输入安全过滤（敏感词 + 越狱检测 + 注入防御）
- 频率限制与熔断机制（防滥用）
- 输出内容二次审核（生成内容敏感扫描）
- 多档位套餐（月度/季度/年度）
- 微信/支付宝模拟支付
- VIP 到期续费与状态管理

### 🔍 全局搜索
- 基于 Elasticsearch 的全文搜索
- 语义相似度匹配（千问 Embedding 1024 维向量）

### 📊 创作分析
- 写作进度可视化（字数统计、章节完成率）
- 写作时长追踪
- 哨兵问题汇总与趋势分析

### 🎯 情节线管理
- 多线程情节线创建与管理
- 伏笔与回收节点追踪
- 情节线可视化

### 💡 灵感管理
- 灵感碎片随时记录
- AI 灵感推荐
- 灵感与作品的关联管理

### 📤 作品导出
- 支持导出为 TXT/EPUB/JSON 格式
- 导出时自动清理敏感内容

## 🛠️ 技术栈

### 后端
- **框架**：Spring Boot 3.5 + Java 21
- **ORM**：MyBatis Plus 3.5
- **数据库**：MySQL 8.x
- **缓存**：Redis（TTL 缓存 + 熔断降级）
- **搜索**：Elasticsearch 8.x（kNN + BM25 混合检索）
- **实时通信**：SSE 流式 + WebSocket
- **API 文档**：Knife4j
- **AI**：DeepSeek / Mimo / 千问 Embedding（多模型适配层）
- **部署**：Docker Compose 一键编排（MySQL/Redis/ES/前后端）

### 前端
- **框架**：Vue 3.4
- **构建工具**：Vite 5
- **CSS**：Tailwind CSS 4
- **状态管理**：Pinia
- **路由**：Vue Router 4
- **图表**：ECharts 6

## 🧠 核心架构亮点

| 能力 | 说明 |
|------|------|
| RAG 混合检索 | 设定数据向量化入库，kNN 向量召回 + BM25 加权融合，相似度阈值过滤保证召回质量 |
| SSE 流式生成 | 异步流式替代轮询，30s 心跳保活 + 3 次指数退避重连，长文生成不中断 |
| 思考模式治理 | 单一 V4 模型下用 `thinking.type` 切换思考/非思考模式，`reasoning_effort` 控制思考强度，避免思考过长耗尽预算导致正文截断；失败自动降级为非思考模式 |
| 多 Agent 协作 | 编辑/人物/风格/读者 4 个职责单一 Agent，协调器流水线并行调度 |
| 智能巡检 | 规则引擎 + 关键词统计构建 4 类扫描器，规则热更新，WebSocket 秒级告警 |
| 稳定性治理 | Redis 熔断降级、AI 调用指数退避重试、启动缓存预热 |

## 📊 Benchmark

性能声明采用"结果与声明分离"口径：仅入库的实测结果可引用，其余为待跑实验。当前已入库：多 Agent 并行调度 Mock 实测 **4.0x 加速比**（3239ms → 810ms）；混合检索 Recall@5 / P95 延迟 / 流式首字延迟 TTFT 实验已就绪待运行：

- 运行手册与结果记录：[docs/benchmark.md](docs/benchmark.md)
- 评测集：`springboot/src/test/resources/benchmark/sample-data.json`（44 份设定文档 + 40 条标注问题）
- 实测报告：`docs/benchmark-results/`（运行后自动生成）

```bash
mvn test -Dtest=HybridSearchBenchmark -DfailIfNoTests=false   # 实验①+②：召回率与延迟
mvn test -Dtest=AgentParallelBenchmark -DfailIfNoTests=false  # 实验③：多 Agent 串/并行对比
```

## 🔐 安全机制

| 层级 | 机制 | 说明 |
|------|------|------|
| 输入层 | 敏感词过滤 | 政治/暴力/色情等敏感词实时拦截 |
| 输入层 | 越狱检测 | 防御 Jailbreak 等对抗性提示词攻击 |
| 输入层 | 注入攻击防御 | 检测 System Prompt 覆盖尝试 |
| 请求层 | 频率限制 | 普通用户 30次/分钟，VIP 120次/分钟 |
| 请求层 | 熔断机制 | 连续违规自动熔断 5 分钟冷却 |
| 输出层 | 二次审核 | 生成内容实时扫描，违规自动截断 |
| 输出层 | 循环检测 | 复读攻击自动中断输出 |
| 权限层 | VIP 后端校验 | 所有 AI 接口后端二次鉴权 |
| 权限层 | IDOR 防护 | 项目/章节访问需校验归属 |

## 📦 项目结构

```
AI-novel/
├── springboot/                 # Spring Boot 3.5 后端项目
│   ├── Dockerfile              # 后端多阶段构建镜像（Maven 编译 → JRE 运行）
│   ├── .dockerignore
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/           # Java 源代码
│   │   │   └── resources/
│   │   │       ├── mapper/     # MyBatis 映射文件
│   │   │       ├── sql/        # 数据库 SQL 脚本
│   │   │       ├── application.yml           # 本地配置（不入库，含密钥）
│   │   │       ├── application.example.yml   # 配置模板（入库存档）
│   │   │       └── application-prod.yml      # 生产配置（环境变量 + docker Profile）
│   │   └── test/               # 测试代码
│   └── pom.xml                 # Maven 依赖配置
├── vue/                        # Vue 3 前端项目
│   ├── Dockerfile              # 前端多阶段构建镜像（Node 编译 → Nginx 托管）
│   ├── nginx.conf              # Nginx 静态资源 + /api 反向代理（含 SSE 直通）
│   └── .dockerignore
├── elasticsearch/
│   └── Dockerfile              # ES 8 + IK 中文分词插件镜像
├── docker-compose.yml          # MySQL/Redis/ES/后端/前端 一键编排（内置生产加固）
├── .env.docker.example         # Docker 部署环境变量模板
├── .env.example                # 本地开发环境变量模板
├── .gitignore                  # Git 忽略配置
└── README.md                   # 项目说明文档
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.x
- Redis 6.x
- Elasticsearch 8.x

### 0. Docker 一键启动（生产就绪）

无需安装 JDK / Node / MySQL / Redis / Elasticsearch，一条命令拉起全套环境（MySQL 8 + Redis 7 + ES 8 + 后端 + 前端）。Compose 文件已内置生产加固，开发与生产行为一致。

内置加固一览：

| 加固项 | 说明 |
|--------|------|
| 🔒 网络安全隔离 | MySQL/Redis/ES 位于 `internal` 内网（无网关、不可被宿主机直连，仅后端可达），不暴露宿主机端口；前端仅监听 `127.0.0.1` |
| ⚙️ 资源限制 | 各容器 CPU/内存上限（MySQL 2C/2G、ES 2C/1G、Redis 0.5C/512M、后端 2C/2G、前端 0.5C/256M），`docker compose up` 即生效 |
| 📋 日志配置 | `json-file` 驱动 + 轮转（后端单文件 100M×5，其余 20~50M×3~5），避免日志占满磁盘 |

另含 Redis `maxmemory 256mb + allkeys-lru` 淘汰策略、后端 `init` 进程守护、`stop_grace_period 30s` 优雅停机、`no-new-privileges` 权限收紧。

```bash
# 1. 准备环境变量（模板中为占位符，请替换为真实值）
copy .env.docker.example .env
# 编辑 .env，至少填写：DB_PASSWORD、REDIS_PASSWORD、ES_PASSWORD、JWT_SECRET、DEEPSEEK_API_KEY

# 2. 构建并启动（首次构建需拉取镜像，耗时较长）
docker compose up -d --build

# 3. 查看各服务健康状态（等待 mysql/redis/es 通过健康检查）
docker compose ps

# 4. 访问
# 前端：http://127.0.0.1:80        后端 API：http://127.0.0.1:8080（均仅本机可访问）

# 5. 停止 / 清理
docker compose down          # 停止（数据保留在命名卷中）
docker compose down -v       # 停止并删除数据卷（会清空数据库，慎用）
```

> 💡 说明：
> - 需要 **Docker 24+ / Docker Compose v2**（Windows/Mac 请使用 Docker Desktop）。
> - 基础设施已移入 internal 内网，宿主机无法直连；需调试数据库时可临时进容器：
>   `docker compose exec mysql mysql -uroot -p`、`docker compose exec redis redis-cli -a <密码>`、`docker compose exec elasticsearch curl -s localhost:9200`
> - 前端仅监听 `127.0.0.1`，对外访问请在前置主机级反向代理（Nginx/Caddy）终止 TLS 后转发到 `127.0.0.1:${FRONTEND_PORT:-80}`。
> - 首次启动 MySQL 会自动执行 `springboot/src/main/resources/sql/mythweave_complete.sql` 初始化库表与默认账号 `admin`。
> - Elasticsearch 镜像内置 IK 中文分词插件（RAG 混合检索依赖 `ik_max_word`），ES 版本通过 `.env` 中 `ES_VERSION` 控制；若修改 `ES_PASSWORD`，需先 `docker compose down -v` 清空 ES 数据卷再重建。
> - Linux 宿主机需满足 ES 内存映射要求：`sudo sysctl -w vm.max_map_count=262144`（Windows/Mac 的 Docker Desktop 已默认配置）。
> - 前端 Nginx 已配置 `/api` 同源反向代理与 SSE 流式直通（`proxy_buffering off`）；如需前端直连后端，可构建时传 `--build-arg VITE_API_BASE=http://127.0.0.1:8080`。
> - 国内网络拉取 npm 依赖较慢时，可传 `--build-arg NPM_REGISTRY=https://registry.npmmirror.com` 加速。
> - 资源限制为合理默认值，可按机器规格在 `docker-compose.yml` 中调整。

> 以下步骤为本地手动开发方式，与 Docker 方式二选一。

### 1. 克隆项目

```bash
git clone https://github.com/suiyueas/Mythweave.git
cd Mythweave
```

### 2. 配置环境变量与密钥

```bash
# 复制配置模板为本地配置（模板中密钥均为环境变量占位符）
copy springboot/src/main/resources/application.example.yml springboot/src/main/resources/application.yml
```

编辑 `application.yml`,填入本地数据库密码与 API Key;或设置环境变量后保持占位符不动:

```powershell
# Windows PowerShell
$env:DB_PASSWORD="你的数据库密码"
$env:REDIS_PASSWORD="你的Redis密码"
$env:ES_PASSWORD="你的ES密码"
$env:DEEPSEEK_API_KEY="sk-xxx"
$env:MIMO_API_KEY="sk-xxx"
$env:QIANWEN_API_KEY="sk-xxx"
$env:JWT_SECRET="随机64位字符串"
```

> ⚠️ `application.yml` 已被 `.gitignore` 排除,含密钥的配置永远不会被提交。

### 3. 创建数据库

```bash
mysql -u root -p < springboot/src/main/resources/sql/mythweave_complete.sql
```

### 4. 启动后端

```bash
cd springboot

# 使用 Maven 启动
mvn spring-boot:run

# 或打包后运行
mvn package
java -jar target/mythweave-1.0.0-SNAPSHOT.jar
```

### 5. 启动前端

```bash
cd vue
npm install
npm run dev
# 访问 http://localhost:5173
```

## ⚙️ 环境变量说明

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 地址/端口/库名 | `localhost` / `3306` / `mythweave` |
| `DB_PASSWORD` | 数据库密码 | 必填 |
| `REDIS_PASSWORD` | Redis 密码 | 必填 |
| `ES_PASSWORD` | Elasticsearch 密码 | 必填 |
| `JWT_SECRET` | JWT 签名密钥（建议 64 位随机串） | 必填 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | 必填 |
| `DEEPSEEK_MAX_TOKEN` | DeepSeek 生成上限 | `16384` |
| `DEEPSEEK_THINKING` | 思考模式：`enabled`（思考）/ `disabled`（非思考） | `enabled` |
| `DEEPSEEK_REASONING_EFFORT` | 思考强度：`low` / `high` / `max` | `high` |
| `DEEPSEEK_DEGRADE_ON_FAILURE` | 思考模式失败时降级为非思考模式重试 | `true` |
| `MIMO_API_KEY` | Mimo API Key | 可选 |
| `QIANWEN_API_KEY` | 千问（通义）API Key | 可选 |
| `AVATAR_PATH` | 头像存储路径 | `./vue/public/avatar` |

## 📄 API 文档

启动后端后，访问 Knife4j API 文档：
- 开发环境：http://localhost:8080/doc.html

## 🧪 功能模块

| 模块 | 说明 |
|------|------|
| `ProjectController` | 作品管理 |
| `ChapterController` | 章节管理 |
| `CharacterController` | 人物管理 |
| `WorldSettingController` | 世界观管理 |
| `OutlineController` | 大纲管理 |
| `PlotController` | 情节线与伏笔管理 |
| `InspirationController` | 灵感管理 |
| `AiChatController` | AI 对话（SSE 流式） |
| `AiConfigController` | AI 参数配置 |
| `AgentOrchestratorController` | 智能体编排 |
| `NovelSetupController` | 作品设定生成 |
| `SentinelController` | 智能哨兵巡检 |
| `ContextController` | 上下文检索 |
| `DashboardController` | 数据仪表盘 |
| `AnalysisController` | 创作分析 |
| `SearchController` | 全局搜索 |
| `ExportController` | 作品导出 |
| `UserController` | 用户管理 / VIP 会员 |
| `AuthController` | 注册登录 |
| `SettingsController` | 系统设置 |
| `SystemController` | 系统健康检查 |

## 📝 License

本项目采用 [MIT License](LICENSE)。