-- Migration to fix CustomerEntity BaseEntity inheritance
-- Adds missing audit columns (created_by, updated_by) that exist in BaseEntity

-- Add created_by column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'customers' AND column_name = 'created_by'
    ) THEN
        ALTER TABLE customers ADD COLUMN created_by VARCHAR(100);
    END IF;
END $$;

-- Add updated_by column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'customers' AND column_name = 'updated_by'
    ) THEN
        ALTER TABLE customers ADD COLUMN updated_by VARCHAR(100);
    END IF;
END $$;

-- Add index for created_by for better query performance
CREATE INDEX IF NOT EXISTS idx_customers_created_by ON customers(created_by);

-- Add index for updated_by for better query performance
CREATE INDEX IF NOT EXISTS idx_customers_updated_by ON customers(updated_by);

-- Update version column to match BaseEntity (change from INTEGER to BIGINT)
ALTER TABLE customers ALTER COLUMN version TYPE BIGINT;

-- Update version default to match BaseEntity (0 instead of 1)
ALTER TABLE customers ALTER COLUMN version SET DEFAULT 0;

-- Comment on the migration
COMMENT ON COLUMN customers.created_by IS 'User who created this record (from BaseEntity)';
COMMENT ON COLUMN customers.updated_by IS 'User who last updated this record (from BaseEntity)';
COMMENT ON COLUMN customers.version IS 'Optimistic locking version (from BaseEntity)';
