package com.example.doneyet.controller;

import com.example.doneyet.domain.User;
import com.example.doneyet.dto.HouseholdDto;
import com.example.doneyet.service.HouseholdService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/household")
public class HouseholdController {
    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    public ResponseEntity<HouseholdDto.HouseholdResponse> createHousehold(
            @RequestBody HouseholdDto.CreateHouseholdRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        HouseholdDto.HouseholdResponse response = householdService.createHousehold(user.getId(), request.getName());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/{householdId}/invite")
    public ResponseEntity<HouseholdDto.InvitationResponse> sendInvitation(
            @PathVariable UUID householdId,
            @RequestBody HouseholdDto.InvitationRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        HouseholdDto.InvitationResponse response = householdService.sendInvitation(
                householdId,
                request.getInvitedEmail(),
                user.getEmail()
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<List<HouseholdDto.HouseholdResponse>> getUserHouseholds(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        List<HouseholdDto.HouseholdResponse> households = householdService.getUserHouseholds(user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(households);
    }

    @GetMapping("/{householdId}")
    public ResponseEntity<HouseholdDto.HouseholdDetailsResponse> getHouseholdDetails(
            @PathVariable UUID householdId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        HouseholdDto.HouseholdDetailsResponse response = householdService.getHouseholdDetails(householdId, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
