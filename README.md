# 哔哩哔哩MCP完整解决方案

这是一个基于Model Context Protocol (MCP) 的哔哩哔哩视频管理解决方案，包含服务器端和客户端的完整实现。

## 📁 项目结构

```
spring/
├── mcp-bilibili-server-main/     # MCP服务器端 - 提供哔哩哔哩API封装
│   ├── src/
│   │   └── main/
│   │       ├── java/              # Java源代码
│   │       └── resources/         # 配置文件
│   ├── pom.xml                    # Maven配置
│   └── README.md                  # 服务器端文档
│
└── mcp-bilibili-client/          # MCP客户端 - AI智能管理界面
    ├── src/
    │   ├── main/
    │   │   ├── java/              # Java源代码
    │   │   └── resources/         # 配置文件和静态资源
    │   └── test/                  # 测试代码
    ├── pom.xml                    # Maven配置
    └── README.md                  # 客户端文档
```

## 🌟 核心特性

### 🖥️ MCP服务器端 (mcp-bilibili-server-main)
- **基于Spring Boot 3.2.5** + **JDK 21**
- **Spring AI MCP Server 1.0.0** 标准实现
- **STDIO通信模式** - 支持标准输入输出通信
- **完整的哔哩哔哩API封装**

#### 🛠️ 提供的MCP工具
1. **setSessdata** - 设置哔哩哔哩登录状态
2. **searchVideos** - 基础视频搜索
3. **searchVideosFromWeb** - 高级视频搜索（支持排序、筛选）
4. **createDownloadTask** - 创建视频下载任务
5. **getDownloadStatus** - 查询下载任务状态
6. **getServerStatus** - 获取服务器运行状态

### 🤖 MCP客户端 (mcp-bilibili-client)
- **基于Spring Boot 3.2.5** + **JDK 21**
- **Spring AI MCP Client 1.0.0** 标准实现
- **集成通义千问AI** - 智能意图分析
- **双模式界面** - AI聊天 + 手动控制
- **实时步骤展示** - 完整的操作流程可视化

#### 🎯 客户端功能
1. **AI智能助手** - 自然语言交互，自动识别用户意图
2. **手动控制台** - 传统的API调用界面
3. **实时状态监控** - 连接状态、工具列表、服务器状态
4. **批量操作支持** - 搜索后自动批量下载
5. **错误处理机制** - 环环相扣，快速失败

## 🚀 快速开始

### 📋 环境要求
- **Java 21+**
- **Maven 3.6+**
- **哔哩哔哩账号** (用于获取SESSDATA)
- **通义千问API密钥** (用于AI功能)

### 🔧 安装与配置

#### 1. 克隆项目
```bash
git clone https://github.com/q2260391948/bilibili-mcp.git
```

#### 2. 配置服务器端
```bash
cd mcp-bilibili-server-main
cp src/main/resources/application-example.yml src/main/resources/application.yml
# 编辑配置文件，设置必要的参数
```

#### 3. 配置客户端
```bash
cd ../mcp-bilibili-client
cp src/main/resources/application-example.yml src/main/resources/application.yml
# 配置通义千问API密钥和MCP服务器路径
```

#### 4. 构建项目
```bash
# 构建服务器端
cd mcp-bilibili-server-main
mvn clean package -DskipTests

# 构建客户端
cd ../mcp-bilibili-client
mvn clean package -DskipTests
```

### 🏃‍♂️ 运行应用

#### 一：进入server

更新配置

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/a3f43e5aa8224f0f2c2de95a2f3602b.png)

打包

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/999662cf383714e92b1d2cfb038e7fc.png)

#### 二：使用client进行调用
本地Client替换为本地的jar包路径

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/8a381016d42393958fa3e98c65d6217.png)

启动

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/39de94ef66c878caf6a3e7bca24d9c5.png)

#### 三：使用外部client进行调用

配置 **cursor** mcp配置

```json
{
  "mcpServers": {
    "bilibili-downloader": {
      "command": "C:\\Users\\Administrator\\.jdks\\ms-21.0.7\\bin\\java.exe",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar",
         //此处需替换为本地 Serever jar包绝对路径
        "E:\\学习\\MCP\\spring\\mcp-bilibili-server-main\\target\\bilibili-mcp-server-1.0.0-SNAPSHOT.jar"
      ],
      "env": {
        "JAVA_HOME": "C:\\Users\\Administrator\\.jdks\\ms-21.0.7"
      }
    }
  }
}

```

