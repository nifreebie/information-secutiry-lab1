package ru.nifreebie.infoseclab1.dto;

import ru.nifreebie.infoseclab1.model.LogLevel;

import java.time.Instant;
import java.util.UUID;

public record LogResponse(UUID id, LogLevel level, String source, String message, Instant eventTime,
                          Instant createdAt) {
}
