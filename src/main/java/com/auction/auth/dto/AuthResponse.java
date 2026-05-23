package com.auction.auth.dto;

import com.auction.common.BaseResponse;

public class AuthResponse extends BaseResponse {
    private String accessToken;
    private String refreshToken;

    public AuthResponse(boolean status, String message, String accessToken, String refreshToken) {

        super(status, message);
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}