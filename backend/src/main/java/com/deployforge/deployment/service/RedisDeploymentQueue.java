package com.deployforge.deployment.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisDeploymentQueue implements DeploymentQueue {

    private final StringRedisTemplate redisTemplate;
    private static final String QUEUE_KEY = "deployments:queue";

    public RedisDeploymentQueue(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void enqueue(UUID deploymentId) {
        redisTemplate.opsForList().leftPush(QUEUE_KEY, deploymentId.toString());
    }

    @Override
    public UUID dequeue() throws InterruptedException {
        // Perform a blocking pop or a simple pop with a fallback.
        // Let's use rightPop with a timeout so it blocks until an item is available,
        // which avoids high-CPU spinning in our worker.
        String val = redisTemplate.opsForList().rightPop(QUEUE_KEY, 5, TimeUnit.SECONDS);
        if (val != null) {
            return UUID.fromString(val);
        }
        return null;
    }
}
