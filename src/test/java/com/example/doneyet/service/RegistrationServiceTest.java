package com.example.doneyet.service;

import com.example.doneyet.domain.User;
import com.example.doneyet.dto.AuthDto;
import com.example.doneyet.exception.ConflictException;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.UserRepository;
import com.example.doneyet.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {
    private RegistrationService registrationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        registrationService = new RegistrationService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("test@example.com", "password123");
        UUID userId = UUID.randomUUID();
        User savedUser = new User(request.getEmail(), "hashed_password");
        savedUser.setId(userId);

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(userId, "test@example.com")).thenReturn("jwt_token");
        when(jwtTokenProvider.getExpiryHours()).thenReturn(24L);

        AuthDto.AuthResponse response = registrationService.register(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt_token", response.getToken());
        assertNotNull(response.getExpiresAt());
    }

    @Test
    void shouldThrowExceptionForInvalidEmail() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("invalid-email", "password123");

        assertThrows(ValidationException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionForMissingEmail() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("", "password123");

        assertThrows(ValidationException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionForShortPassword() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("test@example.com", "short");

        assertThrows(ValidationException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionForMissingPassword() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("test@example.com", "");

        assertThrows(ValidationException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionForDuplicateEmail() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest("existing@example.com", "password123");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> registrationService.register(request));
        verify(userRepository, never()).save(any());
    }
}
