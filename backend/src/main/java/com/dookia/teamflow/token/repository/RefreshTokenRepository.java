package com.dookia.teamflow.token.repository;

import com.dookia.teamflow.token.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Replay Detection: 재사용이 감지된 family 의 모든 토큰을 삭제해 세션 전체를 무효화한다.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.familyId = :familyId")
    void deleteByFamilyId(@Param("familyId") String familyId);

    /**
     * 만료 토큰 일괄 정리용 (Phase 4 스케줄러에서 사용 예정).
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expireDate < :now")
    int deleteAllExpiredBefore(@Param("now") LocalDateTime now);
}
