-- Invoices table
-- Invoice generation and management

CREATE TYPE invoice_type AS ENUM ('RECURRING', 'ONE_TIME', 'USAGE', 'ADJUSTMENT');
CREATE TYPE invoice_status AS ENUM ('DRAFT', 'ISSUED', 'SENT', 'PAID', 'OVERDUE', 'CANCELLED');

CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    invoice_type invoice_type NOT NULL,
    status invoice_status NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    paid_date DATE,
    billing_period_start DATE,
    billing_period_end DATE,
    subtotal DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    tax_amount DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'PLN',
    payment_terms INTEGER DEFAULT 14,
    late_fee DECIMAL(10,2) DEFAULT 0,
    notes TEXT,
    pdf_url VARCHAR(500),
    sent_to_email VARCHAR(200),
    sent_at TIMESTAMPTZ,
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

-- Indexes for performance
CREATE INDEX idx_invoices_customer ON invoices(customer_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_dates ON invoices(issue_date, due_date);
CREATE INDEX idx_invoices_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_type ON invoices(invoice_type);

-- Update trigger for updated_at column
CREATE TRIGGER update_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
