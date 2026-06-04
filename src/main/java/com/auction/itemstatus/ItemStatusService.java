package com.auction.itemstatus;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.common.BaseObjectResponse;
import com.auction.items.Item;

@Service
public class ItemStatusService {
    private final ItemStatusRepository itemStatusRepository;

    public ItemStatusService(ItemStatusRepository itemStatusRepository) {
        this.itemStatusRepository = itemStatusRepository;
    }

    @Transactional
    public ItemStatus saveStatus(ItemStatus itemStatus) {
        System.out.println("Status Created");
        itemStatusRepository.save(itemStatus);
        return itemStatus;
    }

    @Transactional
    public ItemStatus updateStatus(Item item, Double currentPrice, String username) {
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLock(item);
        itemStatus.setCurrentPrice(currentPrice);
        itemStatus.setHighestBidUser(username);
        itemStatusRepository.save(itemStatus);
        return itemStatus;
    }

    @Transactional
    public BaseObjectResponse<ItemStatus> getStatusResponse(Long itemId) {
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLockByItemId(itemId);
        return new BaseObjectResponse<>(true, "Succesfully get item status", itemStatus);
    }

    @Transactional
    public ItemStatus getItemStatus(Long itemId) {
        return itemStatusRepository.findByItemWithLockByItemId(itemId);
    }

    @Transactional(readOnly = true)
    public List<ItemStatus> getAllItemStatus() {
        return itemStatusRepository.findAll();
    }

    @Transactional
    public boolean auctionEndedOrNot(Long itemId) {
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLockByItemId(itemId);
        if (itemStatus.getItemStatus().equals("ENDED") || itemStatus.getItemStatus().equals("CANCELED")) {
            return true;
        } else if (itemStatus.getEndTime() < Instant.now().toEpochMilli()) {
            itemStatus.setItemStatus("ENDED");
            itemStatusRepository.save(itemStatus);
            return true;
        } else {
            return false;
        }
    }
}
