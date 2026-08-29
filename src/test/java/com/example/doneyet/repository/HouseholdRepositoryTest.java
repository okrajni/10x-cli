package com.example.doneyet.repository;

import com.example.doneyet.domain.Household;
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
class HouseholdRepositoryTest {

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    private User creator;
    private User otherUser;
    private Household household1;
    private Household household2;

    @BeforeEach
    void setUp() {
        creator = new User("creator@example.com", "hash");
        otherUser = new User("other@example.com", "hash");

        creator = userRepository.save(creator);
        otherUser = userRepository.save(otherUser);

        household1 = new Household("Household 1", creator);
        household2 = new Household("Household 2", otherUser);

        household1 = householdRepository.save(household1);
        household2 = householdRepository.save(household2);
    }

    @Test
    void findByCreatedById_ReturnsHouseholdsCreatedByUser() {
        List<Household> result = householdRepository.findByCreatedById(creator.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(household1.getId(), result.get(0).getId());
    }

    @Test
    void findByCreatedById_ReturnsEmptyForUserWithNoHouseholds() {
        User noHouseholdUser = new User("nohousehold@example.com", "hash");
        noHouseholdUser = userRepository.save(noHouseholdUser);

        List<Household> result = householdRepository.findByCreatedById(noHouseholdUser.getId());

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
