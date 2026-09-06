package com.example.doneyet.repository;

import com.example.doneyet.domain.HouseholdInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HouseholdInvitationRepository extends JpaRepository<HouseholdInvitation, UUID> {
    Optional<HouseholdInvitation> findByInvitationToken(String token);

    Optional<HouseholdInvitation> findByHouseholdIdAndInvitedEmail(UUID householdId, String email);

    List<HouseholdInvitation> findByHouseholdIdAndAcceptedFalse(UUID householdId);

    List<HouseholdInvitation> findByExpiresAtBeforeAndAcceptedFalse(LocalDateTime now);
}
