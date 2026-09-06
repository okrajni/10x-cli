package com.example.doneyet.repository;

import com.example.doneyet.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByHouseholdIdAndDeletedAtIsNull(UUID householdId);

    List<Task> findByHouseholdIdAndAssigneeIdAndDeletedAtIsNull(UUID householdId, UUID assigneeId);

    List<Task> findByHouseholdIdAndDueDateAndDeletedAtIsNull(UUID householdId, LocalDate dueDate);

    List<Task> findByHouseholdIdAndCompletedAndDeletedAtIsNull(UUID householdId, boolean completed);

    Optional<Task> findByIdAndHouseholdId(UUID id, UUID householdId);
}
