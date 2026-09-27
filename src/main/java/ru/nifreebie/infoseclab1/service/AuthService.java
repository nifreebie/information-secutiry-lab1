package ru.nifreebie.infoseclab1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nifreebie.infoseclab1.dto.LoginRequest;
import ru.nifreebie.infoseclab1.dto.LoginResponse;
import ru.nifreebie.infoseclab1.dto.RegisterRequest;
import ru.nifreebie.infoseclab1.dto.RegisterResponse;
import ru.nifreebie.infoseclab1.model.User;
import ru.nifreebie.infoseclab1.repository.UserRepository;
import ru.nifreebie.infoseclab1.security.JwtService;
import ru.nifreebie.infoseclab1.utils.ConflictException;
import ru.nifreebie.infoseclab1.utils.InvalidCredentialsException;
import ru.nifreebie.infoseclab1.utils.RequestValidationException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already registered");
        }
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new RequestValidationException("Password is too long after UTF-8 encoding");
        }

        User user = new User(username, passwordEncoder.encode(request.getPassword()));
        try {
            User saved = userRepository.saveAndFlush(user);
            return new RegisterResponse(saved.getId(), saved.getUsername());
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Username is already registered");
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);
        String token = jwtService.createToken(user);
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }

    private String normalizeUsername(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
