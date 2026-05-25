package com.auction.items.dto;

import com.auction.common.BaseResponse;
import com.auction.items.Item;
import org.springframework.data.domain.Page;

/** Response DTO containing a paginated list of {@link Item} entities. */
public class GetItemPagesResponse extends BaseResponse {

  private final Page<Item> pages;

  public GetItemPagesResponse(boolean status, String message, Page<Item> pages) {
    super(status, message);
    this.pages = pages;
  }

  public Page<Item> getPages() {
    return pages;
  }
}
