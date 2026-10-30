package com.example.hotel.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorDto(Instant timestamp, int status, String error, String message,
                       String path, String traceId, Map<String, String> validationErrors) {
}
