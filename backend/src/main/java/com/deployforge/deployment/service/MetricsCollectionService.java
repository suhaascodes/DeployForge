package com.deployforge.deployment.service;

import com.deployforge.deployment.dto.ContainerMetricsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MetricsCollectionService {

    private final Map<String, CachedMetrics> cache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 5000; // 5-second cache window

    private static class CachedMetrics {
        final ContainerMetricsDto metrics;
        final long timestamp;

        CachedMetrics(ContainerMetricsDto metrics) {
            this.metrics = metrics;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }

    public ContainerMetricsDto getRuntimeMetrics(String containerName) {
        if (containerName == null || containerName.trim().isEmpty()) {
            return ContainerMetricsDto.builder().cpuUsagePercent(0.0).memoryUsageMb(0.0).uptimeSeconds(0L).build();
        }

        CachedMetrics cached = cache.get(containerName);
        if (cached != null && !cached.isExpired()) {
            return cached.metrics;
        }

        ContainerMetricsDto metrics = fetchLiveMetrics(containerName);
        cache.put(containerName, new CachedMetrics(metrics));
        return metrics;
    }

    private ContainerMetricsDto fetchLiveMetrics(String containerName) {
        Double cpu = 0.0;
        Double memory = 0.0;
        Long uptime = 0L;

        // 1. Fetch CPU and Memory stats from docker stats
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "stats", "--no-stream", "--format", "{{.CPUPerc}},{{.MemUsage}}", containerName);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        // CPUPerc e.g. "0.15%" -> 0.15
                        String cpuStr = parts[0].trim().replace("%", "");
                        cpu = Double.parseDouble(cpuStr);

                        // MemUsage e.g. "12.5MiB / 1.95GiB" or "500KiB / 1.95GiB"
                        String memStr = parts[1].split("/")[0].trim();
                        memory = parseMemoryToMb(memStr);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve docker stats for container {}", containerName, e);
        }

        // 2. Fetch startedAt timestamp to calculate uptime seconds
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "inspect", "--format", "{{.State.StartedAt}}", containerName);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    // Docker started at is standard ISO format like "2026-07-01T12:00:39.123456789Z"
                    Instant startedAt = Instant.parse(line.trim());
                    uptime = Math.max(0, Duration.between(startedAt, Instant.now()).toSeconds());
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve startedAt timestamp for container {}", containerName, e);
        }

        return ContainerMetricsDto.builder()
                .cpuUsagePercent(cpu)
                .memoryUsageMb(memory)
                .uptimeSeconds(uptime)
                .build();
    }

    private Double parseMemoryToMb(String memStr) {
        try {
            String clean = memStr.replaceAll("[^0-9.]", "").trim();
            double val = Double.parseDouble(clean);
            String unit = memStr.replaceAll("[0-9.]", "").trim().toLowerCase();

            if (unit.contains("gib")) {
                return val * 1024;
            } else if (unit.contains("kib")) {
                return val / 1024;
            } else if (unit.contains("mib")) {
                return val;
            } else {
                return val / (1024 * 1024); // Fallback assume bytes
            }
        } catch (Exception e) {
            return 0.0;
        }
    }

    public Double getImageSizeMb(String imageName, String imageTag) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "image", "inspect", "--format", "{{.Size}}", imageName + ":" + imageTag);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    long bytes = Long.parseLong(line.trim());
                    return (double) bytes / (1024 * 1024);
                }
            }
        } catch (Exception e) {
            log.error("Failed to inspect docker image size", e);
        }
        return 0.0;
    }
}
