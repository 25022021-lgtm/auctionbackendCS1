package com.auction.common;

/** Base runtime exception that wraps a {@link BaseResponse} for API error handling. */
public class BaseException extends RuntimeException {

  private final BaseResponse response;

  public BaseException(String message) {
    super(message);
    this.response = new BaseResponse(false, message);
  }

  public BaseResponse getResponse() {
    return response;
  }
}
