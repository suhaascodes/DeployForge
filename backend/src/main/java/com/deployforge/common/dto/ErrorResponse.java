package com.deployforge.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private boolean success;
    private String error;
    private String message;
    private Map<String, String> details;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ErrorResponse of(String error, String message) {
        return ErrorResponse.builder()
                .success(false)
                .error(error)
                .message(message)
                .build();
    }

    public static ErrorResponse of(String error, String message, Map<String, String> details) {
        return ErrorResponse.builder()
                .success(false)
                .error(error)
                .message(message)
                .details(details)
                .build();
    }
}
