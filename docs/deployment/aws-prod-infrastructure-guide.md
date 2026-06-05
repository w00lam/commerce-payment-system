# AWS HTTPS 비공개 인프라 구축 상세 매뉴얼

이 문서는 `commerce-payment-system`을 AWS 환경에서 완전 비공개(Private) 상태로 구성하고, HTTPS(SSL) 보안 연결 및 HTTP ➡️ HTTPS 자동 리디렉션을 적용하기 위한 상세 가이드라인을 제공합니다.

---

### [1단계] VPC 및 서브넷 네트워크 자동 구축 (VPC 마법사 사용)
보안 영역과 공개 영역의 네트워크 경계를 구성합니다.

1. **AWS VPC 콘솔** 접속 ➡️ 우측 상단 **[VPC 생성]** 클릭
2. **생성할 리소스**: **`VPC 등(VPC and more)`** 라디오 버튼 선택
3. **세부 설정 입력**:
   - **이름 태그 자동 생성**: `commerce-payment` 입력
   - **IPv4 CIDR 블록**: `10.0.0.0/16` (기본값)
   - **가용 영역(AZ) 개수**: `2` 선택
   - **퍼블릭 서브넷 개수**: `2`
   - **프라이빗 서브넷 개수**: `2`
   - **NAT 게이트웨이($)**: **`기본 1개(1 in total)`** 선택 (비공개 서버의 아웃바운드 인터넷 통신을 위해 필수)
   - **VPC 엔드포인트**: `없음` 선택
   - **DNS 옵션**: `DNS 호스트 이름 활성화`, `DNS 확인 활성화` 모두 체크 상태 유지
4. **[VPC 생성]**을 누르고 완료될 때까지 대기합니다. (약 1분 소요)

---

### [1.5단계] DB 서브넷 그룹 생성 (RDS 데이터베이스 배치를 위한 필수 작업)
프라이빗 서브넷들에 데이터베이스가 안전하게 자리 잡을 수 있도록 DB 서브넷 그룹을 생성합니다. 이 단계가 완료되어야 RDS 생성 시 해당 VPC를 선택할 수 있습니다.

1. **AWS RDS 콘솔** 접속 ➡️ 왼쪽 메뉴 **[서브넷 그룹(Subnet groups)]** 클릭
2. 우측 상단의 **[DB 서브넷 그룹 생성(Create DB subnet group)]** 클릭
3. **세부 정보 입력**:
   - **이름**: `commerce-payment-rds-subnet-group`
   - **설명**: `Subnet group for commerce payment`
   - **VPC**: 1단계에서 생성한 **`commerce-payment-vpc`** 선택
4. **서브넷 추가**:
   - **가용 영역(AZ)**: `ap-northeast-2a`와 `ap-northeast-2c` (혹은 서울 리전의 사용 가능한 두 가용 영역) 선택
   - **서브넷**: 아래 나타나는 서브넷 목록 중 프라이빗 대역(예: `10.0.128.0/20`, `10.0.144.0/20`)의 서브넷 2개 체크 후 추가 (프라이빗 구분이 어려울 경우 목록에 나타나는 해당 VPC의 서브넷을 모두 추가해도 무방합니다.)
5. 맨 아래 **[생성]**을 클릭해 완료합니다.

---

### [2단계] Route 53 호스팅 영역 생성 및 네임서버 등록
도메인의 네임서버 권한을 AWS로 가져오는 작업입니다.

1. **AWS Route 53 콘솔** 접속 ➡️ 왼쪽 메뉴 **[호스팅 영역]** ➡️ **[호스팅 영역 생성]** 클릭
2. **도메인 이름**: 구매하신 도메인 주소(예: `yourdomain.com`) 입력
3. **유형**: `퍼블릭 호스팅 영역` 선택 후 **[호스팅 영역 생성]** 클릭
4. 영역 생성 시 보이는 **NS 레코드 값(4개의 AWS 네임서버 주소)**을 복사합니다.
5. 도메인을 구매하신 대행 사이트(가비아, 후이즈, 고대디 등)의 도메인 설정 페이지로 이동하여 네임서버 주소를 방금 복사한 **AWS 네임서버 4개**로 교체해 줍니다.

