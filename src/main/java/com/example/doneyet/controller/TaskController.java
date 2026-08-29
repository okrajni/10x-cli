package com.example.doneyet.controller;

import com.example.doneyet.domain.User;
import com.example.doneyet.dto.TaskDto;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.HouseholdMemberRepository;
import com.example.doneyet.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/task")
public class TaskController {
    private final TaskService taskService;
    private final HouseholdMemberRepository householdMemberRepository;

    public TaskController(TaskService taskService, HouseholdMemberRepository householdMemberRepository) {
        this.taskService = taskService;
        this.householdMemberRepository = householdMemberRepository;
    }

    private UUID getUserHouseholdId(UUID userId) {
        return householdMemberRepository.findByUserId(userId)
                .stream()
                .map(member -> member.getHousehold().getId())
                .findFirst()
                .orElseThrow(() -> new ValidationException("User is not associated with a household"));
    }

    @PostMapping
    public ResponseEntity<TaskDto.TaskResponse> createTask(
            @RequestBody TaskDto.CreateTaskRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        UUID householdId = getUserHouseholdId(user.getId());
        TaskDto.TaskResponse response = taskService.createTask(householdId, request, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto.TaskResponse> getTask(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        UUID householdId = getUserHouseholdId(user.getId());
        TaskDto.TaskResponse response = taskService.getTask(id, householdId, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskDto.TaskResponse>> listTasks(
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        UUID householdId = getUserHouseholdId(user.getId());
        List<TaskDto.TaskResponse> response = taskService.listTasks(householdId, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto.TaskResponse> updateTask(
            @PathVariable UUID id,
            @RequestBody TaskDto.UpdateTaskRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        UUID householdId = getUserHouseholdId(user.getId());
        TaskDto.TaskResponse response = taskService.updateTask(id, householdId, request, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        UUID householdId = getUserHouseholdId(user.getId());
        taskService.deleteTask(id, householdId, user.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
