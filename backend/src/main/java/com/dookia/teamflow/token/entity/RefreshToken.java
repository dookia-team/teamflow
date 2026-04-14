package com.dookia.teamflow.token.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * REFRESH_TOKEN 엔티티. ERD v0.1 §1 을 따른다.
 * - token_hash : 원문이 아닌 SHA-256 해시
 * - family_id  : Token Rotation 계열 식별 (UUID 문자열)
 * - used       : Replay Detection 플래그
 *
 * ERD 주석상 최종 저장소는 Redis 로 가정하나, Sprint 1 범위에서는 RDB 로 구현한다.
 */
@Entity
@Table(
    name = "refresh_token",
    indexes = @Index(name = "idx_rt_family", columnList = "family_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no")
    private Long no;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "family_id", nullable = false, length = 64)
    private String familyId;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "user_agent", nullable = false, length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "expire_date", nullable = false)
    private LocalDateTime expireDate;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    void onCreate() {
        if (createDate == null) {
            createDate = LocalDateTime.now();
        }
    }

    public void markUsed() {
        this.used = true;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireDate);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
