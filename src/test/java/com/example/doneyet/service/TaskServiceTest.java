package com.example.doneyet.service;

import com.example.doneyet.domain.*;
import com.example.doneyet.dto.TaskDto;
import com.example.doneyet.exception.ForbiddenException;
import com.example.doneyet.exception.NotFoundException;
import com.example.doneyet.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
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
    private UserRepository userRepository;

    private Household testHousehold;
    private User testUser;
    private User otherUser;
    private UUID householdId;
    private UUID userId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("user1@test.com", "password");
        otherUser = new User("user2@test.com", "password");
        userRepository.save(testUser);
        userRepository.save(otherUser);
        userId = testUser.getId();
        otherUserId = otherUser.getId();

        testHousehold = new Household("Test Household", testUser);
        householdRepository.save(testHousehold);
        householdId = testHousehold.getId();
    }

    @Test
    void testCreateTask() {
        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("Test Task");
        request.setDescription("Test description");
        request.setCategory(TaskCategory.CLEANING);
        request.setDueDate(LocalDate.now().plusDays(1));

        TaskDto.TaskResponse response = taskService.createTask(householdId, request, userId);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Test Task", response.getTitle());
        assertEquals("Test description", response.getDescription());
        assertEquals(TaskCategory.CLEANING, response.getCategory());
        assertEquals(householdId, response.getHouseholdId());
    }

    @Test
    void testListTasks() {
        TaskDto.CreateTaskRequest request1 = new TaskDto.CreateTaskRequest();
        request1.setTitle("Task 1");
        request1.setCategory(TaskCategory.CLEANING);
        request1.setDueDate(LocalDate.now().plusDays(1));

        TaskDto.CreateTaskRequest request2 = new TaskDto.CreateTaskRequest();
        request2.setTitle("Task 2");
        request2.setCategory(TaskCategory.SHOPPING);
        request2.setDueDate(LocalDate.now().plusDays(2));

        taskService.createTask(householdId, request1, userId);
        taskService.createTask(householdId, request2, userId);

        List<TaskDto.TaskResponse> tasks = taskService.listTasks(householdId, userId);

        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 1")));
        assertTrue(tasks.stream().anyMatch(t -> t.getTitle().equals("Task 2")));
    }

    @Test
    void testUpdateTask() {
        TaskDto.CreateTaskRequest createRequest = new TaskDto.CreateTaskRequest();
        createRequest.setTitle("Original Title");
        createRequest.setCategory(TaskCategory.CLEANING);
        createRequest.setDueDate(LocalDate.now().plusDays(1));

        TaskDto.TaskResponse created = taskService.createTask(householdId, createRequest, userId);
        UUID taskId = created.getId();

        TaskDto.UpdateTaskRequest updateRequest = new TaskDto.UpdateTaskRequest();
        updateRequest.setTitle("Updated Title");

        TaskDto.TaskResponse updated = taskService.updateTask(taskId, householdId, updateRequest, userId);

        assertEquals("Updated Title", updated.getTitle());
    }

    @Test
    void testDeleteTask() {
        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("Task to Delete");
        request.setCategory(TaskCategory.CLEANING);
        request.setDueDate(LocalDate.now().plusDays(1));

        TaskDto.TaskResponse created = taskService.createTask(householdId, request, userId);
        UUID taskId = created.getId();

        taskService.deleteTask(taskId, householdId, userId);

        List<TaskDto.TaskResponse> tasks = taskService.listTasks(householdId, userId);
        assertFalse(tasks.stream().anyMatch(t -> t.getId().equals(taskId)));
    }

    @Test
    void testAccessDeniedForNonOwner() {
        TaskDto.CreateTaskRequest request = new TaskDto.CreateTaskRequest();
        request.setTitle("Task");
        request.setCategory(TaskCategory.MAINTENANCE);
        request.setDueDate(LocalDate.now().plusDays(1));

        assertThrows(ForbiddenException.class, () ->
            taskService.createTask(householdId, request, otherUserId)
        );
    }
}
