-- Alter deployments table to include build timestamps and framework tracking
ALTER TABLE deployments ADD COLUMN build_started_at TIMESTAMP WITHOUT TIME ZONE NULL;
ALTER TABLE deployments ADD COLUMN build_completed_at TIMESTAMP WITHOUT TIME ZONE NULL;
ALTER TABLE deployments ADD COLUMN framework_detected VARCHAR(50) NULL;

-- Create deployment_runtime table
CREATE TABLE deployment_runtime (
    id UUID PRIMARY KEY,
    deployment_id UUID NOT NULL,
    container_id VARCHAR(255) NULL,
    container_name VARCHAR(255) NULL,
    image_tag VARCHAR(255) NULL,
    host_port INTEGER NULL,
    runtime_status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    image_created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    stopped_at TIMESTAMP WITHOUT TIME ZONE NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_deployment_runtime_deployment FOREIGN KEY (deployment_id) REFERENCES deployments(id) ON DELETE CASCADE
);

CREATE INDEX idx_deployment_runtime_deployment ON deployment_runtime(deployment_id);
