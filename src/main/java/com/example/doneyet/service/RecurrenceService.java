package com.example.doneyet.service;

import com.example.doneyet.domain.RecurrenceFrequency;
import com.example.doneyet.domain.Task;
import com.example.doneyet.domain.User;
import com.example.doneyet.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class RecurrenceService {
    private final TaskRepository taskRepository;

    public RecurrenceService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public LocalDate computeNextDueDate(Task currentTask) {
        if (currentTask.getRecurrenceFrequency() == null) {
            return null;
        }

        LocalDate currentDueDate = currentTask.getDueDate();
        if (currentDueDate == null) {
            return null;
        }

        return switch (currentTask.getRecurrenceFrequency()) {
            case DAILY -> currentDueDate.plusDays(1);
            case WEEKLY -> getNextWeekday(currentDueDate, currentTask.getRecurrenceWeekday());
            case MONTHLY -> addMonths(currentDueDate, 1);
        };
    }

    private LocalDate getNextWeekday(LocalDate currentDate, Integer targetWeekday) {
        if (targetWeekday == null) {
            return currentDate.plusDays(7);
        }
        int currentDayOfWeek = currentDate.getDayOfWeek().getValue() % 7; // Convert to 0-6 (Sun=0)
        int daysUntilTarget = (targetWeekday - currentDayOfWeek + 7) % 7;
        // If target is today, move to next week
        if (daysUntilTarget == 0) {
            daysUntilTarget = 7;
        }
        return currentDate.plusDays(daysUntilTarget);
    }

    public Task generateNextInstance(Task parentTask, LocalDate nextDueDate, User createdBy) {
        Task nextInstance = new Task(parentTask.getTitle(), parentTask.getHousehold(), createdBy);
        nextInstance.setDescription(parentTask.getDescription());
        nextInstance.setCategory(parentTask.getCategory());
        nextInstance.setDueDate(nextDueDate);
        nextInstance.setParentTaskId(parentTask.getId());
        nextInstance.setRecurrenceFrequency(parentTask.getRecurrenceFrequency());
        nextInstance.setRecurrenceEndDate(parentTask.getRecurrenceEndDate());
        nextInstance.setRecurrenceWeekday(parentTask.getRecurrenceWeekday());

        return taskRepository.save(nextInstance);
    }

    public boolean shouldGenerateNext(Task task) {
        if (task.getRecurrenceFrequency() == null) {
            return false;
        }

        LocalDate nextDueDate = computeNextDueDate(task);
        if (nextDueDate == null) {
            return false;
        }

        if (task.getRecurrenceEndDate() != null && nextDueDate.isAfter(task.getRecurrenceEndDate())) {
            return false;
        }

        return true;
    }

    private LocalDate addMonths(LocalDate date, int months) {
        YearMonth yearMonth = YearMonth.from(date);
        YearMonth nextYearMonth = yearMonth.plusMonths(months);
        int originalDay = date.getDayOfMonth();
        int maxDayOfMonth = nextYearMonth.lengthOfMonth();
        return nextYearMonth.atDay(Math.min(originalDay, maxDayOfMonth));
    }
}
