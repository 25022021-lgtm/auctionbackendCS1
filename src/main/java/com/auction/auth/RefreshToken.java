package com.auction.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @Column(name = "username")
    private String username;

    @Column(name = "token")
    private String refreshToken;

    @Column(name = "created_at")
    private Long createdAt;

    @PrePersist
    @PreUpdate
    void addTime() {
        this.createdAt = Instant.now().toEpochMilli();
    }

    protected RefreshToken() {
    }

    public RefreshToken(String username, String refreshToken) {
        this.username = username;
        this.refreshToken = refreshToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

}
