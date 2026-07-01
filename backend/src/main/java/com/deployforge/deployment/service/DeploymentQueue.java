package com.deployforge.deployment.service;

import java.util.UUID;

public interface DeploymentQueue {
    void enqueue(UUID deploymentId);
    UUID dequeue() throws InterruptedException;
}
