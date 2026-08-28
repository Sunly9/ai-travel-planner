<div align="center">

# 🤖 AI Travel Planner

> 基于大语言模型的智能旅游规划助手 · Spring Boot + Vue 3 全栈项目

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/)
[![Vue](https://img.shields.io/badge/Vue-3.x-42b883?style=for-the-badge&logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8+-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Node.js](https://img.shields.io/badge/Node.js-18+-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)](https://nodejs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

[功能特性](#-功能特性) · [技术栈](#-技术栈) · [快速开始](#-快速开始) · [项目结构](#-项目结构) · [接口文档](#-主要接口)

</div>

---

## ✨ 功能特性

| 功能 | 说明 |
|---|---|
| 🧠 **AI 智能规划** | 根据目的地、预算、天数，大模型生成详细行程（SSE 流式输出） |
| 🔐 **用户系统** | 注册 / 登录 / JWT 鉴权 / 个人资料管理 |
| 📋 **行程管理** | 保存行程 / 查看历史 / 一键编辑 |
| ⭐ **景点收藏** | 喜欢的景点随时收藏，快速回顾 |
| 🌤️ **实时天气** | 目的地 3 天天气预报，出行前心中有数 |
| 🗺️ **景点地图** | 高德地图标注景点，按天数/时段筛选，行程一目了然 |
| 🧭 **一键导航** | 点击景点直接跳转高德地图 APP/网页版导航 |
| 💬 **AI 对话** | 旅游相关问题智能问答 |
| 📸 **行程分享** | 生成行程图片，下载或分享给朋友 |

## 🛠️ 技术栈

### 后端
- [Spring Boot 4.x](https://spring.io/projects/spring-boot) — Web 框架
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa) — 数据访问
- [MySQL 8](https://www.mysql.com/) — 数据库
- [OkHttp](https://square.github.io/okhttp/) — HTTP 客户端（调用 LLM API）
- [JWT](https://jwt.io/) — 无状态鉴权
- [SSE](https://developer.mozilla.org/docs/Web/API/Server-sent_events) — 流式输出

### 前端
- [Vue 3](https://vuejs.org/) + Composition API — UI 框架
- [Vant 4](https://vant-ui.github.io/vant/) — 移动端 UI 组件库
- [Vite](https://vitejs.dev/) — 构建工具
- [Pinia](https://pinia.vuejs.org/) — 状态管理
- [Leaflet](https://leafletjs.com/) — 地图

### 第三方服务
| 服务 | 用途 | 是否免费 |
|---|---|---|
| [SiliconFlow](https://cloud.siliconflow.cn) | LLM 推理（GLM-5.1） | 有免费额度 |
| [Open-Meteo](https://open-meteo.com) | 天气数据 | 完全免费 |
| [高德开放平台](https://open.amap.com) | 地理编码 / 地图瓦片 / 导航 | 个人开发者免费 |

## 📁 项目结构

```
ai-travel-planner/
├── travel-project1/                  # 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/org/example/travel_project1/
│       │   ├── controller/           # REST API
│       │   │   ├── AuthController         # 登录注册
│       │   │   ├── TravelController       # AI 行程推荐（SSE）
│       │   │   ├── TravelPlanController   # 行程 CRUD
│       │   │   ├── FavoriteController     # 收藏
│       │   │   ├── WeatherController      # 天气
│       │   │   ├── GeoController          # 高德地理编码代理
│       │   │   ├── ChatMessageController  # AI 对话
│       │   │   └── UserController         # 用户信息
│       │   ├── service/              # 业务逻辑
│       │   ├── repository/           # 数据访问
│       │   ├── entity/               # JPA 实体
│       │   ├── dto/                  # 请求参数
│       │   ├── vo/                   # 返回值
│       │   ├── common/               # 配置 / 过滤器 / 异常处理
│       │   └── utils/                # LLM 调用工具
│       └── resources/
│           ├── application.example.yml   # 配置模板
│           └── ...
├── travl-1/                          # 前端
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── views/                    # 页面
│       ├── components/               # 组件
│       ├── router/                   # 路由
│       ├── stores/                   # Pinia
│       └── utils/                    # 请求封装
└── docs/screenshots/                 # 项目截图
```

## 🚀 快速开始

### 环境要求

| 工具 | 版本 |
|---|---|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8+ |
| Maven | 3.8+ |

### 1. 克隆项目

```bash
git clone https://github.com/Sunly9/ai-travel-planner.git
cd ai-travel-planner
```

### 2. 配置后端

```bash
cd travel-project1
cp src/main/resources/application.example.yml src/main/resources/application.yml
```

编辑 `application.yml`，填入你的密钥：

```yaml
llm:
  api-key: sk-你的SiliconFlow密钥

spring:
  datasource:
    password: 你的MySQL密码

jwt:
  secret: 随便一串50+字符

amap:
  key: 你的高德Web服务Key
```

### 3. 数据库

```sql
CREATE DATABASE travel_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

JPA 配置 `ddl-auto: update`，首次启动自动建表。

### 4. 启动后端

```bash
cd travel-project1
mvn spring-boot:run
# → http://localhost:3200
```

### 5. 启动前端

```bash
cd travl-1
npm install
npm run dev
# → http://localhost:5173
```

## 📡 主要接口

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| POST | `/api/auth/register` | 用户注册 | ❌ |
| POST | `/api/auth/login` | 用户登录 | ❌ |
| POST | `/api/travel/recommend/stream` | AI 行程推荐（SSE 流式） | ❌ |
| POST | `/api/travel/recommend` | AI 行程推荐（同步） | ❌ |
| POST | `/api/plans` | 保存行程 | ✅ |
| GET | `/api/plans` | 我的行程列表 | ✅ |
| GET | `/api/plans/{id}` | 行程详情 | ✅ |
| PUT | `/api/plans/{id}` | 更新行程 | ✅ |
| DELETE | `/api/plans/{id}` | 删除行程 | ✅ |
| GET | `/api/weather` | 天气查询 | ❌ |
| GET | `/api/geo/search` | 地理编码（高德代理） | ❌ |
| POST | `/api/favorites` | 收藏景点 | ✅ |
| GET | `/api/favorites` | 收藏列表 | ✅ |
| DELETE | `/api/favorites/{id}` | 取消收藏 | ✅ |
| POST | `/api/chat` | AI 对话 | ✅ |
| GET | `/api/chat/history` | 对话历史 | ✅ |

## 🔐 安全说明

- `application.yml` 不提交到 Git（已加入 `.gitignore`）
- 密钥通过环境变量或本地配置文件注入
- JWT token 有效期 24 小时，支持自动续期
- 密码 BCrypt 加密存储
- 敏感接口需登录访问

## 📝 License

MIT © [Sunly9](https://github.com/Sunly9)
