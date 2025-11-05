-- Junction table for billing cycle to invoice relationship
CREATE TABLE billing_cycle_invoices (
    id VARCHAR(36) PRIMARY KEY,
    billing_cycle_id VARCHAR(36) NOT NULL,
    invoice_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_billing_cycle_invoices_cycle ON billing_cycle_invoices(billing_cycle_id);
CREATE INDEX idx_billing_cycle_invoices_invoice ON billing_cycle_invoices(invoice_id);

-- Foreign keys
ALTER TABLE billing_cycle_invoices
    ADD CONSTRAINT fk_bci_billing_cycle
    FOREIGN KEY (billing_cycle_id) REFERENCES billing_cycles(id);

ALTER TABLE billing_cycle_invoices
    ADD CONSTRAINT fk_bci_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id);

-- Unique constraint to prevent duplicate associations
ALTER TABLE billing_cycle_invoices
    ADD CONSTRAINT uk_bci_cycle_invoice
    UNIQUE (billing_cycle_id, invoice_id);

-- Comments
COMMENT ON TABLE billing_cycle_invoices IS 'Junction table linking billing cycles to invoices';
