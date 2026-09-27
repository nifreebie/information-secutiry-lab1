package ru.nifreebie.infoseclab1.security;

public class JwtAuthenticationException extends RuntimeException {

    public JwtAuthenticationException() {
        super("Invalid or expired JWT");
    }
}
