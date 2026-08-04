# Mythweave AI - AI智能小说创作平台

<div align="center">

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-brightgreen.svg)](https://vuejs.org/)

**让 AI 成为作者的副脑 — 全能型 AI 写作工作台**

</div>

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
- AI 续写与润色
- 多章节并发写作
- 章节版本管理
- 上下文记忆（RAG + 向量检索）

### 🛡️ 智能哨兵
- 伏笔遗漏巡查
- 逻辑矛盾检测
- 写作节奏分析
- 人物出场统计

### 👑 VIP 会员系统
- AI 功能权限拦截（免费用户限次，VIP 无限畅用）
- 专属写作模板与续写润色服务
- 多档位套餐（月度/季度/年度）
- 微信/支付宝模拟支付
- VIP 到期续费与状态管理

### 🔍 全局搜索
- 基于 Elasticsearch 的全文搜索
- 语义相似度匹配

## 🛠️ 技术栈

### 后端
- **框架**：Spring Boot 3.5 + Java 21
- **ORM**：MyBatis Plus 3.5
- **数据库**：MySQL 8.x
- **缓存**：Redis
- **搜索**：Elasticsearch 8.x
- **API 文档**：Knife4j
- **AI**：DeepSeek API + 千问 Embedding

### 前端
- **框架**：Vue 3.4
- **构建工具**：Vite 5
- **CSS**：Tailwind CSS 4
- **状态管理**：Pinia
- **路由**：Vue Router 4
- **图表**：ECharts 6

## 📦 项目结构

```
AI-novel/
├── springboot/          # Spring Boot 3.5 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/    # Java 源代码
│   │   │   └── resources/
│   │   │       ├── mapper/      # MyBatis 映射文件
│   │   │       ├── sql/         # 数据库 SQL 脚本
│   │   │       └── application*.yml  # 应用配置
│   │   └── test/       # 测试代码
│   └── pom.xml         # Maven 依赖配置
├── .env.example        # 环境变量模板
├── .gitignore          # Git 忽略配置
└── README.md           # 项目说明文档
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.x
- Redis 6.x
- Elasticsearch 8.x

### 1. 克隆项目

```bash
git clone https://github.com/yourusername/AI-novel.git
cd AI-novel
```

### 2. 配置环境变量

```bash
# 复制环境变量模板
copy .env.example .env
# 编辑 .env 填入你的配置

# 或直接编辑配置文件
copy springboot/src/main/resources/application.yml springboot/src/main/resources/application-local.yml
```

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

## ⚙️ 环境变量说明

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `DB_HOST` | MySQL 地址 | `localhost` |
| `DB_PORT` | MySQL 端口 | `3306` |
| `DB_NAME` | 数据库名 | `mythweave` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `***` |
| `REDIS_*` | Redis 配置 | - |
| `ES_*` | Elasticsearch 配置 | - |
| `JWT_SECRET` | JWT 签名密钥 | 256位随机字符串 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | `sk-***` |
| `QW_API_KEY` | 千问 API Key | `sk-***` |

## 📄 API 文档

启动后端后，访问 Knife4j API 文档：
- 开发环境：http://localhost:8080/doc.html

## 🧪 功能模块

| 模块 | 说明 |
|------|------|
| `NovelProjectController` | 作品管理 |
| `ChapterController` | 章节管理 |
| `CharacterController` | 人物管理 |
| `WorldSettingController` | 世界观管理 |
| `OutlineController` | 大纲管理 |
| `AiChatController` | AI 对话 |
| `SentinelController` | 智能哨兵 |
| `SearchController` | 全局搜索 |
| `UserController` | 用户管理 / VIP 会员 |

## 📝 License

本项目采用 [MIT License](LICENSE)。