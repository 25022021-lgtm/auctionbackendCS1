package com.auction.itemstatus;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.auction.items.Item;
import com.auction.items.ItemRepository;
import com.auction.itemstatus.dto.ItemStatusGetResponse;

@Service
public class ItemStatusService {
    public final ItemStatusRepository itemStatusRepository;
    public final ItemRepository itemRepository;

    public ItemStatusService(ItemStatusRepository itemStatusRepository, ItemRepository itemRepository) {
        this.itemStatusRepository = itemStatusRepository;
        this.itemRepository = itemRepository;
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
        ItemStatus itemStatus = itemStatusRepository.findByItemWithLock(itemRepository.getReferenceById(itemId));
        return new ItemStatusGetResponse(true, "Succesfully get item status", itemStatus);
    }

    @Transactional
    public ItemStatus getItemStatus(Long itemId) {
        return itemStatusRepository.findByItemWithLock(itemRepository.getReferenceById(itemId));
    }

    @Transactional(readOnly = true)
    public List<ItemStatus> getAllItemStatus() {
        return itemStatusRepository.findAll();
    }

}
