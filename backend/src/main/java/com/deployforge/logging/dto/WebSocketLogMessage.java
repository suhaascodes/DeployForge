package com.deployforge.logging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketLogMessage {
    private String type; // "LOG"
    private String category; // e.g. "BUILD", "DEPLOYMENT", "RUNTIME"
    private String timestamp;
    private String message;
}
