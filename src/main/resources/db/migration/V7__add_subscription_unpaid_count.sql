-- Add unpaid_count column to subscriptions table
ALTER TABLE subscriptions ADD COLUMN unpaid_count INT NOT NULL DEFAULT 0;
