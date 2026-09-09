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

    @Query(value = "SELECT t.* FROM tasks t WHERE t.parent_task_id = :parentTaskId AND t.completed = false AND t.due_date > :dueDateThreshold AND t.deleted_at IS NULL ORDER BY t.due_date ASC LIMIT 1", nativeQuery = true)
    Optional<Task> findByParentTaskIdAndCompletedFalseAndDueDateAfter(@Param("parentTaskId") UUID parentTaskId, @Param("dueDateThreshold") LocalDate dueDateThreshold);
}
