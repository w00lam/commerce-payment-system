FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 타임존 설정 (한국 시간 기준)
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# 빌드된 JAR 복사 (빌드 완료 후 복사)
COPY build/libs/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
