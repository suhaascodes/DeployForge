package com.deployforge.project.dto;

import com.deployforge.project.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private String repositoryUrl;
    private UUID ownerId;
    private LocalDateTime createdAt;
    private String webhookSecret;

    public static ProjectResponse fromEntity(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .repositoryUrl(project.getRepositoryUrl())
                .ownerId(project.getOwner().getId())
                .createdAt(project.getCreatedAt())
                .webhookSecret(project.getWebhookSecret())
                .build();
    }
}