---

### [3단계] AWS ACM에서 SSL 인증서 발급 및 DNS 검증
HTTPS 암호화 통신에 사용할 무료 SSL 인증서를 발급합니다.

1. **AWS Certificate Manager (ACM) 콘솔** 접속 ➡️ **[인증서 요청]** 클릭
2. **인증서 유형**: `퍼블릭 인증서 요청` ➡️ [다음]
3. **도메인 이름 입력**:
   - 첫 번째 도메인: `yourdomain.com`
   - **[이 인증서에 다른 이름 추가]** 클릭 후 두 번째 도메인: `*.yourdomain.com` (서브 도메인 대응)
4. **검증 방법**: `DNS 검증 - 권장` 선택
5. **키 알고리즘**: `RSA 2048` 선택 후 **[요청]** 클릭
6. 인증서 목록에서 생성된 도메인을 클릭하여 상세 페이지로 들어갑니다.
7. **[Route 53에서 레코드 생성]** 버튼을 클릭하고 **[레코드 생성]**을 누릅니다.
   * *약 5~10분 후 인증서 상태가 `검증 대기 중`에서 `발급됨`으로 바뀝니다.*

---

### [4단계] EC2 IAM 역할(Role) 생성 (SSM 및 ALB 제어 권한)
비공개 EC2 서버에 통신 및 무중단 배포 제어 권한을 부여합니다.

1. **AWS IAM 콘솔** ➡️ **[역할(Roles)]** ➡️ **[역할 생성]** 클릭
2. **신뢰할 수 있는 엔터티 유형**: `AWS 서비스`, **서비스 또는 사용 사례**: `EC2` 선택 후 [다음]
3. **권한 정책 연결**:
   - 검색창에 **`AmazonSSMManagedInstanceCore`**를 검색하여 체크하고 [다음] 클릭
4. **역할 이름 지정**: `commerce-payment-ec2-role` 입력 후 **[역할 생성]**
5. 생성된 `commerce-payment-ec2-role` 이름을 클릭하여 상세 정보 페이지로 들어갑니다.
6. **권한** 탭 ➡️ **[권한 추가]** ➡️ **[인라인 정책 생성]** 클릭
7. 우측 상단의 **JSON** 탭을 클릭하여 기존 코드를 지우고 아래의 ALB 제어 정책 권한 코드를 복사해서 붙여넣습니다:
   ```json
   {
       "Version": "2012-10-17",
       "Statement": [
           {
               "Effect": "Allow",
               "Action": [
                   "elasticloadbalancing:RegisterTargets",
                   "elasticloadbalancing:DeregisterTargets",
                   "elasticloadbalancing:DescribeTargetHealth"
               ],
               "Resource": "*"
           }
       ]
   }
   ```
8. **[다음]** 클릭 후 정책 이름에 `commerce-payment-alb-policy` 입력 ➡️ **[정책 생성]** 클릭

---

### [5단계] RDS MySQL 생성 (Private Subnet 배치)
데이터베이스를 안전한 내부 프라이빗 서브넷에 생성합니다.

1. **AWS RDS 콘솔** ➡️ **[데이터베이스 생성]** 클릭
2. **생성 방식**: `표준 생성` ➡️ **엔진 옵션**: `MySQL` ➡️ **템플릿**: `프리 티어`
3. **설정**:
   - **DB 인스턴스 식별자**: `commerce-payment-db`
   - **자격 증명 설정**: 마스터 사용자 이름(`admin`), 암호를 안전하게 입력하고 따로 메모해 둡니다.
