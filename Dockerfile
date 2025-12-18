# =========================
# Stage 1: Build
# =========================
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Gradle 캐시 최적화
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# 소스 복사 및 빌드
COPY src ./src

# Build application
RUN gradle bootJar --no-daemon -x test

# =========================
# Stage 2: Runtime
# =========================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Timezone 설정
ENV TZ=Asia/Seoul

# 보안: non-root 유저
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트
EXPOSE 8080

# Docker 헬스체크 (ALB 헬스체크와 동일 경로)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 실행
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-Xms128m", \
  "-Xmx256m", \
  "-Duser.timezone=Asia/Seoul", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
