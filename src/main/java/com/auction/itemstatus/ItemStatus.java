package com.auction.itemstatus;

import com.auction.items.Item;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** Entity representing the auction status and pricing details for an {@link Item}. */
@Entity
@Table(name = "item_statuses")
public class ItemStatus {

  // Put this here so that JPA does not throw an error.
  @Id private Long id;

  @JsonIgnore
  @OneToOne
  @MapsId
  @JoinColumn(name = "item_id")
  private Item item;

  @Column(name = "current_price")
  private Double currentPrice;

  @Column(name = "username")
  private String highestBidUser;

  // We store as Unix time so backend has less of a headache.
  @Column(name = "start_time")
  private Long startTime;

  @Column(name = "end_time")
  private Long endTime;

  @Column(name = "max_end_time")
  private Long maxEndTime;

  @Column(name = "starting_price")
  private Double startingPrice;

  @Column(name = "buy_it_now_price")
  private Double buyItNowPrice;

  @Column(name = "bid_increment")
  private Double bidIncrement;

  @Column(name = "item_status")
  private String itemStatus;

  @PrePersist
  void makeItemActive() {
    this.startTime = Instant.now().toEpochMilli();
    this.itemStatus = "ACTIVE";
  }

  public ItemStatus() {}

  public ItemStatus(
      Item item,
      Double currentPrice,
      String username,
      Long endTime,
      Double startingPrice,
      Double buyItNowPrice,
      Double bidIncrement,
      Long maxEndTime) {
    this.item = item;
    this.currentPrice = currentPrice;
    this.highestBidUser = username;
    this.endTime = endTime;
    this.startingPrice = startingPrice;
    this.buyItNowPrice = buyItNowPrice;
    this.bidIncrement = bidIncrement;
    this.maxEndTime = maxEndTime;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public Double getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(Double currentPrice) {
    this.currentPrice = currentPrice;
  }

  public String getHighestBidUser() {
    return highestBidUser;
  }

  public void setHighestBidUser(String highestBidUser) {
    this.highestBidUser = highestBidUser;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public Double getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(Double startingPrice) {
    this.startingPrice = startingPrice;
  }

  public Double getBuyItNowPrice() {
    return buyItNowPrice;
  }

  public void setBuyItNowPrice(Double buyItNowPrice) {
    this.buyItNowPrice = buyItNowPrice;
  }

  public Double getBidIncrement() {
    return bidIncrement;
  }

  public void setBidIncrement(Double bidIncrement) {
    this.bidIncrement = bidIncrement;
  }

  public String getItemStatus() {
    return itemStatus;
  }

  public void setItemStatus(String itemStatus) {
    this.itemStatus = itemStatus;
  }

  public Long getId() {
    return id;
  }

  public Long getStartTime() {
    return startTime;
  }

  public Long getMaxEndTime() {
    return maxEndTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ItemStatus)) {
      return false;
    }
    ItemStatus that = (ItemStatus) o;
    return id != null && id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ItemStatus{"
        + "id=" + id
        + ", currentPrice=" + currentPrice
        + ", highestBidUser='" + highestBidUser + '\''
        + ", startTime=" + startTime
        + ", endTime=" + endTime
        + ", maxEndTime=" + maxEndTime
        + ", startingPrice=" + startingPrice
        + ", buyItNowPrice=" + buyItNowPrice
        + ", bidIncrement=" + bidIncrement
        + ", itemStatus='" + itemStatus + '\''
        + '}';
  }
}
