package com.example.doneyet.controller;

import com.example.doneyet.dto.HouseholdDto;
import com.example.doneyet.security.JwtTokenProvider;
import com.example.doneyet.service.HouseholdService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/invitation")
public class InvitationController {
    private final HouseholdService householdService;
    private final JwtTokenProvider jwtTokenProvider;

    public InvitationController(HouseholdService householdService, JwtTokenProvider jwtTokenProvider) {
        this.householdService = householdService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<HouseholdDto.HouseholdResponse> acceptInvitation(
            @PathVariable String token,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        HouseholdDto.HouseholdResponse household = householdService.acceptInvitation(token, userId);
        return ResponseEntity.status(HttpStatus.OK).body(household);
    }
}
