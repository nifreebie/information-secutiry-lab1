package ru.nifreebie.infoseclab1.dto;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       Map<String, String> violations) {

    public ApiError(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> violations
    ) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.violations = Map.copyOf(violations);
    }

    @Override
    public Map<String, String> violations() {
        return Map.copyOf(violations);
    }
}
