ALTER TABLE point_histories ADD COLUMN source_type VARCHAR(50) NOT NULL DEFAULT 'ORDER';

ALTER TABLE point_histories DROP CONSTRAINT uk_point_history_idempotency;

ALTER TABLE point_histories ADD CONSTRAINT uk_point_history_idempotency UNIQUE (payment_id, type, refund_id, source_type);
