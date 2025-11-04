-- Payments table
-- Payment tracking

CREATE TYPE payment_method AS ENUM ('CARD', 'BANK_TRANSFER', 'CASH', 'DIRECT_DEBIT', 'MOBILE_PAY');
CREATE TYPE payment_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REFUNDED');

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id UUID NOT NULL REFERENCES customers(id),
    invoice_id UUID REFERENCES invoices(id),
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'PLN',
    payment_method payment_method NOT NULL,
    payment_status payment_status NOT NULL,
    transaction_id VARCHAR(100),
    gateway VARCHAR(50),
    payment_date DATE NOT NULL,
    received_date DATE,
    reference_number VARCHAR(100),
    notes TEXT,
    reversal_reason TEXT,
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

-- Indexes for performance
CREATE INDEX idx_payments_customer ON payments(customer_id);
CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_status ON payments(payment_status);
CREATE INDEX idx_payments_date ON payments(payment_date);
CREATE INDEX idx_payments_number ON payments(payment_number);
CREATE INDEX idx_payments_gateway ON payments(gateway);

-- Update trigger for updated_at column
CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
