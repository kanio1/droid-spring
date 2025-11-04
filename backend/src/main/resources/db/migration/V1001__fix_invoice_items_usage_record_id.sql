-- Fix invoice_items.usage_record_id column type mismatch
-- Entity expects VARCHAR(255) but column was created as UUID
-- Changing to VARCHAR(255) for consistency

ALTER TABLE invoice_items ALTER COLUMN usage_record_id TYPE VARCHAR(255);
