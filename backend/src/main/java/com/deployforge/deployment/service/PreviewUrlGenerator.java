package com.deployforge.deployment.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PreviewUrlGenerator {

    public String generatePreviewUrl(UUID deploymentId, int hostPort) {
        // Local preview format: http://localhost:{hostPort}
        // Future production format could be: "https://deploy-" + deploymentId.toString().substring(0, 8) + ".deployforge.dev"
        return "http://localhost:" + hostPort;
    }
}
