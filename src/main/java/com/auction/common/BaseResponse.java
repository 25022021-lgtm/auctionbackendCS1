package com.auction.common;

/** Base response DTO containing status and message fields for API responses. */
public class BaseResponse {

  private boolean status;
  private String message;

  /** Empty constructor so that Jackson can create and use the set methods to inject data in. */
  public BaseResponse() {}

  public BaseResponse(boolean status, String message) {
    this.status = status;
    this.message = message;
  }

  public boolean getStatus() {
    return status;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
