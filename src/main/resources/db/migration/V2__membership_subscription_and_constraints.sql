-- Align legacy numeric column types after V1.
ALTER TABLE cart_items MODIFY COLUMN quantity BIGINT NOT NULL;

ALTER TABLE products MODIFY COLUMN price BIGINT NOT NULL;
ALTER TABLE products MODIFY COLUMN stock BIGINT NOT NULL;

ALTER TABLE members MODIFY COLUMN point_balance BIGINT NOT NULL;

ALTER TABLE orders MODIFY COLUMN total_price BIGINT NOT NULL;
ALTER TABLE orders MODIFY COLUMN used_point_amount BIGINT NOT NULL;

ALTER TABLE order_items MODIFY COLUMN order_price BIGINT NOT NULL;
ALTER TABLE order_items MODIFY COLUMN quantity BIGINT NOT NULL;
ALTER TABLE order_items MODIFY COLUMN source_cart_item_id BIGINT;

ALTER TABLE payments MODIFY COLUMN total_order_amount BIGINT NOT NULL;
ALTER TABLE payments MODIFY COLUMN final_payment_amount BIGINT NOT NULL;
ALTER TABLE payments MODIFY COLUMN used_point_amount BIGINT NOT NULL;
ALTER TABLE payments MODIFY COLUMN earned_point_amount BIGINT NOT NULL;

ALTER TABLE refunds MODIFY COLUMN payment_id BIGINT NOT NULL;
ALTER TABLE refunds MODIFY COLUMN pg_refund_amount BIGINT NOT NULL;
ALTER TABLE refunds MODIFY COLUMN point_refund_amount BIGINT NOT NULL;

ALTER TABLE refund_items MODIFY COLUMN order_item_id BIGINT NOT NULL;
ALTER TABLE refund_items MODIFY COLUMN refund_quantity BIGINT NOT NULL;
ALTER TABLE refund_items MODIFY COLUMN pg_refund_amount BIGINT NOT NULL;
ALTER TABLE refund_items MODIFY COLUMN point_refund_amount BIGINT NOT NULL;

ALTER TABLE point_histories MODIFY COLUMN member_id BIGINT NOT NULL;
ALTER TABLE point_histories MODIFY COLUMN payment_id BIGINT NOT NULL;
ALTER TABLE point_histories MODIFY COLUMN refund_id BIGINT;
ALTER TABLE point_histories MODIFY COLUMN amount BIGINT NOT NULL;

-- Add point source type to preserve idempotency by business source.
ALTER TABLE point_histories ADD COLUMN source_type VARCHAR(50) NOT NULL DEFAULT 'ORDER';
ALTER TABLE point_histories DROP CONSTRAINT uk_point_history_idempotency;
ALTER TABLE point_histories ADD CONSTRAINT uk_point_history_idempotency UNIQUE (payment_id, type, refund_id, source_type);

-- Membership and subscription schema.
CREATE TABLE membership_grades (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    min_cumulative_payment_amount BIGINT NOT NULL,
    point_reward_rate INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE member_memberships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    membership_grade_id BIGINT NOT NULL,
    cumulative_payment_amount BIGINT NOT NULL,
    grade_updated_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_member_memberships_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_member_memberships_grade FOREIGN KEY (membership_grade_id) REFERENCES membership_grades(id)
);

CREATE TABLE plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    monthly_amount BIGINT NOT NULL,
    description VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE payment_methods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    portone_billing_key VARCHAR(255) NOT NULL UNIQUE,
    card_company_name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_payment_methods_member FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    payment_method_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    active_plan_key VARCHAR(255) UNIQUE,
    next_billing_date DATE NOT NULL,
    started_at DATETIME(6) NOT NULL,
    cancelled_at DATETIME(6),
    is_unpaid BOOLEAN NOT NULL DEFAULT FALSE,
    unpaid_count INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_subscriptions_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES plans(id),
    CONSTRAINT fk_subscriptions_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id)
);

CREATE TABLE subscription_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    billing_period VARCHAR(7) NOT NULL,
    portone_payment_id VARCHAR(100) NOT NULL UNIQUE,
    billing_amount BIGINT NOT NULL,
    membership_grade_name VARCHAR(50) NOT NULL,
    point_reward_rate INT NOT NULL,
    earned_point_amount BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    paid_at DATETIME(6),
    failure_reason VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_subscription_invoices_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    CONSTRAINT uk_subscription_invoices_subscription_billing_period UNIQUE (subscription_id, billing_period)
);

