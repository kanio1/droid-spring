-- =====================================================
-- Billing Functions for pg_cron Jobs
-- =====================================================

-- Function to aggregate daily usage data
CREATE OR REPLACE FUNCTION aggregate_daily_usage()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    processing_date DATE := CURRENT_DATE - INTERVAL '1 day';
    record_count INTEGER;
BEGIN
    -- Log the start of aggregation
    RAISE NOTICE 'Starting daily usage aggregation for %', processing_date;

    -- Example: Aggregate usage from usage_records table
    -- This is a placeholder - adapt to your actual usage tracking schema
    INSERT INTO usage_records (customer_id, usage_date, usage_type, quantity, created_at)
    SELECT
        customer_id,
        processing_date,
        'AGGREGATED',
        SUM(quantity),
        NOW()
    FROM usage_records
    WHERE usage_date = processing_date
    GROUP BY customer_id
    ON CONFLICT (customer_id, usage_date, usage_type) DO UPDATE SET
        quantity = EXCLUDED.quantity,
        created_at = NOW();

    GET DIAGNOSTICS record_count = ROW_COUNT;

    RETURN QUERY SELECT 'Aggregated ' || record_count::TEXT || ' usage records' as summary;

    RAISE NOTICE 'Completed daily usage aggregation: % records processed', record_count;
END;
$$ LANGUAGE plpgsql;

-- Function to generate monthly invoices
CREATE OR REPLACE FUNCTION generate_monthly_invoices(target_month DATE)
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    invoice_count INTEGER := 0;
    total_amount DECIMAL := 0;
BEGIN
    RAISE NOTICE 'Starting monthly invoice generation for %', target_month;

    -- Get billing cycle start and end dates
    -- This is a simplified example - adapt to your business logic
    DECLARE
        cycle_start DATE := DATE_TRUNC('month', target_month);
        cycle_end DATE := cycle_start + INTERVAL '1 month' - INTERVAL '1 day';
    BEGIN
        -- Create invoices for active subscriptions
        -- This is a placeholder - adapt to your actual invoice schema
        INSERT INTO invoices (
            id, customer_id, invoice_date, due_date, status, total_amount,
            created_at, updated_at
        )
        SELECT
            gen_random_uuid(),
            s.customer_id,
            cycle_start,
            cycle_start + INTERVAL '30 days',
            'PENDING',
            COALESCE(s.price, 0) + COALESCE(usage.total_usage_cost, 0),
            NOW(),
            NOW()
        FROM subscriptions s
        LEFT JOIN (
            -- Calculate usage-based costs
            SELECT
                customer_id,
                SUM(quantity * unit_price) as total_usage_cost
            FROM usage_records
            WHERE usage_date BETWEEN cycle_start AND cycle_end
            GROUP BY customer_id
        ) usage ON s.customer_id = usage.customer_id
        WHERE s.status = 'ACTIVE'
        AND s.billing_cycle_start <= cycle_start
        AND (s.billing_cycle_end IS NULL OR s.billing_cycle_end >= cycle_end)
        ON CONFLICT DO NOTHING;

        GET DIAGNOSTICS invoice_count = ROW_COUNT;

        -- Calculate total amount
        SELECT COALESCE(SUM(total_amount), 0)
        INTO total_amount
        FROM invoices
        WHERE invoice_date = cycle_start;

        RAISE NOTICE 'Generated % invoices with total amount: %', invoice_count, total_amount;
    END;

    RETURN QUERY SELECT 'Generated ' || invoice_count::TEXT || ' invoices (Total: $' || total_amount::TEXT || ')' as summary;
END;
$$ LANGUAGE plpgsql;

-- Function to send invoice notifications
CREATE OR REPLACE FUNCTION send_invoice_notifications()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    notification_count INTEGER := 0;
BEGIN
    RAISE NOTICE 'Starting invoice notification dispatch';

    -- Send notifications for new invoices
    -- This is a placeholder - implement actual email/SMS sending
    UPDATE invoices
    SET updated_at = NOW()
    WHERE status = 'PENDING'
    AND created_at >= CURRENT_DATE - INTERVAL '1 day';

    GET DIAGNOSTICS notification_count = ROW_COUNT;

    RAISE NOTICE 'Sent % invoice notifications', notification_count;

    RETURN QUERY SELECT 'Sent notifications for ' || notification_count::TEXT || ' invoices' as summary;
END;
$$ LANGUAGE plpgsql;

-- Function to process pending payments
CREATE OR REPLACE FUNCTION process_pending_payments()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    processed_count INTEGER := 0;
    success_count INTEGER := 0;
    failure_count INTEGER := 0;
