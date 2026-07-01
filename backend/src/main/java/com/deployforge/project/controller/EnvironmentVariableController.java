package com.deployforge.project.controller;

import com.deployforge.common.dto.ApiResponse;
import com.deployforge.project.dto.EnvironmentVariableCreateRequest;
import com.deployforge.project.dto.EnvironmentVariableDto;
import com.deployforge.project.service.EnvironmentVariableService;
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
@RequestMapping("/api/projects/{projectId}/env-vars")
@Tag(name = "Environment Variables", description = "Endpoints for project environment secrets")
@SecurityRequirement(name = "bearerAuth")
public class EnvironmentVariableController {

    private final EnvironmentVariableService environmentVariableService;

    public EnvironmentVariableController(EnvironmentVariableService environmentVariableService) {
        this.environmentVariableService = environmentVariableService;
    }

    @GetMapping
    @Operation(summary = "List environment variables", description = "Returns a list of masked environment variables for the project")
    public ResponseEntity<ApiResponse<List<EnvironmentVariableDto>>> listVariables(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<EnvironmentVariableDto> vars = environmentVariableService.listVariables(projectId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(vars, "Environment variables loaded"));
    }

    @PostMapping
    @Operation(summary = "Add or update environment variable", description = "Saves or overwrites an environment variable, encrypting its value")
    public ResponseEntity<ApiResponse<EnvironmentVariableDto>> saveVariable(
            @PathVariable UUID projectId,
            @Valid @RequestBody EnvironmentVariableCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        EnvironmentVariableDto var = environmentVariableService.saveOrUpdateVariable(projectId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(var, "Environment variable saved"));
    }

    @DeleteMapping("/{variableId}")
    @Operation(summary = "Delete environment variable", description = "Removes the specified environment variable")
    public ResponseEntity<ApiResponse<Void>> deleteVariable(
            @PathVariable UUID projectId,
            @PathVariable UUID variableId,
            @AuthenticationPrincipal UserPrincipal principal) {
        environmentVariableService.deleteVariable(projectId, variableId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Environment variable deleted"));
    }
}
