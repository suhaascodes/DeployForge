package com.deployforge.logging.service;

import com.deployforge.logging.entity.DeploymentLog;
import com.deployforge.logging.entity.LogCategory;
import com.deployforge.logging.repository.LogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LogService {

    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public List<DeploymentLog> getLogs(String deploymentId) {
        return logRepository.findByDeploymentIdOrderByTimestampAsc(deploymentId);
    }

    public void addLog(String deploymentId, String message, String level, LogCategory category) {
        String prefix = category != null ? "[" + category.name() + "] " : "";
        DeploymentLog logEntry = DeploymentLog.builder()
                .deploymentId(deploymentId)
                .message(prefix + message)
                .level(level)
                .category(category)
                .timestamp(Instant.now())
                .build();
        logRepository.save(logEntry);
    }

    public void addLog(String deploymentId, String message, String level) {
        addLog(deploymentId, message, level, null);
    }

    public void info(String deploymentId, String message) {
        addLog(deploymentId, message, "INFO", null);
    }

    public void info(String deploymentId, String message, LogCategory category) {
        addLog(deploymentId, message, "INFO", category);
    }

    public void warn(String deploymentId, String message) {
        addLog(deploymentId, message, "WARN", null);
    }

    public void warn(String deploymentId, String message, LogCategory category) {
        addLog(deploymentId, message, "WARN", category);
    }

    public void error(String deploymentId, String message) {
        addLog(deploymentId, message, "ERROR", null);
    }

    public void error(String deploymentId, String message, LogCategory category) {
        addLog(deploymentId, message, "ERROR", category);
    }
}
