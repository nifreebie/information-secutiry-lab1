package ru.nifreebie.infoseclab1.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nifreebie.infoseclab1.model.LogEntry;

import java.util.List;
import java.util.UUID;

public interface LogRepository extends JpaRepository<LogEntry, UUID> {

    List<LogEntry> findAllByOwnerIdOrderByEventTimeDesc(UUID ownerId);
}
