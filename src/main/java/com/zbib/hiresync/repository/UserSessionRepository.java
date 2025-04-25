package com.zbib.hiresync.repository;

import com.zbib.hiresync.entity.User;
import com.zbib.hiresync.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    Optional<UserSession> findBySessionId(String sessionId);
    
    Optional<UserSession> findBySessionIdAndUser(String sessionId, User user);
    
    List<UserSession> findByUserAndRevokedFalse(User user);
    
    List<UserSession> findByUser(User user);
} 