package com.auction.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    @Query(value = "SELECT t FROM RefreshToken t WHERE t.refreshToken = :token")
    Optional<RefreshToken> findRefreshTokenData(@Param("token") String token);
}
