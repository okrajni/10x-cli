package com.example.doneyet.repository;

import com.example.doneyet.domain.Household;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class TaskSoftDeleteTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private UserRepository userRepository;

    private User creator;
    private Household household;
    private Task task;

    @BeforeEach
    void setUp() {
        creator = new User("creator@example.com", "hash");
        creator = userRepository.save(creator);

        household = new Household("Test Household", creator);
        household = householdRepository.save(household);

        task = new Task("Clean kitchen", household, creator, creator);
        task = taskRepository.save(task);
    }

    @Test
    void newTaskIsNotSoftDeleted() {
        assertNull(task.getDeletedAt());
        assertTrue(taskRepository.findByHouseholdIdAndDeletedAtIsNull(household.getId()).contains(task));
    }

    @Test
    void activeTasks_IncludedInDefaultQuery() {
        List<Task> result = taskRepository.findByHouseholdIdAndDeletedAtIsNull(household.getId());

        assertFalse(result.isEmpty());
        assertTrue(result.contains(task));
    }

    @Test
    void softDeletedTasks_ExcludedFromDefaultQuery() {
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);

        List<Task> result = taskRepository.findByHouseholdIdAndDeletedAtIsNull(household.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void softDeletedTasks_CanBeQueriedExplicitly() {
        LocalDateTime deletedTime = LocalDateTime.now();
        task.setDeletedAt(deletedTime);
        taskRepository.save(task);

        List<Task> result = taskRepository.findAll();
        Optional<Task> deletedTask = result.stream()
                .filter(t -> t.getId().equals(task.getId()))
                .filter(t -> t.getDeletedAt() != null)
                .findFirst();

        assertTrue(deletedTask.isPresent());
        assertNotNull(deletedTask.get().getDeletedAt());
    }

    @Test
    void cascadeDelete_SoftDeletes_DoNotAffectUserOrHousehold() {
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);

        Optional<User> creatorCheck = userRepository.findById(creator.getId());
        Optional<Household> householdCheck = householdRepository.findById(household.getId());

        assertTrue(creatorCheck.isPresent());
        assertTrue(householdCheck.isPresent());
    }

    @Test
    void multipleTasksWithSoftDelete_OnlyActiveTasks_Returned() {
        Task task2 = new Task("Buy groceries", household, creator, creator);
        task2 = taskRepository.save(task2);

        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);

        List<Task> result = taskRepository.findByHouseholdIdAndDeletedAtIsNull(household.getId());

        assertEquals(1, result.size());
        assertEquals(task2.getId(), result.get(0).getId());
        assertNull(result.get(0).getDeletedAt());
    }

    @Test
    void assigneeQuery_WithSoftDelete_ExcludesDeleted() {
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);

        List<Task> result = taskRepository.findByHouseholdIdAndAssigneeIdAndDeletedAtIsNull(
                household.getId(), creator.getId()
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void completionQuery_WithSoftDelete_ExcludesDeleted() {
        task.setCompleted(true);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);

        List<Task> result = taskRepository.findByHouseholdIdAndCompletedAndDeletedAtIsNull(
                household.getId(), true
        );

        assertTrue(result.isEmpty());
    }
}
