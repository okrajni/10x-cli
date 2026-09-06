package com.example.doneyet.repository;

import com.example.doneyet.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.user.id = ?1")
    void deleteByUserId(UUID userId);
}
