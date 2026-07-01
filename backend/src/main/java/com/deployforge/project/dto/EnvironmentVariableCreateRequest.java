package com.deployforge.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EnvironmentVariableCreateRequest {

    @NotBlank(message = "Variable key is required")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "Variable key must contain only alphanumeric characters or underscores and cannot start with a number")
    private String key;

    @NotBlank(message = "Variable value is required")
    private String value;
}
