-- Product Features table
-- Configurable features/parameters for products

CREATE TYPE feature_data_type AS ENUM ('STRING', 'NUMBER', 'BOOLEAN', 'JSON');

CREATE TABLE product_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    feature_key VARCHAR(100) NOT NULL,
    feature_value TEXT NOT NULL,
    data_type feature_data_type NOT NULL,
    is_configurable BOOLEAN DEFAULT true,
    display_order INTEGER DEFAULT 0,
    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version INTEGER NOT NULL DEFAULT 1,
    -- Unique constraint
    UNIQUE(product_id, feature_key)
);

-- Indexes for performance
CREATE INDEX idx_product_features_product ON product_features(product_id);
CREATE INDEX idx_product_features_key ON product_features(feature_key);
CREATE INDEX idx_product_features_type ON product_features(data_type);
CREATE INDEX idx_product_features_order ON product_features(display_order);

-- Update trigger for updated_at column
CREATE TRIGGER update_product_features_updated_at
    BEFORE UPDATE ON product_features
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
