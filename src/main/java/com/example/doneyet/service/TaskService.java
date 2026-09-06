package com.example.doneyet.service;

import com.example.doneyet.domain.Household;
import com.example.doneyet.domain.Task;
import com.example.doneyet.domain.User;
import com.example.doneyet.dto.TaskDto;
import com.example.doneyet.exception.ForbiddenException;
import com.example.doneyet.exception.NotFoundException;
import com.example.doneyet.exception.ValidationException;
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
    private final UserRepository userRepository;
    private final RecurrenceService recurrenceService;

    public TaskService(TaskRepository taskRepository, HouseholdRepository householdRepository,
                      UserRepository userRepository, RecurrenceService recurrenceService) {
        this.taskRepository = taskRepository;
        this.householdRepository = householdRepository;
        this.userRepository = userRepository;
        this.recurrenceService = recurrenceService;
    }

    @Transactional
    public TaskDto.TaskResponse createTask(UUID householdId, TaskDto.CreateTaskRequest request, UUID userId) {
        validateHouseholdOwner(householdId, userId);

        validateRecurrenceFields(request.getRecurrenceFrequency(), request.getRecurrenceWeekday());

        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new NotFoundException("Household not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        Task task = new Task(request.getTitle(), household, user);
        task.setDescription(request.getDescription());
        task.setCategory(request.getCategory());
        task.setDueDate(request.getDueDate());
        task.setRecurrenceFrequency(request.getRecurrenceFrequency());
        task.setRecurrenceEndDate(request.getRecurrenceEndDate());
        task.setRecurrenceWeekday(request.getRecurrenceWeekday());

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskDto.TaskResponse getTask(UUID taskId, UUID householdId, UUID userId) {
        validateHouseholdOwner(householdId, userId);

        Task task = taskRepository.findByIdAndHouseholdId(taskId, householdId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDto.TaskResponse> listTasks(UUID householdId, UUID userId) {
        validateHouseholdOwner(householdId, userId);

        List<Task> tasks = taskRepository.findActiveByHouseholdId(householdId);
        return tasks.stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public TaskDto.TaskResponse updateTask(UUID taskId, UUID householdId, TaskDto.UpdateTaskRequest request, UUID userId) {
        validateHouseholdOwner(householdId, userId);

        validateRecurrenceFields(request.getRecurrenceFrequency(), request.getRecurrenceWeekday());

        Task task = taskRepository.findByIdAndHouseholdId(taskId, householdId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (request.getCompletedAt() != null) {
            throw new IllegalArgumentException("Task completed flag and completedAt timestamp must be consistent; use PUT /task/{id}/complete endpoint instead");
        }

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
        if (request.getRecurrenceFrequency() != null) {
            task.setRecurrenceFrequency(request.getRecurrenceFrequency());
        }
        if (request.getRecurrenceEndDate() != null) {
            task.setRecurrenceEndDate(request.getRecurrenceEndDate());
        }
        if (request.getRecurrenceWeekday() != null) {
            task.setRecurrenceWeekday(request.getRecurrenceWeekday());
        }

        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID householdId, UUID userId) {
        validateHouseholdOwner(householdId, userId);

        Task task = taskRepository.findByIdAndHouseholdId(taskId, householdId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
    }

    private void validateHouseholdOwner(UUID householdId, UUID userId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new NotFoundException("Household not found"));
        if (!household.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("User is not the owner of this household");
        }
    }

    private void validateRecurrenceFields(com.example.doneyet.domain.RecurrenceFrequency frequency, Integer weekday) {
        if (frequency == com.example.doneyet.domain.RecurrenceFrequency.WEEKLY && weekday == null) {
            throw new ValidationException("Weekday is required for WEEKLY recurrence");
        }
        if (weekday != null && (weekday < 0 || weekday > 6)) {
            throw new ValidationException("Weekday must be between 0 (Sunday) and 6 (Saturday)");
        }
    }

    @Transactional
    public TaskDto.TaskResponse completeTask(UUID taskId, UUID householdId, UUID userId) {
        validateHouseholdOwner(householdId, userId);

        Task task = taskRepository.findByIdAndHouseholdId(taskId, householdId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        if (task.isCompleted()) {
            return mapToResponse(task);
        }

        task.setCompleted(true);
        task.setCompletedAt(LocalDateTime.now());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));
        task.setCompletedBy(user);

        Task completedTask = taskRepository.save(task);

        // Generate next instance if this is a parent recurring task or an instance of one
        Task parentTask = task; // Assume this is the parent
        if (task.getParentTaskId() != null) {
            // This is an instance of a recurring task, fetch the parent
            parentTask = taskRepository.findById(task.getParentTaskId())
                    .orElseThrow(() -> new NotFoundException("Parent task not found"));
        }

        // Generate next instance if the parent has recurrence and should continue
        if (parentTask.getRecurrenceFrequency() != null && recurrenceService.shouldGenerateNext(completedTask)) {
            java.time.LocalDate nextDueDate = recurrenceService.computeNextDueDate(completedTask);
            recurrenceService.generateNextInstance(parentTask, nextDueDate, user);
        }

        return mapToResponse(completedTask);
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
                task.getUpdatedAt(),
                task.getParentTaskId(),
                task.getRecurrenceFrequency(),
                task.getRecurrenceEndDate(),
                task.getRecurrenceWeekday()
        );
    }
}