效果

**绿色**表示已经连接到了 MCP server，红色则表示连接失败

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/7e419d4e244d90929db482d7ca160fa.png)

浅浅调用一下吧~

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/b5b78e150a23eb8082c392f362947c9.png)

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/60798d1a477db6a8960ffb1f247c4c1.png)

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/5b20606cc83f0453a35a9319bb078e5.png)

Cherry Studio 调用

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/a59a2b83d6b8ff0faba6ce3abf055b0.png)

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/bee0d41ca79803eb5ae0e98e048316b.png)

### 🌐 访问界面

启动客户端后，可以通过以下地址访问：

- **AI智能助手**: http://localhost:8090/ai-chat.html
- **手动控制台**: http://localhost:8090/index.html
- **API文档**: http://localhost:8090/swagger-ui.html (如果启用)

## 📖 使用指南

### 🤖 AI智能助手使用

AI助手支持自然语言交互，以下是一些使用示例：

#### 1. 设置登录信息
```
设置SESSDATA=XXX
```

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/image.png)

#### 2. 搜索视频

```
搜索JAVA学习的视频
```

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/cbfd1db342402885902f5bb999aa8a3.png)

#### 3. 下载(单个/批量)

```
下载JAVA学习的所有视频
```

单个

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/d6fe5f7548d7a0ace506d07c5f517bb.png)

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/eb01ad7e6a0bdb1741bd078c97fc87f.png)

批量

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/c83098231beb5dc581335d461037bff.png)

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/cc15e708b56b02b8498430814dd20f1.png)

#### 4. 检查状态

```
检查系统状态
```

### 🎛️ 手动控制台使用

手动控制台提供传统的表单界面，支持：
- 系统状态监控
- 登录信息设置
- 视频搜索（基础和高级）
- 下载任务管理

## 🏗️ 技术架构

### 📊 整体架构图
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   前端界面       │    │   MCP客户端      │    │   MCP服务器     │
│ (HTML/JS/CSS)  │    │ (Spring Boot)    │    │ (Spring Boot)   │
├─────────────────┤    ├──────────────────┤    ├─────────────────┤
│ • AI聊天界面    │───▶│ • 通义千问集成   │───▶│ • 哔哩哔哩API   │
│ • 手动控制台    │    │ • MCP客户端      │    │ • 下载管理      │
│ • 实时反馈      │    │ • 意图分析       │    │ • 状态监控      │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │   AI分析引擎     │    │  哔哩哔哩平台   │
                       │ (通义千问API)    │    │ (外部API服务)   │
                       └──────────────────┘    └─────────────────┘
```

### 🔧 核心组件

#### MCP服务器端
- **BilibiliMcpServerApplication** - 主启动类
- **MCP工具实现** - 各种哔哩哔哩操作的封装
- **STDIO通信** - 标准输入输出通信协议
- **配置管理** - Spring Boot配置体系

#### MCP客户端
- **BilibiliMcpClientApplication** - 主启动类
- **SimpleAIService** - 基于推理的意图分析
- **AIBilibiliService** - 通义千问AI集成（可选）
- **BilibiliClientService** - MCP客户端调用封装
- **Web界面** - AI聊天和手动控制界面

### 📡 通信协议

项目使用标准的MCP (Model Context Protocol) 进行通信：

```
客户端 ──STDIO──▶ 服务器
       ◀──────── 
```

- **协议版本**: MCP 1.0.0
- **通信方式**: STDIO (标准输入输出)
- **数据格式**: JSON-RPC 2.0
- **工具调用**: 同步调用模式

## 🛠️ 开发指南

### 📝 添加新的MCP工具

#### 服务器端
1. 在服务器端实现新的工具方法
2. 添加对应的参数验证
3. 更新工具列表

#### 客户端
1. 在`BilibiliClientService`中添加调用方法
2. 在AI服务中添加意图识别
3. 更新前端界面（如需要）

### 🧪 测试

#### 单元测试
```bash
# 服务器端测试
cd mcp-bilibili-server-main
mvn test

# 客户端测试
cd mcp-bilibili-client
mvn test
```

#### 集成测试
```bash
# 启动服务器后，运行客户端测试
mvn test -Dtest=IntegrationTest
```

### 📦 构建与部署

#### 开发环境
```bash
mvn spring-boot:run
```

#### 生产环境
```bash
mvn clean package
java -jar target/app.jar
```

#### Docker部署
```dockerfile
FROM openjdk:21-jre-slim
COPY target/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 🔧 配置说明

