# Docker 部署指南

使用 Docker Compose 一键部署项目（MySQL + Spring Boot 应用）

## 快速开始

### 1. 准备一些必要的文件

确保你有以下文件：

```bash
# 微信支付证书文件
apiclient_key.pem

# Docker环境变量配置文件（从.env.docker复制）
.env
```

将 `.env.docker` 复制为 `.env`：

```bash
cp .env.docker .env
```

并编辑 `.env` 文件，填入真实的配置信息。

### 2. 构建并启动项目

```bash
# 在包含 docker-compose.yml 的目录中执行
docker compose up -d
```

该命令将自动：
- 构建 Spring Boot 应用镜像
- 创建并启动 MySQL 容器
- 创建并启动应用容器
- 配置网络连接

### 3. 查看容器状态

```bash
# 查看容器运行状态
docker compose ps

# 查看应用日志
docker compose logs app

# 查看 MySQL 日志
docker compose logs mysql
```

### 4. 检查应用是否正常运行

```bash
# 查看应用健康状态
curl http://localhost:8080/actuator/health

# 检查 Swagger 文档
open http://localhost:8080/swagger-ui.html
```

## 管理命令

### 启动服务

```bash
docker compose up -d
```

### 停止服务

```bash
# 停止但不删除容器
docker compose stop

# 停止并删除容器
docker compose down

# 停止、删除容器和卷（数据会丢失）
docker compose down -v
```

### 重启服务

```bash
# 重启所有服务
docker compose restart

# 重启指定服务
docker compose restart app
docker compose restart mysql
```

### 查看日志

```bash
# 查看所有日志
docker compose logs

# 实时查看日志
docker compose logs -f

# 只查看应用的日志
docker compose logs -f app
```

### 进入容器

```bash
# 进入应用容器
docker compose exec app bash

# 进入 MySQL 容器
docker compose exec mysql mysql -u root -p
```

## 数据持久化

MySQL 数据存储在 Docker 卷中：

```bash
# 查看卷
docker volume ls

# 查看具体卷的位置
docker volume inspect $(docker compose config --volumes | grep mysql-data | cut -d: -f2 | tr -d ' ')
```

## 配置文件说明

### docker-compose.yml

包含两个服务：
- **mysql**: MySQL 8.0 数据库
  - 端口：3306（可外部访问）
  - 数据持久化：mysql-data 卷
  - 健康检查：每30秒检查一次

- **app**: Spring Boot 应用
  - 端口：8080
  - 依赖：等待 MySQL 健康后启动
  - 健康检查：启动后60秒开始检查

### Dockerfile

基于 OpenJDK 11 构建 Spring Boot 应用镜像：
- 使用非 root 用户运行（安全考虑）
- 暴露 8080 端口
- 包含健康检查

## 环境变量

所有配置通过 `.env` 文件设置，主要变量：

- **数据库**: `DB_PASSWORD`
- **JWT**: `JWT_SECRET`
- **微信**: `WX_APPID`, `WX_SECRET`, `WX_MCHID`等
- **OSS**: `OSS_ACCESS_KEY_ID`, `OSS_ACCESS_KEY_SECRET`等

## 故障排查

### 1. MySQL 无法连接

```bash
# 检查 MySQL 是否正常运行
docker compose logs mysql

# 进入 MySQL 容器手动连接
docker compose exec mysql mysql -u root -p${DB_PASSWORD}

# 检查应用的数据库连接
cat logs/app.log | grep "SQLException"
```

### 2. 应用启动失败

```bash
# 查看应用日志
docker compose logs app

# 常见问题：
# - 数据库连接失败：检查 .env 中的 DB_PASSWORD
# - 微信支付证书找不到：确保 apiclient_key.pem 存在
# - 阿里云OSS配置错误：检查 OSS 环境变量
```

### 3. 端口被占用

如果8080或3306端口被占用，修改 `docker-compose.yml`：

```yaml
services:
  app:
    ports:
      - "8081:8080"  # 改为8081

  mysql:
    ports:
      - "3307:3306"  # 改为3307
```

## 查看应用日志

```bash
# 实时查看所有日志
docker compose logs -f

# 只查看应用日志
docker compose logs -f app

# 查看最新100行日志
docker compose logs --tail=100 app
```

## 更新和重建

### 修改代码后重新构建

```bash
# 重新构建并启动
docker compose up -d --build

# 只重新构建应用服务
docker compose build app
```

### 更新环境变量

1. 修改 `.env` 文件
2. 重启服务：
```bash
docker compose restart
```

## 注意事项

1. **首次启动较慢**：MySQL 初始化需要时间（约30-60秒）
2. **微信支付证书**：确保 `apiclient_key.pem` 文件存在，否则支付功能无法使用
3. **数据备份**：定期备份MySQL数据卷，防止数据丢失
4. **环境变量**：不要提交 `.env` 文件到Git（已在 .gitignore 中）

## 性能优化

### MySQL 配置
在 `docker/mysql/my.cnf` 中可以调整：
- `innodb_buffer_pool_size`：根据系统内存调整
- `max_connections`：最大连接数

### JVM 参数
在 `Dockerfile` 中可以添加 JAVA_OPTS：
```dockerfile
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
```

## 卸载和清理

```bash
# 停止并删除容器、网络、卷
docker compose down -v

# 只删除卷（保留容器）
docker volume prune -f

# 清理无用镜像
docker image prune -a
```

## 了解更多

- [Docker Compose 官方文档](https://docs.docker.com/compose/)
- [Spring Boot Docker 部署指南](https://spring.io/guides/topicals/spring-boot-docker/)
- [项目 README](../README.md)
- [项目 CLAUDE.md](../CLAUDE.md)
