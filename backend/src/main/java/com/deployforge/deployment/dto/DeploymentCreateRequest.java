package com.deployforge.deployment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DeploymentCreateRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;
}
