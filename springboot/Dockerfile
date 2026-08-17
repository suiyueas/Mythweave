# ========================================================
# Mythweave AI 后端镜像（多阶段构建）
# 阶段一：Maven + JDK 21 编译打包
# 阶段二：仅含 JRE 21 的轻量运行镜像
# ========================================================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# 先拷贝 POM 并预下载依赖，利用 Docker 层缓存加速后续构建
COPY pom.xml .
RUN mvn dependency:go-offline -B -q || true

COPY src ./src
RUN mvn package -DskipTests -B

# ========================================================
# 运行阶段
# ========================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# 以非 root 用户运行，降低容器逃逸风险
RUN useradd -r -u 1001 -m -d /app mythweave \
    && mkdir -p /var/lib/mythweave/avatar /var/log/mythweave \
    && chown -R mythweave:mythweave /app /var/lib/mythweave /var/log/mythweave
USER mythweave

COPY --from=builder /build/target/mythweave-1.0.0-SNAPSHOT.jar /app/mythweave.jar

ENV SPRING_PROFILES_ACTIVE=prod,docker \
    SERVER_PORT=8080 \
    LOG_PATH=/var/log/mythweave

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080'

ENTRYPOINT ["java", "-jar", "/app/mythweave.jar"]
