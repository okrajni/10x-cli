package com.example.doneyet;

import com.example.doneyet.domain.RecurrenceFrequency;

import java.time.LocalDate;
import java.util.List;

public class TestRecurrenceFixtures {
    public record RecurrenceTestCase(
            LocalDate startDate,
            RecurrenceFrequency frequency,
            Integer weekday,
            LocalDate expectedNextDate,
            String description
    ) {
        @Override
        public String toString() {
            return description;
        }
    }

    public static List<RecurrenceTestCase> getRecurrenceBoundaryCases() {
        return List.of(
            // Daily recurrence - simple case
            new RecurrenceTestCase(
                LocalDate.of(2026, 9, 5),
                RecurrenceFrequency.DAILY,
                null,
                LocalDate.of(2026, 9, 6),
                "Daily: Sep 5 => Sep 6"
            ),
            // Weekly recurrence - Monday to Monday
            new RecurrenceTestCase(
                LocalDate.of(2026, 9, 7), // Monday
                RecurrenceFrequency.WEEKLY,
                1, // Monday
                LocalDate.of(2026, 9, 14),
                "Weekly: Monday Sep 7 => Monday Sep 14"
            ),
            // Monthly recurrence - day 31 to month with 30 days
            new RecurrenceTestCase(
                LocalDate.of(2026, 1, 31),
                RecurrenceFrequency.MONTHLY,
                null,
                LocalDate.of(2026, 2, 28), // February has 28 days in 2026 (not a leap year)
                "Monthly: Jan 31 => Feb 28 (clamped)"
            ),
            // Leap year - Feb 29
            new RecurrenceTestCase(
                LocalDate.of(2024, 2, 29), // Leap year 2024
                RecurrenceFrequency.DAILY,
                null,
                LocalDate.of(2024, 3, 1),
                "Daily: Feb 29 leap year => Mar 1"
            ),
            // Month-end cascade - Jan 31 to Feb to Mar
            new RecurrenceTestCase(
                LocalDate.of(2026, 3, 31),
                RecurrenceFrequency.MONTHLY,
                null,
                LocalDate.of(2026, 4, 30), // April has 30 days
                "Monthly: Mar 31 => Apr 30 (clamped)"
            )
        );
    }
}
