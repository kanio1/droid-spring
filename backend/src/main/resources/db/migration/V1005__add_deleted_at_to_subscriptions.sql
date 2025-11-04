-- Add soft delete column to subscriptions table
-- Required by SubscriptionEntity.deletedAt field

ALTER TABLE subscriptions ADD COLUMN IF NOT EXISTS deleted_at DATE NULL;
