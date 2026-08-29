package com.example.doneyet.domain;

import com.example.doneyet.repository.HouseholdMemberRepository;
import com.example.doneyet.repository.HouseholdRepository;
import com.example.doneyet.repository.TaskRepository;
import com.example.doneyet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.example.doneyet.TestConfig;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class ConstraintTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HouseholdMemberRepository householdMemberRepository;

    private User user;
    private Household household;

    @BeforeEach
    void setUp() {
        user = new User("test@example.com", "hash");
        user = userRepository.save(user);

        household = new Household("Test Household", user);
        household = householdRepository.save(household);
    }

    @Test
    void duplicateEmail_ThrowsConstraintViolation() {
        User firstUser = new User("duplicate@example.com", "hash");
        userRepository.save(firstUser);

        User secondUser = new User("duplicate@example.com", "hash");

        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.save(secondUser);
            userRepository.flush();
        });
    }

    @Test
    void taskWithNullHouseholdId_ThrowsConstraintViolation() {
        Task task = new Task("Test", null, user, user);

        assertThrows(Exception.class, () -> {
            taskRepository.save(task);
            taskRepository.flush();
        });
    }

    @Test
    void taskWithNullAssigneeId_SucceedsNoConstraintViolation() {
        Task task = new Task("Test", household, user);

        assertDoesNotThrow(() -> {
            taskRepository.save(task);
            taskRepository.flush();
        });

        assertTrue(taskRepository.existsById(task.getId()));
        assertNull(taskRepository.findById(task.getId()).get().getAssignee());
    }

    @Test
    void duplicateHouseholdMembership_ThrowsConstraintViolation() {
        HouseholdMember member1 = new HouseholdMember(household, user, HouseholdMemberRole.CREATOR);
        householdMemberRepository.save(member1);

        HouseholdMember member2 = new HouseholdMember(household, user, HouseholdMemberRole.PARTNER);

        assertThrows(DataIntegrityViolationException.class, () -> {
            householdMemberRepository.save(member2);
            householdMemberRepository.flush();
        });
    }

    @Test
    void taskCreatedByCannotBeNull_ThrowsConstraintViolation() {
        Task task = new Task("Test", household, user, null);

        assertThrows(Exception.class, () -> {
            taskRepository.save(task);
            taskRepository.flush();
        });
    }

    @Test
    void nullCreatedBy_ThrowsConstraintViolation() {
        Household house = new Household("House", null);

        assertThrows(Exception.class, () -> {
            householdRepository.save(house);
            householdRepository.flush();
        });
    }

    @Test
    void nullTaskTitle_ThrowsConstraintViolation() {
        Task task = new Task(null, household, user, user);

        assertThrows(Exception.class, () -> {
            taskRepository.save(task);
            taskRepository.flush();
        });
    }

    @Test
    void householdMemberWithNullRole_ThrowsConstraintViolation() {
        HouseholdMember member = new HouseholdMember(household, user, null);

        assertThrows(Exception.class, () -> {
            householdMemberRepository.save(member);
            householdMemberRepository.flush();
        });
    }
}
