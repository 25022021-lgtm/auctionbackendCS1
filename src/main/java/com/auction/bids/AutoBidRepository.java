package com.auction.bids;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AutoBidRepository extends JpaRepository<AutoBid, Long> {

    Optional<AutoBid> findByItemId(Long itemId);
}

