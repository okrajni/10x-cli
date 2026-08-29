package com.example.doneyet.service;

import com.example.doneyet.domain.Household;
import com.example.doneyet.domain.Task;
import com.example.doneyet.domain.User;
import com.example.doneyet.dto.TaskDto;
import com.example.doneyet.exception.ForbiddenException;
import com.example.doneyet.exception.NotFoundException;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.HouseholdMemberRepository;
import com.example.doneyet.repository.HouseholdRepository;
import com.example.doneyet.repository.TaskRepository;
import com.example.doneyet.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository householdMemberRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, HouseholdRepository householdRepository,
                      HouseholdMemberRepository householdMemberRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.householdRepository = householdRepository;
        this.householdMemberRepository = householdMemberRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TaskDto.TaskResponse createTask(UUID householdId, TaskDto.CreateTaskRequest request, UUID userId) {
        validateHouseholdExists(householdId);
        validateUserIsMember(householdId, userId);

        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new NotFoundException("Household not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        Task task = new Task(request.getTitle(), household, user);
        task.setDescription(request.getDescription());
        task.setCategory(request.getCategory());
        task.setDueDate(request.getDueDate());

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskDto.TaskResponse getTask(UUID taskId, UUID householdId, UUID userId) {
        validateUserIsMember(householdId, userId);

        Task task = taskRepository.findByIdAndHouseholdId(taskId, householdId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDto.TaskResponse> listTasks(UUID householdId, UUID userId) {
        validateHouseholdExists(householdId);
        validateUserIsMember(householdId, userId);

        List<Task> tasks = taskRepository.findActiveByHouseholdId(householdId);
        return tasks.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public TaskDto.TaskResponse updateTask(UUID taskId, UUID householdId, TaskDto.UpdateTaskRequest request, UUID userId) {
        validateUserIsMember(householdId, userId);

        Task task = taskRepository.findByIdAndHouseholdId(taskId, householdId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            task.setCategory(request.getCategory());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getCompletedAt() != null) {
            task.setCompletedAt(request.getCompletedAt());
        }

        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID householdId, UUID userId) {
        validateUserIsMember(householdId, userId);

        Task task = taskRepository.findByIdAndHouseholdId(taskId, householdId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private void validateHouseholdExists(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new NotFoundException("Household not found");
        }
    }

    private void validateUserIsMember(UUID householdId, UUID userId) {
        boolean isMember = householdMemberRepository.findByHouseholdIdAndUserId(householdId, userId).isPresent();
        if (!isMember) {
            throw new ForbiddenException("User is not a member of this household");
        }
    }

    private TaskDto.TaskResponse mapToResponse(Task task) {
        return new TaskDto.TaskResponse(
                task.getId(),
                task.getHousehold().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategory(),
                task.getDueDate(),
                task.getCompletedAt(),
                task.getDeletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
