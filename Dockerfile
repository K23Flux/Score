# 基础镜像（Java17，适配SpringBoot 3.x）
FROM eclipse-temurin:17-jre

# 固定时区为东八区
ENV TZ=Asia/Shanghai

# 安装时区数据包并生效（适配Ubuntu系基础镜像）
RUN apt-get update \
    && apt-get install -y --no-install-recommends tzdata \
    && ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone \
    && rm -rf /var/lib/apt/lists/*

# 容器内创建工作目录
WORKDIR /app

# 把本地jar包复制到容器里
COPY score-0.0.1-SNAPSHOT.jar /app/app.jar

# 启动命令：增加JVM时区参数，双保险确保时间正确
ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]