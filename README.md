# 民国年华小程序后端

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.6.13-green" alt="Spring Boot">
  <img src="https://img.shields.io/badge/JDK-11-blue" alt="JDK">
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL">
  <img src="https://img.shields.io/badge/MyBatis%20Plus-3.5.3-orange" alt="MyBatis Plus">
  <img src="https://img.shields.io/badge/Alibaba%20OSS-3.18.1-red" alt="OSS">
</p>

## 项目简介

民国年华小程序后端是一个基于 Spring Boot 2.6.13 + MySQL 8.0 + MyBatis-Plus 构建的微信小程序后端服务。提供完整的主题晚宴预订、微信支付、核销验证等功能。

**主要特性：**
- ✅ 微信登录认证
- ✅ JWT Token 鉴权
- ✅ 主题场次预订
- ✅ 微信支付集成
- ✅ 核销码自动生成
- ✅ 二维码核销验证
- ✅ 订单管理
- ✅ 阿里云OSS文件存储

## 技术栈

### 核心技术
- **后端框架**: Spring Boot 2.6.13
- **JDK版本**: JDK 11
- **数据库**: MySQL 8.0
- **ORM框架**: MyBatis-Plus 3.5.3.2
- **连接池**: Druid 1.2.18

### 工具库
- **微信生态**: 微信支付API、微信登录
- **二维码**: ZXing 3.5.2（Hutool集成）
- **对象存储**: 阿里云OSS 3.18.1
- **JWT认证**: jjwt 0.11.5
- **工具包**: Hutool 5.8.18

### 文档和测试
- **API文档**: Swagger 3.0.0（OpenAPI 3.0）
- **接口测试**: Postman 兼容

## 项目结构

```
nianhua-wechat-miniprogram-backend/
├── src/main/java/org/exh/nianhuawechatminiprogrambackend/
│   ├── config/          # 配置类
│   ├── controller/      # RESTful控制器
│   ├── dto/            # 数据传输对象
│   ├── entity/         # 数据库实体
│   ├── enums/          # 枚举类
│   ├── exception/      # 自定义异常
│   ├── mapper/         # MyBatis-Plus Mapper
│   ├── service/        # 服务接口
│   │   └── impl/       # 服务实现
│   └── util/           # 工具类
├── src/main/resources/
│   ├── application.yml    # Spring Boot配置
│   ├── .env              # 环境变量（敏感信息）
│   └── db/schema.sql      # 数据库初始化脚本
├── target/              # 打包输出目录
└── pom.xml              # Maven依赖配置
```

## 快速开始

### 环境要求
- JDK 11+
- Maven 3.6+
- MySQL 8.0

### 1. 配置环境变量

创建 `.env` 文件（参考 `.env.example`）：

```bash
# 数据库配置
DB_PASSWORD=your_db_password

# JWT配置
JWT_SECRET=xxxx

# 微信小程序配置
WX_APPID=wx9d30dac0xxxxxxxx
WX_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# 微信支付配置
WX_MCHID=your_mchid
WX_PRIVATE_KEY_PATH=/path/to/apiclient_key.pem
WX_SERIAL_NUMBER=your_serial_number
WX_API_V3_KEY=your_apiv3_key
WX_NOTIFY_URL=https://your-domain.com/api/v1/payment/notify

# 阿里云OSS配置
OSS_ACCESS_KEY_ID=your_access_key_id
OSS_ACCESS_KEY_SECRET=your_access_key_secret
OSS_ENDPOINT=oss-cn-shanghai.aliyuncs.com
OSS_BUCKET_DOMAIN=your-bucket.oss-cn-shanghai.aliyuncs.com
OSS_BUCKET_NAME=your-bucket
```

### 2. 初始化数据库

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. 编译项目

```bash
# 设置JDK 11环境
export JAVA_HOME=/usr/local/opt/openjdk@11

# 编译
mvn clean compile

# 打包
mvn clean package
```

### 4. 启动应用

```bash
java -jar target/nianhua-wechat-miniprogram-backend-0.0.1-SNAPSHOT.jar
```

默认端口：8080

## API文档

启动应用后，访问 Swagger UI：

```
http://localhost:8080/swagger-ui.html
```

## API接口列表

### 核销模块

| 接口路径 | 方法 | 功能 | 认证 |
|---------|------|------|------|
| `/api/v1/verification-codes` | GET | 获取核销码 | Bearer Token |
| `/api/v1/verification/verify` | POST | 核销验证（核对信息） | Bearer Token |
| `/api/v1/verification/confirm` | POST | 核销确认（实际核销） | Bearer Token |

### 支付模块

