# 🎧 懒人听书 - 有声书服务平台

<div>
  <p>一个基于Spring Cloud的分布式有声书服务系统，提供完整的有声内容生态解决方案。</p>
</div>

## 📖 项目介绍

懒人听书是一个采用微服务架构设计的有声书服务平台，致力于为用户提供高质量的有声内容体验。

系统涵盖了从内容上传、管理、分发到用户交互的完整业务流程，支持多终端访问，具备高可用性和可扩展性。

无论是小说、教育、商业还是其他类型的有声内容，懒人听书都能提供稳定、高效的服务支持。

## ✨ 核心功能

- 🎙️ 专辑管理：支持有声书专辑的创建、编辑、审核和发布
- 👤 用户服务：包含注册、登录、个人信息管理和会员体系
- 🛒 订单支付：完整的订单流程和多种支付方式集成
- 🔍 智能搜索：基于Elasticsearch的精准内容检索
- 📊 数据分析：用户行为分析和内容热度统计
- 💬 互动评论：支持用户对专辑进行评论和互动
- 📱 多端适配：响应式设计，支持Web、移动端访问

## 🛠️ 技术栈

### 核心框架
- Spring Boot 3.0.5
- Spring Cloud 2022.0.2
- Spring Cloud Alibaba

### 服务组件
- **服务注册与发现**：Nacos
- **API网关**：Spring Cloud Gateway
- **服务熔断与限流**：Sentinel
- **分布式事务**：Seata

### 数据存储
- **关系型数据库**：MySQL 8.0
- **文档数据库**：MongoDB 4.4+
- **缓存**：Redis 5.0+、Redisson
- **搜索引擎**：Elasticsearch 7.8+

### 消息与通信
- **消息队列**：RabbitMQ 3.8+
- **服务通信**：OpenFeign
- **API文档**：Knife4j (Swagger)

### 部署与构建
- **构建工具**：Maven 3.6+
- **容器化**：Docker
- **JDK版本**：JDK 17

## 🚀 快速开始

### 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | 17+ |
| MySQL | 8.0+ |
| Redis | 5.0+ |
| MongoDB | 4.4+ |
| RabbitMQ | 3.8+ |
| Elasticsearch | 7.8+ |
| Nacos | 2.1.0+ |

### 本地部署

1. **克隆项目**
   ```bash
   git clone https://github.com/bfyqjzsgkqycf/tingshu.git
   cd tingshu
   ```

2. **初始化数据库**
   - 执行 `sql/` 目录下的初始化脚本

3. **配置修改**
   - 修改各服务模块下的 `application.yaml` 配置文件
   - 配置数据库连接、Redis、RabbitMQ等中间件信息
   - 配置Nacos服务注册中心地址

4. **构建与启动**
   ```bash
   # 编译打包
   mvn clean package -Dmaven.test.skip=true
   
   # 启动Nacos服务 (单独启动)
   
   # 启动网关服务
   java -jar gateway/target/gateway-1.0.0.jar
   
   # 启动基础服务
   java -jar service/service-user/target/service-user-1.0.0.jar
   java -jar service/service-album/target/service-album-1.0.0.jar
   
   # 启动其他服务
   java -jar service/service-order/target/service-order-1.0.0.jar
   # ... 按需启动其他服务
   ```

5. **访问系统**
   - 服务启动后，访问API网关：`http://localhost:8500`
   - 访问API文档：`http://localhost:8500/doc.html`

## 🔌 API示例

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/album/fileUpload` | POST | 上传专辑封面图片 |
| `/api/album/trackInfo/uploadTrack` | POST | 上传音频文件 |
| `/api/user/userListenProcess/getLatelyTrack` | GET | 获取最近播放记录 |
| `/api/album/info/getAlbumDetail/{albumId}` | GET | 获取专辑详情 |
| `/api/search/searchAlbum` | GET | 搜索专辑 |

完整API文档请查看系统内置的Knife4j文档。

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开Pull Request

## 📜 许可证

本项目基于MIT许可证开源 - 详见 [LICENSE](LICENSE) 文件。

## 📞 联系方式

- 项目维护者：LU SHIJIN
- 邮箱：bfyqjzsgkqycf@gmail.com
- 项目地址：https://github.com/bfyqjzsgkqycf/tingshu

---

<div>
  <p>✨ 感谢您对项目的关注 ✨</p>
</div>
