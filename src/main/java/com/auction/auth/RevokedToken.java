package com.auction.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @Column(name = "username")
    private String username;

    @Column(name = "banned_at", nullable = false)
    private Long bannedAt;

    protected RevokedToken() {
    }

    public RevokedToken(String username, Long bannedAt) {
        this.username = username;
        this.bannedAt = bannedAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(Long bannedAt) {
        this.bannedAt = bannedAt;
    }
}
