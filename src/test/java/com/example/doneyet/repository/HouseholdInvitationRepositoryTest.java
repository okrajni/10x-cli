package com.example.doneyet.repository;

import com.example.doneyet.domain.Household;
import com.example.doneyet.domain.HouseholdInvitation;
import com.example.doneyet.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.example.doneyet.TestConfig;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class HouseholdInvitationRepositoryTest {

    @Autowired
    private HouseholdInvitationRepository invitationRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    private User creator;
    private Household household;
    private String invitationToken;
    private LocalDateTime expiresAt;

    @BeforeEach
    void setUp() {
        creator = new User("creator@example.com", "hash");
        creator = userRepository.save(creator);

        household = new Household("Test Household", creator);
        household = householdRepository.save(household);

        invitationToken = UUID.randomUUID().toString();
        expiresAt = LocalDateTime.now().plusHours(24);
    }

    @Test
    void findByInvitationToken_ReturnsInvitationIfExists() {
        HouseholdInvitation invitation = new HouseholdInvitation(
                household, "partner@example.com", invitationToken, expiresAt
        );
        invitationRepository.save(invitation);

        Optional<HouseholdInvitation> result = invitationRepository.findByInvitationToken(invitationToken);

        assertTrue(result.isPresent());
        assertEquals("partner@example.com", result.get().getInvitedEmail());
    }

    @Test
    void findByInvitationToken_ReturnsEmptyIfNotExists() {
        Optional<HouseholdInvitation> result = invitationRepository.findByInvitationToken("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void findByHouseholdIdAndInvitedEmail_ReturnsInvitationIfExists() {
        HouseholdInvitation invitation = new HouseholdInvitation(
                household, "partner@example.com", invitationToken, expiresAt
        );
        invitationRepository.save(invitation);

        Optional<HouseholdInvitation> result = invitationRepository
                .findByHouseholdIdAndInvitedEmail(household.getId(), "partner@example.com");

        assertTrue(result.isPresent());
        assertEquals(household.getId(), result.get().getHousehold().getId());
    }

    @Test
    void findByHouseholdIdAndInvitedEmail_ReturnsEmptyIfNotExists() {
        Optional<HouseholdInvitation> result = invitationRepository
                .findByHouseholdIdAndInvitedEmail(household.getId(), "nonexistent@example.com");

        assertFalse(result.isPresent());
    }

    @Test
    void findByHouseholdIdAndAcceptedFalse_ReturnsPendingInvitations() {
        HouseholdInvitation inv1 = new HouseholdInvitation(
                household, "partner1@example.com", UUID.randomUUID().toString(),
                LocalDateTime.now().plusHours(24)
        );
        HouseholdInvitation inv2 = new HouseholdInvitation(
                household, "partner2@example.com", UUID.randomUUID().toString(),
                LocalDateTime.now().plusHours(24)
        );

        invitationRepository.save(inv1);
        invitationRepository.save(inv2);

        List<HouseholdInvitation> result = invitationRepository
                .findByHouseholdIdAndAcceptedFalse(household.getId());

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(i -> !i.isAccepted()));
    }

    @Test
    void findByHouseholdIdAndAcceptedFalse_ExcludesAcceptedInvitations() {
        HouseholdInvitation pending = new HouseholdInvitation(
                household, "pending@example.com", UUID.randomUUID().toString(),
                LocalDateTime.now().plusHours(24)
        );
        HouseholdInvitation accepted = new HouseholdInvitation(
                household, "accepted@example.com", UUID.randomUUID().toString(),
                LocalDateTime.now().plusHours(24)
        );
        accepted.setAccepted(true);
        accepted.setAcceptedAt(LocalDateTime.now());

        invitationRepository.save(pending);
        invitationRepository.save(accepted);

        List<HouseholdInvitation> result = invitationRepository
                .findByHouseholdIdAndAcceptedFalse(household.getId());

        assertEquals(1, result.size());
        assertEquals("pending@example.com", result.get(0).getInvitedEmail());
    }

    @Test
    void findByExpiresAtBeforeAndAcceptedFalse_ReturnsExpiredPendingInvitations() {
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        HouseholdInvitation expired = new HouseholdInvitation(
                household, "expired@example.com", UUID.randomUUID().toString(), pastTime
        );
        HouseholdInvitation valid = new HouseholdInvitation(
                household, "valid@example.com", UUID.randomUUID().toString(),
                LocalDateTime.now().plusHours(24)
        );

        invitationRepository.save(expired);
        invitationRepository.save(valid);

        List<HouseholdInvitation> result = invitationRepository
                .findByExpiresAtBeforeAndAcceptedFalse(LocalDateTime.now());

        assertEquals(1, result.size());
        assertEquals("expired@example.com", result.get(0).getInvitedEmail());
    }

    @Test
    void findByExpiresAtBeforeAndAcceptedFalse_ExcludesAcceptedInvitations() {
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        HouseholdInvitation expiredButAccepted = new HouseholdInvitation(
                household, "accepted@example.com", UUID.randomUUID().toString(), pastTime
        );
        expiredButAccepted.setAccepted(true);

        invitationRepository.save(expiredButAccepted);

        List<HouseholdInvitation> result = invitationRepository
                .findByExpiresAtBeforeAndAcceptedFalse(LocalDateTime.now());

        assertTrue(result.isEmpty());
    }
}
