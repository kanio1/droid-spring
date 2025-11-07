-- V1041__create_workflow_engine_tables.sql
-- Workflow Engine Database Schema

-- Workflow definitions (templates)
CREATE TABLE IF NOT EXISTS workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    version INT NOT NULL DEFAULT 1,
    trigger_event VARCHAR(50) NOT NULL, -- e.g., "customer.created", "payment.failed"
    is_active BOOLEAN NOT NULL DEFAULT true,
    workflow_definition JSONB NOT NULL, -- Workflow steps, conditions, actions
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- Workflow executions (instances)
CREATE TABLE IF NOT EXISTS workflow_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL, -- e.g., "customer", "invoice", "payment"
    entity_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    current_step INT NOT NULL DEFAULT 0,
    total_steps INT NOT NULL,
    context JSONB DEFAULT '{}', -- Execution context (variables, data)
    result JSONB DEFAULT '{}', -- Execution result
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(50)
);

-- Workflow step executions (individual step tracking)
CREATE TABLE IF NOT EXISTS workflow_step_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_execution_id UUID NOT NULL REFERENCES workflow_executions(id) ON DELETE CASCADE,
    step_number INT NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    step_type VARCHAR(50) NOT NULL, -- e.g., "action", "condition", "delay", "notification"
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RUNNING, COMPLETED, FAILED, SKIPPED
    input_data JSONB DEFAULT '{}',
    output_data JSONB DEFAULT '{}',
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(workflow_execution_id, step_number)
);

-- Workflow history (audit log)
CREATE TABLE IF NOT EXISTS workflow_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_execution_id UUID NOT NULL REFERENCES workflow_executions(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL, -- e.g., "started", "step_completed", "completed", "failed"
    event_data JSONB NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_workflows_trigger_event ON workflows(trigger_event);
CREATE INDEX IF NOT EXISTS idx_workflows_active ON workflows(is_active);

CREATE INDEX IF NOT EXISTS idx_workflow_executions_workflow_id ON workflow_executions(workflow_id);
CREATE INDEX IF NOT EXISTS idx_workflow_executions_entity ON workflow_executions(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_workflow_executions_status ON workflow_executions(status);
CREATE INDEX IF NOT EXISTS idx_workflow_executions_started_at ON workflow_executions(started_at);

CREATE INDEX IF NOT EXISTS idx_workflow_step_executions_execution_id ON workflow_step_executions(workflow_execution_id);
CREATE INDEX IF NOT EXISTS idx_workflow_step_executions_status ON workflow_step_executions(status);

CREATE INDEX IF NOT EXISTS idx_workflow_history_execution_id ON workflow_history(workflow_execution_id);
CREATE INDEX IF NOT EXISTS idx_workflow_history_timestamp ON workflow_history(event_timestamp);

-- Create default workflows
INSERT INTO workflows (name, description, trigger_event, workflow_definition) VALUES
(
    'customer_onboarding',
    'Customer onboarding workflow - sends welcome email, provisions default services, schedules check-in',
    'customer.created',
    '{
        "steps": [
            {
                "number": 1,
                "name": "send_welcome_email",
                "type": "action",
                "action": "send_email",
                "config": {
                    "template": "welcome_email",
                    "to": "{{customer.email}}",
                    "subject": "Welcome to BSS!"
                },
                "delay_seconds": 0
            },
            {
                "number": 2,
                "name": "provision_default_services",
                "type": "action",
                "action": "provision_service",
                "config": {
                    "services": ["basic_support", "customer_portal"]
                },
                "delay_seconds": 0
            },
            {
                "number": 3,
                "name": "schedule_30_day_checkin",
                "type": "delay",
                "config": {
                    "days": 30
                }
            },
            {
                "number": 4,
                "name": "create_customer_success_ticket",
                "type": "action",
                "action": "create_ticket",
                "config": {
                    "queue": "customer_success",
                    "priority": "normal",
                    "subject": "30-day check-in for {{customer.id}}",
                    "description": "Please follow up with the customer after 30 days"
                }
            }
        ]
    }'::jsonb
),
(
    'payment_failed_recovery',
    'Payment failed recovery workflow - sends alerts, retries payment, escalates if needed',
    'payment.failed',
    '{
        "steps": [
            {
                "number": 1,
                "name": "send_payment_alert",
                "type": "action",
                "action": "send_email",
                "config": {
                    "template": "payment_failed",
                    "to": "{{customer.email}}",
                    "subject": "Payment Failed - Action Required"
                },
                "delay_seconds": 0
            },
            {
                "number": 2,
                "name": "retry_payment_in_3_days",
                "type": "delay",
                "config": {
                    "days": 3
                }
            },
            {
                "number": 3,
                "name": "retry_payment",
                "type": "action",
                "action": "retry_payment",
                "config": {
                    "max_retries": 3
                }
            },
            {
                "number": 4,
                "name": "check_payment_status",
                "type": "condition",
                "config": {
                    "condition": "{{payment.status}} == \"COMPLETED\""
                }
            },
            {
                "number": 5,
                "name": "suspend_services",
                "type": "action",
                "action": "suspend_services",
                "config": {
                    "delay_days": 7
                },
                "condition": "payment_failed"
            },
            {
                "number": 6,
                "name": "escalate_to_human",
                "type": "action",
                "action": "create_ticket",
                "config": {
                    "queue": "billing",
                    "priority": "high",
                    "subject": "Payment failed after retries - {{customer.id}}",
                    "description": "Payment has failed after automatic retry attempts. Manual intervention required."
                },
                "condition": "payment_failed"
            }
        ]
    }'::jsonb
)
ON CONFLICT (name) DO NOTHING;

