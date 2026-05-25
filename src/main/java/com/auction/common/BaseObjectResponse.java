package com.auction.common;

/** Generic response DTO that extends {@link BaseResponse} with an entity payload. */
public class BaseObjectResponse<T> extends BaseResponse {

  private T entity;

  public BaseObjectResponse(boolean status, String message, T entity) {
    super(status, message);
    this.entity = entity;
  }

  public T getEntity() {
    return entity;
  }
}
