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
    CONSTRAINT fk_subscription_invoices_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);

-- Seed initial data
INSERT INTO membership_grades (name, min_cumulative_payment_amount, point_reward_rate, created_at, updated_at)
VALUES 
('NORMAL', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('VIP', 100000, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('VVIP', 500000, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO plans (name, monthly_amount, description, created_at, updated_at)
VALUES
('Basic Plan', 9900, '기본 요금제', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Premium Plan', 19900, '프리미엄 요금제', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
