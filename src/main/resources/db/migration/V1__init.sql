CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_cart_product UNIQUE (cart_id, product_id)
);

CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    point_balance BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6)
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price BIGINT NOT NULL,
    stock BIGINT NOT NULL,
    description VARCHAR(255),
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6)
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    total_price BIGINT NOT NULL,
    used_point_amount BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    order_price BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    source_cart_item_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(100) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    order_name VARCHAR(100),
    total_order_amount BIGINT NOT NULL,
    final_payment_amount BIGINT NOT NULL,
    used_point_amount BIGINT NOT NULL,
    earned_point_amount BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    paid_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    pg_refund_amount BIGINT NOT NULL,
    point_refund_amount BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE refund_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    refund_quantity BIGINT NOT NULL,
    pg_refund_amount BIGINT NOT NULL,
    point_refund_amount BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE point_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    refund_id BIGINT,
    amount BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_point_history_idempotency UNIQUE (payment_id, type, refund_id)
);

CREATE TABLE webhook_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payment_id VARCHAR(100),
    result_message VARCHAR(500),
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

-- Foreign Keys
ALTER TABLE cart_items 
    ADD CONSTRAINT fk_cart_items_cart_id 
    FOREIGN KEY (cart_id) REFERENCES carts(id);

ALTER TABLE order_items 
    ADD CONSTRAINT fk_order_items_order_id 
    FOREIGN KEY (order_id) REFERENCES orders(id);

ALTER TABLE order_items 
    ADD CONSTRAINT fk_order_items_product_id 
    FOREIGN KEY (product_id) REFERENCES products(id);

ALTER TABLE orders 
    ADD CONSTRAINT fk_orders_member_id 
    FOREIGN KEY (member_id) REFERENCES members(id);

ALTER TABLE refund_items 
    ADD CONSTRAINT fk_refund_items_refund_id 
    FOREIGN KEY (refund_id) REFERENCES refunds(id);