| 接口路径 | 方法 | 功能 | 认证 |
|---------|------|------|------|
| `/api/v1/payment/create-order` | POST | 创建支付订单 | Bearer Token |
| `/api/v1/payment/notify` | POST | 微信支付回调通知 | 无 |
| `/api/v1/payment/status/{orderNo}` | GET | 查询支付状态 | Bearer Token |
| `/api/v1/payment/refund` | POST | 申请退款 | Bearer Token |

### 预订模块

| 接口路径 | 方法 | 功能 | 认证 |
|---------|------|------|------|
| `/api/v1/bookings/themePageInfo` | GET | 主题预订详情页 | Bearer Token |
| `/api/v1/bookings/checkSeats` | GET | 校验座位是否可满足 | Bearer Token |
| `/api/v1/bookings/enselectSeatDetail` | GET | 场次座位详情 | Bearer Token |
| `/api/v1/bookings/createOrder` | POST | 创建预订订单 | Bearer Token |
| `/api/v1/bookings/queryDetail` | GET | 获取预订详情 | Bearer Token |

### 用户模块

| 接口路径 | 方法 | 功能 | 认证 |
|---------|------|------|------|
| `/api/v1/auth/wechat-login` | POST | 微信登录 | 无 |
| `/api/v1/auth/refresh` | POST | 刷新Token | Bearer Token |

### 其他接口

| 接口路径 | 方法 | 功能 | 认证 |
|---------|------|------|------|
| `/api/v1/uploadFile` | POST | 上传文件 | Bearer Token |
| `/api/v1/homepage/query` | GET | 商品查询 | 无 |
| `/api/v1/homepage/photoStore` | GET | 写真列表 | 无 |

**总计**: 20个用户端接口

## 核销流程示例

### 1. 用户支付后获得核销码

用户完成支付后，系统自动生成核销码和二维码。

### 2. 管理员扫码验证

```javascript
// 管理员扫描二维码，获取核销码
GET /api/v1/verification-codes?orderNo=202511291235001234

// 响应
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "code": "A1B2C3D4",
    "qrCodeUrl": "https://bucket.oss.com/qrcode.jpg",
    "status": 0
  }
}
```

### 3. 核销验证（核对信息）

```javascript
// 管理员扫码后验证订单信息
POST /api/v1/verification/verify
{
  "code": "A1B2C3D4"
}

// 响应
{
  "code": 0,
  "message": "success",
  "data": {
    "orderInfo": {
      "orderNo": "202511291235001234",
      "themeTitle": "民国年华 - 上海滩之夜",
      "amount": 198.00,
      "peopleCount": 2,
      "contactName": "张三",
      "contactPhone": "13800138000",
      "sessionTime": "2025-12-03T19:00:00"
    }
  }
}
```

### 4. 核销确认（实际核销）

```javascript
// 管理员确认核销
POST /api/v1/verification/confirm
{
  "code": "A1B2C3D4",
  "remarks": "用户已入场"
}

// 响应
{
  "code": 0,
  "message": "核销成功",
  "data": {
    "verificationId": 1,
    "code": "A1B2C3D4",
    "verifiedAt": "2025-12-02T20:00:00",
    "adminId": 0,
    "remarks": "用户已入场",
    "orderInfo": {
      "orderNo": "202511291235001234",
      "themeTitle": "民国年华 - 上海滩之夜",
      "amount": 198.00
    }
  }
}
```

## 项目特点

### 1. 完整的核销流程
- 支付自动生成核销码
- 二维码生成和存储
- 验证核对机制
- 确认核销状态管理

### 2. 规范的开发
- 标准的MVC架构
- 统一的API响应格式
- 完整的Swagger文档
- 全局异常处理

### 3. 安全设计
- JWT Token认证
- Bearer Token鉴权
- 敏感信息环境变量管理
- .env不提交到Git

### 4. 企业级特性
- 事务管理（核销确认）
- 逻辑删除
- 详细的日志记录
- 统一的业务异常

## 开发状态

✅ **已完成**:
- 核销模块完整实现
- 支付模块（微信支付集成）
- 预订模块
- 用户认证模块
- JDK 11升级

⚠️ **待测试**:
- 微信支付回调（需要真实支付环境）
- 微信登录（需要微信小程序环境）

## 版本信息

- **当前版本**: v0.2.0-pre-release
- **Java版本**: JDK 11
- **Spring Boot**: 2.6.13
- **MyBatis-Plus**: 3.5.3.2

## 贡献指南

1. 遵循现有代码风格
2. 保持MVC架构规范
3. 所有新接口添加到Swagger文档
4. 敏感信息使用环境变量
5. 提交前运行 `mvn clean package -DskipTests`

## 许可证

MIT License

## 联系方式

- 项目地址: https://github.com/alyxe1/alyxe1-minguonianhua-java-mysql-backend
- 联系邮箱: claude@nianhua.com

---

**Made with ❤️ by Claude Code**
