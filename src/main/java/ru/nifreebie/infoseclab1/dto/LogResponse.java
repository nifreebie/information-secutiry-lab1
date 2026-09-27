package ru.nifreebie.infoseclab1.dto;

import lombok.Value;
import ru.nifreebie.infoseclab1.model.LogLevel;

import java.time.Instant;
import java.util.UUID;

@Value
public class LogResponse {
    UUID id;
    LogLevel level;
    String source;
    String message;
    Instant eventTime;
    Instant createdAt;
}