### 服务器端配置 (mcp-bilibili-server-main)
```yaml
spring:
  ai:
    mcp:
      server:
        stdio: true  # 启用STDIO通信
  application:
    name: bilibili-mcp-server

# 哔哩哔哩相关配置
bilibili:
  api:
    base-url: https://api.bilibili.com
    timeout: 30s
  download:
    path: ./downloads
    concurrent: 3
```

### 客户端配置 (mcp-bilibili-client)
```yaml
spring:
  ai:
    dashscope:
      api-key: ${AI_API_KEY}  # 通义千问API密钥
      chat:
        options:
          model: qwen-plus
    mcp:
      client:
        enabled: true
        stdio:
          connections:
            bilibili-server:
              command: java
              args:
                - -jar
                - ../mcp-bilibili-server-main/target/bilibili-mcp-server-1.0.0-SNAPSHOT.jar

server:
  port: 8090
```

## 🔍 故障排除

### 常见问题

#### 1. MCP连接失败
**症状**: 客户端显示"MCP客户端未连接"
**解决方案**:
- 检查服务器是否正常启动
- 确认JAR文件路径正确
- 查看日志中的详细错误信息

#### 2. AI调用失败
**症状**: AI分析返回错误
**解决方案**:

- 检查通义千问API密钥是否正确
- 确认网络连接正常
- 查看API调用限制

#### 3. 视频下载失败
**症状**: 下载任务创建失败
**解决方案**:
- 确认SESSDATA已正确设置
- 检查视频链接或BV号有效性
- 确认磁盘空间充足

#### 4. 权限问题
**症状**: 无法访问哔哩哔哩API
**解决方案**:
- 检查SESSDATA是否过期
- 确认账号状态正常
- 检查IP是否被限制

### 📋 日志排查

#### 启用调试日志
```yaml
logging:
  level:
    com.demo.mcp: DEBUG
    org.springframework.ai: DEBUG
    root: INFO
```

#### 关键日志位置
- **MCP通信日志**: `org.springframework.ai.mcp`
- **业务逻辑日志**: `com.demo.mcp`
- **HTTP请求日志**: `org.springframework.web`

## 📊 性能监控

### 关键指标
- **MCP连接状态**: 客户端与服务器的连接健康度
- **API响应时间**: 哔哩哔哩API调用延迟
- **下载任务数量**: 当前活跃的下载任务
- **内存使用率**: JVM内存占用情况

### 监控端点
- **健康检查**: `/actuator/health`
- **指标信息**: `/actuator/metrics`
- **MCP状态**: `/api/bilibili/status`

## 🔐 安全说明

### 敏感信息保护
- **SESSDATA**: 妥善保管，定期更换
- **API密钥**: 使用环境变量，不要硬编码
- **日志脱敏**: 避免在日志中输出敏感信息

### 网络安全
- **HTTPS**: 生产环境建议使用HTTPS
- **防火墙**: 限制不必要的端口访问
- **认证授权**: 考虑添加用户认证机制

## 🤝 贡献指南

### 提交代码
1. Fork项目
2. 创建特性分支
3. 提交更改
4. 创建Pull Request

### 代码规范
- 使用Java代码规范
- 添加必要的注释
- 编写单元测试
- 更新相关文档

## 📄 许可证

本项目采用 [MIT许可证](LICENSE)

## 📞 联系方式

- **项目维护者**: [程序员小张]
- **邮箱**: [2260391948@qq.com]
- **微信**

![](https://raw.githubusercontent.com/q2260391948/bilibili-mcp/refs/heads/main/img/e860618f2f9b981d0a8b2e57b0e28bb.png)

## 🎯 路线图

### 即将推出
- [ ] 支持更多视频平台
- [ ] 增加批量操作优化
- [ ] 添加用户权限管理
- [ ] 支持Docker容器化部署
- [ ] 添加WebSocket实时通信

### 长期规划
- [ ] 支持分布式部署
- [ ] 添加机器学习推荐
- [ ] 集成更多AI模型
- [ ] 支持插件扩展机制

---

> 💡 **提示**: 这是一个教育和学习项目，仅作为学习使用，遵守相关平台的使用条款。 
