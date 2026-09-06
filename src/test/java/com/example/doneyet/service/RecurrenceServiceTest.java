package com.example.doneyet.service;

import com.example.doneyet.TestRecurrenceFixtures;
import com.example.doneyet.domain.*;
import com.example.doneyet.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class RecurrenceServiceTest {
    @Mock
    private TaskRepository taskRepository;

    private RecurrenceService recurrenceService;
    private Household household;
    private User user;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        recurrenceService = new RecurrenceService(taskRepository);

        household = new Household();
        household.setId(UUID.randomUUID());
        household.setName("Test Household");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
    }

    @Test
    public void testComputeNextDueDateDaily() {
        Task task = new Task("Test", household, user);
        task.setDueDate(LocalDate.of(2026, 9, 4));
        task.setRecurrenceFrequency(RecurrenceFrequency.DAILY);

        LocalDate nextDueDate = recurrenceService.computeNextDueDate(task);

        assertEquals(LocalDate.of(2026, 9, 5), nextDueDate);
    }

    @Test
    public void testComputeNextDueDateWeekly() {
        Task task = new Task("Test", household, user);
        task.setDueDate(LocalDate.of(2026, 9, 7)); // Monday
        task.setRecurrenceFrequency(RecurrenceFrequency.WEEKLY);

        LocalDate nextDueDate = recurrenceService.computeNextDueDate(task);

        assertEquals(LocalDate.of(2026, 9, 14), nextDueDate); // Next Monday
    }

    @Test
    public void testComputeNextDueDateMonthlySimple() {
        Task task = new Task("Test", household, user);
        task.setDueDate(LocalDate.of(2026, 9, 4));
        task.setRecurrenceFrequency(RecurrenceFrequency.MONTHLY);

        LocalDate nextDueDate = recurrenceService.computeNextDueDate(task);

        assertEquals(LocalDate.of(2026, 10, 4), nextDueDate);
    }

    @Test
    public void testComputeNextDueDateMonthlyEdgeCase() {
        Task task = new Task("Test", household, user);
        task.setDueDate(LocalDate.of(2026, 1, 31));
        task.setRecurrenceFrequency(RecurrenceFrequency.MONTHLY);

        LocalDate nextDueDate = recurrenceService.computeNextDueDate(task);

        // February has 28 days in 2026, so should be Feb 28
        assertEquals(LocalDate.of(2026, 2, 28), nextDueDate);
    }

    @Test
    public void testShouldGenerateNextWithoutEndDate() {
        Task task = new Task("Test", household, user);
        task.setDueDate(LocalDate.of(2026, 9, 4));
        task.setRecurrenceFrequency(RecurrenceFrequency.DAILY);
        task.setRecurrenceEndDate(null);

        assertTrue(recurrenceService.shouldGenerateNext(task));
    }

    @Test
    public void testShouldGenerateNextWithValidEndDate() {
        Task task = new Task("Test", household, user);
        task.setDueDate(LocalDate.of(2026, 9, 4));
        task.setRecurrenceFrequency(RecurrenceFrequency.DAILY);
        task.setRecurrenceEndDate(LocalDate.of(2026, 9, 10));

        assertTrue(recurrenceService.shouldGenerateNext(task));
    }

    @Test
    public void testShouldGenerateNextWithExpiredEndDate() {
        Task task = new Task("Test", household, user);
        task.setDueDate(LocalDate.of(2026, 9, 10));
        task.setRecurrenceFrequency(RecurrenceFrequency.DAILY);
        task.setRecurrenceEndDate(LocalDate.of(2026, 9, 10));

        assertFalse(recurrenceService.shouldGenerateNext(task));
    }

    @Test
    public void testShouldGenerateNextWithoutRecurrence() {
        Task task = new Task("Test", household, user);
        task.setDueDate(LocalDate.of(2026, 9, 4));
        task.setRecurrenceFrequency(null);

        assertFalse(recurrenceService.shouldGenerateNext(task));
    }

    @Test
    public void testGenerateNextInstance() {
        Task parentTask = new Task("Buy groceries", household, user);
        parentTask.setId(UUID.randomUUID());
        parentTask.setDescription("Weekly shopping");
        parentTask.setCategory(TaskCategory.SHOPPING);
        parentTask.setRecurrenceFrequency(RecurrenceFrequency.WEEKLY);
        parentTask.setRecurrenceWeekday(1); // Monday

        LocalDate nextDueDate = LocalDate.of(2026, 9, 14);

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task arg = invocation.getArgument(0);
            arg.setId(UUID.randomUUID());
            return arg;
        });

        Task nextInstance = recurrenceService.generateNextInstance(parentTask, nextDueDate, user);

        assertNotNull(nextInstance);
        assertEquals(parentTask.getId(), nextInstance.getParentTaskId());
        assertEquals(nextDueDate, nextInstance.getDueDate());
        assertEquals("Buy groceries", nextInstance.getTitle());
        assertEquals("Weekly shopping", nextInstance.getDescription());
        assertEquals(TaskCategory.SHOPPING, nextInstance.getCategory());
        assertEquals(RecurrenceFrequency.WEEKLY, nextInstance.getRecurrenceFrequency());
        assertEquals(1, nextInstance.getRecurrenceWeekday());
    }

    @ParameterizedTest(name = "Recurrence {0}")
    @MethodSource("getRecurrenceBoundaryCases")
    public void testComputeNextDueDateMatchesRfc5545Oracle(TestRecurrenceFixtures.RecurrenceTestCase testCase) {
        Task task = new Task("Test", household, user);
        task.setDueDate(testCase.startDate());
        task.setRecurrenceFrequency(testCase.frequency());
        if (testCase.weekday() != null) {
            task.setRecurrenceWeekday(testCase.weekday());
        }

        LocalDate actual = recurrenceService.computeNextDueDate(task);
        LocalDate expected = testCase.expectedNextDate();

        assertEquals(expected, actual, "Mismatch for " + testCase.description());
    }

    private static Stream<TestRecurrenceFixtures.RecurrenceTestCase> getRecurrenceBoundaryCases() {
        return TestRecurrenceFixtures.getRecurrenceBoundaryCases()
                .stream();
    }
}
