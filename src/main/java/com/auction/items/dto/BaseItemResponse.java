package com.auction.items.dto;

import com.auction.common.BaseResponse;
import com.auction.items.Item;

/** Response DTO containing a single {@link Item} entity. */
public class BaseItemResponse extends BaseResponse {

  private final Item item;

  public BaseItemResponse(boolean status, String message, Item item) {
    super(status, message);
    this.item = item;
  }

  // Need getters here so that Spring Boot can access them.
  public Item getItem() {
    return item;
  }
}