package com.auction.items.dto;

import com.auction.common.BaseResponse;
import com.auction.items.Item;
import java.util.List;

/** Response DTO containing a list of all {@link Item} entities. */
public class GetItemsResponse extends BaseResponse {

  private final List<Item> items;

  public GetItemsResponse(Boolean status, String message, List<Item> items) {
    super(status, message);
    this.items = items;
  }

  public List<Item> getItems() {
    return items;
  }
}
