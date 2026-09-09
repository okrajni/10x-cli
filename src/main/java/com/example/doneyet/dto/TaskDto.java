package com.example.doneyet.dto;

import com.example.doneyet.domain.RecurrenceFrequency;
import com.example.doneyet.domain.TaskCategory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class TaskDto {

    public static class CreateTaskRequest {
        private String title;
        private String description;
        private TaskCategory category;
        private LocalDate dueDate;
        private RecurrenceFrequency recurrenceFrequency;
        private LocalDate recurrenceEndDate;
        private Integer recurrenceWeekday;

        public CreateTaskRequest() {
        }

        public CreateTaskRequest(String title, TaskCategory category, LocalDate dueDate) {
            this.title = title;
            this.category = category;
            this.dueDate = dueDate;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public TaskCategory getCategory() {
            return category;
        }

        public void setCategory(TaskCategory category) {
            this.category = category;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
        }

        public RecurrenceFrequency getRecurrenceFrequency() {
            return recurrenceFrequency;
        }

        public void setRecurrenceFrequency(RecurrenceFrequency recurrenceFrequency) {
            this.recurrenceFrequency = recurrenceFrequency;
        }

        public LocalDate getRecurrenceEndDate() {
            return recurrenceEndDate;
        }

        public void setRecurrenceEndDate(LocalDate recurrenceEndDate) {
            this.recurrenceEndDate = recurrenceEndDate;
        }

        public Integer getRecurrenceWeekday() {
            return recurrenceWeekday;
        }

        public void setRecurrenceWeekday(Integer recurrenceWeekday) {
            this.recurrenceWeekday = recurrenceWeekday;
        }
    }

    public static class UpdateTaskRequest {
        private String title;
        private String description;
        private TaskCategory category;
        private LocalDate dueDate;
        private LocalDateTime completedAt;
        private RecurrenceFrequency recurrenceFrequency;
        private LocalDate recurrenceEndDate;
        private Integer recurrenceWeekday;

        public UpdateTaskRequest() {
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public TaskCategory getCategory() {
            return category;
        }

        public void setCategory(TaskCategory category) {
            this.category = category;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
        }

        public RecurrenceFrequency getRecurrenceFrequency() {
            return recurrenceFrequency;
        }

        public void setRecurrenceFrequency(RecurrenceFrequency recurrenceFrequency) {
            this.recurrenceFrequency = recurrenceFrequency;
        }

        public LocalDate getRecurrenceEndDate() {
            return recurrenceEndDate;
        }

        public void setRecurrenceEndDate(LocalDate recurrenceEndDate) {
            this.recurrenceEndDate = recurrenceEndDate;
        }

        public Integer getRecurrenceWeekday() {
            return recurrenceWeekday;
        }

        public void setRecurrenceWeekday(Integer recurrenceWeekday) {
            this.recurrenceWeekday = recurrenceWeekday;
        }
    }

    public static class TaskResponse {
        private UUID id;
        private UUID householdId;
        private String title;
        private String description;
        private TaskCategory category;
        private LocalDate dueDate;
        private LocalDateTime completedAt;
        private LocalDateTime deletedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private UUID parentTaskId;
        private RecurrenceFrequency recurrenceFrequency;
        private LocalDate recurrenceEndDate;
        private Integer recurrenceWeekday;

        public TaskResponse() {
        }

        public TaskResponse(UUID id, UUID householdId, String title, String description, TaskCategory category, LocalDate dueDate, LocalDateTime completedAt, LocalDateTime deletedAt, LocalDateTime createdAt, LocalDateTime updatedAt, UUID parentTaskId, RecurrenceFrequency recurrenceFrequency, LocalDate recurrenceEndDate, Integer recurrenceWeekday) {
            this.id = id;
            this.householdId = householdId;
            this.title = title;
            this.description = description;
            this.category = category;
            this.dueDate = dueDate;
            this.completedAt = completedAt;
            this.deletedAt = deletedAt;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.parentTaskId = parentTaskId;
            this.recurrenceFrequency = recurrenceFrequency;
            this.recurrenceEndDate = recurrenceEndDate;
            this.recurrenceWeekday = recurrenceWeekday;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public UUID getHouseholdId() {
            return householdId;
        }

        public void setHouseholdId(UUID householdId) {
            this.householdId = householdId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public TaskCategory getCategory() {
            return category;
        }

        public void setCategory(TaskCategory category) {
            this.category = category;
        }

        public LocalDate getDueDate() {
            return dueDate;
        }

        public void setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(LocalDateTime completedAt) {
            this.completedAt = completedAt;
        }

        public LocalDateTime getDeletedAt() {
            return deletedAt;
        }

        public void setDeletedAt(LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public UUID getParentTaskId() {
            return parentTaskId;
        }

        public void setParentTaskId(UUID parentTaskId) {
            this.parentTaskId = parentTaskId;
        }

        public RecurrenceFrequency getRecurrenceFrequency() {
            return recurrenceFrequency;
        }

        public void setRecurrenceFrequency(RecurrenceFrequency recurrenceFrequency) {
            this.recurrenceFrequency = recurrenceFrequency;
        }

        public LocalDate getRecurrenceEndDate() {
            return recurrenceEndDate;
        }

        public void setRecurrenceEndDate(LocalDate recurrenceEndDate) {
            this.recurrenceEndDate = recurrenceEndDate;
        }

        public Integer getRecurrenceWeekday() {
            return recurrenceWeekday;
        }

        public void setRecurrenceWeekday(Integer recurrenceWeekday) {
            this.recurrenceWeekday = recurrenceWeekday;
        }
    }
}