BEGIN
    RAISE NOTICE 'Starting payment processing';

    -- Process pending payments
    -- This is a simplified example - implement actual payment gateway integration
    FOR processed_count IN
        SELECT COUNT(*)
        FROM invoices
        WHERE status = 'PENDING'
    LOOP
        EXIT;
    END LOOP;

    -- Simulate payment processing
    UPDATE invoices
    SET status = 'PAID', updated_at = NOW()
    WHERE status = 'PENDING'
    AND id IN (
        SELECT id
        FROM invoices
        WHERE status = 'PENDING'
        ORDER BY created_at ASC
        LIMIT 10
    );

    GET DIAGNOSTICS success_count = ROW_COUNT;
    failure_count := processed_count - success_count;

    RAISE NOTICE 'Processed payments: % successful, % failed', success_count, failure_count;

    RETURN QUERY SELECT 'Processed: ' || success_count::TEXT || ' successful, ' || failure_count::TEXT || ' failed' as summary;
END;
$$ LANGUAGE plpgsql;

-- Function to check subscription renewals
CREATE OR REPLACE FUNCTION check_subscription_renewals()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    upcoming_renewals INTEGER := 0;
    renewal_count INTEGER := 0;
BEGIN
    RAISE NOTICE 'Checking subscription renewals';

    -- Find subscriptions that will renew in the next 7 days
    SELECT COUNT(*)
    INTO upcoming_renewals
    FROM subscriptions
    WHERE status = 'ACTIVE'
    AND billing_cycle_end BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days';

    RAISE NOTICE 'Found % upcoming renewals', upcoming_renewals;

    -- Send renewal notifications
    -- This is a placeholder - implement actual notification logic
    renewal_count := upcoming_renewals;

    RETURN QUERY SELECT 'Found ' || upcoming_renewals::TEXT || ' upcoming renewals, sent ' || renewal_count::TEXT || ' notifications' as summary;
END;
$$ LANGUAGE plpgsql;

-- Function to process subscription renewals
CREATE OR REPLACE FUNCTION process_subscription_renewals()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    renewed_count INTEGER := 0;
BEGIN
    RAISE NOTICE 'Processing subscription renewals';

    -- Renew subscriptions that are due
    UPDATE subscriptions
    SET
        billing_cycle_start = billing_cycle_end + INTERVAL '1 day',
        billing_cycle_end = billing_cycle_end + INTERVAL '1 month',
        updated_at = NOW()
    WHERE status = 'ACTIVE'
    AND billing_cycle_end < CURRENT_DATE
    AND billing_cycle_end >= CURRENT_DATE - INTERVAL '7 days';

    GET DIAGNOSTICS renewed_count = ROW_COUNT;

    RAISE NOTICE 'Renewed % subscriptions', renewed_count;

    RETURN QUERY SELECT 'Renewed ' || renewed_count::TEXT || ' subscriptions' as summary;
END;
$$ LANGUAGE plpgsql;

-- Function to generate quarterly reports
CREATE OR REPLACE FUNCTION generate_quarterly_reports()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    report_count INTEGER := 0;
    quarter_start DATE := DATE_TRUNC('quarter', CURRENT_DATE - INTERVAL '3 months');
    quarter_end DATE := quarter_start + INTERVAL '3 months' - INTERVAL '1 day';
BEGIN
    RAISE NOTICE 'Generating quarterly reports for % to %', quarter_start, quarter_end;

    -- Calculate total revenue for the quarter
    -- This is a placeholder - implement actual report generation
    DECLARE
        total_revenue DECIMAL;
    BEGIN
        SELECT COALESCE(SUM(total_amount), 0)
        INTO total_revenue
        FROM invoices
        WHERE invoice_date BETWEEN quarter_start AND quarter_end
        AND status = 'PAID';

        report_count := 1;

        RAISE NOTICE 'Generated quarterly report: Total revenue $%', total_revenue;
    END;

    RETURN QUERY SELECT 'Generated ' || report_count::TEXT || ' quarterly reports' as summary;
END;
$$ LANGUAGE plpgsql;

-- Function to retry failed payments
CREATE OR REPLACE FUNCTION retry_failed_payments()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    retry_count INTEGER := 0;
    success_count INTEGER := 0;
BEGIN
    RAISE NOTICE 'Retrying failed payments from the past week';

    -- Retry failed payments
    -- This is a simplified example - implement actual payment retry logic
    UPDATE invoices
    SET status = 'RETRY_SCHEDULED', updated_at = NOW()
    WHERE status = 'FAILED'
    AND updated_at >= CURRENT_DATE - INTERVAL '7 days';

    GET DIAGNOSTICS retry_count = ROW_COUNT;

    -- Simulate successful retries
    success_count := CEIL(retry_count * 0.7)::INTEGER;

    RAISE NOTICE 'Scheduled % payment retries, % expected to succeed', retry_count, success_count;

    RETURN QUERY SELECT 'Scheduled ' || retry_count::TEXT || ' payment retries (' || success_count::TEXT || ' expected to succeed)' as summary;
