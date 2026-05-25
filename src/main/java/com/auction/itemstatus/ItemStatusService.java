package com.auction.itemstatus;

import com.auction.items.Item;
import com.auction.itemstatus.dto.ItemStatusGetResponse;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing item auction status operations. */
@Service
public class ItemStatusService {

  private static final Logger logger = Logger.getLogger(ItemStatusService.class.getName());

  private final ItemStatusRepository itemStatusRepository;

  public ItemStatusService(ItemStatusRepository itemStatusRepository) {
    this.itemStatusRepository = itemStatusRepository;
  }

  @Transactional
  public ItemStatus saveStatus(ItemStatus itemStatus) {
    logger.info("Status Created");
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
    ItemStatus itemStatus = itemStatusRepository.findByItemWithLockByItemId(itemId);
    return new ItemStatusGetResponse(true, "Successfully get item status", itemStatus);
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
    if (itemStatus.getItemStatus().equals("ENDED")
        || itemStatus.getItemStatus().equals("CANCELED")) {
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
