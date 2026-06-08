-- Add is_unpaid column to subscriptions table
ALTER TABLE subscriptions ADD COLUMN is_unpaid BOOLEAN NOT NULL DEFAULT FALSE;
