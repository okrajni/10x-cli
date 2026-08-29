package com.example.doneyet.controller;

import com.example.doneyet.domain.User;
import com.example.doneyet.dto.HouseholdDto;
import com.example.doneyet.service.HouseholdService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitation")
public class InvitationController {
    private final HouseholdService householdService;

    public InvitationController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<HouseholdDto.HouseholdResponse> acceptInvitation(
            @PathVariable String token,
            Authentication authentication
    ) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            householdService.acceptInvitationForUser(token, user.getId());
        }
        HouseholdDto.HouseholdResponse response = householdService.acceptInvitation(token);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
