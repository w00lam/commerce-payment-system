# Point

포인트는 `members.point_balance`에 현재 잔액을 저장하고, `point_histories`에 모든 변경 이력을 저장합니다.

## 적립 정책

### 주문 결제 적립

- 결제 확정 후 `PaymentPostProcessService`가 `PointService.earnPoint`를 호출합니다.
- 적립 금액은 결제 생성 시점의 멤버십 등급 적립률로 계산됩니다.
- `Payment.create`는 `finalPaymentAmount * pointRewardRate / 100`을 `earnedPointAmount`로 저장합니다.
- 적립 이력 type은 `EARN`입니다.
- 주문 결제 적립의 source type은 `ORDER`입니다.

### 구독 결제 적립

- 구독 결제 성공 후 `SubscriptionBillingFinalizer`가 `PointService.earnPoint`를 호출합니다.
- 적립 금액은 구독 invoice의 `billingAmount * pointRewardRate / 100`입니다.
- 구독 결제 적립의 source type은 `SUBSCRIPTION`입니다.

## 사용 정책

- 주문 생성 시 `usedPointAmount`를 지정할 수 있습니다.
- `OrderFacade`는 회원 row를 pessimistic lock으로 조회해 포인트 잔액을 검증합니다.
- 결제 확정 후 `PaymentPostProcessService`가 실제 포인트 차감을 수행합니다.
- 사용 이력 type은 `USE`입니다.
- 포인트 전액 결제는 `finalPaymentAmount == 0`이며 PortOne 검증 없이 내부 결제를 확정합니다.

## 환불 정책

### 사용 포인트 복구

- 환불에 포함된 `pointRefundAmount`가 0보다 크면 `PointService.restorePoint`를 호출합니다.
- 복구 이력 type은 `USE_CANCEL`입니다.
- 원본 `USE` 이력이 없으면 복구할 수 없습니다.

### 적립 포인트 회수

- 결제 때 적립한 포인트는 환불 시 회수합니다.
- 부분 환불은 `pgRefundAmount * earnedPointAmount / finalPaymentAmount`로 회수 금액을 계산합니다.
- 전체 환불은 아직 회수되지 않은 적립 포인트를 모두 회수합니다.
- 회원 잔액이 부족하면 가능한 범위까지만 회수합니다.
- 회수 이력 type은 `EARN_REVOKE`입니다.

## 멱등성

`point_histories`는 다음 조합으로 중복 처리를 방지합니다.

- `payment_id`
- `type`
- `refund_id`
- `source_type`

같은 결제 또는 환불 이벤트가 반복 처리되어도 동일 포인트 이력이 중복 저장되지 않도록 서비스에서 존재 여부를 확인합니다.

