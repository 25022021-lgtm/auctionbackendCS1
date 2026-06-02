package com.auction.bids;

import java.time.Instant;

import com.auction.users.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name="autobids")
public class AutoBid {
    @Id 
    @JsonIgnore
    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "max_bid_limit")
    private Double maxBidLimit;
    @Column(name = "current_bid_value")
    private Double currentBidValue;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "bidder_username")
    private User bidder;

    @Column(name = "bid_time")
    private Long time;

    @PrePersist
    void addTime() {
        time = Instant.now().toEpochMilli();
    }
    public AutoBid() {};
    public AutoBid(Long itemId, User bidder ,Double maxBidLimit, Double currentBidValue) {
        this.itemId = itemId;
        this.maxBidLimit = maxBidLimit;
        this.currentBidValue = currentBidValue;
        this.bidder = bidder;
    }

    public Double getCurrentBidValue() {
        return currentBidValue;
    }
    public void setCurrentBidValue(Double currentBidValue) {
        this.currentBidValue = currentBidValue;
    }
    public Long getItemId() {
        return itemId;
    }
    public Double getMaxBidLimit() {
        return maxBidLimit;
    }

    public User getBidder() {
        return bidder;
    }

    public Long getTime() {
        return time;
    }

    public void setMaxBidLimit(Double maxBidLimit) {
        this.maxBidLimit = maxBidLimit;
    }

    
}
