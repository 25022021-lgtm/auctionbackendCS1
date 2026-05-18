package com.auction.common;

public class BaseException extends RuntimeException {
    BaseResponse response;

    public BaseException(String message) {
        super(message);
        this.response = new BaseResponse(false, message);
    }
}
