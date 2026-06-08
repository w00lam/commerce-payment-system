-- H2 DB compatibility: Separate ALTER TABLE statements for each column modification
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
