package com.example.doneyet.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityRelationshipTest {

    @Test
    void householdCanHaveMultipleTasks() {
        User creator = new User("creator@example.com", "hash");
        creator.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);
        household.setId(UUID.randomUUID());

        Task task1 = new Task("Task 1", household, creator);
        task1.setId(UUID.randomUUID());

        Task task2 = new Task("Task 2", household, creator);
        task2.setId(UUID.randomUUID());

        household.getTasks().add(task1);
        household.getTasks().add(task2);

        assertEquals(2, household.getTasks().size());
    }

    @Test
    void householdLinksToCreator() {
        User creator = new User("creator@example.com", "hash");
        creator.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);

        assertEquals(creator, household.getCreatedBy());
    }

    @Test
    void taskBelongsToHousehold() {
        User creator = new User("creator@example.com", "hash");
        Household household = new Household("Test Household", creator);
        Task task = new Task("Test Task", household, creator);

        assertEquals(household, task.getHousehold());
        assertEquals(creator, task.getCreatedBy());
    }

    @Test
    void taskSoftDeleteFieldExists() {
        User creator = new User("creator@example.com", "hash");
        Household household = new Household("Test Household", creator);
        Task task = new Task("Test Task", household, creator);

        assertNull(task.getDeletedAt());

        LocalDateTime deletedTime = LocalDateTime.now();
        task.setDeletedAt(deletedTime);

        assertEquals(deletedTime, task.getDeletedAt());
    }

    @Test
    void taskFieldsMapCorrectly() {
        User creator = new User("creator@example.com", "hash");
        Household household = new Household("Test Household", creator);

        Task task = new Task("Clean kitchen", household, creator);
        task.setDescription("Clean the kitchen thoroughly");
        task.setCategory(TaskCategory.CLEANING);
        task.setDueDate(LocalDate.now().plusDays(1));
        task.setReminderTime(LocalTime.of(10, 0));
        task.setCompleted(true);
        task.setCompletedBy(creator);
        task.setCompletedAt(LocalDateTime.now());

        assertEquals("Clean kitchen", task.getTitle());
        assertEquals("Clean the kitchen thoroughly", task.getDescription());
        assertEquals(TaskCategory.CLEANING, task.getCategory());
        assertTrue(task.isCompleted());
    }
}
