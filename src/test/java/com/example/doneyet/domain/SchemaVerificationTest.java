package com.example.doneyet.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import com.example.doneyet.domain.HouseholdMember;
import com.example.doneyet.domain.HouseholdMemberRole;

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
    void householdMemberHasCorrectColumns() {
        User creator = new User("creator@example.com", "hash");
        creator.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);
        household.setId(UUID.randomUUID());

        HouseholdMember member = new HouseholdMember(household, creator, HouseholdMemberRole.CREATOR);
        member.setId(UUID.randomUUID());

        assertEquals(household.getId(), member.getHousehold().getId());
        assertEquals(creator.getId(), member.getUser().getId());
        assertEquals(HouseholdMemberRole.CREATOR, member.getRole());
    }

    @Test
    void taskTableHasAllRequiredColumns() {
        User creator = new User("creator@example.com", "hash");
        creator.setId(UUID.randomUUID());

        User assignee = new User("assignee@example.com", "hash");
        assignee.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);
        household.setId(UUID.randomUUID());

        HouseholdMember assigneeMember = new HouseholdMember(household, assignee, HouseholdMemberRole.PARTNER);
        assigneeMember.setId(UUID.randomUUID());

        Task task = new Task("Clean kitchen", household, assigneeMember, creator);
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
        assertEquals(assigneeMember.getId(), task.getAssignee().getId());
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

        HouseholdMember creatorMember = new HouseholdMember(household, creator, HouseholdMemberRole.CREATOR);
        creatorMember.setId(UUID.randomUUID());

        Task task = new Task("Test", household, creatorMember, creator);
        task.setId(UUID.randomUUID());

        assertNull(task.getDeletedAt());

        LocalDateTime now = LocalDateTime.now();
        task.setDeletedAt(now);
        assertEquals(now, task.getDeletedAt());
    }

    @Test
    void householdInvitationHasCorrectColumns() {
        User creator = new User("creator@example.com", "hash");
        creator.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);
        household.setId(UUID.randomUUID());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

        HouseholdInvitation invitation = new HouseholdInvitation(
                household,
                "partner@example.com",
                token,
                expiresAt
        );
        invitation.setId(UUID.randomUUID());

        assertEquals(household.getId(), invitation.getHousehold().getId());
        assertEquals("partner@example.com", invitation.getInvitedEmail());
        assertEquals(token, invitation.getInvitationToken());
        assertEquals(expiresAt, invitation.getExpiresAt());
        assertFalse(invitation.isAccepted());
        assertNull(invitation.getAcceptedAt());
        assertNull(invitation.getAcceptedByUser());
    }

    @Test
    void householdMemberRoleEnumHasAllValues() {
        assertEquals(2, HouseholdMemberRole.values().length);
        assertTrue(enumValueExists(HouseholdMemberRole.class, "CREATOR"));
        assertTrue(enumValueExists(HouseholdMemberRole.class, "PARTNER"));
    }

    @Test
    void taskCategoryEnumHasAllValues() {
        assertEquals(5, TaskCategory.values().length);
        assertTrue(enumValueExists(TaskCategory.class, "CLEANING"));
        assertTrue(enumValueExists(TaskCategory.class, "SHOPPING"));
        assertTrue(enumValueExists(TaskCategory.class, "LAUNDRY"));
        assertTrue(enumValueExists(TaskCategory.class, "MAINTENANCE"));
        assertTrue(enumValueExists(TaskCategory.class, "BILLS"));
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
