package com.auction.items;

import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.items.dto.BaseItemResponse;
import com.auction.items.dto.GetItemPagesResponse;
import com.auction.items.dto.GetItemsResponse;
import com.auction.items.dto.PublishItemRequest;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing item lifecycle: publishing, canceling, and querying items. */
@Service
public class ItemService {

  private final ItemRepository itemRepository;
  private final UserService userService;
  private final ItemStatusService itemStatusService;

  @Value("${max-extra-time}")
  private Long maxExtraTime;

  public ItemService(
      ItemRepository itemRepository,
      UserService userService,
      ItemStatusService itemStatusService) {
    this.itemRepository = itemRepository;
    this.userService = userService;
    this.itemStatusService = itemStatusService;
  }

  @Transactional
  public BaseItemResponse publishItem(PublishItemRequest request, String username) {
    User user = userService.getUserReferenceByUsername(username);
    if (request.endTime() < Instant.now().toEpochMilli()) {
      throw new BaseException("Your end time must be higher than the current time");
    }
    Item item = saveItem(new Item(user, request.title(), request.description()));
    itemStatusService.saveStatus(
        new ItemStatus(
            item,
            0.0,
            username,
            request.endTime(),
            request.startingPrice(),
            request.buyItNowPrice(),
            request.bidIncrement(),
            request.endTime() + maxExtraTime));

    return new BaseItemResponse(true, "Created new item.", item);
  }

  @Transactional
  public BaseResponse cancelItem(Long itemId, String username) {
    Item item = getItem(itemId);

    if (!item.getUser().getUsername().equals(username)) {
      throw new BaseException("You are not the owner of this item");
    }

    ItemStatus status = itemStatusService.getItemStatus(itemId);
    if (!"ACTIVE".equals(status.getItemStatus())
        || itemStatusService.auctionEndedOrNot(itemId)) {
      throw new BaseException("Only ACTIVE items can be canceled.");
    }

    status.setItemStatus("CANCELED");
    status.setEndTime(Instant.now().toEpochMilli());
    itemStatusService.saveStatus(status);

    return new BaseResponse(true, "Item successfully canceled.");
  }

  @Transactional(readOnly = true)
  public BaseItemResponse getItemRes(Long itemId) {
    Item item =
        itemRepository
            .findById(itemId)
            .orElseThrow(() -> new BaseException("This Item Id does not exist"));
    return new BaseItemResponse(true, "Successfully retrieved item", item);
  }

  @Transactional(readOnly = true)
  public GetItemsResponse getItems() {
    List<Item> items = itemRepository.findAll();
    return new GetItemsResponse(true, "Successfully retrieved all items", items);
  }

  @Transactional(readOnly = true)
  public GetItemPagesResponse getActiveItemsByPageTitle(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("item.title"));
    Page<Item> pages =
        itemRepository.findActiveItemPage(pageable, Instant.now().toEpochMilli());
    return new GetItemPagesResponse(true, "Successfully retrieved pages of active items", pages);
  }

  @Transactional(readOnly = true)
  public BaseObjectResponse<Page<Item>> getListingByUser(
      int page, int size, String username) {
    Pageable pageable = PageRequest.of(page, size);
    User userRef = userService.getUserRef(username);
    Page<Item> items = itemRepository.findItemByUser(pageable, userRef);
    return new BaseObjectResponse<>(true, "Successfully retrieved listing", items);
  }

  @Transactional
  public boolean existByItemId(Long itemId) {
    return itemRepository.existsById(itemId);
  }

  @Transactional
  public Item getItemRef(Long itemId) {
    return itemRepository.getReferenceById(itemId);
  }

  @Transactional
  public Item getItem(Long itemId) {
    return itemRepository
        .findById(itemId)
        .orElseThrow(() -> new BaseException("There is no such Item with that ID"));
  }

  @Transactional
  public Item saveItem(Item item) {
    item = itemRepository.save(item);
    return item;
  }
}
