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
import com.example.doneyet.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class HouseholdServiceTest {
    @Autowired
    private HouseholdService householdService;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdMemberRepository householdMemberRepository;

    @Autowired
    private HouseholdInvitationRepository invitationRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private MockEmailService mockEmailService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        invitationRepository.deleteAll();
        householdMemberRepository.deleteAll();
        householdRepository.deleteAll();
        userSessionRepository.deleteAll();
        userRepository.deleteAll();
        mockEmailService.clear();

        testUser = new User("alice@example.com", "hashed_password");
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId();
    }

    @Test
    void testCreateHousehold() {
        HouseholdDto.HouseholdResponse response = householdService.createHousehold(testUserId, "Smith Household");

        assertNotNull(response);
        assertNotNull(response.getHouseholdId());
        assertEquals("Smith Household", response.getName());
        assertEquals(testUserId, response.getCreatedBy());
    }

    @Test
    void testCreateHouseholdWithEmptyName() {
        assertThrows(ValidationException.class, () -> {
            householdService.createHousehold(testUserId, "");
        });
    }

    @Test
    void testCreateHouseholdWithNullName() {
        assertThrows(ValidationException.class, () -> {
            householdService.createHousehold(testUserId, null);
        });
    }

    @Test
    void testSendInvitation() {
        // Create a household first
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();

        // Send invitation
        HouseholdDto.InvitationResponse response = householdService.sendInvitation(householdId, "bob@example.com", "alice");

        assertNotNull(response);
        assertNotNull(response.getInvitationId());
        assertEquals("bob@example.com", response.getInvitedEmail());
        assertNotNull(response.getExpiresAt());
        assertTrue(response.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void testSendInvitationWithInvalidEmail() {
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();

        assertThrows(ValidationException.class, () -> {
            householdService.sendInvitation(householdId, "invalid-email", "alice");
        });
    }

    @Test
    void testSendInvitationGeneratesUniqueTokens() {
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();

        HouseholdDto.InvitationResponse response1 = householdService.sendInvitation(householdId, "bob1@example.com", "alice");
        HouseholdDto.InvitationResponse response2 = householdService.sendInvitation(householdId, "bob2@example.com", "alice");

        assertNotEquals(response1.getInvitationId(), response2.getInvitationId());
    }

    @Test
    void testAcceptInvitation() {
        // Create household and send invitation
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();
        HouseholdDto.InvitationResponse invitationResponse = householdService.sendInvitation(householdId, "bob@example.com", "alice");

        // Get the actual token from the repository
        Optional<HouseholdInvitation> invitation = invitationRepository.findById(invitationResponse.getInvitationId());
        assertTrue(invitation.isPresent());
        String token = invitation.get().getInvitationToken();

        // Accept invitation
        HouseholdDto.HouseholdResponse acceptResponse = householdService.acceptInvitation(token);
        assertNotNull(acceptResponse);
        assertEquals(householdId, acceptResponse.getHouseholdId());

        // Verify invitation is marked as accepted
        Optional<HouseholdInvitation> updatedInvitation = invitationRepository.findById(invitationResponse.getInvitationId());
        assertTrue(updatedInvitation.isPresent());
        assertTrue(updatedInvitation.get().isAccepted());
    }

    @Test
    void testAcceptExpiredInvitation() {
        // Create household and send invitation
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();
        HouseholdDto.InvitationResponse invitationResponse = householdService.sendInvitation(householdId, "bob@example.com", "alice");

        // Get the invitation and set it to expired
        Optional<HouseholdInvitation> invitation = invitationRepository.findById(invitationResponse.getInvitationId());
        assertTrue(invitation.isPresent());
        invitation.get().setExpiresAt(LocalDateTime.now().minusHours(1));
        invitationRepository.save(invitation.get());
        String token = invitation.get().getInvitationToken();

        // Try to accept expired invitation
        assertThrows(ResourceExpiredException.class, () -> {
            householdService.acceptInvitation(token);
        });
    }

    @Test
    void testAcceptAlreadyAcceptedInvitation() {
        // Create household and send invitation
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();
        HouseholdDto.InvitationResponse invitationResponse = householdService.sendInvitation(householdId, "bob@example.com", "alice");

        // Get token and accept invitation
        Optional<HouseholdInvitation> invitation = invitationRepository.findById(invitationResponse.getInvitationId());
        assertTrue(invitation.isPresent());
        String token = invitation.get().getInvitationToken();

        householdService.acceptInvitation(token);

        // Try to accept same invitation again
        assertThrows(ConflictException.class, () -> {
            householdService.acceptInvitation(token);
        });
    }

    @Test
    void testAcceptInvalidToken() {
        assertThrows(ValidationException.class, () -> {
            householdService.acceptInvitation("invalid-token-xyz");
        });
    }

    @Test
    void testJoinHousehold() {
        // Create household
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();

        // Create another user
        User bob = new User("bob@example.com", "hashed_password");
        bob = userRepository.save(bob);

        // Join household
        householdService.joinHousehold(bob.getId(), householdId);

        // Verify membership
        Optional<HouseholdMember> member = householdMemberRepository.findByHouseholdIdAndUserId(householdId, bob.getId());
        assertTrue(member.isPresent());
        assertEquals(HouseholdMemberRole.PARTNER, member.get().getRole());
    }

    @Test
    void testJoinHouseholdDuplicate() {
        // Create household
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();

        // Create another user
        User bob = new User("bob@example.com", "hashed_password");
        User savedBob = userRepository.save(bob);

        // Join household
        householdService.joinHousehold(savedBob.getId(), householdId);

        // Try to join again
        assertThrows(ConflictException.class, () -> {
            householdService.joinHousehold(savedBob.getId(), householdId);
        });
    }

    @Test
    void testGetUserHouseholds() {
        // Create multiple households
        HouseholdDto.HouseholdResponse household1 = householdService.createHousehold(testUserId, "Household 1");
        HouseholdDto.HouseholdResponse household2 = householdService.createHousehold(testUserId, "Household 2");

        // Get user households
        List<HouseholdDto.HouseholdResponse> households = householdService.getUserHouseholds(testUserId);

        assertEquals(2, households.size());
        assertTrue(households.stream().anyMatch(h -> h.getName().equals("Household 1")));
        assertTrue(households.stream().anyMatch(h -> h.getName().equals("Household 2")));
    }

    @Test
    void testGetUserHouseholdsOrdered() {
        // Create households with small delay to ensure different timestamps
        HouseholdDto.HouseholdResponse household1 = householdService.createHousehold(testUserId, "Household 1");
        try { Thread.sleep(10); } catch (InterruptedException e) { }
        HouseholdDto.HouseholdResponse household2 = householdService.createHousehold(testUserId, "Household 2");

        // Get user households
        List<HouseholdDto.HouseholdResponse> households = householdService.getUserHouseholds(testUserId);

        assertEquals(2, households.size());
        assertEquals("Household 2", households.get(0).getName());
        assertEquals("Household 1", households.get(1).getName());
    }
}
