package com.auction.items;

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
import com.auction.items.dto.BaseItemResponse;
import com.auction.items.dto.GetItemPagesResponse;
import com.auction.items.dto.GetItemsResponse;
import com.auction.users.User;
import com.auction.users.UserService;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;

    public ItemService(ItemRepository itemRepository, UserService userService) {
        this.itemRepository = itemRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public BaseItemResponse getItemRes(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BaseException("This Item Id does not exist"));
        return new BaseItemResponse(true, "Successfully get Item", item);
    }

    @Transactional(readOnly = true)
    public GetItemsResponse getItems() {
        List<Item> items = itemRepository.findAll();
        return new GetItemsResponse(true, "Successfully get all items", items);
    }

    @Transactional(readOnly = true)
    public GetItemPagesResponse getActiveItemsByPageTitle(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("item.title"));
        Page<Item> pages = itemRepository.findActiveItemPage(pageable, Instant.now().toEpochMilli());
        return new GetItemPagesResponse(true, "successfully got pages", pages);
    }

    @Transactional(readOnly = true)
    public BaseObjectResponse<Page<Item>> getListingByUser(int page, int size, String username) {
        Pageable pageable = PageRequest.of(page, size);
        User userRef = userService.getUserRef(username);
        Page<Item> items = itemRepository.findItemByUser(pageable, userRef);
        return new BaseObjectResponse<Page<Item>>(true, "succesfully got listing", items);
    }

    @Transactional
    public boolean existByItemId(Long itemId) {
        return itemRepository.existsById(itemId);
    }

    // getItemRef is used for internal
    @Transactional
    public Item getItemRef(Long itemId) {
        return itemRepository.getReferenceById(itemId);
    }

    @Transactional
    public Item getItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new BaseException("There is no such Item with that ID"));
    }

    @Transactional
    public Item saveItem(Item item) {
        item = itemRepository.save(item);
        return item;
    }
}
