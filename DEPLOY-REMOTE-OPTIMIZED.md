# 远程 Docker 一键部署指南（优化版）

使用优化后的脚本，实现一键远程部署，无需手动干预。

## 快速开始

### 1. 准备环境

```bash
# 1. 复制环境变量文件
cp .env.docker .env

# 2. 编辑 .env 文件，填入正确的配置信息
vim .env

# 3. 确保项目已打包（如果未打包，脚本会自动提示）
mvn clean package

# 4. 确保你有 SSH PEM 密钥
#    例如：~/.ssh/aws-key.pem 或 ./home-edu.pem
```

### 2. 一键部署

```bash
# 给脚本添加执行权限
chmod +x deploy-to-remote-optimized.sh

# 执行一键部署（替换为你的 IP 和密钥）
./deploy-to-remote-optimized.sh -i 118.178.190.93 -k ./home-edu.pem
```

部署过程大约需要 **3-5 分钟**，包含：
- 环境检查（30秒）
- MySQL 卸载（如需要，1分钟）
- Docker 安装（如需要，2分钟）
- 文件上传（30秒，取决于网络）
- Docker 构建和启动（1分钟）

### 3. 查看部署进度

脚本会显示详细的部署进度，包括：

```
==========================================
  步骤 1: 验证本地环境
==========================================

✓ .env 文件已存在
✓ docker-compose.yml 文件已存在
✓ 项目已打包 (48M)
✓ 数据库初始化脚本已存在

==========================================
  步骤 2: 连接远程服务器并检查环境
==========================================

✓ SSH 连接成功

==========================================
  步骤 3: 检查并卸载远程 MySQL
==========================================

✓ 远程服务器未运行 MySQL

==========================================
  步骤 4: 安装 Docker 环境
==========================================

✓ Docker 已安装
✓ Docker Compose 已安装

==========================================
  步骤 5: 上传项目文件
==========================================

✓ docker-compose.yml
✓ Dockerfile
✓ .env
✓ docker/ 配置目录
✓ src/ 源代码
✓ pom.xml
✓ target/*.jar (应用包)
✓ 文件上传完成

==========================================
  步骤 6: Docker 部署
==========================================

✓ 停止旧容器
✓ 构建 Docker 镜像
✓ 启动 Docker 服务
✓ MySQL 初始化
✓ 数据库表创建完成 (10个表)
✓ Docker 部署完成

==========================================
  步骤 7: 验证部署
==========================================

✓ 服务状态
✓ 数据库状态
✓ 应用健康检查
✓ 部署验证完成

==========================================
  部署完成！
==========================================

✓ 项目已成功部署到远程服务器

服务器信息：
  - 地址: root@118.178.190.93
  - 路径: /opt/nianhua-wechat-miniprogram-backend

服务信息：
  - MySQL: 3306
  - Spring Boot: 8080

访问地址：
  - Swagger UI: http://118.178.190.93:8080/swagger-ui.html
  - 健康检查: http://118.178.190.93:8080/actuator/health

管理命令：
  ssh -i ./home-edu.pem root@118.178.190.93
  cd /opt/nianhua-wechat-miniprogram-backend
  docker compose logs -f app    # 查看实时日志
  docker compose restart        # 重启服务
  docker compose down           # 停止服务
```

## 功能特点

### ✅ 自动化流程

**智能检查**：
- 自动检查 .env、docker-compose.yml 等必要文件
- 自动检查项目是否已打包，未打包会提示
- 自动准备数据库初始化脚本

**MySQL 处理**：
- 自动检测远程服务器是否运行 MySQL
- 如检测到，提供卸载选项，释放 3306 端口
- 完全自动化卸载流程（停止服务、删除包、清理数据）

**Docker 环境**：
- 自动检查 Docker 和 Docker Compose
- 如未安装，自动安装并配置

**文件上传**：
- 自动上传所有必要文件（配置、源码、jar、证书）
- 清晰的进度显示

**数据库初始化**：
- 自动检测 MySQL 是否已初始化表结构
- 如未初始化，自动执行建表脚本
- 验证表数量，确保初始化成功

**部署验证**：
- 自动检查容器状态
- 自动检查数据库状态
- 自动验证应用端口
- 显示最近日志

### 🔧 完整错误处理

脚本包含完善的错误处理机制：
- SSH 连接失败检测
- 文件不存在检测
- 命令执行失败检测
- 服务启动失败检测

### 📊 清晰的输出

- 颜色标记（绿色成功、黄色警告、红色错误、蓝色信息）
- 步骤分隔线
- 实时进度显示
- 部署完成总结

## 命令行参数

```bash
./deploy-to-remote-optimized.sh -i <IP> -k <密钥> [-u <用户>] [-p <路径>]

参数：
  -i, --ip        远程服务器IP地址（必需）
  -k, --key       SSH PEM密钥文件路径（必需）
  -u, --user      SSH用户名（可选，默认：root）
  -p, --path      远程部署路径（可选，默认：/opt/nianhua-wechat-miniprogram-backend）
  -h, --help      显示帮助信息

示例：
  ./deploy-to-remote-optimized.sh -i 192.168.1.100 -k ~/.ssh/key.pem
  ./deploy-to-remote-optimized.sh -i 118.178.190.93 -k ./home-edu.pem
  ./deploy-to-remote-optimized.sh -i 192.168.1.100 -k ~/.ssh/key.pem -u ubuntu
```

## 部署后管理

### 登录服务器

```bash
# 使用密钥登录
ssh -i ./home-edu.pem root@118.178.190.93

# 进入项目目录
cd /opt/nianhua-wechat-miniprogram-backend
```

### 常用命令

