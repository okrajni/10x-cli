package com.example.doneyet.service;

import com.example.doneyet.domain.User;
import com.example.doneyet.domain.UserSession;
import com.example.doneyet.dto.AuthDto;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.UserRepository;
import com.example.doneyet.repository.UserSessionRepository;
import com.example.doneyet.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class LoginService {
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginService(UserRepository userRepository, UserSessionRepository userSessionRepository,
                        PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request, HttpServletRequest httpRequest) {
        validateLoginRequest(request);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        String userAgent = extractUserAgent(httpRequest);
        String ipAddress = extractIpAddress(httpRequest);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setDeviceName(userAgent);
        session.setIpAddress(ipAddress);
        userSessionRepository.save(session);

        String expiresAt = Instant.now().plus(jwtTokenProvider.getExpiryHours(), ChronoUnit.HOURS).toString();
        return new AuthDto.AuthResponse(user.getId(), user.getEmail(), token, expiresAt);
    }

    private void validateLoginRequest(AuthDto.LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ValidationException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ValidationException("Password is required");
        }
    }

    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null && !userAgent.isBlank() ? userAgent : "Unknown";
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
