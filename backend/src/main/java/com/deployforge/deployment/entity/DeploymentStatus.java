package com.deployforge.deployment.entity;

public enum DeploymentStatus {
    QUEUED,
    CLONING,
    BUILDING,
    CREATING_IMAGE,
    STARTING_CONTAINER,
    RUNNING,
    DEPLOYING, // legacy V1
    FAILED, // legacy V1
    BUILD_FAILED,
    RUNTIME_FAILED
}
