package com.deployforge.logging.service;

import com.deployforge.logging.dto.WebSocketLogMessage;
import com.deployforge.logging.entity.DeploymentLog;
import com.deployforge.logging.entity.LogCategory;
import com.deployforge.logging.repository.LogRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LogService {

    private final LogRepository logRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public LogService(LogRepository logRepository, @Lazy SimpMessagingTemplate messagingTemplate) {
        this.logRepository = logRepository;
        this.messagingTemplate = messagingTemplate;
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

        // Publish live websocket message
        try {
            WebSocketLogMessage wsMsg = WebSocketLogMessage.builder()
                    .type("LOG")
                    .category(category != null ? category.name() : "GENERAL")
                    .timestamp(logEntry.getTimestamp().toString())
                    .message(logEntry.getMessage())
                    .build();
            messagingTemplate.convertAndSend("/topic/deployments/" + deploymentId + "/logs", wsMsg);
        } catch (Exception e) {
            // Graceful fallback if STOMP messaging broker is offline or during bootstrap testing
        }
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
