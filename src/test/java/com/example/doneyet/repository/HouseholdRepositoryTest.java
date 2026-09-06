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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class HouseholdRepositoryTest {

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdMemberRepository householdMemberRepository;

    private User creator;
    private User partner;
    private Household household1;
    private Household household2;

    @BeforeEach
    void setUp() {
        creator = new User("creator@example.com", "hash");
        partner = new User("partner@example.com", "hash");

        creator = userRepository.save(creator);
        partner = userRepository.save(partner);

        household1 = new Household("Household 1", creator);
        household2 = new Household("Household 2", partner);

        household1 = householdRepository.save(household1);
        household2 = householdRepository.save(household2);
    }

    @Test
    void findByCreatedBy_ReturnsHouseholdsCreatedByUser() {
        List<Household> result = householdRepository.findByCreatedBy(creator.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(household1.getId(), result.get(0).getId());
    }

    @Test
    void findByCreatedBy_ReturnsEmptyForUserWithNoHouseholds() {
        User other = new User("other@example.com", "hash");
        other = userRepository.save(other);

        List<Household> result = householdRepository.findByCreatedBy(other.getId());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findHouseholdsByMemberId_ReturnsHouseholdsUserIsMemberOf() {
        HouseholdMember member = new HouseholdMember(household1, partner, HouseholdMemberRole.PARTNER);
        householdMemberRepository.save(member);

        List<Household> result = householdRepository.findHouseholdsByMemberId(partner.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(household1.getId(), result.get(0).getId());
    }

    @Test
    void findHouseholdsByMemberId_ReturnsEmptyForNonMember() {
        List<Household> result = householdRepository.findHouseholdsByMemberId(partner.getId());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void findById_ReturnsHouseholdById() {
        Optional<Household> result = householdRepository.findById(household1.getId());

        assertTrue(result.isPresent());
        assertEquals(household1.getId(), result.get().getId());
        assertEquals("Household 1", result.get().getName());
    }
}