```bash
# 查看容器状态
docker compose ps

# 查看实时日志（追踪模式）
docker compose logs -f app          # 应用日志
docker compose logs -f mysql        # MySQL 日志

# 查看最后 50 行日志
docker compose logs app --tail=50

# 重启服务
docker compose restart

# 停止服务
docker compose down

# 启动服务
docker compose up -d

# 重新构建并启动
docker compose up -d --build
```

### 验证部署

```bash
# 检查容器状态
docker compose ps

# 应该看到：
# nianhua-app   Up (healthy)   0.0.0.0:8080->8080/tcp
# nianhua-mysql Up (healthy)   0.0.0.0:3306->3306/tcp

# 检查数据库表
source .env
docker exec nianhua-mysql mysql -u root -p$DB_PASSWORD nianhua -e 'SHOW TABLES;'

# 应该看到 10+ 个表：
# bookings
# daily_sessions
# goods
# orders
# payments
# refunds
# seats
# sessions
# themes
# users
# verification_codes
```

### 访问服务

```bash
# 本地测试（服务器上）
curl http://localhost:8080

# 外部访问
# Swagger UI: http://118.178.190.93:8080/swagger-ui.html
# 健康检查: http://118.178.190.93:8080/actuator/health
```

## 故障排查

### 1. SSH 连接失败

```bash
# 检查密钥权限
chmod 600 ~/.ssh/key.pem

# 测试连接
ssh -i ./home-edu.pem root@118.178.190.93 "echo OK"

# 检查服务器安全组/防火墙
# 确保 22 端口开放
```

### 2. 部署失败

```bash
# 查看详细错误
./deploy-to-remote-optimized.sh -i 118.178.190.93 -k ./home-edu.pem 2>&1 | tee deploy.log

# 常见问题：
# - 3306 端口被占用：脚本会自动检测并提示卸载 MySQL
# - .env 文件不存在：请复制 .env.docker 并编辑
# - 项目未打包：脚本会提示执行 mvn clean package
```

### 3. MySQL 连接失败

```bash
# 检查 MySQL 容器状态
docker compose ps mysql

# 查看 MySQL 日志
docker compose logs mysql --tail=50

# 检查端口占用
netstat -tlnp | grep 3306

# 进入 MySQL 容器
docker exec -it nianhua-mysql bash
mysql -u root -p
```

### 4. 应用启动失败

```bash
# 查看应用日志
docker compose logs app --tail=100

# 常见问题：
# - 表不存在：检查数据库初始化日志
# - 连接失败：检查 MySQL 状态和网络
# - 配置错误：检查 .env 文件
```

## 工作原理

### 部署流程

1. **本地验证**
   - 检查所有必要文件
   - 确保项目已打包
   - 生成数据库初始化脚本

2. **远程连接**
   - SSH 连接测试
   - 检查远程 MySQL
   - 可选：自动卸载 MySQL

3. **环境准备**
   - 检查 Docker 环境
   - 自动安装 Docker 和 Docker Compose（如需）
   - 配置用户权限

4. **文件上传**
   - 创建远程目录
   - 上传所有配置文件
   - 上传应用 jar 包

5. **Docker 部署**
   - 停止旧容器
   - 构建新镜像
   - 启动 MySQL 和应用
   - 验证数据库初始化

6. **验证部署**
   - 检查容器状态
   - 验证表结构
   - 测试应用端口
   - 显示访问信息

### 数据库初始化

MySQL 容器使用 Docker 官方镜像的自动初始化机制：
- `/docker-entrypoint-initdb.d` 目录下的 `.sql` 文件
- 按字母顺序执行
- 仅在首次启动时执行（数据卷为空）

### MySQL 卸载流程

完全卸载远程 MySQL：
1. 停止 MySQL 服务
2. 删除 MySQL 包（yum/apt）
3. 删除数据目录（/var/lib/mysql）
4. 删除配置文件（/etc/my.cnf）
5. 释放 3306 端口

## 性能

- 首次部署：3-5 分钟
- 重新部署：2-3 分钟（跳过 Docker 安装）
- 网络要求：上传 48MB jar 文件
- 服务器要求：至少 2GB 内存，10GB 磁盘空间

## 安全建议

1. **及时关闭端口**
   ```bash
   # 如果不需要外部访问 MySQL，关闭 3306
   firewall-cmd --zone=public --remove-port=3306/tcp --permanent
   firewall-cmd --reload
   ```

2. **配置 HTTPS**
   - 使用 Nginx + SSL 证书
   - 反向代理到 8080 端口

3. **修改默认密码**
   ```bash
   # 首次部署后修改数据库密码
   mysql -u root -p
   ALTER USER 'root'@'%' IDENTIFIED BY '新密码';
   ```

4. **定期备份**
   ```bash
   # 备份 MySQL 数据卷
   docker run --rm -v nianhua-wechat-miniprogram-backend_mysql-data:/data -v $(pwd):/backup busybox tar cvf /backup/mysql-backup.tar /data
   ```

## 更新日志

### v2.0.0 (2024-12-09)
- ✅ 一键部署，无需手动干预
- ✅ 自动检测并卸载远程 MySQL
- ✅ 自动验证数据库初始化
- ✅ 完善的错误处理和重试机制
- ✅ 清晰的进度显示和部署报告
- ✅ 部署后自动验证

### v1.0.0 (2024-12-02)
- 初始版本
- 支持远程部署
- 手动配置步骤较多

## 总结

使用优化版脚本，实现：
- **一键部署**：单个命令完成全部流程
- **零配置**：自动处理 MySQL 卸载、Docker 安装等复杂操作
- **零干预**：无需手动执行任何命令
- **可靠验证**：自动检查部署状态
- **快速上线**：3-5 分钟即可完成部署

下次部署只需执行：
```bash
./deploy-to-remote-optimized.sh -i <IP> -k <密钥>
```