END;
$$ LANGUAGE plpgsql;

-- Function to calculate usage-based billing
CREATE OR REPLACE FUNCTION calculate_usage_billing()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    billing_count INTEGER := 0;
    total_cost DECIMAL := 0;
BEGIN
    RAISE NOTICE 'Calculating usage-based billing';

    -- Calculate and apply usage-based charges
    -- This is a placeholder - implement actual usage-based billing calculation
    INSERT INTO usage_charges (
        id, customer_id, billing_period, total_cost, created_at
    )
    SELECT
        gen_random_uuid(),
        customer_id,
        DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month'),
        SUM(quantity * unit_price),
        NOW()
    FROM usage_records
    WHERE usage_date >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')
    AND usage_date < DATE_TRUNC('month', CURRENT_DATE)
    GROUP BY customer_id
    ON CONFLICT DO NOTHING;

    GET DIAGNOSTICS billing_count = ROW_COUNT;

    SELECT COALESCE(SUM(total_cost), 0)
    INTO total_cost
    FROM usage_charges
    WHERE billing_period = DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month');

    RAISE NOTICE 'Calculated usage-based billing for % customers, total cost: $%', billing_count, total_cost;

    RETURN QUERY SELECT 'Calculated usage billing for ' || billing_count::TEXT || ' customers (Total: $' || total_cost::TEXT || ')' as summary;
END;
$$ LANGUAGE plpgsql;

-- Function to clean up subscription status
CREATE OR REPLACE FUNCTION cleanup_subscription_status()
RETURNS TABLE(summary TEXT) AS $$
DECLARE
    expired_count INTEGER := 0;
    cancelled_count INTEGER := 0;
BEGIN
    RAISE NOTICE 'Cleaning up subscription status';

    -- Mark expired subscriptions
    UPDATE subscriptions
    SET status = 'EXPIRED', updated_at = NOW()
    WHERE status = 'ACTIVE'
    AND billing_cycle_end < CURRENT_DATE - INTERVAL '30 days';

    GET DIAGNOSTICS expired_count = ROW_COUNT;

    -- Process cancelled subscriptions
    UPDATE subscriptions
    SET status = 'CANCELLED', updated_at = NOW()
    WHERE status = 'PENDING_CANCELLATION'
    AND updated_at < CURRENT_DATE - INTERVAL '7 days';

    GET DIAGNOSTICS cancelled_count = ROW_COUNT;

    RAISE NOTICE 'Cleaned up: % expired, % cancelled subscriptions', expired_count, cancelled_count;

    RETURN QUERY SELECT 'Cleaned up: ' || expired_count::TEXT || ' expired, ' || cancelled_count::TEXT || ' cancelled' as summary;
END;
$$ LANGUAGE plpgsql;

-- Create helper tables for billing (if they don't exist)
CREATE TABLE IF NOT EXISTS usage_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    usage_date DATE NOT NULL,
    usage_type VARCHAR(50) NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(customer_id, usage_date, usage_type)
);

CREATE TABLE IF NOT EXISTS usage_charges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    billing_period DATE NOT NULL,
    total_cost DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(customer_id, billing_period)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_usage_records_date ON usage_records(usage_date);
CREATE INDEX IF NOT EXISTS idx_usage_records_customer ON usage_records(customer_id);
CREATE INDEX IF NOT EXISTS idx_usage_charges_period ON usage_charges(billing_period);
CREATE INDEX IF NOT EXISTS idx_usage_charges_customer ON usage_charges(customer_id);

-- Grant necessary permissions
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO postgres;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO bss_app;

COMMENT ON FUNCTION aggregate_daily_usage() IS 'Aggregates daily usage data for billing purposes';
COMMENT ON FUNCTION generate_monthly_invoices(DATE) IS 'Generates invoices for the specified month';
COMMENT ON FUNCTION send_invoice_notifications() IS 'Sends notifications for new invoices';
COMMENT ON FUNCTION process_pending_payments() IS 'Processes pending payments';
COMMENT ON FUNCTION check_subscription_renewals() IS 'Checks for upcoming subscription renewals';
COMMENT ON FUNCTION process_subscription_renewals() IS 'Processes subscription renewals';
COMMENT ON FUNCTION generate_quarterly_reports() IS 'Generates quarterly financial reports';
COMMENT ON FUNCTION retry_failed_payments() IS 'Retries failed payments from the past week';
COMMENT ON FUNCTION calculate_usage_billing() IS 'Calculates usage-based billing for the previous month';
COMMENT ON FUNCTION cleanup_subscription_status() IS 'Cleans up expired and cancelled subscriptions';
