package com.auction.items;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auction.common.BaseObjectResponse;
import com.auction.items.dto.BaseItemResponse;
import com.auction.items.dto.GetItemPagesResponse;
import com.auction.items.dto.GetItemsResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@SecurityRequirement(name = "bearerAuth")
@RestController // Assumes every method in the the class will return data in the HTTP body
                // (response type application/json)
@RequestMapping("/items")
public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<BaseItemResponse> getItem(@PathVariable Long itemId) {
        BaseItemResponse response = itemService.getItemRes(itemId);
        return ResponseEntity.ok().body(response);
    }

    // This API is only for development. Remove this in prod.
    @GetMapping("/all")
    public ResponseEntity<GetItemsResponse> getItems() {
        GetItemsResponse response = itemService.getItems();
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("")
    public ResponseEntity<GetItemPagesResponse> getActiveItems(
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(20) @RequestParam(defaultValue = "10") int size) {
        GetItemPagesResponse request = itemService.getActiveItemsByPageTitle(page, size);
        return ResponseEntity.ok().body(request);
    }

    @GetMapping("/listings/{username}")
    public ResponseEntity<BaseObjectResponse<Page<Item>>> getListing(
            @PathVariable String username,
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Min(1) @Max(20) @RequestParam(defaultValue = "10") int size) {
        BaseObjectResponse<Page<Item>> request = itemService.getListingByUser(page, size, username);
        return ResponseEntity.ok().body(request);
    }

}
