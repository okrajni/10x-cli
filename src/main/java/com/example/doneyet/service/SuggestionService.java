package com.example.doneyet.service;

import com.example.doneyet.domain.DomainTask;
import com.example.doneyet.domain.DomainTasks;
import com.example.doneyet.domain.Season;
import com.example.doneyet.domain.Task;
import com.example.doneyet.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class SuggestionService {
    private final TaskRepository taskRepository;

    public SuggestionService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<DomainTask> getSuggestions(UUID householdId) {
        List<Task> householdTasks = taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdId);

        return DomainTasks.DOMAIN_TASKS.stream()
                .map(domainTask -> new ScoredTask(domainTask, scoreTask(domainTask, householdTasks)))
                .filter(scored -> scored.score >= 0.8)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(5)
                .map(scored -> scored.task)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SuggestedTaskWithScore> getSuggestionsWithScores(UUID householdId) {
        List<Task> householdTasks = taskRepository.findByHouseholdIdAndDeletedAtIsNull(householdId);

        return DomainTasks.DOMAIN_TASKS.stream()
                .map(domainTask -> new ScoredTask(domainTask, scoreTask(domainTask, householdTasks)))
                .filter(scored -> scored.score >= 0.8)
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(5)
                .map(scored -> new SuggestedTaskWithScore(scored.task, scored.score))
                .toList();
    }

    private double scoreTask(DomainTask domainTask, List<Task> householdTasks) {
        LocalDateTime lastCompletion = findLastCompletion(domainTask, householdTasks);
        int daysSince = calculateDaysSince(lastCompletion);

        boolean isInSeason = isInSeason(domainTask.getSeason());
        double seasonalBonus = isInSeason ? 1.0 : 0.2;

        double overdueFactor = Math.max(0, (double) daysSince / domainTask.getFrequencyDays());

        return overdueFactor * seasonalBonus;
    }

    private LocalDateTime findLastCompletion(DomainTask domainTask, List<Task> householdTasks) {
        return householdTasks.stream()
                .filter(task -> task.isCompleted() && matchesTitle(domainTask.getTitle(), task.getTitle()))
                .map(Task::getCompletedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private boolean matchesTitle(String domainTitle, String taskTitle) {
        return taskTitle.toLowerCase().contains(domainTitle.toLowerCase());
    }

    private int calculateDaysSince(LocalDateTime lastCompletion) {
        if (lastCompletion == null) {
            return 999;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(lastCompletion.toLocalDate(), LocalDate.now());
    }

    private boolean isInSeason(Season season) {
        if (season == null) {
            return true;
        }

        int currentMonth = LocalDate.now().getMonthValue();

        return switch (season) {
            case SPRING -> currentMonth >= 3 && currentMonth <= 5;
            case SUMMER -> currentMonth >= 6 && currentMonth <= 8;
            case FALL -> currentMonth >= 9 && currentMonth <= 11;
            case WINTER -> currentMonth == 12 || currentMonth == 1 || currentMonth == 2;
        };
    }

    private static class ScoredTask {
        final DomainTask task;
        final double score;

        ScoredTask(DomainTask task, double score) {
            this.task = task;
            this.score = score;
        }
    }

    public static class SuggestedTaskWithScore {
        private final DomainTask task;
        private final double score;

        public SuggestedTaskWithScore(DomainTask task, double score) {
            this.task = task;
            this.score = score;
        }

        public DomainTask getTask() {
            return task;
        }

        public double getScore() {
            return score;
        }
    }
}
