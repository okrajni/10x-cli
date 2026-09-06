package com.example.doneyet.repository;

import com.example.doneyet.domain.HouseholdMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, UUID> {
    Optional<HouseholdMember> findByHouseholdIdAndUserId(UUID householdId, UUID userId);

    List<HouseholdMember> findByHouseholdId(UUID householdId);

    List<HouseholdMember> findByUserId(UUID userId);

    long countByHouseholdId(UUID householdId);
}
