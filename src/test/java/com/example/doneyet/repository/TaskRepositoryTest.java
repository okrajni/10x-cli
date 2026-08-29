package com.example.doneyet.repository;

import com.example.doneyet.domain.Household;
import com.example.doneyet.domain.HouseholdMember;
import com.example.doneyet.domain.HouseholdMemberRole;
import com.example.doneyet.domain.Task;
import com.example.doneyet.domain.TaskCategory;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdMemberRepository householdMemberRepository;

    private User creator;
    private User assignee;
    private User otherUser;
    private Household household1;
    private Household household2;
    private HouseholdMember assigneeMember;
    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        creator = new User("creator@example.com", "hash");
        assignee = new User("assignee@example.com", "hash");
        otherUser = new User("other@example.com", "hash");

        creator = userRepository.save(creator);
        assignee = userRepository.save(assignee);
        otherUser = userRepository.save(otherUser);

        household1 = new Household("Household 1", creator);
        household2 = new Household("Household 2", otherUser);

        household1 = householdRepository.save(household1);
        household2 = householdRepository.save(household2);

        assigneeMember = new HouseholdMember(household1, assignee, HouseholdMemberRole.PARTNER);
        assigneeMember = householdMemberRepository.save(assigneeMember);

        task1 = new Task("Task 1", household1, assigneeMember, creator);
        task1.setCategory(TaskCategory.CLEANING);
        task1.setDueDate(LocalDate.now().plusDays(1));
        task1 = taskRepository.save(task1);

        task2 = new Task("Task 2", household1, assigneeMember, creator);
        task2.setCategory(TaskCategory.SHOPPING);
        task2.setDueDate(LocalDate.now().plusDays(2));
        task2 = taskRepository.save(task2);
    }

    @Test
    void findByHouseholdIdAndDeletedAtIsNull_ReturnsActiveTasks() {
        List<Task> result = taskRepository.findByHouseholdIdAndDeletedAtIsNull(household1.getId());

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> t.getId().equals(task1.getId())));
        assertTrue(result.stream().anyMatch(t -> t.getId().equals(task2.getId())));
    }

    @Test
    void findByHouseholdIdAndDeletedAtIsNull_ExcludesSoftDeletedTasks() {
        task1.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task1);

        List<Task> result = taskRepository.findByHouseholdIdAndDeletedAtIsNull(household1.getId());

        assertEquals(1, result.size());
        assertEquals(task2.getId(), result.get(0).getId());
    }

    @Test
    void findByHouseholdIdAndDeletedAtIsNull_ReturnsEmptyForHouseholdWithNoActiveTasks() {
        task1.setDeletedAt(LocalDateTime.now());
        task2.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task1);
        taskRepository.save(task2);

        List<Task> result = taskRepository.findByHouseholdIdAndDeletedAtIsNull(household1.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByHouseholdIdAndAssigneeIdAndDeletedAtIsNull_ReturnsTasks() {
        List<Task> result = taskRepository.findByHouseholdIdAndAssigneeIdAndDeletedAtIsNull(
                household1.getId(), assigneeMember.getId()
        );

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getAssignee().getId().equals(assigneeMember.getId())));
    }

    @Test
    void findByHouseholdIdAndAssigneeIdAndDeletedAtIsNull_ExcludesSoftDeletedTasks() {
        task1.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task1);

        List<Task> result = taskRepository.findByHouseholdIdAndAssigneeIdAndDeletedAtIsNull(
                household1.getId(), assigneeMember.getId()
        );

        assertEquals(1, result.size());
        assertEquals(task2.getId(), result.get(0).getId());
    }

    @Test
    void findByHouseholdIdAndDueDateAndDeletedAtIsNull_ReturnsTasks() {
        LocalDate dueDate = task1.getDueDate();
        List<Task> result = taskRepository.findByHouseholdIdAndDueDateAndDeletedAtIsNull(
                household1.getId(), dueDate
        );

        assertEquals(1, result.size());
        assertEquals(task1.getId(), result.get(0).getId());
    }

    @Test
    void findByHouseholdIdAndCompletedAndDeletedAtIsNull_ReturnsTasks() {
        task1.setCompleted(true);
        taskRepository.save(task1);

        List<Task> result = taskRepository.findByHouseholdIdAndCompletedAndDeletedAtIsNull(
                household1.getId(), true
        );

        assertEquals(1, result.size());
        assertEquals(task1.getId(), result.get(0).getId());
    }

    @Test
    void findByIdAndHouseholdId_EnforcesIsolation() {
        Optional<Task> result = taskRepository.findByIdAndHouseholdId(task1.getId(), household2.getId());

        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndHouseholdId_ReturnsTaskInCorrectHousehold() {
        Optional<Task> result = taskRepository.findByIdAndHouseholdId(task1.getId(), household1.getId());

        assertTrue(result.isPresent());
        assertEquals(task1.getId(), result.get().getId());
    }
}
