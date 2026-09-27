package ru.nifreebie.infoseclab1.security;

import java.util.UUID;

public record JwtClaims(UUID userId, String username) {
}
