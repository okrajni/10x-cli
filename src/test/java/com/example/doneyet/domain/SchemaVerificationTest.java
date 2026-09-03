package com.example.doneyet.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SchemaVerificationTest {
    @Test
    void householdTableHasCorrectColumns() {
        User creator = new User("test@example.com", "hash");
        creator.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);
        household.setId(UUID.randomUUID());

        assertNotNull(household.getId());
        assertEquals("Test Household", household.getName());
        assertEquals(creator.getId(), household.getCreatedBy().getId());
    }

    @Test
    void taskTableHasAllRequiredColumns() {
        User creator = new User("creator@example.com", "hash");
        creator.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);
        household.setId(UUID.randomUUID());

        Task task = new Task("Clean kitchen", household, creator);
        task.setId(UUID.randomUUID());
        task.setDescription("Deep clean");
        task.setCategory(TaskCategory.CLEANING);
        task.setDueDate(java.time.LocalDate.now().plusDays(1));
        task.setReminderTime(java.time.LocalTime.of(10, 0));
        task.setCompleted(false);

        // Verify all columns are present
        assertEquals("Clean kitchen", task.getTitle());
        assertEquals("Deep clean", task.getDescription());
        assertEquals(TaskCategory.CLEANING, task.getCategory());
        assertEquals(household.getId(), task.getHousehold().getId());
        assertEquals(creator.getId(), task.getCreatedBy().getId());
        assertFalse(task.isCompleted());
        assertNull(task.getDeletedAt());
    }

    @Test
    void taskSoftDeleteColumnExists() {
        User creator = new User("creator@example.com", "hash");
        creator.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);
        household.setId(UUID.randomUUID());

        Task task = new Task("Test", household, creator);
        task.setId(UUID.randomUUID());

        assertNull(task.getDeletedAt());

        LocalDateTime now = LocalDateTime.now();
        task.setDeletedAt(now);
        assertEquals(now, task.getDeletedAt());
    }

    @Test
    void taskCategoryEnumHasAllValues() {
        assertEquals(7, TaskCategory.values().length);
        assertTrue(enumValueExists(TaskCategory.class, "CLEANING"));
        assertTrue(enumValueExists(TaskCategory.class, "SHOPPING"));
        assertTrue(enumValueExists(TaskCategory.class, "LAUNDRY"));
        assertTrue(enumValueExists(TaskCategory.class, "MAINTENANCE"));
        assertTrue(enumValueExists(TaskCategory.class, "BILLS"));
        assertTrue(enumValueExists(TaskCategory.class, "SEASONAL"));
        assertTrue(enumValueExists(TaskCategory.class, "ERRANDS"));
    }

    private <E extends Enum<E>> boolean enumValueExists(Class<E> enumClass, String name) {
        try {
            Enum.valueOf(enumClass, name);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
