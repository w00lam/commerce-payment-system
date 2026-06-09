# API Specification

모든 API 응답은 기본적으로 `ApiResponse<T>` 형태입니다.

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

인증이 필요한 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

## Auth API

### POST `/api/auth/signup`

목적: 신규 회원을 생성하고 기본 멤버십(`NORMAL`)을 부여합니다.

Request:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동",
  "phone": "010-0000-0000"
}
```

Response: `SignupResponse`

- `memberId`
- `email`
- `name`

주요 예외:

- 이메일 중복
- 입력값 검증 실패
- 기본 멤버십 등급 정책 누락

### POST `/api/auth/login`

목적: 이메일과 비밀번호를 검증하고 JWT Access Token을 발급합니다.

Request:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Response: `LoginResponse`

- `accessToken`
- `tokenType`
- `member.memberId`
- `member.email`
- `member.name`

주요 예외:

- 회원 없음
- 탈퇴 회원
- 비밀번호 불일치

### POST `/api/auth/logout`

목적: 클라이언트가 보관 중인 JWT를 제거하도록 하는 no-op 로그아웃입니다.

Request: 없음

Response: `Void`

주요 예외: 없음

## Member API

### DELETE `/api/members/signout`

목적: 인증 회원의 계정을 탈퇴 처리합니다.

Request: `MemberDeleteRequest`

Response: `Void`

주요 예외:

- 인증 실패
- 회원 없음
- 비밀번호 불일치

## Product API

### POST `/api/products`

목적: 상품을 등록합니다.

Request: `ProductCreateRequest`

Response: `ProductCreateResponse`

주요 예외:

- 입력값 검증 실패
- 상품 금액/재고 정책 위반

### GET `/api/products`

목적: 상품 목록을 페이징 조회합니다.

Request:

- Query: `page`, `size`, `sort`
- Search condition: `ProductSearchCondition`

Response: `PageResponse<ProductListResponse>`

주요 예외: 없음

### GET `/api/products/{productId}`

목적: 상품 상세 정보를 조회합니다.

Request:

- Path: `productId`

Response: `ProductDetailResponse`

주요 예외:

- 상품 없음

### PUT `/api/products/{productId}`

목적: 상품 정보를 수정합니다.

Request:

- Path: `productId`
- Body: `ProductUpdateRequest`

Response: `ProductUpdateResponse`

주요 예외:

- 상품 없음
- 입력값 검증 실패

### DELETE `/api/products/{productId}`

목적: 상품을 소프트 삭제합니다.

Request:

- Path: `productId`

Response: `ProductDeleteResponse`

주요 예외:

- 상품 없음

## Cart API

### POST `/api/carts/items`

목적: 인증 회원의 장바구니에 상품을 추가합니다.

Request:

```json
{
  "productId": 1,
  "quantity": 2
}
```

Response: `CartItemAddResponse`

주요 예외:

- 인증 실패
- 상품 없음
- 판매 중이 아닌 상품
- 유효하지 않은 수량

### GET `/api/carts`

목적: 인증 회원의 장바구니를 조회합니다.

Request: 없음

Response: `CartResponse`

주요 예외:

- 인증 실패

### PUT `/api/carts/items/{cartItemId}`

목적: 장바구니 상품 수량을 변경합니다.

Request:

```json
{
  "quantity": 3
}
```

Response: `CartItemUpdateResponse`

주요 예외:

- 장바구니 항목 없음
- 수량 검증 실패

### DELETE `/api/carts/items/{cartItemId}`

목적: 장바구니 상품을 삭제합니다.

Request:

- Path: `cartItemId`

Response: `CartItemDeleteResponse`

주요 예외:

- 장바구니 항목 없음

### DELETE `/api/carts/items`

목적: 장바구니를 비웁니다.

Request: 없음

Response: `CartClearResponse`

주요 예외:

- 인증 실패

## Order API

### POST `/api/orders/preview`

목적: 장바구니 항목 기준 주문 금액을 미리 계산합니다.

Request:

```json
{
  "cartItemIds": [1, 2]
}
```

Response: `OrderPreviewResponse`

주요 예외:

- 장바구니 항목 없음
- 판매 중이 아닌 상품

### POST `/api/orders`

목적: 주문을 생성하고 `PENDING` 결제를 생성합니다.

Request:

```json
{
  "cartItemIds": [1, 2],
  "usedPointAmount": 1000
}
```

Response: `OrderCreateResponse`

- `orderId`
- `orderNumber`
- `paymentOrderName`
- `totalAmount`
- `usedPointAmount`
- `finalPaymentAmount`
- `paymentId`
- `paymentStatus`
- `items`

주요 예외:

- 포인트 부족
- 빈 주문
- 장바구니 항목 없음
- 재고 부족
- 결제 금액 검증 실패

### GET `/api/orders`

목적: 인증 회원의 주문 목록을 페이징 조회합니다.

Request:

- Query: `page`, `size`, `sort`

Response: `PageResponse<GetOrderResponse>`

주요 예외:

- 인증 실패

### GET `/api/orders/{orderId}`

목적: 인증 회원의 주문 상세와 결제 정보를 조회합니다.

Request:

- Path: `orderId`

Response: `GetOrderDetailResponse`

주요 예외:

- 주문 없음
- 주문 소유자 불일치

### PATCH `/api/orders/{orderId}/cancel`

목적: 결제 전 주문을 취소하고 차감 재고를 복구합니다.

Request:

- Path: `orderId`

Response: `OrderCancelResponse`

주요 예외:

- 주문 없음
- 결제 없음
- 결제 상태가 `PENDING`이 아님

## Payment API

### POST `/api/payments/{paymentId}/confirm`

목적: PortOne 결제 결과를 검증하고 내부 결제를 확정합니다.

Request:

- Path: `paymentId`
- Body: 없음

Response: `PaymentConfirmResult`

- `paymentId`
- `memberId`
- `orderId`
- `finalPaymentAmount`
- `status`
- `paidAt`

주요 예외:

- 결제 없음
- 결제 소유자 불일치
- 결제 상태 오류
- PortOne 결제 조회 실패
- PortOne 결제 검증 실패

## Refund API

### POST `/api/payments/{paymentId}/refunds`

목적: 결제 건에 대해 주문 상품 단위 환불을 요청합니다.

Request:

```json
{
  "reason": "단순 변심",
  "items": [
    {
      "orderItemId": 1,
      "quantity": 1
    }
  ]
}
```

Response: `RefundResponse`

- `refundId`
- `paymentId`
- `status`
- `pointRefundAmount`
- `pgRefundAmount`
- `totalRefundAmount`
- `refundedAt`

주요 예외:

- 결제 없음
- 결제 소유자 불일치
- 환불 불가능한 결제 상태
- 환불 수량 초과
- 환불 금액 초과
- PortOne 환불 실패

## Point API

### GET `/api/points`

목적: 인증 회원의 현재 포인트 잔액을 조회합니다.

Request: 없음

Response: `PointResponse`

주요 예외:

- 회원 없음

### GET `/api/points/histories`

목적: 인증 회원의 포인트 이력을 페이징 조회합니다.

Request:

- Query: `page`, `size`, `sort`

Response: `Page<PointHistoryResponse>`

주요 예외:

- 회원 없음

## Membership API

### GET `/api/memberships/me`

목적: 인증 회원의 현재 멤버십을 조회합니다.

Request: 없음

Response: `MembershipResponse`

주요 예외:

- 멤버십 없음

### GET `/api/memberships/grades`

목적: 멤버십 등급 정책 목록을 조회합니다.

Request: 없음

Response: `List<MembershipGradeResponse>`

주요 예외:

- 등급 정책 없음

### POST `/api/memberships/recalculate`

목적: 확정 결제 금액과 완료 환불 금액을 기준으로 멤버십 누적 금액과 등급을 재계산합니다.

Request: 없음

Response: `MembershipRecalculateResponse`

주요 예외:

- 멤버십 없음
- 등급 정책 오류

## Subscription API

### POST `/api/subscriptions/payment-methods`

목적: PortOne 빌링키를 구독 결제 수단으로 등록합니다.

Request:

```json
{
  "portoneBillingKey": "billing-key",
  "cardCompanyName": "현대카드"
}
```

Response: `Void`

주요 예외:

- 다른 회원이 이미 등록한 빌링키
- 입력값 검증 실패

### POST `/api/subscriptions`

목적: 구독을 시작하고 첫 결제를 즉시 수행합니다.

Request:

```json
{
  "planId": 1,
  "paymentMethodId": 1
}
```

Response: `SubscriptionResponse`

주요 예외:

- 요금제 없음
- 결제 수단 없음
- 이미 활성 구독 존재
- 첫 결제 실패

### POST `/api/subscriptions/cancel/{subscriptionId}`

목적: 구독을 해지합니다.

Request:

- Path: `subscriptionId`

Response: `Void`

주요 예외:

- 구독 없음
- 구독 소유자 불일치

### GET `/api/subscriptions/me`

목적: 인증 회원의 활성 구독을 조회합니다.

Request: 없음

Response: `SubscriptionResponse`

주요 예외:

- 활성 구독 없음

## Webhook API

### POST `/api/payments/webhooks/portone`

목적: PortOne Webhook을 수신하고 결제/환불 후처리를 수행합니다.

Request Headers:

- `webhook-id`
- `webhook-timestamp`
- `webhook-signature`

Request Body:

- PortOne Webhook payload

Response: `WebhookResponse`

- `eventId`
- `status`

주요 예외:

- Webhook 서명 검증 실패
- Payload 파싱 실패
- Payload 필수 값 누락

