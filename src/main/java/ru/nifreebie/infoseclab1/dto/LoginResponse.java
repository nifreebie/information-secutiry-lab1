package ru.nifreebie.infoseclab1.dto;

import lombok.Value;

@Value
public class LoginResponse {
    String token;
    String tokenType;
    long expiresIn;
}
