package com.example.doneyet.service;

import com.example.doneyet.domain.RecurrenceFrequency;
import com.example.doneyet.domain.TaskCategory;
import com.example.doneyet.dto.SuggestionDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiClient {
    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiClient() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public List<SuggestionDto.SuggestedTask> generateTaskSuggestions(
            String householdDescription,
            List<String> existingTaskTitles,
            String apiKey
    ) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("GOOGLE_API_KEY is not configured");
        }

        if (householdDescription == null || householdDescription.isBlank()) {
            throw new IllegalArgumentException("Household description cannot be empty");
        }

        String prompt = buildPrompt(householdDescription, existingTaskTitles);

        try {
            Map<String, Object> request = buildGeminiRequest(prompt);
            String responseBody = callGeminiApi(request, apiKey);
            logger.debug("Gemini response: {}", responseBody);
            return parseGeminiResponse(responseBody);
        } catch (Exception e) {
            logger.error("Error calling Gemini API", e);
            throw new RuntimeException("Failed to generate task suggestions", e);
        }
    }

    private Map<String, Object> buildGeminiRequest(String prompt) {
        Map<String, Object> request = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();

        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);

        content.put("parts", parts);
        contents.add(content);

        request.put("contents", contents);
        return request;
    }

    private String callGeminiApi(Map<String, Object> request, String apiKey) throws Exception {
        String url = GEMINI_API_URL + "?key=" + apiKey;
        String requestBody = objectMapper.writeValueAsString(request);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(
                requestBody,
                new org.springframework.http.HttpHeaders() {{
                    setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                }}
        );

        org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Gemini API call failed with status " + response.getStatusCode());
        }

        return response.getBody();
    }

    private String buildPrompt(String householdDescription, List<String> existingTaskTitles) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a household task planning assistant. Given a household description and existing tasks, ");
        prompt.append("suggest 3-5 recurring tasks that would help keep the household running smoothly.\n\n");

        prompt.append("Household: ").append(householdDescription).append("\n\n");

        prompt.append("Existing tasks (do not duplicate):\n");
        if (existingTaskTitles != null && !existingTaskTitles.isEmpty()) {
            for (String title : existingTaskTitles) {
                prompt.append("- ").append(title).append("\n");
            }
        } else {
            prompt.append("- None\n");
        }

        prompt.append("\nSuggest tasks in the following categories: CLEANING, SHOPPING, LAUNDRY, MAINTENANCE, BILLS\n\n");

        prompt.append("Return a JSON array with this schema:\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"title\": \"string\",\n");
        prompt.append("    \"description\": \"string (optional)\",\n");
        prompt.append("    \"category\": \"CLEANING | SHOPPING | LAUNDRY | MAINTENANCE | BILLS\",\n");
        prompt.append("    \"dueDate\": \"YYYY-MM-DD (14 days from now as baseline)\",\n");
        prompt.append("    \"recurrenceFrequency\": \"DAILY | WEEKLY | MONTHLY (optional)\",\n");
        prompt.append("    \"recurrenceWeekday\": \"0-6 (only if WEEKLY, where 0=Sunday)\",\n");
        prompt.append("    \"recurrenceEndDate\": \"YYYY-MM-DD (optional, 1 year from now)\"\n");
        prompt.append("  }\n");
        prompt.append("]\n");
        prompt.append("Ensure suggestions are realistic, non-obvious, and diverse across categories. ");
        prompt.append("Return ONLY the JSON array, no additional text.");

        return prompt.toString();
    }

    protected List<SuggestionDto.SuggestedTask> parseGeminiResponse(String responseBody) throws Exception {
        List<SuggestionDto.SuggestedTask> suggestions = new ArrayList<>();

        Map<String, Object> response = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");

        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No candidates in Gemini response");
        }

        Map<String, Object> candidate = candidates.get(0);
        Map<String, Object> content = (Map<String, Object>) candidate.get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        String text = (String) parts.get(0).get("text");

        String jsonText = extractJsonArray(text);

        List<SuggestionDto.SuggestedTask> parsed = objectMapper.readValue(
                jsonText,
                new TypeReference<List<SuggestionDto.SuggestedTask>>() {
                }
        );

        for (SuggestionDto.SuggestedTask task : parsed) {
            try {
                validateAndFixTaskSuggestion(task);
                suggestions.add(task);
            } catch (IllegalArgumentException e) {
                logger.warn("Skipping invalid suggestion: {}", e.getMessage());
            }
        }

        if (suggestions.isEmpty()) {
            throw new RuntimeException("No valid suggestions were generated");
        }

        return suggestions;
    }

    private String extractJsonArray(String responseText) throws Exception {
        Pattern pattern = Pattern.compile("\\[.*\\]", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseText);

        if (!matcher.find()) {
            throw new RuntimeException("Could not find JSON array in Gemini response");
        }

        return matcher.group();
    }

    private void validateAndFixTaskSuggestion(SuggestionDto.SuggestedTask task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title is required");
        }

        if (task.getCategory() == null) {
            throw new IllegalArgumentException("Task category is required");
        }

        validateTaskCategory(task.getCategory());

        if (task.getDueDate() == null) {
            task.setDueDate(LocalDate.now().plusDays(14));
        } else {
            validateDueDate(task.getDueDate());
        }

        if (task.getRecurrenceFrequency() != null && task.getRecurrenceFrequency() == RecurrenceFrequency.WEEKLY) {
            if (task.getRecurrenceWeekday() == null) {
                task.setRecurrenceWeekday(0);
            } else if (task.getRecurrenceWeekday() < 0 || task.getRecurrenceWeekday() > 6) {
                throw new IllegalArgumentException("RecurrenceWeekday must be 0-6");
            }
        } else if (task.getRecurrenceWeekday() != null &&
                   (task.getRecurrenceFrequency() == null || task.getRecurrenceFrequency() != RecurrenceFrequency.WEEKLY)) {
            task.setRecurrenceWeekday(null);
        }

        if (task.getRecurrenceEndDate() == null && task.getRecurrenceFrequency() != null) {
            task.setRecurrenceEndDate(LocalDate.now().plusYears(1));
        }
    }

    private void validateTaskCategory(TaskCategory category) {
        try {
            TaskCategory.valueOf(category.name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid task category: " + category);
        }
    }

    private void validateDueDate(LocalDate dueDate) {
        if (dueDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Due date must not be in the past");
        }
    }
}
