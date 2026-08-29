package com.example.doneyet.repository;

import com.example.doneyet.domain.Household;
import com.example.doneyet.domain.HouseholdMember;
import com.example.doneyet.domain.HouseholdMemberRole;
import com.example.doneyet.domain.Task;
import com.example.doneyet.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.example.doneyet.TestConfig;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class HouseholdIsolationTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdMemberRepository householdMemberRepository;

    private User userA;
    private User userB;
    private Household householdA;
    private Household householdB;
    private Task taskA;
    private Task taskB;

    @BeforeEach
    void setUp() {
        userA = new User("userA@example.com", "hash");
        userB = new User("userB@example.com", "hash");

        userA = userRepository.save(userA);
        userB = userRepository.save(userB);

        householdA = new Household("Household A", userA);
        householdB = new Household("Household B", userB);

        householdA = householdRepository.save(householdA);
        householdB = householdRepository.save(householdB);

        HouseholdMember memberA = new HouseholdMember(householdA, userA, HouseholdMemberRole.CREATOR);
        HouseholdMember memberB = new HouseholdMember(householdB, userB, HouseholdMemberRole.CREATOR);

        memberA = householdMemberRepository.save(memberA);
        memberB = householdMemberRepository.save(memberB);

        taskA = new Task("Task A", householdA, memberA, userA);
        taskB = new Task("Task B", householdB, memberB, userB);

        taskA = taskRepository.save(taskA);
        taskB = taskRepository.save(taskB);
    }

    @Test
    void householdA_Tasks_ReturnOnlyTasksFromA() {
        List<Task> result = taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdA.getId());

        assertEquals(1, result.size());
        assertEquals(taskA.getId(), result.get(0).getId());
    }

    @Test
    void householdB_Tasks_ReturnOnlyTasksFromB() {
        List<Task> result = taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdB.getId());

        assertEquals(1, result.size());
        assertEquals(taskB.getId(), result.get(0).getId());
    }

    @Test
    void crossHousehold_TaskLookup_EnforcesIsolation() {
        Optional<Task> result = taskRepository.findByIdAndHouseholdId(taskA.getId(), householdB.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void correctHousehold_TaskLookup_ReturnsTask() {
        Optional<Task> result = taskRepository.findByIdAndHouseholdId(taskA.getId(), householdA.getId());

        assertTrue(result.isPresent());
        assertEquals(taskA.getId(), result.get().getId());
    }

    @Test
    void assigneeQuery_IsIsolatedByHousehold() {
        User commonUser = new User("common@example.com", "hash");
        commonUser = userRepository.save(commonUser);

        HouseholdMember commonMemberA = new HouseholdMember(householdA, commonUser, HouseholdMemberRole.PARTNER);
        HouseholdMember commonMemberB = new HouseholdMember(householdB, commonUser, HouseholdMemberRole.PARTNER);

        commonMemberA = householdMemberRepository.save(commonMemberA);
        commonMemberB = householdMemberRepository.save(commonMemberB);

        Task sharedAssigneeTaskA = new Task("Shared Task A", householdA, commonMemberA, userA);
        Task sharedAssigneeTaskB = new Task("Shared Task B", householdB, commonMemberB, userB);

        taskRepository.save(sharedAssigneeTaskA);
        taskRepository.save(sharedAssigneeTaskB);

        List<Task> resultA = taskRepository.findByHouseholdIdAndAssigneeIdAndDeletedAtIsNull(
                householdA.getId(), commonMemberA.getId()
        );
        List<Task> resultB = taskRepository.findByHouseholdIdAndAssigneeIdAndDeletedAtIsNull(
                householdB.getId(), commonMemberB.getId()
        );

        assertEquals(1, resultA.size());
        assertEquals(1, resultB.size());
        assertEquals(sharedAssigneeTaskA.getId(), resultA.get(0).getId());
        assertEquals(sharedAssigneeTaskB.getId(), resultB.get(0).getId());
    }

    @Test
    void multipleTasksPerHousehold_AllIsolated() {
        HouseholdMember memberA = householdMemberRepository.findAll().stream()
                .filter(m -> m.getHousehold().getId().equals(householdA.getId()) && m.getUser().getId().equals(userA.getId()))
                .findFirst()
                .orElseThrow();

        Task additionalTaskA = new Task("Additional Task A", householdA, memberA, userA);
        additionalTaskA = taskRepository.save(additionalTaskA);

        List<Task> resultA = taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdA.getId());
        List<Task> resultB = taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdB.getId());

        assertEquals(2, resultA.size());
        assertEquals(1, resultB.size());

        assertTrue(resultA.stream().allMatch(t -> t.getHousehold().getId().equals(householdA.getId())));
        assertTrue(resultB.stream().allMatch(t -> t.getHousehold().getId().equals(householdB.getId())));
    }

    @Test
    void householdA_CannotAccessHouseholdB_Tasks() {
        List<Task> tasksA = taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdA.getId());

        assertFalse(tasksA.stream().anyMatch(t -> t.getId().equals(taskB.getId())));
    }

    @Test
    void householdB_CannotAccessHouseholdA_Tasks() {
        List<Task> tasksB = taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdB.getId());

        assertFalse(tasksB.stream().anyMatch(t -> t.getId().equals(taskA.getId())));
    }
}