4. **연결**:
   - **VPC**: 1단계에서 생성한 **`commerce-payment-vpc`** 선택
   - **서브넷 그룹**: 1.5단계에서 생성한 **`commerce-payment-rds-subnet-group`**을 선택합니다.
   - **퍼블릭 액세스**: **`아니요`** 선택
   - **VPC 보안 그룹**: `새로 만들기` 선택 후 이름에 `commerce-payment-rds-sg` 입력
5. **추가 구성 (필수)**:
   - 화면 하단의 **[추가 구성]** 메뉴를 클릭해 펼칩니다.
   - **초기 데이터베이스 이름**: **`commerce_payment`** 입력 (비워둘 시 접속 장애가 납니다.)
6. **[데이터베이스 생성]**을 클릭합니다. (생성 완료까지 약 3~5분 소요)

---

### [6단계] EC2 인스턴스 생성 (Private Subnet 배치 및 ARM64 기반)
애플리케이션을 구동할 서버를 프라이빗 영역에 비공개로 생성합니다.

1. **AWS EC2 콘솔** ➡️ **[인스턴스 시작]** 클릭
2. **이름 및 태그**: `commerce-payment-app`
3. **AMI / 아키텍처**:
   - OS: `Amazon Linux 2023 AMI` 선택
   - 아키텍처: **`64비트(ARM)`** 라디오 버튼 클릭
4. **인스턴스 유형**: **`t4g.micro`** 선택
5. **키 페어**: 기존 키 페어 선택 또는 새 키 페어 생성
6. **네트워크 설정 (우측 상단 [편집] 클릭)**:
   - **VPC**: **`commerce-payment-vpc`** 선택
   - **서브넷**: **`commerce-payment-subnet-private1-ap-northeast-2a`** (반드시 **Private** 서브넷 선택)
   - **퍼블릭 IP 자동 할당**: **`비활성화(Disable)`**
   - **보안 그룹**: `보안 그룹 생성` 선택, 이름을 `commerce-payment-ec2-sg`로 지정
   - **인바운드 보안 그룹 규칙**: 기본으로 지정되어 있는 모든 SSH/HTTP 규칙의 우측 **[제거(X)]** 버튼을 클릭하여 규칙을 **비워둡니다.**
7. **고급 세부 정보 (맨 아래)**:
   - **IAM 인스턴스 프로필**: 4단계에서 생성한 **`commerce-payment-ec2-role`**을 선택합니다.
8. **[인스턴스 시작]** 클릭

---

### [7단계] ALB (Application Load Balancer) 생성 및 HTTPS 리다이렉트 설정
사용자 접속을 수신하여 비공개 EC2에 무중단으로 스왑해 줄 웹 관문을 만듭니다.

1. **EC2 콘솔** ➡️ ① 왼쪽 메뉴 하단 **[로드 밸런서]** ➡️ ② **[로드 밸런서 생성]** 클릭
2. **Application Load Balancer** 아래 **[생성]** 클릭
3. **기본 구성**:
   - **로드 밸런서 이름**: `commerce-payment-alb`
   - **체계(Scheme)**: **`인터넷 경계(Internet-facing)`** 선택
4. **네트워크 매핑**:
   - **VPC**: **`commerce-payment-vpc`** 선택
   - **매핑(가용 영역)**: 2개 가용 영역에 각각 체크하고, 서브넷 목록에서 **`commerce-payment-subnet-public1...`** 및 **`commerce-payment-subnet-public2...`** (반드시 **Public** 서브넷)을 각각 선택합니다.
5. **보안 그룹**:
   - **[새 보안 그룹 생성]** 링크를 클릭하여 새 탭을 엽니다.
     - 보안 그룹 이름: `commerce-payment-alb-sg`
     - 인바운드 규칙 추가 ➡️ 유형: `HTTP` (포트 80), 소스: `위치 무관(0.0.0.0/0)`
     - 인바운드 규칙 추가 ➡️ 유형: `HTTPS` (포트 443), 소스: `위치 무관(0.0.0.0/0)`
     - [보안 그룹 생성] 버튼을 누르고 이 탭을 닫습니다.
   - 로드 밸런서 설정 화면으로 돌아와 새로고침 후 `commerce-payment-alb-sg`를 선택합니다. (기존 default 보안 그룹은 제외)
