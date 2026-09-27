package ru.nifreebie.infoseclab1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.nifreebie.infoseclab1.model.LogLevel;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class CreateLogRequest {

    @NotNull
    private LogLevel level;

    @NotBlank
    @Size(max = 100)
    private String source;

    @NotBlank
    @Size(max = 2000)
    private String message;

    @NotNull
    @PastOrPresent
    private Instant eventTime;
}
