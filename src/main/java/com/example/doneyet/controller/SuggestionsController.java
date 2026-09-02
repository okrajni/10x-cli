package com.example.doneyet.controller;

import com.example.doneyet.domain.User;
import com.example.doneyet.dto.SuggestionDto;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.HouseholdRepository;
import com.example.doneyet.repository.TaskRepository;
import com.example.doneyet.service.GeminiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionsController {
    private final GeminiClient geminiClient;
    private final HouseholdRepository householdRepository;
    private final TaskRepository taskRepository;

    @Value("${GOOGLE_API_KEY:}")
    private String googleApiKey;

    public SuggestionsController(GeminiClient geminiClient,
                               HouseholdRepository householdRepository,
                               TaskRepository taskRepository) {
        this.geminiClient = geminiClient;
        this.householdRepository = householdRepository;
        this.taskRepository = taskRepository;
    }

    @PostMapping
    public ResponseEntity<?> generateSuggestions(
            @RequestBody SuggestionDto.SuggestionsRequest request,
            Authentication authentication
    ) {
        try {
            if (googleApiKey == null || googleApiKey.isBlank()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ErrorResponse("GOOGLE_API_KEY is not configured"));
            }

            User user = (User) authentication.getPrincipal();
            UUID householdId = getUserHouseholdId(user.getId());

            String householdDescription = request.getHouseholdDescription();
            if (householdDescription == null || householdDescription.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Household description cannot be empty"));
            }

            List<String> existingTaskTitles = taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdId)
                    .stream()
                    .map(task -> task.getTitle())
                    .collect(Collectors.toList());

            List<SuggestionDto.SuggestedTask> suggestions = geminiClient.generateTaskSuggestions(
                    householdDescription,
                    existingTaskTitles,
                    googleApiKey
            );

            return ResponseEntity.ok(new SuggestionDto.SuggestionsResponse(suggestions));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to generate suggestions: " + e.getMessage()));
        }
    }

    private UUID getUserHouseholdId(UUID userId) {
        return householdRepository.findByCreatedById(userId)
                .stream()
                .map(household -> household.getId())
                .findFirst()
                .orElseThrow(() -> new ValidationException("User does not have a household"));
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
