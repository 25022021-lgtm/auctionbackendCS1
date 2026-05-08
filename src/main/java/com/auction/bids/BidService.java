package com.auction.bids;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.items.Item;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;

@Service
public class BidService {
    // Don't inject repository, inject service instead. Refactor tomorrow.

    private final BidRepository bidRepository;
    private final UserService userService;
    private final ItemStatusService itemStatusService;

    public BidService(BidRepository bidRepository, UserService userService,
            ItemStatusService itemStatusService) {
        this.bidRepository = bidRepository;
        this.itemStatusService = itemStatusService;
        this.userService = userService;
    }

    @Transactional
    public BaseResponse buyItemNow(Long itemId, String username) {
        ItemStatus itemStatus = itemStatusService.getItemStatus(itemId);
        User user = userService.getUserByUsername(username);
        if (itemStatus.getItemStatus().equals("CANCELED") || itemStatus.getItemStatus().equals("ENDED")
                || itemStatus.getEndTime() < Instant.now().toEpochMilli()
                || itemStatus.getBuyItNowPrice() == 0) {
            throw new BaseException("You can't buy this Item");
        }

        // Buy now if balance >= buyitnow, buyitnow > currentprice
        if (user.getBalance() >= itemStatus.getBuyItNowPrice()
                && itemStatus.getBuyItNowPrice() > itemStatus.getCurrentPrice()) {

            // update bid status
            itemStatus.setHighestBidUser(username);
            itemStatus.setCurrentPrice(itemStatus.getBuyItNowPrice());
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

    @Transactional(readOnly = true)
    public BaseObjectResponse<Page<Bid>> getBidsOnItem(Long itemId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("bidAmount"));
        Page<Bid> items = bidRepository.findItemBidHistory(pageable, itemId);
        return new BaseObjectResponse<Page<Bid>>(true, "Succesfully get all bids", items);
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
        Bid bid = bidRepository.findByUserAndItem(user, item)
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
