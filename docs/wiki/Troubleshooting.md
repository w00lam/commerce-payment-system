# Troubleshooting

## Flyway Migration 충돌

문제:

- 애플리케이션 시작 시 Flyway migration 오류가 발생합니다.
- 같은 버전 migration이 둘 이상 존재한다는 오류가 발생합니다.
- JPA validation에서 table 또는 column이 없다고 실패합니다.

원인:

- migration version 번호가 중복되었습니다.
- migration 파일 이름이 변경되었지만 기존 DB의 `flyway_schema_history`와 맞지 않습니다.
- 엔티티 변경에 대응하는 migration이 없습니다.
- 운영 profile은 `ddl-auto: validate`이므로 JPA가 테이블을 자동 생성하지 않습니다.

해결 방법:

1. `src/main/resources/db/migration`의 migration 파일 번호를 확인합니다.
2. 새 migration은 기존 최신 버전 다음 번호로 생성합니다.
3. 이미 운영 DB에 적용된 migration은 수정하지 않습니다.
4. schema 변경은 새 migration으로 추가합니다.
5. 테스트 시 stale build artifact를 피하려면 `clean`을 포함합니다.

```bash
./gradlew clean test --tests com.commercepaymentsystem.FlywayMigrationValidationTest
```

## JWT 인증 실패

문제:

- 인증 API 호출이 401을 반환합니다.
- `INVALID_TOKEN` 응답이 반환됩니다.
- Controller의 `@AuthenticationPrincipal Long memberId`가 비어 있습니다.

원인:

- `Authorization` 헤더가 없습니다.
- Bearer prefix가 없습니다.
- JWT가 만료되었습니다.
- `JWT_SECRET`이 토큰 발급 시점과 검증 시점에 다릅니다.
- `JWT_SECRET`이 Base64 decode 가능한 값이 아닙니다.

해결 방법:

1. 헤더 형식을 확인합니다.

```http
Authorization: Bearer {accessToken}
```

2. 로그인 응답의 `accessToken`만 저장하고, 요청 시 `Bearer ` prefix를 붙입니다.
3. 운영 환경의 `JWT_SECRET`과 `JWT_ACCESS_TOKEN_EXPIRATION`을 확인합니다.
4. `jwt.secret`은 Base64 decode 가능한 충분한 길이의 secret이어야 합니다.

## PortOne Webhook 중복 수신

문제:

- 같은 Webhook이 여러 번 수신됩니다.
- 결제 또는 환불 후처리가 중복될 가능성이 있습니다.

원인:

- PortOne은 Webhook 재전송을 할 수 있습니다.
- 네트워크 지연 또는 응답 실패로 같은 event가 다시 들어올 수 있습니다.

해결 방법:

1. `webhook_events.event_id`가 primary key인지 확인합니다.
2. 기존 event 상태가 `COMPLETED` 또는 `IGNORED`이면 추가 후처리를 하지 않습니다.
3. 기존 event 상태가 `FAILED`인 경우에만 재처리를 허용합니다.
4. Webhook 처리 결과는 `webhook_events.status`, `result_message`, `processed_at`에서 확인합니다.

## Docker 환경 변수 누락

문제:

- 컨테이너는 실행되지만 애플리케이션이 시작되지 않습니다.
- PortOne, DB, JWT 관련 설정 오류가 발생합니다.
- GitHub Actions 배포 중 `Missing required SSM parameter` 오류가 발생합니다.

원인:

- `.env` 또는 SSM Parameter Store에 필수 환경 변수가 없습니다.
- Parameter 이름이 `/config/prod/{NAME}` 형식이 아닙니다.
- secret 값이 빈 문자열입니다.

해결 방법:

1. SSM Parameter Store에서 `/config/prod/` 하위 값을 확인합니다.
2. workflow의 required 목록과 실제 parameter 이름이 일치하는지 확인합니다.
3. EC2에서 생성된 `.env`에 필수 값이 들어갔는지 확인합니다.
4. 누락된 값을 추가한 뒤 다시 배포합니다.

필수 값:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_EXPIRATION`
- `PORTONE_API_SECRET`
- `PORTONE_STORE_ID`
- `PORTONE_CHANNEL_KEY`
- `PORTONE_BILLING_CHANNEL_KEY`
- `PORTONE_CONNECT_TIMEOUT`
- `PORTONE_READ_TIMEOUT`
- `PORTONE_WEBHOOK_SECRET`

## RDS 연결 실패

문제:

- 애플리케이션 시작 시 datasource 연결에 실패합니다.
- `Communications link failure` 또는 authentication 오류가 발생합니다.

원인:

- `DB_URL` endpoint 또는 database name이 잘못되었습니다.
- RDS Security Group이 EC2 접근을 허용하지 않습니다.
- EC2와 RDS가 같은 VPC 또는 라우팅 가능한 네트워크에 없습니다.
- DB username/password가 잘못되었습니다.
- RDS가 아직 available 상태가 아닙니다.

해결 방법:

1. `DB_URL` 형식을 확인합니다.

```text
jdbc:mysql://<rds-endpoint>:3306/<db-name>?useSSL=false&allowPublicKeyRetrieval=true
```

2. RDS Security Group inbound에서 EC2 Security Group의 3306 접근을 허용합니다.
3. EC2에서 DNS resolution과 3306 연결을 확인합니다.
4. DB 사용자 권한을 확인합니다.
5. 애플리케이션 로그에서 DB 연결 성공 이후 JPA validation 실패인지 구분합니다.

## PortOne 결제 검증 실패

문제:

- 브라우저 결제는 성공했지만 `POST /api/payments/{paymentId}/confirm`이 실패합니다.

원인:

- PortOne API secret이 잘못되었습니다.
- PortOne store/channel과 결제 건이 맞지 않습니다.
- 내부 `paymentId`와 PortOne `id`가 다릅니다.
- PortOne 결제 상태가 `PAID`가 아닙니다.
- 내부 `orderName`과 PortOne `orderName`이 다릅니다.
- PortOne `amount.total`과 내부 `finalPaymentAmount`가 다릅니다.

해결 방법:

1. `PORTONE_API_SECRET`을 확인합니다.
2. 클라이언트 `config.js`의 store/channel key와 서버 환경 변수를 확인합니다.
3. 결제창 호출 시 `paymentId`, `orderName`, `totalAmount`가 주문 생성 응답과 일치하는지 확인합니다.
4. 서버 로그에서 PortOne 조회 실패와 내부 검증 실패를 구분합니다.

## 환불 후처리 실패

문제:

- PortOne 환불은 성공했지만 내부 주문, 재고, 포인트, 멤버십 상태가 일부 반영되지 않았습니다.
- 환불 상태가 `POST_PROCESS_FAILED`로 남습니다.

원인:

- PG 취소 이후 내부 트랜잭션에서 재고 복구, 포인트 처리, 주문 취소, 멤버십 반영 중 오류가 발생했습니다.

해결 방법:

1. `refunds.status`가 `POST_PROCESS_FAILED`인지 확인합니다.
2. `refund_items`의 환불 수량과 금액을 확인합니다.
3. 포인트 이력 중 `USE_CANCEL`, `EARN_REVOKE`가 생성되었는지 확인합니다.
4. 결제 상태가 `PARTIAL_REFUNDED` 또는 `REFUNDED`로 변경되었는지 확인합니다.
5. 재처리 로직을 수행할 때 PG 취소를 다시 호출하지 않도록 주의합니다.

