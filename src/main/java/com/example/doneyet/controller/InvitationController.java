package com.example.doneyet.controller;

import com.example.doneyet.dto.HouseholdDto;
import com.example.doneyet.service.HouseholdService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @PathVariable String token
    ) {
        HouseholdDto.HouseholdResponse household = householdService.acceptInvitation(token);
        return ResponseEntity.status(HttpStatus.OK).body(household);
    }
}
