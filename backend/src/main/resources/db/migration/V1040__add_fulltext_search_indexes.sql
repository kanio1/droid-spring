-- V1040__add_fulltext_search_indexes.sql
-- Add full-text search indexes for advanced search functionality

-- Enable PostgreSQL extensions for full-text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;  -- For trigram similarity
CREATE EXTENSION IF NOT EXISTS unaccent;  -- For accent-insensitive search

-- Add search vector column to customers table
ALTER TABLE customers
ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create function to update search vector
CREATE OR REPLACE FUNCTION update_customer_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.first_name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.last_name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.email, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(NEW.phone, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(NEW.pesel, '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(NEW.nip, '')), 'C') ||
        setweight(to_tsvector('english', coalesce(NEW.address, '')), 'D');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to update search vector on insert/update
DROP TRIGGER IF EXISTS update_customer_search_vector_trigger ON customers;
CREATE TRIGGER update_customer_search_vector_trigger
    BEFORE INSERT OR UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION update_customer_search_vector();

-- Create GIN index for full-text search
CREATE INDEX IF NOT EXISTS idx_customers_search_vector
    ON customers USING GIN (search_vector);

-- Create GIN index for trigram search (for fuzzy matching)
CREATE INDEX IF NOT EXISTS idx_customers_name_trgm
    ON customers USING GIN (first_name gin_trgm_ops, last_name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_customers_email_trgm
    ON customers USING GIN (email gin_trgm_ops);

-- Add search vector column to invoices table
ALTER TABLE invoices
ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create function to update invoice search vector
CREATE OR REPLACE FUNCTION update_invoice_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.invoice_number, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.description, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(NEW.status, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for invoice search vector
DROP TRIGGER IF EXISTS update_invoice_search_vector_trigger ON invoices;
CREATE TRIGGER update_invoice_search_vector_trigger
    BEFORE INSERT OR UPDATE ON invoices
    FOR EACH ROW
    EXECUTE FUNCTION update_invoice_search_vector();

-- Create GIN index for invoices
CREATE INDEX IF NOT EXISTS idx_invoices_search_vector
    ON invoices USING GIN (search_vector);

-- Add search vector column to products table
ALTER TABLE products
ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create function to update product search vector
CREATE OR REPLACE FUNCTION update_product_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', coalesce(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(NEW.description, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(NEW.category, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for product search vector
DROP TRIGGER IF EXISTS update_product_search_vector_trigger ON products;
CREATE TRIGGER update_product_search_vector_trigger
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_product_search_vector();

-- Create GIN index for products
CREATE INDEX IF NOT EXISTS idx_products_search_vector
    ON products USING GIN (search_vector);

-- Update search vectors for existing records
UPDATE customers SET
    search_vector =
        setweight(to_tsvector('english', coalesce(first_name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(last_name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(email, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(phone, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(pesel, '')), 'C') ||
        setweight(to_tsvector('simple', coalesce(nip, '')), 'C') ||
        setweight(to_tsvector('english', coalesce(address, '')), 'D');

UPDATE invoices SET
    search_vector =
        setweight(to_tsvector('english', coalesce(invoice_number, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(status, '')), 'C');

UPDATE products SET
    search_vector =
        setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
        setweight(to_tsvector('simple', coalesce(category, '')), 'C');

-- Create function for advanced search with ranking
CREATE OR REPLACE FUNCTION advanced_customer_search(
    search_query text,
    status_filter text DEFAULT NULL,
    limit_results int DEFAULT 100,
    offset_results int DEFAULT 0
)
RETURNS TABLE(
    customer_id uuid,
    first_name varchar,
    last_name varchar,
    email varchar,
    phone varchar,
    status varchar,
    created_at timestamptz,
    rank real
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.first_name,
        c.last_name,
        c.email,
        c.phone,
        c.status::varchar,
        c.created_at,
        ts_rank(c.search_vector, plainto_tsquery('english', search_query)) as rank
    FROM customers c
    WHERE
        (status_filter IS NULL OR c.status::text = status_filter)
        AND (
            c.search_vector @@ plainto_tsquery('english', search_query)
            OR c.first_name ILIKE '%' || search_query || '%'
            OR c.last_name ILIKE '%' || search_query || '%'
            OR c.email ILIKE '%' || search_query || '%'
        )
    ORDER BY rank DESC, c.created_at DESC
    LIMIT limit_results
    OFFSET offset_results;
END;
$$ LANGUAGE plpgsql;

-- Create function for fuzzy search
CREATE OR REPLACE FUNCTION fuzzy_customer_search(
    search_query text,
    similarity_threshold float DEFAULT 0.3,
    limit_results int DEFAULT 100
)
RETURNS TABLE(
    customer_id uuid,
    first_name varchar,
    last_name varchar,
    email varchar,
    phone varchar,
    status varchar,
    created_at timestamptz,
    similarity real
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.id,
        c.first_name,
        c.last_name,
        c.email,
        c.phone,
        c.status::varchar,
        c.created_at,
        greatest(
            similarity(lower(c.first_name), lower(search_query)),
            similarity(lower(c.last_name), lower(search_query)),
            similarity(lower(c.email), lower(search_query))
        ) as similarity
    FROM customers c
    WHERE
        c.first_name ILIKE '%' || search_query || '%'
        OR c.last_name ILIKE '%' || search_query || '%'
        OR c.email ILIKE '%' || search_query || '%'
        OR similarity(lower(c.first_name), lower(search_query)) > similarity_threshold
        OR similarity(lower(c.last_name), lower(search_query)) > similarity_threshold
    ORDER BY similarity DESC
    LIMIT limit_results;
END;
$$ LANGUAGE plpgsql;