-- Create function to update timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at
DROP TRIGGER IF EXISTS update_workflows_updated_at ON workflows;
CREATE TRIGGER update_workflows_updated_at
    BEFORE UPDATE ON workflows
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_workflow_executions_updated_at ON workflow_executions;
CREATE TRIGGER update_workflow_executions_updated_at
    BEFORE UPDATE ON workflow_executions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create function to get active workflow by trigger
CREATE OR REPLACE FUNCTION get_workflow_by_trigger(trigger_event_name TEXT)
RETURNS UUID AS $$
DECLARE
    workflow_uuid UUID;
BEGIN
    SELECT id INTO workflow_uuid
    FROM workflows
    WHERE trigger_event = trigger_event_name
      AND is_active = true
    ORDER BY version DESC
    LIMIT 1;

    RETURN workflow_uuid;
END;
$$ LANGUAGE plpgsql;

-- Create function to create workflow execution
CREATE OR REPLACE FUNCTION create_workflow_execution(
    p_workflow_id UUID,
    p_entity_type TEXT,
    p_entity_id UUID,
    p_context JSONB DEFAULT '{}'
)
RETURNS UUID AS $$
DECLARE
    execution_id UUID;
    total_steps INT;
BEGIN
    -- Get total steps from workflow definition
    SELECT jsonb_array_length(workflow_definition->'steps') INTO total_steps
    FROM workflows
    WHERE id = p_workflow_id;

    -- Create workflow execution
    INSERT INTO workflow_executions (
        workflow_id,
        entity_type,
        entity_id,
        total_steps,
        context,
        status,
        started_at
    ) VALUES (
        p_workflow_id,
        p_entity_type,
        p_entity_id,
        total_steps,
        p_context,
        'RUNNING',
        NOW()
    ) RETURNING id INTO execution_id;

    -- Create step executions
    INSERT INTO workflow_step_executions (workflow_execution_id, step_number, step_name, step_type, status)
    SELECT
        execution_id,
        (value->>'number')::INT,
        value->>'name',
        value->>'type',
        'PENDING'
    FROM workflows,
         jsonb_array_elements(workflow_definition->'steps') AS value
    WHERE id = p_workflow_id;

    -- Log history
    INSERT INTO workflow_history (workflow_execution_id, event_type, event_data)
    VALUES (execution_id, 'started', jsonb_build_object('workflow_id', p_workflow_id, 'entity_id', p_entity_id));

    RETURN execution_id;
END;
$$ LANGUAGE plpgsql;

-- Create function to update workflow step
CREATE OR REPLACE FUNCTION update_workflow_step(
    p_execution_id UUID,
    p_step_number INT,
    p_status TEXT,
    p_output_data JSONB DEFAULT '{}',
    p_error_message TEXT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    step_count INT;
    all_completed BOOLEAN;
BEGIN
    -- Update step execution
    UPDATE workflow_step_executions
    SET
        status = p_status,
        output_data = COALESCE(p_output_data, output_data),
        error_message = COALESCE(p_error_message, error_message),
        completed_at = CASE WHEN p_status IN ('COMPLETED', 'FAILED', 'SKIPPED') THEN NOW() ELSE completed_at END
    WHERE workflow_execution_id = p_execution_id
      AND step_number = p_step_number;

    -- Check if all steps are completed
    SELECT COUNT(*) INTO step_count
    FROM workflow_step_executions
    WHERE workflow_execution_id = p_execution_id
      AND status NOT IN ('COMPLETED', 'SKIPPED');

    all_completed := (step_count = 0);

    -- If all steps completed, mark execution as completed
    IF all_completed THEN
        UPDATE workflow_executions
        SET
            status = 'COMPLETED',
            completed_at = NOW(),
            result = p_output_data
        WHERE id = p_execution_id;

        -- Log completion
        INSERT INTO workflow_history (workflow_execution_id, event_type, event_data)
        VALUES (p_execution_id, 'completed', jsonb_build_object('completed_at', NOW()));
    END IF;

    RETURN all_completed;
END;
$$ LANGUAGE plpgsql;
