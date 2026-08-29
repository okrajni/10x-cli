package com.example.doneyet.dto;

import java.time.LocalDateTime;
import java.util.List;
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

    public static class HouseholdDetailsResponse {
        private UUID householdId;
        private String name;
        private UUID createdBy;
        private LocalDateTime createdAt;
        private List<HouseholdMemberDto> members;

        public HouseholdDetailsResponse() {
        }

        public HouseholdDetailsResponse(UUID householdId, String name, UUID createdBy, LocalDateTime createdAt, List<HouseholdMemberDto> members) {
            this.householdId = householdId;
            this.name = name;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
            this.members = members;
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

        public List<HouseholdMemberDto> getMembers() {
            return members;
        }

        public void setMembers(List<HouseholdMemberDto> members) {
            this.members = members;
        }
    }

    public static class HouseholdMemberDto {
        private UUID id;
        private String name;
        private String email;

        public HouseholdMemberDto() {
        }

        public HouseholdMemberDto(UUID id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
