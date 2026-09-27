package ru.nifreebie.infoseclab1.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.nifreebie.infoseclab1.dto.CreateLogRequest;
import ru.nifreebie.infoseclab1.dto.LogResponse;
import ru.nifreebie.infoseclab1.service.LogService;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping
    public List<LogResponse> getLogs(Authentication authentication) {
        return logService.findOwnLogs(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LogResponse createLog(
            Authentication authentication,
            @Valid @RequestBody CreateLogRequest request
    ) {
        return logService.create(authentication.getName(), request);
    }
}
