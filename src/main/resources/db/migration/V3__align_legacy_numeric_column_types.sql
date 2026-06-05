ALTER TABLE products
    MODIFY COLUMN price BIGINT NOT NULL,
    MODIFY COLUMN stock BIGINT NOT NULL;

ALTER TABLE members
    MODIFY COLUMN point_balance BIGINT NOT NULL;

ALTER TABLE orders
    MODIFY COLUMN total_price BIGINT NOT NULL,
    MODIFY COLUMN used_point_amount BIGINT NOT NULL;

ALTER TABLE order_items
    MODIFY COLUMN order_price BIGINT NOT NULL,
    MODIFY COLUMN quantity BIGINT NOT NULL,
    MODIFY COLUMN source_cart_item_id BIGINT;

ALTER TABLE payments
    MODIFY COLUMN total_order_amount BIGINT NOT NULL,
    MODIFY COLUMN final_payment_amount BIGINT NOT NULL,
    MODIFY COLUMN used_point_amount BIGINT NOT NULL,
    MODIFY COLUMN earned_point_amount BIGINT NOT NULL;

ALTER TABLE refunds
    MODIFY COLUMN payment_id BIGINT NOT NULL,
    MODIFY COLUMN pg_refund_amount BIGINT NOT NULL,
    MODIFY COLUMN point_refund_amount BIGINT NOT NULL;

ALTER TABLE refund_items
    MODIFY COLUMN order_item_id BIGINT NOT NULL,
    MODIFY COLUMN refund_quantity BIGINT NOT NULL,
    MODIFY COLUMN pg_refund_amount BIGINT NOT NULL,
    MODIFY COLUMN point_refund_amount BIGINT NOT NULL;

ALTER TABLE point_histories
    MODIFY COLUMN member_id BIGINT NOT NULL,
    MODIFY COLUMN payment_id BIGINT NOT NULL,
    MODIFY COLUMN refund_id BIGINT,
    MODIFY COLUMN amount BIGINT NOT NULL;
