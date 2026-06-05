# AWS 최초 배포 런북

이 문서는 `commerce-payment-system`의 AWS 운영 환경 최초 배포 절차를 정리한다.
실제 계정 ID, 서버 IP, RDS 엔드포인트, 비밀번호, 토큰, 시크릿 값은 문서에 기록하지 않는다.

## 목표

- Spring Boot 애플리케이션을 AWS 운영 환경에서 `prod` 프로필로 기동한다.
- AWS RDS MySQL에 운영 테이블을 최초 생성한다.
- 배포 후 결제, 환불, 포인트, 웹훅 흐름을 검증한다.
- 최초 배포 성공 후 운영 DB 스키마 자동 변경을 막기 위해 `ddl-auto`를 `validate`로 변경한다.

## 배포 방식

초기 MVP 배포는 다음 구성을 기준으로 진행한다.

- 애플리케이션 서버: AWS EC2
- 데이터베이스: AWS RDS MySQL
- 설정 및 시크릿 관리: AWS SSM Parameter Store
- 빌드 및 배포 자동화: GitHub Actions

ECS, Elastic Beanstalk, ALB, Route 53, ACM은 필요 시 후속 작업으로 확장한다.

## 사전 확인

- `application-prod.yaml`이 빌드 산출물에 포함되어야 한다.
- 운영 기동 시 `SPRING_PROFILES_ACTIVE=prod`가 설정되어야 한다.
- 최초 기동 전 `spring.jpa.hibernate.ddl-auto`는 `update`로 둔다.
- 최초 테이블 생성 확인 후 `ddl-auto`는 `validate`로 변경한다.
- 운영 환경에서는 `data.sql`이 실행되지 않아야 한다.
- EC2에서 RDS MySQL 3306 포트로 접근 가능해야 한다.

## 환경 변수 체크리스트

운영 환경 변수 값은 SSM Parameter Store에 저장하고, 배포 시 애플리케이션 프로세스 환경 변수로 주입한다.
값 자체는 커밋하지 않는다.

| 이름 | 설명 | 저장 위치 | 필수 여부 | 비고 |
| --- | --- | --- | --- | --- |
| `DB_URL` | RDS MySQL JDBC URL | SSM Parameter Store | 필수 | 예: `jdbc:mysql://<rds-endpoint>:3306/<db-name>?useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | RDS 접속 사용자명 | SSM Parameter Store | 필수 | 실제 값은 기록하지 않는다. |
| `DB_PASSWORD` | RDS 접속 비밀번호 | SSM Parameter Store `SecureString` | 필수 | 실제 값은 기록하지 않는다. |
| `JWT_SECRET` | JWT 서명용 시크릿 | SSM Parameter Store `SecureString` | 필수 | 운영 전용 값 사용 |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Access Token 만료 시간 | SSM Parameter Store | 필수 | 밀리초 단위 |
| `PORTONE_API_SECRET` | PortOne API 시크릿 | SSM Parameter Store `SecureString` | 필수 | 실제 값은 기록하지 않는다. |
| `PORTONE_STORE_ID` | PortOne 상점 ID | SSM Parameter Store | 필수 | 클라이언트에도 노출되는 공개 식별자이며, `static/config.js`의 `storeId`와 일치해야 한다. |
| `PORTONE_CHANNEL_KEY` | PortOne 결제 채널 키 | SSM Parameter Store | 필수 | 클라이언트에도 노출되는 공개 식별자이며, `static/config.js`의 `channelKey`와 일치해야 한다. |
| `PORTONE_WEBHOOK_SECRET` | PortOne 웹훅 검증 시크릿 | SSM Parameter Store `SecureString` | 필수 | 실제 값은 기록하지 않는다. |

`PORTONE_STORE_ID`, `PORTONE_CHANNEL_KEY`는 결제창 호출을 위해 브라우저에 노출되는 값이므로 비밀값으로 취급하지 않는다.
다만 백엔드 PortOne API 호출 설정에도 사용되므로 운영 배포 시 환경 변수로도 주입한다.
비밀값으로 취급해야 하는 항목은 `PORTONE_API_SECRET`, `PORTONE_WEBHOOK_SECRET`이다.

## 운영 설정값

다음 값은 `application-prod.yaml`에서 관리한다.

| 설정 | 값 | 비고 |
| --- | --- | --- |
| `portone.base-url` | `https://api.portone.io` | PortOne API 기본 URL |
| `portone.connect-timeout` | `3s` | 연결 타임아웃 |
| `portone.read-timeout` | `5s` | 읽기 타임아웃 |

## AWS 인프라 준비

### 1. RDS MySQL 생성

1. 운영용 MySQL RDS 인스턴스를 생성한다.
2. DB 이름, 사용자명, 비밀번호를 운영 기준에 맞게 설정한다.
3. RDS 보안 그룹은 애플리케이션 EC2 보안 그룹에서 들어오는 3306 포트만 허용한다.
4. RDS 엔드포인트를 확인한다.

### 2. EC2 생성

