package com.example.doneyet.service;

import com.example.doneyet.domain.*;
import com.example.doneyet.dto.HouseholdDto;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.HouseholdRepository;
import com.example.doneyet.repository.UserRepository;
import com.example.doneyet.repository.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
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
    private UserSessionRepository userSessionRepository;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        householdRepository.deleteAll();
        userSessionRepository.deleteAll();
        userRepository.deleteAll();

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
    void testGetUserHouseholds() {
        // Create one household (users can only have one)
        HouseholdDto.HouseholdResponse household1 = householdService.createHousehold(testUserId, "Test Household");

        // Get user households
        List<HouseholdDto.HouseholdResponse> households = householdService.getUserHouseholds(testUserId);

        assertEquals(1, households.size());
        assertEquals("Test Household", households.get(0).getName());
    }

    @Test
    void testCreateSecondHouseholdThrows() {
        // Given: a user with one household already created
        HouseholdDto.HouseholdResponse first = householdService.createHousehold(testUserId, "First Household");
        assertNotNull(first);

        // When: attempting to create a second household
        // Then: ValidationException is thrown with the correct message
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> householdService.createHousehold(testUserId, "Second Household")
        );
        assertEquals("User already has a household. Use the existing household to create tasks.", exception.getMessage());
    }

    @Test
    void testGetHouseholdDetails() {
        // Create household
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();

        // Get details
        HouseholdDto.HouseholdDetailsResponse details = householdService.getHouseholdDetails(householdId, testUserId);

        assertNotNull(details);
        assertEquals(householdId, details.getHouseholdId());
        assertEquals("Test Household", details.getName());
        assertEquals(testUserId, details.getCreatedBy());
    }

    @Test
    void testGetHouseholdDetailsAccessDenied() {
        // Create household by testUser
        HouseholdDto.HouseholdResponse householdResponse = householdService.createHousehold(testUserId, "Test Household");
        UUID householdId = householdResponse.getHouseholdId();

        // Create another user
        User otherUser = new User("bob@example.com", "hashed_password");
        otherUser = userRepository.save(otherUser);
        UUID otherUserId = otherUser.getId();

        // Try to access as different user - should fail
        assertThrows(ValidationException.class, () -> {
            householdService.getHouseholdDetails(householdId, otherUserId);
        });
    }
}
