package com.example.doneyet.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class HouseholdDto {
    public static class CreateHouseholdRequest {
        private String name;

        public CreateHouseholdRequest() {
        }

        public CreateHouseholdRequest(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class InvitationRequest {
        private String invitedEmail;

        public InvitationRequest() {
        }

        public InvitationRequest(String invitedEmail) {
            this.invitedEmail = invitedEmail;
        }

        public String getInvitedEmail() {
            return invitedEmail;
        }

        public void setInvitedEmail(String invitedEmail) {
            this.invitedEmail = invitedEmail;
        }
    }

    public static class HouseholdResponse {
        private UUID householdId;
        private String name;
        private UUID createdBy;
        private LocalDateTime createdAt;

        public HouseholdResponse() {
        }

        public HouseholdResponse(UUID householdId, String name, UUID createdBy, LocalDateTime createdAt) {
            this.householdId = householdId;
            this.name = name;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }

        public UUID getHouseholdId() {
            return householdId;
        }

        public void setHouseholdId(UUID householdId) {
            this.householdId = householdId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public UUID getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(UUID createdBy) {
            this.createdBy = createdBy;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class InvitationResponse {
        private UUID invitationId;
        private String invitedEmail;
        private LocalDateTime expiresAt;

        public InvitationResponse() {
        }

        public InvitationResponse(UUID invitationId, String invitedEmail, LocalDateTime expiresAt) {
            this.invitationId = invitationId;
            this.invitedEmail = invitedEmail;
            this.expiresAt = expiresAt;
        }

        public UUID getInvitationId() {
            return invitationId;
        }

        public void setInvitationId(UUID invitationId) {
            this.invitationId = invitationId;
        }

        public String getInvitedEmail() {
            return invitedEmail;
        }

        public void setInvitedEmail(String invitedEmail) {
            this.invitedEmail = invitedEmail;
        }

        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
