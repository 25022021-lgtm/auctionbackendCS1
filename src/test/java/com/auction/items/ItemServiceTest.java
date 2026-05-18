package com.auction.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.auction.bids.BidRepository;
import com.auction.common.BaseException;
import com.auction.common.BaseResponse;
import com.auction.items.dto.BaseItemResponse;
import com.auction.items.dto.PublishItemRequest;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserService userService;

    @Mock
    private ItemStatusService itemStatusService;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private ItemService itemService;

    private User testUser;
    private Item testItem;
    private ItemStatus testItemStatus;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "Test User", "hashedpassword", 0.0);
        
        testItem = new Item();
        testItem.setUser(testUser);
        
        testItemStatus = new ItemStatus();
        testItemStatus.setItemStatus("ACTIVE");
    }

    @Test
    void publishItem_Success() {
        // Arrange
        PublishItemRequest request = new PublishItemRequest("Test Item", "Description", 1234567890L, 10.0, 100.0, 5.0);
        String username = "testuser";

        when(userService.getUserReferenceByUsername(username)).thenReturn(testUser);
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        BaseItemResponse response = itemService.publishItem(request, username);

        // Assert
        assertEquals(true, response.getStatus());
        assertEquals("Created new item.", response.getMessage());
        assertNotNull(response.getItem());
        verify(itemStatusService).saveStatus(any(ItemStatus.class));
    }

    @Test
    void deleteItem_Success() {
        // Arrange
        Long itemId = 1L;
        String username = "testuser";

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

        // Act
        BaseResponse response = itemService.deleteItem(itemId, username);

        // Assert
        assertEquals(true, response.getStatus());
        assertEquals("Item " + itemId + " was deleted", response.getMessage());
        verify(itemRepository).deleteById(itemId);
    }

    @Test
    void deleteItem_NotOwner_ThrowsException() {
        // Arrange
        Long itemId = 1L;
        String username = "wronguser";

        when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            itemService.deleteItem(itemId, username);
        });
        assertEquals("You are not the owner of this item", exception.getMessage());
    }

    @Test
    void cancelItem_Success() {
        // Arrange
        Long itemId = 1L;
        String username = "testuser";
        
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
        when(bidRepository.existsByItem(testItem)).thenReturn(false);
        when(itemStatusService.getItemStatus(itemId)).thenReturn(testItemStatus);

        // Act
        BaseResponse response = itemService.cancelItem(itemId, username);

        // Assert
        assertEquals(true, response.getStatus());
        assertEquals("Item successfully canceled.", response.getMessage());
        assertEquals("CANCELED", testItemStatus.getItemStatus());
        verify(itemStatusService).saveStatus(testItemStatus);
    }

    @Test
    void cancelItem_ItemNotFound_ThrowsException() {
        // Arrange
        Long itemId = 1L;
        String username = "testuser";
        
        when(itemRepository.findById(itemId)).thenReturn(Optional.empty());

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            itemService.cancelItem(itemId, username);
        });
        assertEquals("There is no Item with that ID", exception.getMessage());
    }

    @Test
    void cancelItem_UserNotOwner_ThrowsException() {
        // Arrange
        Long itemId = 1L;
        String username = "wronguser";
        
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            itemService.cancelItem(itemId, username);
        });
        assertEquals("You are not the owner of this item", exception.getMessage());
    }

    @Test
    void cancelItem_HasBids_ThrowsException() {
        // Arrange
        Long itemId = 1L;
        String username = "testuser";
        
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
        when(bidRepository.existsByItem(testItem)).thenReturn(true);

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            itemService.cancelItem(itemId, username);
        });
        assertEquals("Cannot cancel item. You must delete all bids on this item first.", exception.getMessage());
    }

    @Test
    void cancelItem_NotActive_ThrowsException() {
        // Arrange
        Long itemId = 1L;
        String username = "testuser";
        testItemStatus.setItemStatus("ENDED");
        
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(testItem));
        when(bidRepository.existsByItem(testItem)).thenReturn(false);
        when(itemStatusService.getItemStatus(itemId)).thenReturn(testItemStatus);

        // Act & Assert
        BaseException exception = assertThrows(BaseException.class, () -> {
            itemService.cancelItem(itemId, username);
        });
        assertEquals("Only ACTIVE items can be canceled.", exception.getMessage());
    }
}
