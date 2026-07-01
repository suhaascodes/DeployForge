package com.deployforge.deployment.service;

import java.util.UUID;

public interface DeploymentExecutionEngine {
    void execute(UUID deploymentId);
}
