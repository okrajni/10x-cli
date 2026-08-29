package com.example.doneyet.service;

import com.example.doneyet.domain.*;
import com.example.doneyet.dto.HouseholdDto;
import com.example.doneyet.exception.ConflictException;
import com.example.doneyet.exception.ResourceExpiredException;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.HouseholdInvitationRepository;
import com.example.doneyet.repository.HouseholdMemberRepository;
import com.example.doneyet.repository.HouseholdRepository;
import com.example.doneyet.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class HouseholdService {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
    private static final int INVITATION_EXPIRY_HOURS = 24;

    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final HouseholdInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public HouseholdService(
            HouseholdRepository householdRepository,
            HouseholdMemberRepository householdMemberRepository,
            HouseholdInvitationRepository invitationRepository,
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public HouseholdDto.HouseholdResponse createHousehold(UUID userId, String name) {
        validateHouseholdName(name);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        Household household = new Household(name, user);
        Household savedHousehold = householdRepository.save(household);

        HouseholdMember member = new HouseholdMember(savedHousehold, user, HouseholdMemberRole.CREATOR);
        householdMemberRepository.save(member);

        return new HouseholdDto.HouseholdResponse(
                savedHousehold.getId(),
                savedHousehold.getName(),
                savedHousehold.getCreatedBy().getId(),
                savedHousehold.getCreatedAt()
        );
    }

    @Transactional
    public HouseholdDto.InvitationResponse sendInvitation(UUID householdId, String invitedEmail, String inviterName) {
        validateEmail(invitedEmail);

        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ValidationException("Household not found"));

        String token = generateInvitationToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(INVITATION_EXPIRY_HOURS);

        HouseholdInvitation invitation = new HouseholdInvitation(household, invitedEmail, token, expiresAt);
        HouseholdInvitation savedInvitation = invitationRepository.save(invitation);

        String invitationLink = String.format("http://localhost:5173/invitation/accept/%s?email=%s", token, invitedEmail);
        emailService.sendInvitationEmail(invitedEmail, invitationLink, household.getName());

        return new HouseholdDto.InvitationResponse(
                savedInvitation.getId(),
                savedInvitation.getInvitedEmail(),
                savedInvitation.getExpiresAt()
        );
    }

    @Transactional
    public HouseholdDto.HouseholdResponse acceptInvitation(String token) {
        HouseholdInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ValidationException("Invalid invitation token"));

        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            throw new ResourceExpiredException("Invitation has expired");
        }

        if (invitation.isAccepted()) {
            throw new ConflictException("This invitation has already been accepted");
        }

        invitation.setAccepted(true);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        Household household = invitation.getHousehold();
        return new HouseholdDto.HouseholdResponse(
                household.getId(),
                household.getName(),
                household.getCreatedBy().getId(),
                household.getCreatedAt()
        );
    }

    @Transactional
    public void acceptInvitationForUser(String token, UUID userId) {
        HouseholdInvitation invitation = invitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ValidationException("Invalid invitation token"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        invitation.setAcceptedByUser(user);
        invitationRepository.save(invitation);

        joinHousehold(userId, invitation.getHousehold().getId());
    }

    @Transactional
    public void joinHousehold(UUID userId, UUID householdId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ValidationException("Household not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        Optional<HouseholdMember> existingMember = householdMemberRepository.findByHouseholdIdAndUserId(householdId, userId);
        if (existingMember.isPresent()) {
            throw new ConflictException("User is already a member of this household");
        }

        HouseholdMember member = new HouseholdMember(household, user, HouseholdMemberRole.PARTNER);
        householdMemberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<HouseholdDto.HouseholdResponse> getUserHouseholds(UUID userId) {
        List<HouseholdMember> members = householdMemberRepository.findByUserId(userId);

        List<HouseholdDto.HouseholdResponse> responses = new ArrayList<>();
        for (HouseholdMember member : members) {
            Household household = member.getHousehold();
            responses.add(new HouseholdDto.HouseholdResponse(
                    household.getId(),
                    household.getName(),
                    household.getCreatedBy().getId(),
                    household.getCreatedAt()
            ));
        }

        responses.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return responses;
    }

    @Transactional(readOnly = true)
    public HouseholdDto.HouseholdDetailsResponse getHouseholdDetails(UUID householdId, UUID userId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ValidationException("Household not found"));

        Optional<HouseholdMember> userMembership = householdMemberRepository.findByHouseholdIdAndUserId(householdId, userId);
        if (userMembership.isEmpty()) {
            throw new ValidationException("User is not a member of this household");
        }

        List<HouseholdMember> householdMembers = householdMemberRepository.findByHouseholdId(householdId);
        List<HouseholdDto.HouseholdMemberDto> memberDtos = new ArrayList<>();
        for (HouseholdMember member : householdMembers) {
            String displayName = member.getUser().getEmail().split("@")[0];
            memberDtos.add(new HouseholdDto.HouseholdMemberDto(
                    member.getId(),
                    displayName,
                    member.getUser().getEmail()
            ));
        }

        return new HouseholdDto.HouseholdDetailsResponse(
                household.getId(),
                household.getName(),
                household.getCreatedBy().getId(),
                household.getCreatedAt(),
                memberDtos
        );
    }

    private void validateHouseholdName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Household name is required");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
        if (!email.matches(EMAIL_REGEX)) {
            throw new ValidationException("Invalid email format");
        }
    }

    private String generateInvitationToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}
