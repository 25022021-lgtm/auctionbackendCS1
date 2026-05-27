package com.auction.bids;

import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.items.Item;
import com.auction.users.User;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing bid operations. */
@Service
public class BidService {

  private final BidRepository bidRepository;

  public BidService(BidRepository bidRepository) {
    this.bidRepository = bidRepository;
  }

  @Transactional(readOnly = true)
  public BaseObjectResponse<Page<Bid>> getBidsOnItem(Long itemId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("bidAmount"));
    Page<Bid> items = bidRepository.findItemBidHistory(pageable, itemId);
    return new BaseObjectResponse<>(true, "Successfully retrieved all bids", items);
  }

  @Transactional(readOnly = true)
  public boolean existUserAndItem(User user, Item item) {
    return bidRepository.existsByUserAndItem(user, item);
  }

  @Transactional
  public Bid saveBid(Bid bid) {
    bid = bidRepository.save(bid);
    return bid;
  }

  @Transactional
  public Bid getBidByUserAndItem(User user, Item item) {
    Bid bid =
        bidRepository
            .findByUserAndItem(user, item)
            .orElseThrow(() -> new BaseException("Unable to find user or item"));
    return bid;
  }

  @Transactional(readOnly = true)
  public Page<Bid> getAllUserBid(User userRef, Pageable pageable) {
    Page<Bid> bids = bidRepository.findAllByUser(userRef, pageable);
    return bids;
  }

  @Transactional
  public List<Bid> getUserWins(String username) {
    List<Bid> bids = bidRepository.getWinsByUser(username, Instant.now().toEpochMilli());
    return bids;
  }
}
