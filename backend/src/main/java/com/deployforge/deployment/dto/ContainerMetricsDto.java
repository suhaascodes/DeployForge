package com.deployforge.deployment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerMetricsDto {
    private Double cpuUsagePercent;
    private Double memoryUsageMb;
    private Long uptimeSeconds;
}
