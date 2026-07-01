package com.deployforge.logging.controller;

import com.deployforge.common.dto.ApiResponse;
import com.deployforge.logging.entity.DeploymentLog;
import com.deployforge.logging.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Logs", description = "Endpoints for viewing deployment build and runtime logs")
@SecurityRequirement(name = "bearerAuth")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping("/{deploymentId}")
    @Operation(summary = "Get deployment logs", description = "Retrieves all chronological log statements for a given deployment from MongoDB")
    public ResponseEntity<ApiResponse<List<DeploymentLog>>> getLogs(@PathVariable String deploymentId) {
        List<DeploymentLog> logs = logService.getLogs(deploymentId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
