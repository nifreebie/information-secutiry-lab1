package ru.nifreebie.infoseclab1.dto;

import lombok.Value;

import java.util.UUID;

@Value
public class RegisterResponse {
    UUID id;
    String username;
}
