# Deployment Guide

## Build

로컬 또는 CI에서 다음 명령으로 테스트와 jar 빌드를 수행합니다.

```bash
./gradlew clean test bootJar
```

빌드 결과:

```text
build/libs/app.jar
```

## Docker Image

Dockerfile은 `build/libs/app.jar`를 `app.jar`로 복사합니다.

```bash
docker build -t commerce-payment:local .
```

실행 profile은 Dockerfile에서 `prod`로 지정됩니다.

```dockerfile
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
```

## Docker Run

운영 환경과 동일하게 실행하려면 env file을 준비합니다.

```bash
docker run -d \
  --name commerce-payment \
  -p 8080:8080 \
  --env-file .env \
  commerce-payment:local
```

필수 `.env`:

```dotenv
DB_URL=jdbc:mysql://<rds-endpoint>:3306/commerce_payment?useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=<username>
DB_PASSWORD=<password>
JWT_SECRET=<base64-secret>
JWT_ACCESS_TOKEN_EXPIRATION=3600000
PORTONE_API_SECRET=<secret>
PORTONE_STORE_ID=<store-id>
PORTONE_CHANNEL_KEY=<payment-channel-key>
PORTONE_BILLING_CHANNEL_KEY=<billing-channel-key>
PORTONE_CONNECT_TIMEOUT=2s
PORTONE_READ_TIMEOUT=5s
PORTONE_WEBHOOK_SECRET=<webhook-secret>
```

## EC2 배포

현재 GitHub Actions workflow는 Private EC2에 직접 SSH로 접속하지 않고 SSM Run Command를 사용합니다.

배포 절차:

1. `main` 브랜치에 push합니다.
2. GitHub Actions가 테스트와 bootJar를 수행합니다.
3. Docker image를 ECR에 push합니다.
4. SSM Run Command가 EC2에서 ECR login을 수행합니다.
5. EC2가 새 image를 pull합니다.
6. `/config/prod/` Parameter Store 값을 `.env`로 작성합니다.
7. 현재 실행 중인 컨테이너가 `commerce-payment-a`이면 새 컨테이너는 `commerce-payment-b`로 실행합니다.
8. 반대 경우 새 컨테이너는 `commerce-payment-a`로 실행합니다.
9. 새 컨테이너 health check를 수행합니다.
10. 새 포트를 ALB Target Group에 등록합니다.
11. 기존 포트를 Target Group에서 제거하고 기존 컨테이너를 중지합니다.
12. Docker image prune을 수행합니다.

## RDS 연결

`application-prod.yaml`은 다음 환경 변수를 사용합니다.

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

확인할 항목:

- RDS endpoint와 DB name이 `DB_URL`에 포함되어 있는지 확인합니다.
- RDS Security Group이 EC2 Security Group의 3306 접근을 허용하는지 확인합니다.
- RDS 사용자가 대상 database에 접근 가능한지 확인합니다.
- 운영 DB schema는 Flyway migration으로 생성되어야 합니다.
- `ddl-auto: validate`가 실패하면 애플리케이션이 시작되지 않습니다.

## 배포 확인

health check:

```bash
curl http://localhost:8080/health
```

정적 페이지 확인:

```bash
curl http://localhost:8080/index.html
```

ALB 연결 확인:

```bash
curl https://roviq.click/health
```

PortOne Webhook URL:

```text
https://roviq.click/api/payments/webhooks/portone
```

