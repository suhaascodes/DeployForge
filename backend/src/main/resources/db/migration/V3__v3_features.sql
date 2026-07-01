-- Alter projects table to include webhook secret
ALTER TABLE projects ADD COLUMN webhook_secret VARCHAR(255) NULL;

-- Alter deployments table to include trigger types, webhook push meta, performance metrics, cancellation, and error summary trace
ALTER TABLE deployments ADD COLUMN trigger_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE deployments ADD COLUMN github_commit_hash VARCHAR(40) NULL;
ALTER TABLE deployments ADD COLUMN github_commit_message TEXT NULL;
ALTER TABLE deployments ADD COLUMN github_author VARCHAR(255) NULL;
ALTER TABLE deployments ADD COLUMN github_push_timestamp TIMESTAMP WITHOUT TIME ZONE NULL;
ALTER TABLE deployments ADD COLUMN deployment_duration_ms BIGINT NULL;
ALTER TABLE deployments ADD COLUMN image_size_mb DOUBLE PRECISION NULL;
ALTER TABLE deployments ADD COLUMN source_deployment_id UUID NULL;
ALTER TABLE deployments ADD COLUMN failure_stage VARCHAR(50) NULL;
ALTER TABLE deployments ADD COLUMN failure_summary TEXT NULL;

-- Set foreign key for source_deployment_id referencing deployments table
ALTER TABLE deployments ADD CONSTRAINT fk_deployments_source_deployment FOREIGN KEY (source_deployment_id) REFERENCES deployments(id) ON DELETE SET NULL;

-- Create project_environment_variables table
CREATE TABLE project_environment_variables (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    variable_key VARCHAR(255) NOT NULL,
    encrypted_value TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_project_environment_variables_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT unique_project_key UNIQUE (project_id, variable_key)
);

CREATE INDEX idx_project_environment_variables_project ON project_environment_variables(project_id);

-- Create deployment_events table
CREATE TABLE deployment_events (
    id UUID PRIMARY KEY,
    deployment_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_deployment_events_deployment FOREIGN KEY (deployment_id) REFERENCES deployments(id) ON DELETE CASCADE
);

CREATE INDEX idx_deployment_events_deployment ON deployment_events(deployment_id);

-- Create processed_webhooks table for GitHub push replay protection & logs
CREATE TABLE processed_webhooks (
    delivery_id VARCHAR(255) PRIMARY KEY,
    project_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_processed_webhooks_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_processed_webhooks_project ON processed_webhooks(project_id);
