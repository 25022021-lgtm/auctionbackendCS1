package com.auction.auctionorchestration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
    public final ItemService itemService;
    public final UserService userService;
    public final ItemStatusService itemStatusService;
    public final BidService bidService;
    public final ItemPricesSink itemPricesSink;

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
        Bid bid;
        Item itemRef = itemService.getItemRef(request.itemId());
        User user = userService.getUserRef(username);

        ItemStatus itemStatus = itemStatusService.getItemStatus(request.itemId());

        // big amount must be higher than starting price and bid time must be lower than
        // endtime and bid amount must be higher than current balance
        if (request.bidAmount() < itemStatus.getStartingPrice() + itemStatus.getBidIncrement()) {
            throw new BaseException("Bid amount is below the required minimum (starting price + bid increment)");
        } else if (Instant.now().toEpochMilli() > itemStatus.getEndTime()) {
            throw new BaseException("Auction has already ended");
        } else if (request.bidAmount() > user.getBalance()) {
            throw new BaseException("Insufficient balance to place bid");
        }
        // if bid exist then get bid from DB and then edit bid and save it again to db
        if (bidService.existUserAndItem(user, itemRef)) {
            bid = bidService.getBidByUserAndItem(user, itemRef);
            bid.setBidAmount(request.bidAmount());
            bidService.saveBid(bid);
        } else { // Else make new bid
            bid = new Bid(itemRef, user, request.bidAmount());
            bidService.saveBid(bid);
        }
        // If user bid amount if higher than the current highest + increment, they would
        // be the highest bidder
        if (request.bidAmount() > itemStatus.getCurrentPrice() + itemStatus.getBidIncrement()) {
            itemStatusService.updateStatus(itemRef, request.bidAmount(), username);
        }
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

}
