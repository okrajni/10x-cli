package com.example.doneyet.service;

import com.example.doneyet.domain.RecurrenceFrequency;
import com.example.doneyet.domain.TaskCategory;
import com.example.doneyet.dto.SuggestionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeminiClientTest {
    private GeminiClient geminiClient;

    @BeforeEach
    void setUp() {
        geminiClient = new GeminiClient();
    }

    private String wrapInGeminiResponse(String suggestionsJson) {
        return """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "%s"
                          }
                        ]
                      }
                    }
                  ]
                }
                """.formatted(suggestionsJson.replace("\"", "\\\"").replace("\n", "\\n"));
    }

    @Test
    void testParseValidGeminiResponse() throws Exception {
        String suggestionsJson = """
                [
                  {
                    "title": "Clean kitchen",
                    "description": "Deep clean kitchen surfaces",
                    "category": "CLEANING",
                    "dueDate": "2026-09-16",
                    "recurrenceFrequency": "WEEKLY",
                    "recurrenceWeekday": 0,
                    "recurrenceEndDate": "2027-09-02"
                  },
                  {
                    "title": "Grocery shopping",
                    "category": "SHOPPING",
                    "dueDate": "2026-09-09"
                  }
                ]
                """;

        String mockResponse = wrapInGeminiResponse(suggestionsJson);
        List<SuggestionDto.SuggestedTask> suggestions = geminiClient.parseGeminiResponse(mockResponse);

        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());

        SuggestionDto.SuggestedTask first = suggestions.get(0);
        assertEquals("Clean kitchen", first.getTitle());
        assertEquals(TaskCategory.CLEANING, first.getCategory());
        assertEquals(LocalDate.of(2026, 9, 16), first.getDueDate());
        assertEquals(RecurrenceFrequency.WEEKLY, first.getRecurrenceFrequency());
        assertEquals(0, first.getRecurrenceWeekday());

        SuggestionDto.SuggestedTask second = suggestions.get(1);
        assertEquals("Grocery shopping", second.getTitle());
        assertEquals(TaskCategory.SHOPPING, second.getCategory());
    }

    @Test
    void testParseResponseWithInvalidCategory() throws Exception {
        String suggestionsJson = """
                [
                  {
                    "title": "Invalid category task",
                    "category": "INVALID_CATEGORY",
                    "dueDate": "2026-09-16"
                  }
                ]
                """;
        String mockResponse = wrapInGeminiResponse(suggestionsJson);

        assertThrows(Exception.class, () -> geminiClient.parseGeminiResponse(mockResponse));
    }

    @Test
    void testParseResponseWithMissingTitle() throws Exception {
        String suggestionsJson = """
                [
                  {
                    "category": "CLEANING",
                    "dueDate": "2026-09-16"
                  }
                ]
                """;
        String mockResponse = wrapInGeminiResponse(suggestionsJson);

        assertThrows(RuntimeException.class, () -> geminiClient.parseGeminiResponse(mockResponse));
    }

    @Test
    void testParseResponseWithMissingCategory() throws Exception {
        String suggestionsJson = """
                [
                  {
                    "title": "Task without category",
                    "dueDate": "2026-09-16"
                  }
                ]
                """;
        String mockResponse = wrapInGeminiResponse(suggestionsJson);

        assertThrows(RuntimeException.class, () -> geminiClient.parseGeminiResponse(mockResponse));
    }

    @Test
    void testParseResponseWithPastDueDate() throws Exception {
        String suggestionsJson = """
                [
                  {
                    "title": "Past task",
                    "category": "CLEANING",
                    "dueDate": "2020-01-01"
                  }
                ]
                """;
        String mockResponse = wrapInGeminiResponse(suggestionsJson);

        assertThrows(RuntimeException.class, () -> geminiClient.parseGeminiResponse(mockResponse));
    }

    @Test
    void testParseResponseWithInvalidWeekday() throws Exception {
        String suggestionsJson = """
                [
                  {
                    "title": "Weekly task",
                    "category": "CLEANING",
                    "dueDate": "2026-09-16",
                    "recurrenceFrequency": "WEEKLY",
                    "recurrenceWeekday": 7
                  }
                ]
                """;
        String mockResponse = wrapInGeminiResponse(suggestionsJson);

        assertThrows(RuntimeException.class, () -> geminiClient.parseGeminiResponse(mockResponse));
    }

    @Test
    void testParseResponseWithWeekdayOnlyForWeeklyRecurrence() throws Exception {
        String suggestionsJson = """
                [
                  {
                    "title": "Daily task with weekday",
                    "category": "CLEANING",
                    "dueDate": "2026-09-16",
                    "recurrenceFrequency": "DAILY",
                    "recurrenceWeekday": 3
                  }
                ]
                """;
        String mockResponse = wrapInGeminiResponse(suggestionsJson);

        List<SuggestionDto.SuggestedTask> suggestions = geminiClient.parseGeminiResponse(mockResponse);

        assertEquals(1, suggestions.size());
        SuggestionDto.SuggestedTask task = suggestions.get(0);
        assertEquals(RecurrenceFrequency.DAILY, task.getRecurrenceFrequency());
        assertNull(task.getRecurrenceWeekday());
    }

    @Test
    void testParseResponseNoValidJson() throws Exception {
        String mockResponse = "This is not JSON";

        assertThrows(Exception.class, () -> geminiClient.parseGeminiResponse(mockResponse));
    }

    @Test
    void testGenerateTaskSuggestionsNoApiKey() {
        assertThrows(
                IllegalArgumentException.class,
                () -> geminiClient.generateTaskSuggestions("Test household", new ArrayList<>(), null)
        );
    }

    @Test
    void testGenerateTaskSuggestionsEmptyDescription() {
        assertThrows(
                IllegalArgumentException.class,
                () -> geminiClient.generateTaskSuggestions("", new ArrayList<>(), "api-key")
        );
    }

    @Test
    void testParseResponseWithDefaultDueDate() throws Exception {
        String suggestionsJson = """
                [
                  {
                    "title": "Task without due date",
                    "category": "CLEANING"
                  }
                ]
                """;
        String mockResponse = wrapInGeminiResponse(suggestionsJson);

        List<SuggestionDto.SuggestedTask> suggestions = geminiClient.parseGeminiResponse(mockResponse);

        assertEquals(1, suggestions.size());
        SuggestionDto.SuggestedTask task = suggestions.get(0);
        assertNotNull(task.getDueDate());
        assertTrue(task.getDueDate().isAfter(LocalDate.now()));
    }

    @Test
    void testParseResponseWithRecurrenceEndDate() throws Exception {
        String suggestionsJson = """
                [
                  {
                    "title": "Weekly task with end",
                    "category": "MAINTENANCE",
                    "dueDate": "2026-09-16",
                    "recurrenceFrequency": "WEEKLY",
                    "recurrenceWeekday": 2,
                    "recurrenceEndDate": "2027-09-02"
                  }
                ]
                """;
        String mockResponse = wrapInGeminiResponse(suggestionsJson);

        List<SuggestionDto.SuggestedTask> suggestions = geminiClient.parseGeminiResponse(mockResponse);

        assertEquals(1, suggestions.size());
        SuggestionDto.SuggestedTask task = suggestions.get(0);
        assertEquals(LocalDate.of(2027, 9, 2), task.getRecurrenceEndDate());
    }

    @Test
    void testParseResponseWithAllCategories() throws Exception {
        String suggestionsJson = """
                [
                  {"title": "Cleaning task", "category": "CLEANING", "dueDate": "2026-09-16"},
                  {"title": "Shopping task", "category": "SHOPPING", "dueDate": "2026-09-16"},
                  {"title": "Laundry task", "category": "LAUNDRY", "dueDate": "2026-09-16"},
                  {"title": "Maintenance task", "category": "MAINTENANCE", "dueDate": "2026-09-16"},
                  {"title": "Bills task", "category": "BILLS", "dueDate": "2026-09-16"}
                ]
                """;
        String mockResponse = wrapInGeminiResponse(suggestionsJson);

        List<SuggestionDto.SuggestedTask> suggestions = geminiClient.parseGeminiResponse(mockResponse);

        assertEquals(5, suggestions.size());
        assertTrue(suggestions.stream().map(SuggestionDto.SuggestedTask::getCategory)
                .allMatch(cat -> cat != null));
    }
}
