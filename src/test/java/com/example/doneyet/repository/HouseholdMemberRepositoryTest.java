package com.example.doneyet.repository;

import com.example.doneyet.domain.Household;
import com.example.doneyet.domain.HouseholdMember;
import com.example.doneyet.domain.HouseholdMemberRole;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class HouseholdMemberRepositoryTest {

    @Autowired
    private HouseholdMemberRepository householdMemberRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    private User creator;
    private User partner;
    private Household household;

    @BeforeEach
    void setUp() {
        creator = new User("creator@example.com", "hash");
        partner = new User("partner@example.com", "hash");

        creator = userRepository.save(creator);
        partner = userRepository.save(partner);

        household = new Household("Test Household", creator);
        household = householdRepository.save(household);
    }

    @Test
    void findByHouseholdIdAndUserId_ReturnsMembershipIfExists() {
        HouseholdMember member = new HouseholdMember(household, partner, HouseholdMemberRole.PARTNER);
        householdMemberRepository.save(member);

        Optional<HouseholdMember> result = householdMemberRepository
                .findByHouseholdIdAndUserId(household.getId(), partner.getId());

        assertTrue(result.isPresent());
        assertEquals(partner.getId(), result.get().getUser().getId());
        assertEquals(HouseholdMemberRole.PARTNER, result.get().getRole());
    }

    @Test
    void findByHouseholdIdAndUserId_ReturnsEmptyIfMembershipDoesNotExist() {
        Optional<HouseholdMember> result = householdMemberRepository
                .findByHouseholdIdAndUserId(household.getId(), partner.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void findByHouseholdId_ReturnsAllMembers() {
        HouseholdMember creatorMember = new HouseholdMember(household, creator, HouseholdMemberRole.CREATOR);
        HouseholdMember partnerMember = new HouseholdMember(household, partner, HouseholdMemberRole.PARTNER);

        householdMemberRepository.save(creatorMember);
        householdMemberRepository.save(partnerMember);

        List<HouseholdMember> result = householdMemberRepository.findByHouseholdId(household.getId());

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(m -> m.getRole() == HouseholdMemberRole.CREATOR));
        assertTrue(result.stream().anyMatch(m -> m.getRole() == HouseholdMemberRole.PARTNER));
    }

    @Test
    void findByHouseholdId_ReturnsEmptyForHouseholdWithNoMembers() {
        List<HouseholdMember> result = householdMemberRepository.findByHouseholdId(household.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void countByHouseholdId_ReturnsCorrectMemberCount() {
        HouseholdMember creatorMember = new HouseholdMember(household, creator, HouseholdMemberRole.CREATOR);
        HouseholdMember partnerMember = new HouseholdMember(household, partner, HouseholdMemberRole.PARTNER);

        householdMemberRepository.save(creatorMember);
        householdMemberRepository.save(partnerMember);

        long count = householdMemberRepository.countByHouseholdId(household.getId());

        assertEquals(2, count);
    }

    @Test
    void countByHouseholdId_ReturnsZeroForHouseholdWithNoMembers() {
        long count = householdMemberRepository.countByHouseholdId(household.getId());

        assertEquals(0, count);
    }
}
