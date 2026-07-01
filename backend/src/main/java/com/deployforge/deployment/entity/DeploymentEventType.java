package com.deployforge.deployment.entity;

public enum DeploymentEventType {
    WEBHOOK_RECEIVED,
    QUEUED,
    CLONING,
    BUILD_STARTED,
    BUILD_COMPLETED,
    IMAGE_CREATED,
    CONTAINER_STARTED,
    HEALTHCHECK_PASSED,
    DEPLOYMENT_READY,
    DEPLOYMENT_FAILED
}
