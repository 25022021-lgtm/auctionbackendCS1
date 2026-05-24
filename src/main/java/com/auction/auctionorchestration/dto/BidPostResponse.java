package com.auction.auctionorchestration.dto;

import com.auction.bids.Bid;
import com.auction.common.BaseResponse;

/** Response DTO for a bid placement operation, containing the created {@link Bid}. */
public class BidPostResponse extends BaseResponse {

  private final Bid bid;

  public BidPostResponse(boolean status, String message, Bid bid) {
    super(status, message);
    this.bid = bid;
  }

  public Bid getBid() {
    return bid;
  }
}
