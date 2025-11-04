-- Products table
-- Contains services, tariffs, bundles, and add-ons

CREATE TYPE product_type AS ENUM ('SERVICE', 'TARIFF', 'BUNDLE', 'ADDON');
CREATE TYPE product_category AS ENUM ('MOBILE', 'BROADBAND', 'TV', 'CLOUD');
CREATE TYPE product_status AS ENUM ('ACTIVE', 'INACTIVE', 'DEPRECATED');

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    product_type product_type NOT NULL,
    category product_category,
    price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'PLN',
    billing_period VARCHAR(20) NOT NULL,
    status product_status DEFAULT 'ACTIVE',
    validity_start DATE,
    validity_end DATE,
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1
);

-- Indexes for performance
CREATE INDEX idx_products_type ON products(product_type);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_code ON products(product_code);
CREATE INDEX idx_products_created_at ON products(created_at);

-- Update trigger for updated_at column
CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
