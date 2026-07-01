package com.deployforge.deployment.controller;

import com.deployforge.common.dto.ApiResponse;
import com.deployforge.deployment.dto.DeploymentCreateRequest;
import com.deployforge.deployment.dto.DeploymentResponse;
import com.deployforge.deployment.service.DeploymentService;
import com.deployforge.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/deployments")
@Tag(name = "Deployments", description = "Endpoints for triggering and monitoring deployments")
@SecurityRequirement(name = "bearerAuth")
public class DeploymentController {

    private final DeploymentService deploymentService;
    private final com.deployforge.deployment.service.MetricsCollectionService metricsCollectionService;
    private final com.deployforge.deployment.repository.DeploymentRuntimeRepository deploymentRuntimeRepository;

    public DeploymentController(DeploymentService deploymentService,
                                com.deployforge.deployment.service.MetricsCollectionService metricsCollectionService,
                                com.deployforge.deployment.repository.DeploymentRuntimeRepository deploymentRuntimeRepository) {
        this.deploymentService = deploymentService;
        this.metricsCollectionService = metricsCollectionService;
        this.deploymentRuntimeRepository = deploymentRuntimeRepository;
    }

    @PostMapping
    @Operation(summary = "Create deployment", description = "Queues a new deployment run for the specified project")
    public ResponseEntity<ApiResponse<DeploymentResponse>> createDeployment(
            @Valid @RequestBody DeploymentCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeploymentResponse response = deploymentService.createDeployment(request, principal.getId());
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Deployment queued successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deployment status", description = "Retrieves current execution status and metadata for a deployment")
    public ResponseEntity<ApiResponse<DeploymentResponse>> getDeployment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeploymentResponse response = deploymentService.getDeployment(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get project deployments history", description = "Lists all historical deployments for a given project")
    public ResponseEntity<ApiResponse<List<DeploymentResponse>>> listDeploymentsForProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<DeploymentResponse> response = deploymentService.listDeploymentsForProject(projectId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent deployments across all projects", description = "Lists recent deployments for the dashboard")
    public ResponseEntity<ApiResponse<List<DeploymentResponse>>> listRecentDeployments(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<DeploymentResponse> response = deploymentService.listRecentDeployments(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/redeploy")
    @Operation(summary = "Redeploy commit", description = "Queues a manual redeployment of the commit details in a specific deployment run")
    public ResponseEntity<ApiResponse<DeploymentResponse>> redeploy(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeploymentResponse response = deploymentService.redeploy(id, principal.getId());
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Redeployment queued successfully"));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get deployment events timeline", description = "Retrieves chronological timeline events for a deployment run")
    public ResponseEntity<ApiResponse<List<com.deployforge.deployment.dto.DeploymentEventDto>>> getDeploymentEvents(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<com.deployforge.deployment.dto.DeploymentEventDto> response = deploymentService.getDeploymentEvents(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/metrics")
    @Operation(summary = "Get container live metrics", description = "Query dynamic CPU, Memory, and Uptime on-demand from the Docker daemon")
    public ResponseEntity<ApiResponse<com.deployforge.deployment.dto.ContainerMetricsDto>> getMetrics(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        DeploymentResponse dep = deploymentService.getDeployment(id, principal.getId());
        if (dep.getContainerName() == null || dep.getContainerName().trim().isEmpty() || !"RUNNING".equals(dep.getStatus().name())) {
            return ResponseEntity.ok(ApiResponse.success(
                    com.deployforge.deployment.dto.ContainerMetricsDto.builder()
                            .cpuUsagePercent(0.0)
                            .memoryUsageMb(0.0)
                            .uptimeSeconds(0L)
                            .build()
            ));
        }
        com.deployforge.deployment.dto.ContainerMetricsDto metrics = metricsCollectionService.getRuntimeMetrics(dep.getContainerName());
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}
