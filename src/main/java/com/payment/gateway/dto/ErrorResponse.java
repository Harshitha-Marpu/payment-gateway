package com.payment.gateway.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ErrorResponse {

    private String errorCode;
    private String message;
    private List<String> details;
    private LocalDateTime timestamp;
    private String path;

    public static ErrorResponse of(String errorCode, String message, String path) {
        return ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .timestamp(LocalDateTime.now())
            .path(path)
            .build();
    }

    public static ErrorResponse of(String errorCode, String message,
                                   List<String> details, String path) {
        return ErrorResponse.builder()
            .errorCode(errorCode)
            .message(message)
            .details(details)
            .timestamp(LocalDateTime.now())
            .path(path)
            .build();
    }
}