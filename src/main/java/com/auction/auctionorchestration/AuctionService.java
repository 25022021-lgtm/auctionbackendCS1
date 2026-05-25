package com.auction.auctionorchestration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class AuctionService {
    private final ItemService itemService;
    private final UserService userService;
    private final ItemStatusService itemStatusService;
    private final BidService bidService;
    private final ItemPricesSink itemPricesSink;

    @Value("${extra-time}")
    private Long extraTime;

    public AuctionService(ItemService itemService, UserService userService, ItemStatusService itemStatusService,
            BidService bidService, ItemPricesSink itemPricesSink) {
        this.itemService = itemService;
        this.userService = userService;
        this.itemStatusService = itemStatusService;
        this.bidService = bidService;
        this.itemPricesSink = itemPricesSink;
    }

    @Transactional
    public BidPostResponse createBid(BidPostRequest request, String username) {
        Item item = itemService.getItem(request.itemId());
        User user = userService.getUserByUsername(username);

        ItemStatus itemStatus = itemStatusService.getItemStatus(request.itemId());

        if (itemStatusService.auctionEndedOrNot(request.itemId())) {
            throw new BaseException("Auction has already ended");
        }

        // The seller can't bid on their own item.
        if (username.equals(item.getUser().getUsername())) {
            throw new BaseException("You can't place bids on your own item.");
        }

        // You can only bid if your bid is higher than the current highest bid
        if (request.bidAmount() < itemStatus.getCurrentPrice() + itemStatus.getBidIncrement()) {
            throw new BaseException("You can only bid if your bid is higher than the current highest bid.");
        }

        // Bid amount must be higher than starting price + bid increment
        if (request.bidAmount() < itemStatus.getStartingPrice() + itemStatus.getBidIncrement()) {
            throw new BaseException("Bid amount is below the required minimum (starting price + bid increment)");
        }

        // Calculate how much additional money needs to be locked
        Bid bid;
        double additionalDeduction;
        boolean isExistingBid = bidService.existUserAndItem(user, item);

        if (isExistingBid) {
            bid = bidService.getBidByUserAndItem(user, item);
            additionalDeduction = request.bidAmount() - bid.getBidAmount();
            bid.setBidAmount(request.bidAmount());
        } else {
            bid = new Bid(item, user, request.bidAmount());
            additionalDeduction = request.bidAmount();
        }

        // Check if user has enough balance for the additional amount
        if (additionalDeduction > user.getBalance()) {
            throw new BaseException("Insufficient balance to place bid");
        }

        bidService.saveBid(bid);

        // Deduct the additional amount from user's balance
        user.setBalance(user.getBalance() - additionalDeduction);
        userService.saveUser(user);

        // Refund previous highest bidder (if not the seller and not the current user)
        String prevHighestBidder = itemStatus.getHighestBidUser();
        if (!prevHighestBidder.equals(item.getUser().getUsername())
                && !prevHighestBidder.equals(username)) {
            User prevUser = userService.getUserByUsername(prevHighestBidder);
            prevUser.setBalance(prevUser.getBalance() + itemStatus.getCurrentPrice());
            userService.saveUser(prevUser);
        }

        // Update item status to the current highest bidder
        itemStatus.setHighestBidUser(username);
        itemStatus.setCurrentPrice(request.bidAmount());

        // Anti-sniping: if item has less than extraTime remaining, extend the auction
        Long time = itemStatus.getEndTime();
        Long extraTimes = Long.valueOf(extraTime);
        Long now = Instant.now().toEpochMilli();
        if (time - now < extraTime && time < itemStatus.getMaxEndTime()) {
            itemStatus.setEndTime(now + extraTimes);
        }

        itemStatusService.saveStatus(itemStatus);
        itemPricesSink.publishPrice(request.itemId(), request.bidAmount());
        return new BidPostResponse(true, "Successfully created bid for an item", bid);
    }

    @Transactional(readOnly = true)
    public BaseObjectResponse<Page<Bid>> getMyCurrentBids(String username, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        User userRef = userService.getUserRef(username);

        Page<Bid> bids = bidService.getAllUserBid(userRef, pageable);

        return new BaseObjectResponse<Page<Bid>>(true, "successfully got my bids", bids);
    }

    @Transactional(readOnly = true)
    public BaseObjectResponse<List<BidAndItem>> getMyWinnings(String username) {

        List<Bid> bids = bidService.getUserWins(username);
        ArrayList<BidAndItem> items = new ArrayList<BidAndItem>();
        for (Bid bid : bids) {
            items.add(new BidAndItem(bid, bid.getItem()));
        }
        return new BaseObjectResponse<List<BidAndItem>>(true, "sucesfully returned winnings", items);
    }

    @Transactional
    public BaseResponse buyItemNow(Long itemId, String username) {
        ItemStatus itemStatus = itemStatusService.getItemStatus(itemId);
        User user = userService.getUserByUsername(username);
        Item item = itemService.getItem(itemId);

        if (itemStatusService.auctionEndedOrNot(itemId)) {
            throw new BaseException("This auction has ended");
        }

        // Seller can't buy their own item
        if (username.equals(item.getUser().getUsername())) {
            throw new BaseException("You can't buy your own item.");
        }

        // Buy now if balance >= buyitnow, buyitnow > currentprice
        if (user.getBalance() >= itemStatus.getBuyItNowPrice()
                && itemStatus.getBuyItNowPrice() > itemStatus.getCurrentPrice()) {

            // Refund previous highest bidder (if not the seller)
            String prevHighestBidder = itemStatus.getHighestBidUser();
            if (!prevHighestBidder.equals(item.getUser().getUsername())) {
                User prevUser = userService.getUserByUsername(prevHighestBidder);
                prevUser.setBalance(prevUser.getBalance() + itemStatus.getCurrentPrice());
                userService.saveUser(prevUser);
            }

            // Update bid status
            itemStatus.setHighestBidUser(username);
            itemStatus.setCurrentPrice(itemStatus.getBuyItNowPrice());
            itemStatus.setEndTime(Instant.now().toEpochMilli());
            itemStatusService.saveStatus(itemStatus);

            // Deduct money from buyer's fund
            user.setBalance(user.getBalance() - itemStatus.getBuyItNowPrice());
            userService.saveUser(user);
        } else {
            throw new BaseException("You don't have enough money in your balance to buy the item");
        }
        return new BaseResponse(true, "Successfully bought item");
    }
}
