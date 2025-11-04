-- Add soft delete column to products table
-- Required by ProductEntity.deletedAt field

ALTER TABLE products ADD COLUMN IF NOT EXISTS deleted_at DATE NULL;
