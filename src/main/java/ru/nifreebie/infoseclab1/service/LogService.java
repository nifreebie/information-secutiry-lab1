package ru.nifreebie.infoseclab1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import ru.nifreebie.infoseclab1.dto.CreateLogRequest;
import ru.nifreebie.infoseclab1.dto.LogResponse;
import ru.nifreebie.infoseclab1.model.User;
import ru.nifreebie.infoseclab1.model.LogEntry;
import ru.nifreebie.infoseclab1.repository.LogRepository;
import ru.nifreebie.infoseclab1.repository.UserRepository;
import ru.nifreebie.infoseclab1.utils.ResourceNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;
    private final UserRepository userRepository;

    @Transactional
    public LogResponse create(String username, CreateLogRequest request) {
        User owner = findUser(username);
        LogEntry entry = new LogEntry(
                request.getLevel(),
                request.getSource(),
                request.getMessage(),
                request.getEventTime(),
                owner
        );
        return toResponse(logRepository.save(entry));
    }

    @Transactional(readOnly = true)
    public List<LogResponse> findOwnLogs(String username) {
        User owner = findUser(username);
        return logRepository.findAllByOwnerIdOrderByEventTimeDesc(owner.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }

    private LogResponse toResponse(LogEntry entry) {
        return new LogResponse(
                entry.getId(),
                entry.getLevel(),
                HtmlUtils.htmlEscape(entry.getSource()),
                HtmlUtils.htmlEscape(entry.getMessage()),
                entry.getEventTime(),
                entry.getCreatedAt()
        );
    }
}
