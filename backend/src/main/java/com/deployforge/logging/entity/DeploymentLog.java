package com.deployforge.logging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "deployment_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentLog {

    @Id
    private String id;
    private String deploymentId;
    private String message;
    private String level; // INFO, WARN, ERROR
    private LogCategory category;
    private Instant timestamp;
}
