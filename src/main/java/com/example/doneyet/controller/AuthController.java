package com.example.doneyet.controller;

import com.example.doneyet.dto.AuthDto;
import com.example.doneyet.repository.UserSessionRepository;
import com.example.doneyet.security.RateLimiter;
import com.example.doneyet.service.LoginService;
import com.example.doneyet.service.RegistrationService;
import com.example.doneyet.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegistrationService registrationService;
    private final LoginService loginService;
    private final RateLimiter rateLimiter;
    private final UserSessionRepository userSessionRepository;

    public AuthController(RegistrationService registrationService, LoginService loginService,
                          RateLimiter rateLimiter, UserSessionRepository userSessionRepository) {
        this.registrationService = registrationService;
        this.loginService = loginService;
        this.rateLimiter = rateLimiter;
        this.userSessionRepository = userSessionRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@RequestBody AuthDto.RegisterRequest request, HttpServletRequest httpRequest) {
        String ipAddress = extractIpAddress(httpRequest);

        if (!rateLimiter.isRegistrationAllowed(ipAddress)) {
            throw new RateLimitException("Too many registration attempts. Try again later.", 3600);
        }

        rateLimiter.recordRegistrationAttempt(ipAddress);
        AuthDto.AuthResponse response = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@RequestBody AuthDto.LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = extractIpAddress(httpRequest);

        if (!rateLimiter.isLoginAllowed(request.getEmail(), ipAddress)) {
            long retryAfter = rateLimiter.getLoginRetryAfterSeconds(request.getEmail(), ipAddress);
            throw new RateLimitException("Too many login attempts. Try again in " + retryAfter + " seconds.", retryAfter);
        }

        try {
            AuthDto.AuthResponse response = loginService.login(request, httpRequest);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            rateLimiter.recordLoginAttempt(request.getEmail(), ipAddress);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.example.doneyet.domain.User) {
            com.example.doneyet.domain.User user = (com.example.doneyet.domain.User) authentication.getPrincipal();
            userSessionRepository.deleteByUserId(user.getId());
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
