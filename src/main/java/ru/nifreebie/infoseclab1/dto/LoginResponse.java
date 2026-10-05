package ru.nifreebie.infoseclab1.dto;

public record LoginResponse(String token, String tokenType, long expiresIn) {
}