6. **리스너 및 라우팅 (대상 그룹 생성)**:
   - **[대상 그룹 생성(Create target group)]** 링크를 클릭하여 새 탭을 엽니다.
     - 대상 유형: `인스턴스(Instances)` 선택
     - 대상 그룹 이름: `commerce-payment-tg`
     - 프로토콜: `HTTP`, 포트: **`8080`** (Spring Boot 포트)
     - VPC: **`commerce-payment-vpc`** 선택 후 [다음] 클릭
     - 대상 등록: 목록에서 `commerce-payment-app` EC2 인스턴스를 체크하고, **[아래에 보류 중인 것으로 포함]** 클릭 후 **[대상 그룹 생성]** 클릭 후 탭을 닫습니다.
   - 로드 밸런서 설정 화면으로 돌아와 새로고침 후, `commerce-payment-tg` 대상 그룹을 선택합니다.
7. **리스너 규칙 구성**:
   - **기존 HTTP 80 리스너**: 작업을 기존 전달에서 **[리디렉션(Redirect)]**으로 변경 ➡️ HTTPS / 443 포트 / 301 상태 코드 입력
   - **[리스너 추가] 클릭**: 프로토콜 `HTTPS`, 포트 `443` 지정 ➡️ 작업을 `대상 그룹으로 전달` ➡️ 대상 그룹 `commerce-payment-tg` 선택 ➡️ SSL 인증서에서 3단계에서 발급받은 **ACM 인증서**를 선택합니다.
8. **[로드 밸런서 생성]** 클릭

---

### [8단계] Route 53에서 도메인을 로드 밸런서(ALB)에 매핑 (최종 연결)
1. **Route 53 콘솔** ➡️ **[호스팅 영역]** ➡️ 내 도메인 (`yourdomain.com`) 클릭
2. **[레코드 생성]** 버튼 클릭
3. **설정**:
   - **레코드 이름**: 비워둠 (루트 도메인 접속용)
   - **레코드 유형**: `A - IPv4 주소 및 일부 AWS 리소스로 트래픽 라우팅` 선택
   - **별칭(Alias)**: **스위치 토글을 켭니다(활성화).**
   - **트래픽 라우팅 대상**:
     - `Application Load Balancer에 대한 별칭` 선택
     - **리전**: ALB를 생성한 리전(예: ap-northeast-2) 선택
     - **로드 밸런서 선택**: 생성해 둔 `commerce-payment-alb` 지정
4. **[레코드 생성]** 클릭
   - *필요 시 레코드를 하나 더 만들어 레코드 이름에 `www`를 적고 똑같이 ALB를 매핑해 줍니다.*

---

### [9단계] 보안 그룹(Security Group) 삼각 연동 설정 (핵심 보안)
각 컴포넌트들이 지정된 올바른 통신 포트만 허용하게 강제합니다.

1. **EC2 보안 그룹 설정 (`commerce-payment-ec2-sg`)**:
   - EC2 콘솔 ➡️ 보안 그룹 ➡️ `commerce-payment-ec2-sg` 선택 ➡️ **[인바운드 규칙 편집]**
   - 규칙 추가 ➡️ 유형: `사용자 지정 TCP` / 포트 범위: `8080` / 소스: `사용자 지정` ➡️ 검색창에 **`commerce-payment-alb-sg`** 보안 그룹 ID 선택 및 저장
   - 규칙 추가 ➡️ 유형: `사용자 지정 TCP` / 포트 범위: `8081` / 소스: `사용자 지정` ➡️ 검색창에 **`commerce-payment-alb-sg`** 보안 그룹 ID 선택 및 저장
