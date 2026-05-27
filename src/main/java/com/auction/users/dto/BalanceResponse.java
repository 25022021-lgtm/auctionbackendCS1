package com.auction.users.dto;

import com.auction.common.BaseResponse;

/** Response DTO containing the user's current balance. */
public class BalanceResponse extends BaseResponse {

  private Double balance;

  public BalanceResponse(boolean status, String message, Double balance) {
    super(status, message);
    this.balance = balance;
  }

  public Double getBalance() {
    return balance;
  }
}
