package com.deployforge.project.dto;

import com.deployforge.project.entity.ProjectEnvironmentVariable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentVariableDto {
    private UUID id;
    private String key;
    private String value; // Always masked
    private String createdAt;
    private String updatedAt;

    public static EnvironmentVariableDto fromEntity(ProjectEnvironmentVariable entity) {
        return EnvironmentVariableDto.builder()
                .id(entity.getId())
                .key(entity.getKey())
                .value("••••••••") // Mirroring production platform standards (Railway, Vercel, Render)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : java.time.LocalDateTime.now().toString())
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : java.time.LocalDateTime.now().toString())
                .build();
    }
}
