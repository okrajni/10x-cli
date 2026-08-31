package com.example.doneyet.repository;

import com.example.doneyet.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.household WHERE t.household.id = :householdId AND t.deletedAt IS NULL ORDER BY t.dueDate ASC")
    List<Task> findActiveByHouseholdId(@Param("householdId") UUID householdId);

    List<Task> findByHouseholdIdAndDeletedAtIsNull(UUID householdId);

    List<Task> findByHouseholdIdAndDueDateAndDeletedAtIsNull(UUID householdId, LocalDate dueDate);

    List<Task> findByHouseholdIdAndCompletedAndDeletedAtIsNull(UUID householdId, boolean completed);

    @Query("SELECT t FROM Task t WHERE t.id = :taskId AND t.household.id = :householdId AND t.deletedAt IS NULL")
    Optional<Task> findByIdAndHouseholdId(@Param("taskId") UUID taskId, @Param("householdId") UUID householdId);

    @Query("SELECT t FROM Task t WHERE t.id = :id AND t.parentTaskId IS NULL AND t.deletedAt IS NULL")
    Optional<Task> findByIdAndParentTaskIdIsNull(@Param("id") UUID id);

    @Query("SELECT t FROM Task t WHERE t.parentTaskId = :parentTaskId AND t.deletedAt IS NULL ORDER BY t.dueDate ASC")
    List<Task> findByParentTaskIdOrderByDueDateAsc(@Param("parentTaskId") UUID parentTaskId);

    @Query("SELECT t FROM Task t WHERE t.parentTaskId = :parentTaskId AND t.completed = false AND t.dueDate > :dueDateThreshold AND t.deletedAt IS NULL ORDER BY t.dueDate ASC LIMIT 1")
    Optional<Task> findByParentTaskIdAndCompletedFalseAndDueDateAfter(@Param("parentTaskId") UUID parentTaskId, @Param("dueDateThreshold") LocalDate dueDateThreshold);
}
