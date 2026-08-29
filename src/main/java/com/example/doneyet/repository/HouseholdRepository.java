package com.example.doneyet.repository;

import com.example.doneyet.domain.Household;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HouseholdRepository extends JpaRepository<Household, UUID> {
    List<Household> findByCreatedById(UUID userId);
    boolean existsByCreatedById(UUID userId);
}
