# Commerce Payment System Wiki

이 Wiki는 Commerce Payment System을 처음 보는 개발자가 도메인, 코드 구조, 데이터 모델, API, 배포 환경을 빠르게 이해할 수 있도록 작성한 기술 문서입니다.

README는 프로젝트 소개용 문서이고, Wiki는 상세 기술 문서입니다.

## 문서 목차

| Page | Description |
| --- | --- |
| [Architecture](Architecture.md) | 프로젝트 구조, 레이어 구조, 패키지 구조, 도메인 구조 |
| [ERD](ERD.md) | 현재 JPA Entity와 Flyway schema 기준 ERD |
| [API Specification](API-Specification.md) | Controller 기준 API Endpoint 명세 |
| [Authentication](Authentication.md) | JWT 인증 흐름과 Security Filter Chain |
| [Payment](Payment.md) | 일반 결제, 포인트 전액 결제, 복합 결제 흐름 |
| [Refund](Refund.md) | 부분 환불, 전체 환불, 상태 전이, 후처리 |
| [Point](Point.md) | 포인트 적립, 사용, 환불 정책 |
| [Membership](Membership.md) | 멤버십 등급 정책과 누적 결제 금액 산정 |
| [Subscription](Subscription.md) | 구독 신청, 해지, 정기 결제, 미납 재시도 |
| [Webhook](Webhook.md) | PortOne Webhook 수신, 검증, 저장, 후처리 |
| [AWS Infrastructure](AWS-Infrastructure.md) | EC2, RDS, Security Group, Docker 기반 운영 구성 |
| [Deployment Guide](Deployment-Guide.md) | Build, Docker Image, Docker Run, EC2/RDS 배포 절차 |
| [Troubleshooting](Troubleshooting.md) | Flyway, JWT, Webhook, Docker, RDS 문제 해결 |

## 빠른 진입 경로

- 결제 도메인을 이해하려면 [Payment](Payment.md), [Refund](Refund.md), [Webhook](Webhook.md)를 먼저 읽습니다.
- 데이터 관계를 보려면 [ERD](ERD.md)를 읽습니다.
- API 호출 순서를 확인하려면 [API Specification](API-Specification.md)를 읽습니다.
- 배포와 운영을 확인하려면 [AWS Infrastructure](AWS-Infrastructure.md), [Deployment Guide](Deployment-Guide.md), [Troubleshooting](Troubleshooting.md)를 읽습니다.

