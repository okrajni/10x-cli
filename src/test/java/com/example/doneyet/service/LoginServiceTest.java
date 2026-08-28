package com.example.doneyet.service;

import com.example.doneyet.domain.User;
import com.example.doneyet.dto.AuthDto;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.UserRepository;
import com.example.doneyet.repository.UserSessionRepository;
import com.example.doneyet.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginServiceTest {
    private LoginService loginService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private HttpServletRequest httpRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loginService = new LoginService(userRepository, userSessionRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void shouldLoginUserSuccessfully() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("test@example.com", "password123");
        UUID userId = UUID.randomUUID();
        User user = new User("test@example.com", "hashed_password");
        user.setId(userId);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(userId, "test@example.com")).thenReturn("jwt_token");
        when(jwtTokenProvider.getExpiryHours()).thenReturn(24L);
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        AuthDto.AuthResponse response = loginService.login(request, httpRequest);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("jwt_token", response.getToken());
        assertNotNull(response.getExpiresAt());
        verify(userSessionRepository, times(1)).save(any());
    }

    @Test
    void shouldThrowExceptionForNonExistentUser() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("nonexistent@example.com", "password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> loginService.login(request, httpRequest));
        verify(userSessionRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionForWrongPassword() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("test@example.com", "wrongpassword");
        User user = new User("test@example.com", "hashed_password");
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed_password")).thenReturn(false);

        assertThrows(ValidationException.class, () -> loginService.login(request, httpRequest));
        verify(userSessionRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionForMissingEmail() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("", "password123");

        assertThrows(ValidationException.class, () -> loginService.login(request, httpRequest));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void shouldThrowExceptionForMissingPassword() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("test@example.com", "");

        assertThrows(ValidationException.class, () -> loginService.login(request, httpRequest));
        verify(userRepository, never()).findByEmail(any());
    }
}
