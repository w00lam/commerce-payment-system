src/main/java/com/sparta/paymentsystem
├─ global
│   ├─ config
│   │   ├─ JpaAuditingConfig.java
│   │   └─ WebConfig.java
│   │
│   ├─ entity
│   │   └─ BaseEntity.java
│   │
│   ├─ exception
│   │   ├─ ErrorCode.java
│   │   ├─ GlobalErrorCode.java
│   │   ├─ ServiceException.java
│   │   ├─ FieldErrorResponse.java
│   │   └─ GlobalExceptionHandler.java
│   │
│   ├─ response
│   │   └─ ApiResponse.java
│   │
│   └─ security
│       └─ SecurityConfig.java
│
└─ domain
    ├─ member
    │   └─ exception
    │       └─ MemberErrorCode.java
    │
    ├─ product
    │   └─ exception
    │       └─ ProductErrorCode.java
    │
    ├─ order
    │   └─ exception
    │       └─ OrderErrorCode.java
    │
    └─ payment
    │    └─ exception
    │        └─ PaymentErrorCode.java
    └─ point
        └─ exception
            └─ PointErrorCode.java