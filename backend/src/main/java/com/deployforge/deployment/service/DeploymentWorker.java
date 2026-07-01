package com.deployforge.deployment.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class DeploymentWorker {

    private final DeploymentQueue deploymentQueue;
    private final DeploymentExecutionEngine deploymentExecutionEngine;
    private final ExecutorService executorService;
    private volatile boolean running = true;

    public DeploymentWorker(DeploymentQueue deploymentQueue,
                            DeploymentExecutionEngine deploymentExecutionEngine) {
        this.deploymentQueue = deploymentQueue;
        this.deploymentExecutionEngine = deploymentExecutionEngine;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @PostConstruct
    public void start() {
        executorService.submit(this::runWorker);
        log.info("Deployment background worker started.");
    }

    @PreDestroy
    public void stop() {
        running = false;
        executorService.shutdownNow();
        log.info("Deployment background worker stopped.");
    }

    private void runWorker() {
        while (running) {
            try {
                UUID deploymentId = deploymentQueue.dequeue();
                if (deploymentId != null) {
                    deploymentExecutionEngine.execute(deploymentId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Worker thread interrupted, exiting.");
                break;
            } catch (Exception e) {
                log.error("Error in deployment worker loop", e);
            }
        }
    }
}
