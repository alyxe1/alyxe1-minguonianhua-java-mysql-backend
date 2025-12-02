# 使用 OpenJDK 11 为基础镜像
FROM openjdk:11-jre-slim

# 设置工作目录
WORKDIR /app

# 复制 JAR 文件
COPY target/nianhua-wechat-miniprogram-backend-0.0.1-SNAPSHOT.jar app.jar

# 创建非 root 用户
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# 更改文件所有者
RUN chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
