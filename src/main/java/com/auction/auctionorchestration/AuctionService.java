package com.auction.auctionorchestration;

import com.auction.admin.AdminController;
import com.auction.config.SecurityConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.auctionorchestration.dto.AutoBidRequest;
import com.auction.auctionorchestration.dto.BidPostRequest;
import com.auction.auctionorchestration.dto.BidPostResponse;
import com.auction.bids.AutoBid;
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
    private final AdminController adminController;
    private final SecurityConfig securityConfig;
    private final ItemService itemService;
    private final UserService userService;
    private final ItemStatusService itemStatusService;
    private final BidService bidService;
    private final ItemPricesSink itemPricesSink;

    @Value("${extra_time}")
    private Long extraTime;

    public AuctionService(ItemService itemService, UserService userService, ItemStatusService itemStatusService,
            BidService bidService, ItemPricesSink itemPricesSink, SecurityConfig securityConfig, AdminController adminController) {
        this.itemService = itemService;
        this.userService = userService;
        this.itemStatusService = itemStatusService;
        this.bidService = bidService;
        this.itemPricesSink = itemPricesSink;
        this.securityConfig = securityConfig;
        this.adminController = adminController;
    }

    @Transactional
    public BidPostResponse createBid(BidPostRequest request, String username) {
        Bid bid;
        Item item = itemService.getItemRef(request.itemId());
        User user = userService.getUserRef(username);

        ItemStatus itemStatus = itemStatusService.getItemStatus(request.itemId());

        validateAuctionNotEnded(request.itemId());

        // You can only bid if your bid is higher than the current highest bid
        if (request.bidAmount() < itemStatus.getCurrentPrice() + itemStatus.getBidIncrement()) {
            throw new BaseException("You can only bid if your bid is higher than the current highest bid.");
        }

        // The seller can't bid on their own item.
        if (username.equals(item.getUser().getUsername())) {
            throw new BaseException("You can't place bids on your own item.");
        }

        // big amount must be higher than starting price
        // bid amount must be smaller or equals to than current balance
        boolean isBidBelowMinimum = request.bidAmount() < itemStatus.getStartingPrice() + itemStatus.getBidIncrement();
        boolean isBidExceedsBalance = request.bidAmount() + itemStatus.getBidIncrement() >= user.getBalance();

        if (isBidBelowMinimum) {
            throw new BaseException("Bid amount is below the required minimum (starting price + bid increment)");
        } else if (isBidExceedsBalance) {
            throw new BaseException("Insufficient balance to place bid");
        }
        // if bid exist then get bid from DB and then edit bid and save it again to db
        if (bidService.existUserAndItem(user, item)) {
            bid = bidService.getBidByUserAndItem(user, item);
            bid.setBidAmount(request.bidAmount());
            bidService.saveBid(bid);
        } else { // Else make new bid
            bid = new Bid(item, user, request.bidAmount());
            bidService.saveBid(bid);
        }

        user.setBalance(user.getBalance() - (itemStatus.getCurrentPrice() + itemStatus.getBidIncrement()));
        userService.saveUser(user);
        // If the former highest bid user is not the seller (when the item was first
        // published, the seller would be the current highest bidder) the former highest
        // bidder would be refuded.
        if (!itemStatus.getHighestBidUser().equals(item.getUser().getUsername())) {
            User prevUser = userService.getUserByUsername(itemStatus.getHighestBidUser());
            prevUser.setBalance(prevUser.getBalance() + itemStatus.getCurrentPrice());
            userService.saveUser(prevUser);
        }
        // Update item status in repository to the current highest bidder.
        updateItemStatusHighestBidder(itemStatus, username, request.bidAmount());

        applyAntiBidExtension(itemStatus);

        itemStatusService.saveStatus(itemStatus);
        itemPricesSink.publishPrice(request.itemId(), request.bidAmount());
        return new BidPostResponse(true, "Successfully created bid for an item", bid);
    }

    @Transactional(readOnly = true)
    public BaseObjectResponse<Page<Bid>> getMyCurrentBids(String username, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        User userRef = userService.getUserRef(username);

        Page<Bid> bids = bidService.getAllUserBid(userRef, pageable);

        return new BaseObjectResponse<Page<Bid>>(true, "succesfully got my bids", bids);
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
        validateAuctionNotEnded(itemId);
        // Buy now if balance >= buyitnow, buyitnow > currentprice
        boolean hasEnoughBalance = user.getBalance() >= itemStatus.getBuyItNowPrice();
        boolean isBuyNowAboveCurrent = itemStatus.getBuyItNowPrice() > itemStatus.getCurrentPrice();

        if (hasEnoughBalance && isBuyNowAboveCurrent) {

            // update bid status
            updateItemStatusHighestBidder(itemStatus, username, itemStatus.getBuyItNowPrice());
            itemStatus.setEndTime(Instant.now().toEpochMilli());
            itemStatusService.saveStatus(itemStatus);

            // Deduct money from user's fund
            user.setBalance(user.getBalance() - itemStatus.getBuyItNowPrice());
            userService.saveUser(user);
        } else {
            throw new BaseException("You don't have enough money in your balance to buy the item");
        }
        return new BaseResponse(true, "Successfully bought item");
    }

    @Transactional
    public BaseResponse createAutoBid(AutoBidRequest request, String bidderName) {
        User bidder = userService.getUserByUsername(bidderName);
        Optional<AutoBid> autoBidOP = bidService.getAutoBidByItemId(request.itemId());
        ItemStatus itemStatus = itemStatusService.getItemStatus(request.itemId());
        AutoBid currentAutoBid = new AutoBid(request.itemId(), bidder, request.maxBidLimit(), null);
        
        validateBasicRequirement(request.itemId(), bidder, itemStatus ,request.maxBidLimit());


        boolean isHighestBidderAutoBidding = autoBidOP.isPresent() && autoBidOP.get().getBidder().getUsername().equals(itemStatus.getHighestBidUser());

        if (isHighestBidderAutoBidding) {
            // check if current auto bid max value > previous auto bid max value
            AutoBid prevAutoBid = autoBidOP.get();
            boolean isHigherPrevAutoBid = request.maxBidLimit() > prevAutoBid.getMaxBidLimit();
            User prevUser = prevAutoBid.getBidder();
            if (isHigherPrevAutoBid) {
                if (prevUser.getUsername().equals(bidderName)) {
                    prevAutoBid.setMaxBidLimit(request.maxBidLimit());
                    bidService.saveAutoBid(prevAutoBid);
                }
                //refund previous bidder
                prevUser.addBalance(prevAutoBid.getMaxBidLimit());
                userService.saveUser(prevUser);
                //deduct fund from user and update autobid
                bidder.deductBalance(request.maxBidLimit());
                currentAutoBid.setCurrentBidValue(itemStatus.getNextBidStep());
                //save new bidder info
                userService.saveUser(bidder);

                //Update item status
                itemStatus.setNextBidStep(bidderName);

                //save bid
                itemStatusService.saveStatus(itemStatus);

                //save autobid
                bidService.saveAutoBid(currentAutoBid);
            } else {
                // pull the previous auto bid equals to current auto bid
                prevAutoBid.setCurrentBidValue(request.maxBidLimit());
                //update item status
                updateItemStatusHighestBidder(itemStatus, prevUser.getUsername(), prevAutoBid.getCurrentBidValue());
                //save bid service and item status
                bidService.saveAutoBid(prevAutoBid);
                itemStatusService.saveStatus(itemStatus);
            }
        
        } else {
            // Since max of auto bid must be higher than current bid
            //refund previous bidder
            User prevBidder = userService.getUserByUsername(itemStatus.getHighestBidUser());
            prevBidder.addBalance(itemStatus.getCurrentPrice());
            userService.saveUser(prevBidder);
            // Add autobidder to autobid
            currentAutoBid.setCurrentBidValue(itemStatus.getNextBidStep());
            bidService.saveAutoBid(currentAutoBid);
            // update itemstatus by step and save it
            itemStatus.setNextBidStep(bidderName);
            itemStatusService.saveStatus(itemStatus);
            // deduct auto bidder money
            bidder.deductBalance(request.maxBidLimit());
            userService.saveUser(bidder);           

        }
        applyAntiBidExtension(itemStatus);
        return new BaseResponse(true, "succesfully make auto bid");
    }

    private void validateBasicRequirement(Long itemId, User user, ItemStatus itemStatus,Double value) {
        validateAuctionNotEnded(itemId);
        validateUserHaveEnoughMoney(user, value);
        validateHigherThanCurrentPrice(itemStatus, value);
    }

    private void validateUserHaveEnoughMoney(User user, Double value) {
        if (user.getBalance() < value) {
            throw new BaseException("You don't have enough money");
        }
    }

    private void validateAuctionNotEnded(Long itemId) {
        if (itemStatusService.auctionEndedOrNot(itemId)) {
            throw new BaseException("Auction has already ended");
        }
    }
    
    private void updateItemStatusHighestBidder(ItemStatus itemStatus, String username, Double bidAmount) {
        itemStatus.setHighestBidUser(username);
        itemStatus.setCurrentPrice(bidAmount);
    }

    private void validateHigherThanCurrentPrice(ItemStatus itemStatus, Double value) {
        if (itemStatus.getCurrentPrice() + itemStatus.getBidIncrement() > value) {
            throw new BaseException("Your bid must be higher than the current highest");
        }
    }

    private void applyAntiBidExtension(ItemStatus itemStatus) {
        Long remainingTime = itemStatus.getEndTime() - Instant.now().toEpochMilli();
        if (remainingTime < extraTime && itemStatus.getEndTime() < itemStatus.getMaxEndTime()) {
            itemStatus.setEndTime(Instant.now().toEpochMilli() + extraTime);
        }
        itemStatusService.saveStatus(itemStatus);
    }
}
