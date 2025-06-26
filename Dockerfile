# syntax=docker/dockerfile:1
# Multi-stage build for Spring Boot application

# Dependencies stage - 가장 변경이 적은 부분을 먼저 처리
FROM gradle:8.14-jdk21-alpine AS dependencies

WORKDIR /app

# JVM 옵션 최적화 - 빌드 성능 향상
ENV GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8 -Dorg.gradle.daemon=false -Dorg.gradle.parallel=true"
ENV JAVA_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"

# Gradle wrapper 복사
COPY gradle gradle
COPY gradlew ./

# 모든 Gradle 설정 파일들을 명시적으로 복사
COPY build.gradle.kts ./
COPY settings.gradle.kts ./

# 의존성만 먼저 다운로드 (가장 시간이 오래 걸리지만 변경이 적음)
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew dependencies --no-daemon --parallel --build-cache

# Build stage
FROM dependencies AS builder

# 소스 코드 복사 (가장 자주 변경되는 부분)
COPY src ./src

# 빌드를 한 번에 처리
RUN --mount=type=cache,target=/root/.gradle/caches \
    --mount=type=cache,target=/root/.gradle/wrapper \
    ./gradlew assemble --no-daemon --parallel --build-cache --exclude-task test && \
    # 레이어 추출
    cd build/libs && \
    java -Djarmode=layertools -jar chat-server-0.0.1-SNAPSHOT.jar extract

# Runtime stage - Using JRE alpine for smaller size
FROM eclipse-temurin:21-jre-alpine

# Install dumb-init for proper signal handling and FFmpeg for media processing
RUN apk add --no-cache dumb-init ffmpeg

# Create non-root user
RUN addgroup -g 1000 spring && \
    adduser -u 1000 -G spring -s /bin/sh -D spring

WORKDIR /app

# Create necessary directories
RUN mkdir -p logs uploads && \
    chown -R spring:spring logs uploads

# Copy layers from builder (order matters for caching)
COPY --from=builder --chown=spring:spring /app/build/libs/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/build/libs/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/build/libs/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/build/libs/application/ ./

USER spring:spring

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=50.0 \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:+UseStringDeduplication \
    -XX:+ParallelRefProcEnabled \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

EXPOSE 8080

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]