1. 애플리케이션 실행용 EC2 인스턴스를 생성한다.
2. Java 실행 환경을 설치한다.
3. 애플리케이션 실행 사용자를 준비한다.
4. EC2 IAM Role에 SSM Parameter Store 읽기 권한을 부여한다.
5. 애플리케이션 포트 접근 정책을 결정한다.
   - 임시 검증: EC2 보안 그룹에서 8080 허용
   - 운영 권장: ALB를 앞에 두고 EC2는 ALB에서만 접근 허용

### 3. SSM Parameter Store 등록

1. 운영 환경 변수 값을 SSM Parameter Store에 등록한다.
2. 시크릿 값은 `SecureString`으로 저장한다.
3. EC2 또는 배포 스크립트에서 필요한 파라미터를 읽을 수 있는지 확인한다.

## 최초 배포 절차

### 1. 애플리케이션 빌드

```bash
./gradlew clean bootJar
```

빌드가 성공하면 `build/libs` 아래의 jar 파일을 배포 대상으로 사용한다.

### 2. EC2에 산출물 배포

1. jar 파일을 EC2의 배포 디렉터리에 업로드한다.
2. SSM Parameter Store에서 운영 설정 값을 읽어 환경 변수로 주입한다.
3. `SPRING_PROFILES_ACTIVE=prod`로 애플리케이션을 실행한다.
4. 가능하면 `systemd` 서비스로 등록해 재시작과 로그 확인을 표준화한다.

### 3. 애플리케이션 기동 확인

1. 프로세스가 정상 실행 중인지 확인한다.
2. 애플리케이션 로그에서 RDS 연결 오류가 없는지 확인한다.
3. 애플리케이션 포트가 열려 있는지 확인한다.
4. 헬스 체크 또는 기본 페이지가 응답하는지 확인한다.

## 최초 DB 생성 확인

RDS에 접속해 다음 테이블이 생성되었는지 확인한다.

- `member`
- `product`
- `orders`
- `order_item`
- `payment`
- `refund`
- `refund_item`
- `point_history`
- `cart`
- `cart_item`
- `webhook_event`

테이블이 생성되지 않았거나 앱이 기동 실패하면 다음을 먼저 확인한다.

- RDS 보안 그룹에서 EC2 접근이 허용되어 있는지
- `DB_URL` 형식이 올바른지
- DB 이름이 실제로 생성되어 있는지
- DB 사용자 권한이 충분한지
- 운영 환경 변수가 애플리케이션 프로세스에 주입되었는지

## 최초 배포 후 필수 변경

테이블 생성과 기본 기동 검증이 끝나면 `application-prod.yaml`에서 다음 설정을 변경하고 재배포한다.

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

이 변경은 운영 DB 스키마가 애플리케이션 기동 중 자동 변경되는 것을 막기 위한 필수 사후 작업이다.

## 배포 후 검증

### 기본 검증

- 애플리케이션이 `prod` 프로필로 실행 중인지 확인한다.
- 서버 포트가 정상 응답하는지 확인한다.
- `/index.html`이 정상 로드되는지 확인한다.
- 상품 목록 조회가 정상 동작하는지 확인한다.

### 결제 검증

- PortOne 테스트 결제창이 정상 호출되는지 확인한다.
- 결제 성공 후 `payment` 테이블에 데이터가 저장되는지 확인한다.
- 결제 성공 후 회원 포인트가 적립되는지 확인한다.

### 환불 검증

- 전체 환불이 정상 처리되는지 확인한다.
- 부분 환불이 정상 처리되는지 확인한다.
- 환불 후 포인트가 회수되는지 확인한다.
- 환불 데이터가 `refund`, `refund_item` 테이블에 저장되는지 확인한다.

### 웹훅 검증

- PortOne 웹훅 URL이 운영 도메인으로 설정되어 있는지 확인한다.
- 웹훅 수신 시 `webhook_event` 이력이 저장되는지 확인한다.
- 중복 웹훅 또는 이미 처리된 결제 상태가 안전하게 처리되는지 확인한다.

## 롤백 기준

다음 상황에서는 배포를 중단하고 원인을 확인한다.

- 애플리케이션이 반복적으로 기동 실패한다.
- RDS 연결이 지속적으로 실패한다.
- 결제 승인 또는 환불 API 호출이 실패한다.
- 결제 성공 후 주문, 결제, 포인트 데이터 정합성이 깨진다.
- 웹훅 수신 후 상태 동기화가 잘못된다.

## 롤백 절차

1. 배포 파이프라인을 중단한다.
2. EC2 애플리케이션 서비스를 중지한다.
3. 최근 애플리케이션 로그와 배포 로그를 보관한다.
4. 환경 변수, RDS 연결 정보, 보안 그룹, IAM 권한을 점검한다.
5. 원인 확인 전까지 새 배포를 진행하지 않는다.

## 후속 작업

- GitHub Actions 배포 workflow 추가
- EC2 `systemd` 서비스 파일 추가
- SSM 파라미터 로딩 스크립트 추가
- 운영 헬스 체크 엔드포인트 검토
- ALB, HTTPS, 도메인 연결 검토
- 운영 로그 수집과 모니터링 검토
