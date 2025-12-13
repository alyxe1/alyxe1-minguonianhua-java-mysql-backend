#!/bin/bash

# 民国年华小程序后端 - Docker 一键启动脚本

set -e

echo "=========================================="
echo "  民国年华小程序后端 - Docker 部署"
echo "=========================================="
echo ""

# 准备数据库初始化脚本
echo "📋 准备数据库初始化脚本..."
if [ ! -f docker/mysql/init/02-schema.sql ] || [ ! -s docker/mysql/init/02-schema.sql ]; then
    echo "正在从 src/main/resources/db/schema.sql 生成数据库初始化脚本..."
    # 删除 CREATE DATABASE 和 USE 语句，保留所有建表语句
    grep -v "CREATE DATABASE" src/main/resources/db/schema.sql | grep -v "^USE " > docker/mysql/init/02-schema.sql
    echo "✅ 数据库初始化脚本已生成"
else
    echo "✅ 数据库初始化脚本已存在"
fi
echo ""

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: 未安装 Docker"
    echo "请先安装 Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# 检查 Docker Compose
if ! command -v docker compose &> /dev/null; then
    echo "❌ 错误: Docker Compose 未安装"
    echo "请安装 Docker Compose 或更新 Docker Desktop"
    exit 1
fi

# 检查 .env 文件
if [ ! -f .env ]; then
    echo "⚠️  警告: 未找到 .env 文件"
    echo ""
    echo "将使用 .env.docker 作为模板创建 .env 文件"
    echo "请修改 .env 文件中的配置信息"
    echo ""

    if [ -f .env.docker ]; then
        cp .env.docker .env
        echo "✅ 已创建 .env 文件"
        echo ""
        echo "请编辑 .env 文件并填入正确的配置信息，然后重新运行此脚本"
        echo ""
        exit 1
    else
        echo "❌ 错误: 未找到 .env.docker 模板文件"
        exit 1
    fi
fi

# 检查 apiclient_key.pem（微信支付证书）
if [ ! -f apiclient_key.pem ]; then
    echo "⚠️  警告: 未找到微信支付证书文件 apiclient_key.pem"
    echo "如果你没有微信支付证书，可以先忽略此警告"
    echo "但支付功能将无法使用"
    echo ""
    read -p "是否继续? (y/n): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# 提示用户检查配置
echo ""
echo "⚠️  重要提示："
echo "   请确保 .env 文件中的配置信息已正确填写"
echo "   特别是数据库密码、JWT密钥、微信配置等"
echo ""
read -p "是否继续部署? (y/n): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi

echo ""
echo "=========================================="
echo "  开始构建和启动服务..."
echo "=========================================="
echo ""

# 停止并清理旧的容器
echo "🛑 停止旧的容器..."
docker compose down

echo ""
echo "🔨 构建和启动服务..."

# 构建和启动
docker compose up -d --build

# 等待服务启动
echo ""
echo "⏳ 等待服务启动..."
sleep 30

# 检查服务状态
echo ""
echo "📊 检查服务状态："
docker compose ps

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "✅ MySQL 服务:"
echo "   - 端口: 3306"
echo "   - 数据卷: mysql-data"
echo ""
echo "✅ Spring Boot 应用:"
echo "   - 端口: 8080"
echo "   - Swagger: http://localhost:8080/swagger-ui.html"
echo ""
echo "📊 查看日志："
echo "   docker compose logs -f"
echo ""
echo "📊 查看应用日志："
echo "   docker compose logs -f app"
echo ""
echo "🛑 停止服务："
echo "   docker compose down"
echo ""
echo "🔄 重启服务："
echo "   docker compose restart"
echo ""
echo "ℹ️  更多信息请查看："
echo "   - DOCKER.md"
echo "   - README.md"
echo ""
echo "=========================================="

# 健康检查
echo ""
echo "🏥 执行健康检查..."
sleep 5

# 检查 MySQL
if docker compose exec -T mysql mysqladmin ping -h localhost --silent; then
    echo "✅ MySQL 运行正常"
else
    echo "⚠️  MySQL 可能还在启动中，请稍后再试"
fi

# 检查应用
if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ 应用运行正常"
    echo "✅ Swagger: http://localhost:8080/swagger-ui.html"
else
    echo "⚠️  应用可能还在启动中，请查看日志: docker compose logs app"
fi

echo ""
echo "=========================================="
echo "  部署脚本执行完成"
echo "=========================================="
