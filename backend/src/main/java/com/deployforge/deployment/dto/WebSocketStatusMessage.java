package com.deployforge.deployment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketStatusMessage {
    private String type; // "STATUS"
    private String status; // e.g. "BUILDING", "RUNNING", "FAILED"
    private String timestamp;
}
