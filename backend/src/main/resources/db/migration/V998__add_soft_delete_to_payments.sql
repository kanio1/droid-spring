-- Migration to add soft delete support to PaymentEntity
-- Adds deleted_at column to payments table

-- Add deleted_at column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'payments' AND column_name = 'deleted_at'
    ) THEN
        ALTER TABLE payments ADD COLUMN deleted_at DATE;
    END IF;
END $$;

-- Add index for deleted_at for better query performance
CREATE INDEX IF NOT EXISTS idx_payments_deleted_at ON payments(deleted_at);

-- Comment on the column
COMMENT ON COLUMN payments.deleted_at IS 'Soft delete timestamp - null means active record';
