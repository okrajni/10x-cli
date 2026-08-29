package com.example.doneyet.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import com.example.doneyet.domain.HouseholdMember;
import com.example.doneyet.domain.HouseholdMemberRole;

class EntityRelationshipTest {

    @Test
    void householdCanHaveMultipleTasks() {
        User creator = new User("creator@example.com", "hash");
        creator.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);
        household.setId(UUID.randomUUID());

        HouseholdMember creatorMember = new HouseholdMember(household, creator, HouseholdMemberRole.CREATOR);
        creatorMember.setId(UUID.randomUUID());

        Task task1 = new Task("Task 1", household, creatorMember, creator);
        task1.setId(UUID.randomUUID());

        Task task2 = new Task("Task 2", household, creatorMember, creator);
        task2.setId(UUID.randomUUID());

        household.getTasks().add(task1);
        household.getTasks().add(task2);

        assertEquals(2, household.getTasks().size());
    }

    @Test
    void householdCanHaveMultipleMembers() {
        User creator = new User("creator@example.com", "hash");
        creator.setId(UUID.randomUUID());

        User partner = new User("partner@example.com", "hash");
        partner.setId(UUID.randomUUID());

        Household household = new Household("Test Household", creator);
        household.setId(UUID.randomUUID());

        HouseholdMember creatorMember = new HouseholdMember(household, creator, HouseholdMemberRole.CREATOR);
        creatorMember.setId(UUID.randomUUID());

        HouseholdMember partnerMember = new HouseholdMember(household, partner, HouseholdMemberRole.PARTNER);
        partnerMember.setId(UUID.randomUUID());

        household.getMembers().add(creatorMember);
        household.getMembers().add(partnerMember);

        assertEquals(2, household.getMembers().size());
    }

    @Test
    void householdMemberLinksCorrectUserAndHousehold() {
        User user = new User("user@example.com", "hash");
        Household household = new Household("Test Household", user);
        HouseholdMember member = new HouseholdMember(household, user, HouseholdMemberRole.CREATOR);

        assertEquals(household, member.getHousehold());
        assertEquals(user, member.getUser());
        assertEquals(HouseholdMemberRole.CREATOR, member.getRole());
    }

    @Test
    void taskAssigneeReferenceIsCorrect() {
        User assignee = new User("assignee@example.com", "hash");
        User creator = new User("creator@example.com", "hash");
        Household household = new Household("Test Household", creator);
        HouseholdMember assigneeMember = new HouseholdMember(household, assignee, HouseholdMemberRole.PARTNER);
        Task task = new Task("Test Task", household, assigneeMember, creator);

        assertEquals(assigneeMember, task.getAssignee());
        assertEquals(household, task.getHousehold());
    }

    @Test
    void taskSoftDeleteFieldExists() {
        User creator = new User("creator@example.com", "hash");
        Household household = new Household("Test Household", creator);
        HouseholdMember creatorMember = new HouseholdMember(household, creator, HouseholdMemberRole.CREATOR);
        Task task = new Task("Test Task", household, creatorMember, creator);

        assertNull(task.getDeletedAt());

        LocalDateTime deletedTime = LocalDateTime.now();
        task.setDeletedAt(deletedTime);

        assertEquals(deletedTime, task.getDeletedAt());
    }

    @Test
    void householdInvitationHasExpiryAndToken() {
        User creator = new User("creator@example.com", "hash");
        Household household = new Household("Test Household", creator);

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        String token = UUID.randomUUID().toString();
        HouseholdInvitation invitation = new HouseholdInvitation(
                household,
                "partner@example.com",
                token,
                expiresAt
        );

        assertEquals("partner@example.com", invitation.getInvitedEmail());
        assertEquals(token, invitation.getInvitationToken());
        assertEquals(expiresAt, invitation.getExpiresAt());
        assertFalse(invitation.isAccepted());
    }

    @Test
    void taskFieldsMapCorrectly() {
        User creator = new User("creator@example.com", "hash");
        User assignee = new User("assignee@example.com", "hash");
        Household household = new Household("Test Household", creator);
        HouseholdMember assigneeMember = new HouseholdMember(household, assignee, HouseholdMemberRole.PARTNER);

        Task task = new Task("Clean kitchen", household, assigneeMember, creator);
        task.setDescription("Clean the kitchen thoroughly");
        task.setCategory(TaskCategory.CLEANING);
        task.setDueDate(LocalDate.now().plusDays(1));
        task.setReminderTime(LocalTime.of(10, 0));
        task.setCompleted(true);
        task.setCompletedBy(assignee);
        task.setCompletedAt(LocalDateTime.now());

        assertEquals("Clean kitchen", task.getTitle());
        assertEquals("Clean the kitchen thoroughly", task.getDescription());
        assertEquals(TaskCategory.CLEANING, task.getCategory());
        assertTrue(task.isCompleted());
    }
}
