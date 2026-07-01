package com.deployforge.deployment.controller;

import com.deployforge.common.dto.ApiResponse;
import com.deployforge.deployment.dto.DeploymentResponse;
import com.deployforge.deployment.entity.ProcessedWebhook;
import com.deployforge.deployment.repository.ProcessedWebhookRepository;
import com.deployforge.deployment.service.DeploymentService;
import com.deployforge.project.entity.Project;
import com.deployforge.project.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks/github")
@Slf4j
public class GithubWebhookController {

    private final ProjectRepository projectRepository;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private final DeploymentService deploymentService;
    private final ObjectMapper objectMapper;

    public GithubWebhookController(ProjectRepository projectRepository,
                                   ProcessedWebhookRepository processedWebhookRepository,
                                   DeploymentService deploymentService) {
        this.projectRepository = projectRepository;
        this.processedWebhookRepository = processedWebhookRepository;
        this.deploymentService = deploymentService;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Object>> handleWebhook(
            @PathVariable UUID projectId,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signatureHeader,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestBody String payload) {

        log.info("Received GitHub Webhook for project: {}. Event: {}, Delivery: {}", projectId, eventType, deliveryId);

        // 1. Fetch Project Details
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            log.warn("Project not found: {}", projectId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Project not found"));
        }

        // 2. Validate HMAC-SHA256 signature if secret is configured on the project
        String secret = project.getWebhookSecret();
        if (secret != null && !secret.trim().isEmpty()) {
            if (signatureHeader == null || !verifySignature(payload, secret, signatureHeader)) {
                log.warn("Invalid HMAC signature received for project {}", projectId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Unauthorized: Invalid signature"));
            }
        }

        // 3. Webhook Replay Protection
        if (deliveryId != null && !deliveryId.trim().isEmpty()) {
            if (processedWebhookRepository.existsById(deliveryId)) {
                log.info("Webhook duplicate delivery ID detected (ignored): {}", deliveryId);
                return ResponseEntity.ok(ApiResponse.success(null, "Webhook already processed (duplicate delivery)"));
            }
        }

        // 4. Accept only "push" events
        if (!"push".equalsIgnoreCase(eventType)) {
            log.info("Ignoring webhook event type: {}", eventType);
            return ResponseEntity.ok(ApiResponse.success(null, "Event ignored: " + eventType));
        }

        // 5. Parse Commit Details from JSON payload
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode headCommit = root.path("head_commit");
            if (headCommit.isMissingNode() || headCommit.isNull()) {
                log.warn("Payload missing 'head_commit' details.");
                return ResponseEntity.ok(ApiResponse.success(null, "Commit details missing"));
            }

            String commitHash = headCommit.path("id").asText();
            String commitMsg = headCommit.path("message").asText();
            String author = headCommit.path("author").path("name").asText("github_webhook");

            // 6. Save processed delivery to database
            if (deliveryId != null && !deliveryId.trim().isEmpty()) {
                ProcessedWebhook processed = ProcessedWebhook.builder()
                        .deliveryId(deliveryId)
                        .project(project)
                        .eventType(eventType)
                        .build();
                processedWebhookRepository.save(processed);
            }

            // 7. Enqueue a new automated deployment
            DeploymentResponse deployment = deploymentService.triggerWebhookDeployment(project, commitHash, commitMsg, author);
            return ResponseEntity.ok(ApiResponse.success(deployment, "Webhook deployment enqueued successfully"));

        } catch (Exception e) {
            log.error("Failed to parse GitHub webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid webhook payload format"));
        }
    }

    private boolean verifySignature(String payload, String secret, String signatureHeader) {
        if (!signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String expectedHex = signatureHeader.substring(7); // Strip "sha256="
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] computedHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Convert computed bytes to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : computedHmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return MessageDigest.isEqual(
                    hexString.toString().getBytes(StandardCharsets.UTF_8),
                    expectedHex.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            log.error("Signature verification throws error", e);
            return false;
        }
    }
}
