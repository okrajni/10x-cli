package com.example.doneyet.controller;

import com.example.doneyet.domain.User;
import com.example.doneyet.dto.TaskDto;
import com.example.doneyet.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/household/{householdId}/task")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskDto.TaskResponse> createTask(
            @PathVariable UUID householdId,
            @RequestBody TaskDto.CreateTaskRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        TaskDto.TaskResponse response = taskService.createTask(householdId, request, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDto.TaskResponse> getTask(
            @PathVariable UUID householdId,
            @PathVariable UUID id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        TaskDto.TaskResponse response = taskService.getTask(id, householdId, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskDto.TaskResponse>> listTasks(
            @PathVariable UUID householdId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        List<TaskDto.TaskResponse> response = taskService.listTasks(householdId, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto.TaskResponse> updateTask(
            @PathVariable UUID householdId,
            @PathVariable UUID id,
            @RequestBody TaskDto.UpdateTaskRequest request,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        TaskDto.TaskResponse response = taskService.updateTask(id, householdId, request, user.getId());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID householdId,
            @PathVariable UUID id,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        taskService.deleteTask(id, householdId, user.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
