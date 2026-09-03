package com.example.doneyet.controller;

import com.example.doneyet.domain.DomainTask;
import com.example.doneyet.domain.User;
import com.example.doneyet.dto.SuggestionDto;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.HouseholdRepository;
import com.example.doneyet.repository.TaskRepository;
import com.example.doneyet.service.GeminiClient;
import com.example.doneyet.service.SuggestionService;
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
    private final SuggestionService suggestionService;

    @Value("${GOOGLE_API_KEY:}")
    private String googleApiKey;

    public SuggestionsController(GeminiClient geminiClient,
                               HouseholdRepository householdRepository,
                               TaskRepository taskRepository,
                               SuggestionService suggestionService) {
        this.geminiClient = geminiClient;
        this.householdRepository = householdRepository;
        this.taskRepository = taskRepository;
        this.suggestionService = suggestionService;
    }

    @GetMapping("/heuristic")
    public ResponseEntity<SuggestionDto.SuggestionsResponse> getHeuristicSuggestions(
            Authentication authentication
    ) {
        try {
            User user = (User) authentication.getPrincipal();
            UUID householdId = getUserHouseholdId(user.getId());

            List<SuggestionService.SuggestedTaskWithScore> suggestions =
                    suggestionService.getSuggestionsWithScores(householdId);

            List<SuggestionDto.SuggestedTask> suggestedTasks = suggestions.stream()
                    .map(scored -> {
                        SuggestionDto.SuggestedTask task = new SuggestionDto.SuggestedTask();
                        task.setId(scored.getTask().getId());
                        task.setTitle(scored.getTask().getTitle());
                        task.setCategory(scored.getTask().getCategory());
                        task.setFrequencyDays(scored.getTask().getFrequencyDays());
                        task.setScore(scored.getScore());
                        return task;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new SuggestionDto.SuggestionsResponse(suggestedTasks));
        } catch (ValidationException e) {
            return ResponseEntity.badRequest()
                    .body(new SuggestionDto.SuggestionsResponse(List.of()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SuggestionDto.SuggestionsResponse(List.of()));
        }
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
