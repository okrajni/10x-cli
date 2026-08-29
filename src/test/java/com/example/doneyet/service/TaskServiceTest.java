package com.example.doneyet.service;

import com.example.doneyet.domain.*;
import com.example.doneyet.dto.TaskDto;
import com.example.doneyet.exception.ForbiddenException;
import com.example.doneyet.exception.NotFoundException;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TaskServiceTest {
    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private HouseholdMemberRepository householdMemberRepository;

    @Autowired
    private UserRepository userRepository;

    private Household testHousehold;
    private User user1;
    private User user2;
    private HouseholdMember member1;
    private HouseholdMember member2;
    private UUID householdId;
    private UUID user1Id;
    private UUID user2Id;
    private UUID member1Id;
    private UUID member2Id;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        householdMemberRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();

        user1 = new User("user1@test.com", "password");
        user2 = new User("user2@test.com", "password");
        userRepository.save(user1);
        userRepository.save(user2);
        user1Id = user1.getId();
        user2Id = user2.getId();

        testHousehold = new Household("Test Household", user1);
        householdRepository.save(testHousehold);
        householdId = testHousehold.getId();

        member1 = new HouseholdMember(testHousehold, user1, HouseholdMemberRole.CREATOR);
        member2 = new HouseholdMember(testHousehold, user2, HouseholdMemberRole.PARTNER);
        householdMemberRepository.save(member1);
        householdMemberRepository.save(member2);
        member1Id = member1.getId();
        member2Id = member2.getId();
    }

    @Test
    void testCreateTaskWithExplicitAssigneeId() {
        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("Test Task");
        request.setCategory(TaskCategory.CLEANING);
        request.setDueDate(LocalDate.now().plusDays(1));
        request.setAssigneeId(member2Id);

        TaskDto.TaskResponse response = taskService.createTask(householdId, request, user1Id);

        assertNotNull(response);
        assertEquals("Test Task", response.getTitle());
        assertNotNull(response.getAssignee());
        assertEquals(member2Id, response.getAssignee().getId());
        assertEquals("user2@test.com", response.getAssignee().getEmail());
    }

    @Test
    void testCreateTaskWithoutAssigneeIdDefaultsToCreator() {
        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("Default Assignee Task");
        request.setCategory(TaskCategory.SHOPPING);
        request.setDueDate(LocalDate.now().plusDays(2));
        request.setAssigneeId(null);

        TaskDto.TaskResponse response = taskService.createTask(householdId, request, user1Id);

        assertNotNull(response);
        assertEquals("Default Assignee Task", response.getTitle());
        assertNotNull(response.getAssignee());
        assertEquals(member1Id, response.getAssignee().getId());
        assertEquals("user1@test.com", response.getAssignee().getEmail());
    }

    @Test
    void testCreateTaskWithInvalidAssigneeIdThrowsValidationException() {
        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("Invalid Assignee Task");
        request.setCategory(TaskCategory.LAUNDRY);
        request.setDueDate(LocalDate.now().plusDays(3));
        request.setAssigneeId(UUID.randomUUID());

        assertThrows(ValidationException.class, () ->
            taskService.createTask(householdId, request, user1Id)
        );
    }

    @Test
    void testUpdateTaskWithValidReassignment() {
        Task task = new Task("Task to Reassign", testHousehold, member1, user1);
        taskRepository.save(task);
        UUID taskId = task.getId();

        TaskDto.UpdateTaskRequest request = new TaskDto.UpdateTaskRequest();
        request.setAssigneeId(member2Id);

        TaskDto.TaskResponse response = taskService.updateTask(taskId, householdId, request, user1Id);

        assertNotNull(response);
        assertNotNull(response.getAssignee());
        assertEquals(member2Id, response.getAssignee().getId());
        assertEquals("user2@test.com", response.getAssignee().getEmail());
    }

    @Test
    void testUpdateTaskWithInvalidAssigneeIdThrowsValidationException() {
        Task task = new Task("Task to Fail", testHousehold, member1, user1);
        taskRepository.save(task);
        UUID taskId = task.getId();

        TaskDto.UpdateTaskRequest request = new TaskDto.UpdateTaskRequest();
        request.setAssigneeId(UUID.randomUUID());

        assertThrows(ValidationException.class, () ->
            taskService.updateTask(taskId, householdId, request, user1Id)
        );
    }

    @Test
    void testCreateTaskUserNotMemberThrowsForbiddenException() {
        User outsideUser = new User("outside@test.com", "password");
        userRepository.save(outsideUser);

        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("Task");
        request.setCategory(TaskCategory.MAINTENANCE);
        request.setDueDate(LocalDate.now().plusDays(1));

        assertThrows(ForbiddenException.class, () ->
            taskService.createTask(householdId, request, outsideUser.getId())
        );
    }
}
