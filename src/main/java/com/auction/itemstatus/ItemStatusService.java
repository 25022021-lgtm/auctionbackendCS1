package com.auction.itemstatus;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.items.Item;
import com.auction.items.ItemService;
import com.auction.itemstatus.dto.ItemStatusGetResponse;

@Service
public class ItemStatusService {
    private final ItemStatusRepository itemStatusRepository;
    private final ItemService itemService;

    public ItemStatusService(ItemStatusRepository itemStatusRepository, ItemService itemService) {
        this.itemStatusRepository = itemStatusRepository;
        this.itemService = itemService;
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
    public ItemStatusGetResponse getStatusResponse(Long itemId) {
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLock(itemService.getItemRef(itemId));
        return new ItemStatusGetResponse(true, "Succesfully get item status", itemStatus);
    }

    @Transactional
    public ItemStatus getItemStatus(Long itemId) {
        return itemStatusRepository.findByItemWithLock(itemService.getItemRef(itemId));
    }

    @Transactional(readOnly = true)
    public List<ItemStatus> getAllItemStatus() {
        return itemStatusRepository.findAll();
    }

    @Transactional
    public boolean auctionEndedOrNot(Long itemId) {
        Item itemRef = itemService.getItemRef(itemId);
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLock(itemRef);
        if (itemStatus.getItemStatus().equals("ENDED") || itemStatus.getItemStatus().equals("CANCELED")
                || itemStatus.getEndTime() < Instant.now().toEpochMilli()) {
            itemStatus.setItemStatus("ENDED");
            itemStatusRepository.save(itemStatus);
            return true;
        } else {
            return false;
        }
    }
}