-- Non-negative numeric constraints.
ALTER TABLE products ADD CONSTRAINT chk_products_price_non_negative CHECK (price >= 0);
ALTER TABLE products ADD CONSTRAINT chk_products_stock_non_negative CHECK (stock >= 0);

ALTER TABLE members ADD CONSTRAINT chk_members_point_balance_non_negative CHECK (point_balance >= 0);

ALTER TABLE cart_items ADD CONSTRAINT chk_cart_items_quantity_non_negative CHECK (quantity >= 0);

ALTER TABLE orders ADD CONSTRAINT chk_orders_total_price_non_negative CHECK (total_price >= 0);
ALTER TABLE orders ADD CONSTRAINT chk_orders_used_point_amount_non_negative CHECK (used_point_amount >= 0);

ALTER TABLE order_items ADD CONSTRAINT chk_order_items_order_price_non_negative CHECK (order_price >= 0);
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_quantity_non_negative CHECK (quantity >= 0);

ALTER TABLE payments ADD CONSTRAINT chk_payments_total_order_amount_non_negative CHECK (total_order_amount >= 0);
ALTER TABLE payments ADD CONSTRAINT chk_payments_final_payment_amount_non_negative CHECK (final_payment_amount >= 0);
ALTER TABLE payments ADD CONSTRAINT chk_payments_used_point_amount_non_negative CHECK (used_point_amount >= 0);
ALTER TABLE payments ADD CONSTRAINT chk_payments_earned_point_amount_non_negative CHECK (earned_point_amount >= 0);

ALTER TABLE refunds ADD CONSTRAINT chk_refunds_pg_refund_amount_non_negative CHECK (pg_refund_amount >= 0);
ALTER TABLE refunds ADD CONSTRAINT chk_refunds_point_refund_amount_non_negative CHECK (point_refund_amount >= 0);

ALTER TABLE refund_items ADD CONSTRAINT chk_refund_items_refund_quantity_non_negative CHECK (refund_quantity >= 0);
ALTER TABLE refund_items ADD CONSTRAINT chk_refund_items_pg_refund_amount_non_negative CHECK (pg_refund_amount >= 0);
ALTER TABLE refund_items ADD CONSTRAINT chk_refund_items_point_refund_amount_non_negative CHECK (point_refund_amount >= 0);

ALTER TABLE point_histories ADD CONSTRAINT chk_point_histories_amount_non_negative CHECK (amount >= 0);

ALTER TABLE membership_grades ADD CONSTRAINT chk_membership_grades_min_amount_non_negative CHECK (min_cumulative_payment_amount >= 0);
ALTER TABLE membership_grades ADD CONSTRAINT chk_membership_grades_reward_rate_non_negative CHECK (point_reward_rate >= 0);

ALTER TABLE member_memberships ADD CONSTRAINT chk_member_memberships_cumulative_amount_non_negative CHECK (cumulative_payment_amount >= 0);

ALTER TABLE plans ADD CONSTRAINT chk_plans_monthly_amount_non_negative CHECK (monthly_amount >= 0);

ALTER TABLE subscriptions ADD CONSTRAINT chk_subscriptions_unpaid_count_non_negative CHECK (unpaid_count >= 0);

ALTER TABLE subscription_invoices ADD CONSTRAINT chk_subscription_invoices_billing_amount_non_negative CHECK (billing_amount >= 0);
ALTER TABLE subscription_invoices ADD CONSTRAINT chk_subscription_invoices_reward_rate_non_negative CHECK (point_reward_rate >= 0);
ALTER TABLE subscription_invoices ADD CONSTRAINT chk_subscription_invoices_earned_amount_non_negative CHECK (earned_point_amount >= 0);

-- Seed initial membership and subscription plan data.
INSERT INTO membership_grades (name, min_cumulative_payment_amount, point_reward_rate, created_at, updated_at)
VALUES
('NORMAL', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('VIP', 50000, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('VVIP', 100000, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO plans (name, monthly_amount, description, created_at, updated_at)
VALUES
('Basic Plan', 9900, '기본 요금제', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Premium Plan', 19900, '프리미엄 요금제', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
