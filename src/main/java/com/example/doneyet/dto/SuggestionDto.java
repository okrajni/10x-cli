package com.example.doneyet.dto;

import com.example.doneyet.domain.RecurrenceFrequency;
import com.example.doneyet.domain.TaskCategory;
import java.time.LocalDate;
import java.util.List;

public class SuggestionDto {

    public static class SuggestionsRequest {
        private String householdDescription;

        public SuggestionsRequest() {
        }

        public SuggestionsRequest(String householdDescription) {
            this.householdDescription = householdDescription;
        }

        public String getHouseholdDescription() {
            return householdDescription;
        }

        public void setHouseholdDescription(String householdDescription) {
            this.householdDescription = householdDescription;
        }
    }

    public static class SuggestedTask {
        private String title;
        private String description;
        private TaskCategory category;
        private LocalDate dueDate;
        private RecurrenceFrequency recurrenceFrequency;
        private Integer recurrenceWeekday;
        private LocalDate recurrenceEndDate;

        public SuggestedTask() {
        }

        public SuggestedTask(String title, String description, TaskCategory category,
                           LocalDate dueDate, RecurrenceFrequency recurrenceFrequency,
                           Integer recurrenceWeekday, LocalDate recurrenceEndDate) {
            this.title = title;
            this.description = description;
            this.category = category;
            this.dueDate = dueDate;
            this.recurrenceFrequency = recurrenceFrequency;
            this.recurrenceWeekday = recurrenceWeekday;
            this.recurrenceEndDate = recurrenceEndDate;
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

        public Integer getRecurrenceWeekday() {
            return recurrenceWeekday;
        }

        public void setRecurrenceWeekday(Integer recurrenceWeekday) {
            this.recurrenceWeekday = recurrenceWeekday;
        }

        public LocalDate getRecurrenceEndDate() {
            return recurrenceEndDate;
        }

        public void setRecurrenceEndDate(LocalDate recurrenceEndDate) {
            this.recurrenceEndDate = recurrenceEndDate;
        }
    }

    public static class SuggestionsResponse {
        private List<SuggestedTask> suggestions;

        public SuggestionsResponse() {
        }

        public SuggestionsResponse(List<SuggestedTask> suggestions) {
            this.suggestions = suggestions;
        }

        public List<SuggestedTask> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<SuggestedTask> suggestions) {
            this.suggestions = suggestions;
        }
    }
}