2. **RDS 보안 그룹 설정 (`commerce-payment-rds-sg`)**:
   - EC2 콘솔 ➡️ 보안 그룹 ➡️ `commerce-payment-rds-sg` 선택 ➡️ **[인바운드 규칙 편집]**
   - 규칙 추가 ➡️ 유형: `MySQL/Aurora` (포트 3306) / 소스: `사용자 지정` ➡️ 검색창에 **`commerce-payment-ec2-sg`** 보안 그룹 ID 선택 및 저장 (기존 규칙이 있다면 삭제 및 변경)

---

### [10단계] Systems Manager Parameter Store 환경 변수 등록
애플리케이션에 필요한 민감한 정보 및 주소를 안전하게 저장합니다.

1. **AWS Systems Manager 콘솔** ➡️ **[Parameter Store(파라미터 스토어)]** 이동
2. **[파라미터 생성]** 버튼을 누르고 아래 11개 파라미터를 등록합니다.

| 파라미터 이름 | 유형(Type) | 값(Value) 작성 가이드 |
| :--- | :--- | :--- |
| `/config/prod/DB_URL` | `String` | `jdbc:mysql://<RDS엔드포인트>:3306/commerce_payment?useSSL=false&allowPublicKeyRetrieval=true` |
| `/config/prod/DB_USERNAME` | `String` | `admin` |
| `/config/prod/DB_PASSWORD` | `SecureString` | (RDS 생성 시 설정한 암호) |
| `/config/prod/JWT_SECRET` | `SecureString` | (임의의 256비트 이상 영문+숫자 혼합 보안 키 문자열) |
| `/config/prod/JWT_ACCESS_TOKEN_EXPIRATION` | `String` | `1800000` |
| `/config/prod/PORTONE_API_SECRET` | `SecureString` | (PortOne API Secret 값) |
| `/config/prod/PORTONE_STORE_ID` | `String` | (PortOne 상점 ID) |
| `/config/prod/PORTONE_CHANNEL_KEY` | `String` | (PortOne 결제 채널 키) |
| `/config/prod/PORTONE_WEBHOOK_SECRET` | `SecureString` | (PortOne 웹훅 검증용 Secret 값) |
| `/config/prod/ALB_TARGET_GROUP_ARN` | `String` | **(7단계에서 생성한 대상 그룹 `commerce-payment-tg`의 ARN 주소)** |
| `/config/prod/SPRING_PROFILES_ACTIVE` | `String` | `prod` |

* **RDS 엔드포인트** 확인: RDS 콘솔 ➡️ 데이터베이스 ➡️ `commerce-payment-db` 클릭 ➡️ '연결 및 보안' 탭의 '엔드포인트' 주소 복사
* **ALB Target Group ARN** 확인: EC2 콘솔 ➡️ 대상 그룹 ➡️ `commerce-payment-tg` 클릭 ➡️ '세부 정보' 탭의 'ARN' 주소 복사

---

### [11단계] 비공개 EC2 서버 터미널 접속 및 도커 설치
외부 IP와 포트 22가 닫혀있어도 AWS SSM Agent를 사용해 웹 콘솔에서 즉시 접속합니다.

1. **EC2 콘솔** ➡️ **인스턴스** ➡️ `commerce-payment-app` 체크 ➡️ 상단 **[연결(Connect)]** 클릭
2. **[세션 관리자(Session Manager)]** 탭 선택 ➡️ **[연결]** 클릭 (원격 브라우저 터미널 오픈)
3. 아래 명령어를 실행하여 도커를 설치하고 활성화합니다.
   ```bash
   # bash 쉘 전환
   sudo su - ssm-user
   bash

   # 도커 패키지 설치
   sudo dnf update -y
   sudo dnf install docker -y

   # 도커 서비스 기동 및 자동 부팅 등록
   sudo systemctl start docker
   sudo systemctl enable docker

   # ssm-user에게 도커 제어 권한 부여
   sudo usermod -aG docker ssm-user
   newgrp docker
   ```
