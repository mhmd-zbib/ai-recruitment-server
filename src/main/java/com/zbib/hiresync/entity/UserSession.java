package com.zbib.hiresync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entity to track JWT tokens for security purposes.
 * This does not make the server stateful as it only tracks tokens for security management
 * (revocation, refresh, multi-device logout).
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_sessions")
public class UserSession {
    
    @Id
    @Column(name = "session_id", nullable = false, updatable = false, length = 36)
    private String sessionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "device_info")
    private String deviceInfo;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant createdAt;
    
    @Column(name = "last_used_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Instant lastUsedAt;
    
    @Column(name = "revoked", nullable = false)
    private boolean revoked;
    
    @Column(name = "token_hash", length = 64)
    private String tokenHash;
} 