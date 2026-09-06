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
    @Query("SELECT h FROM Household h WHERE h.createdBy.id = :userId")
    List<Household> findByCreatedBy(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT h FROM Household h JOIN HouseholdMember hm ON h.id = hm.household.id WHERE hm.user.id = :userId")
    List<Household> findHouseholdsByMemberId(@Param("userId") UUID userId);
}
