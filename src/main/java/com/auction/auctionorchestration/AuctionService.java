package com.auction.auctionorchestration;

import com.auction.auctionorchestration.dto.BidPostRequest;
import com.auction.auctionorchestration.dto.BidPostResponse;
import com.auction.bids.Bid;
import com.auction.bids.BidService;
import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.common.ItemPricesSink;
import com.auction.common.jointdata.BidAndItem;
import com.auction.items.Item;
import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service orchestrating auction operations: creating bids, buying now, and retrieving results. */
@Service
public class AuctionService {

  private final ItemService itemService;
  private final UserService userService;
  private final ItemStatusService itemStatusService;
  private final BidService bidService;
  private final ItemPricesSink itemPricesSink;

  @Value("${extra_time}")
  private Long extraTime;

  public AuctionService(
      ItemService itemService,
      UserService userService,
      ItemStatusService itemStatusService,
      BidService bidService,
      ItemPricesSink itemPricesSink) {
    this.itemService = itemService;
    this.userService = userService;
    this.itemStatusService = itemStatusService;
    this.bidService = bidService;
    this.itemPricesSink = itemPricesSink;
  }

  @Transactional
  public BidPostResponse createBid(BidPostRequest request, String username) {
    Bid bid;
    Item item = itemService.getItemRef(request.itemId());
    User user = userService.getUserRef(username);

    ItemStatus itemStatus = itemStatusService.getItemStatus(request.itemId());

    if (itemStatusService.auctionEndedOrNot(request.itemId())) {
      throw new BaseException("Auction has already ended");
    }

    // You can only bid if your bid is higher than the current highest bid
    if (request.bidAmount() < itemStatus.getCurrentPrice() + itemStatus.getBidIncrement()) {
      throw new BaseException(
          "You can only bid if your bid is higher than the current highest bid.");
    }

    // The seller can't bid on their own item.
    if (username.equals(item.getUser().getUsername())) {
      throw new BaseException("You can't place bids on your own item.");
    }

    // Bid amount must be higher than starting price.
    // Bid amount must be smaller or equal to current balance.
    if (request.bidAmount()
        < itemStatus.getStartingPrice() + itemStatus.getBidIncrement()) {
      throw new BaseException(
          "Bid amount is below the required minimum (starting price + bid increment)");
    } else if (request.bidAmount() + itemStatus.getBidIncrement() >= user.getBalance()) {
      throw new BaseException("Insufficient balance to place bid");
    }

    // If bid exists then get bid from DB and then edit bid and save it again to DB.
    if (bidService.existUserAndItem(user, item)) {
      bid = bidService.getBidByUserAndItem(user, item);
      bid.setBidAmount(request.bidAmount());
      bidService.saveBid(bid);
    } else {
      // Else make new bid
      bid = new Bid(item, user, request.bidAmount());
      bidService.saveBid(bid);
    }

    user.setBalance(
        user.getBalance() - (itemStatus.getCurrentPrice() + itemStatus.getBidIncrement()));
    userService.saveUser(user);

    // If the former highest bid user is not the seller (when the item was first published,
    // the seller would be the current highest bidder), the former highest bidder would be
    // refunded.
    if (!itemStatus.getHighestBidUser().equals(item.getUser().getUsername())) {
      User prevUser = userService.getUserByUsername(itemStatus.getHighestBidUser());
      prevUser.setBalance(prevUser.getBalance() + itemStatus.getCurrentPrice());
      userService.saveUser(prevUser);
    }

    // Update item status in repository to the current highest bidder.
    itemStatus.setHighestBidUser(username);
    itemStatus.setCurrentPrice(request.bidAmount());

    // Anti-sniping: if item has less than 5 mins, extend the auction by 5 more minutes.
    Long time = itemStatus.getEndTime();
    Long extraTimes = Long.valueOf(extraTime); // this is 5 mins
    Long now = Instant.now().toEpochMilli();
    if (time - now < extraTime && time < itemStatus.getMaxEndTime()) {
      itemStatus.setEndTime(now + extraTimes);
    }

    itemStatusService.saveStatus(itemStatus);
    itemPricesSink.publishPrice(request.itemId(), request.bidAmount());
    return new BidPostResponse(true, "Successfully created bid for an item", bid);
  }

  @Transactional(readOnly = true)
  public BaseObjectResponse<Page<Bid>> getMyCurrentBids(
      String username, int page, int size) {
    PageRequest pageable = PageRequest.of(page, size);
    User userRef = userService.getUserRef(username);

    Page<Bid> bids = bidService.getAllUserBid(userRef, pageable);

    return new BaseObjectResponse<>(true, "Successfully got my bids", bids);
  }

  @Transactional(readOnly = true)
  public BaseObjectResponse<List<BidAndItem>> getMyWinnings(String username) {
    List<Bid> bids = bidService.getUserWins(username);
    ArrayList<BidAndItem> items = new ArrayList<>();
    for (Bid bid : bids) {
      items.add(new BidAndItem(bid, bid.getItem()));
    }
    return new BaseObjectResponse<>(true, "Successfully returned winnings", items);
  }

  @Transactional
  public BaseResponse buyItemNow(Long itemId, String username) {
    ItemStatus itemStatus = itemStatusService.getItemStatus(itemId);
    User user = userService.getUserByUsername(username);
    if (itemStatusService.auctionEndedOrNot(itemId)) {
      throw new BaseException("This auction has ended");
    }
    // Buy now if balance >= buyItNowPrice, buyItNowPrice > currentPrice
    if (user.getBalance() >= itemStatus.getBuyItNowPrice()
        && itemStatus.getBuyItNowPrice() > itemStatus.getCurrentPrice()) {

      // Update bid status
      itemStatus.setHighestBidUser(username);
      itemStatus.setCurrentPrice(itemStatus.getBuyItNowPrice());
      itemStatus.setEndTime(Instant.now().toEpochMilli());
      itemStatusService.saveStatus(itemStatus);

      // Deduct money from user's fund
      user.setBalance(user.getBalance() - itemStatus.getBuyItNowPrice());
      userService.saveUser(user);
    } else {
      throw new BaseException(
          "You don't have enough money in your balance to buy the item");
    }
    return new BaseResponse(true, "Successfully bought item");
  }
}
