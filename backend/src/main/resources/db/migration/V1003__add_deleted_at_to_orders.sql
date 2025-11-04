-- Add soft delete column to orders table
-- Required by OrderEntity.deletedAt field

ALTER TABLE orders ADD COLUMN IF NOT EXISTS deleted_at DATE NULL;
