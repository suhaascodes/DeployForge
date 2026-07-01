package com.deployforge.deployment.dto;

import com.deployforge.deployment.entity.DeploymentEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentEventDto {
    private String eventType;
    private String message;
    private String createdAt;

    public static DeploymentEventDto fromEntity(DeploymentEvent entity) {
        return DeploymentEventDto.builder()
                .eventType(entity.getEventType().name())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : java.time.LocalDateTime.now().toString())
                .build();
    }
}
