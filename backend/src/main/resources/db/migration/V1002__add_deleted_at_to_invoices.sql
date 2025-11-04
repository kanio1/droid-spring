-- Add soft delete column to invoices table
-- Required by InvoiceEntity.deletedAt field

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS deleted_at DATE NULL;
