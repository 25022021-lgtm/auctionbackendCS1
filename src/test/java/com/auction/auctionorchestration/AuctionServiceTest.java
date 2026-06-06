package com.auction.auctionorchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.auctionorchestration.dto.AutoBidRequest;
import com.auction.auctionorchestration.dto.BidPostRequest;
import com.auction.bids.AutoBid;
import com.auction.bids.Bid;
import com.auction.bids.BidService;
import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.common.ItemPricesSink;
import com.auction.items.Item;
import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

  @Mock private ItemService itemService;
  @Mock private UserService userService;
  @Mock private ItemStatusService itemStatusService;
  @Mock private BidService bidService;
  @Mock private ItemPricesSink itemPricesSink;

  @InjectMocks private AuctionService auctionService;

  private User seller;
  private User bidder;
  private Item testItem;
  private ItemStatus testItemStatus;
  private Bid testBid;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(auctionService, "extraTime", 60000L); // 1 minute

    seller = new User("seller", "Seller", "password", 0.0);
    bidder = new User("bidder", "Bidder", "password", 500.0);
    testItem = new Item(seller, "Test Item", "Description");
    // The ID is set by the database, so we use reflection for the test
    ReflectionTestUtils.setField(testItem, "itemId", 1L);

    testItemStatus = new ItemStatus();
    testItemStatus.setItem(testItem);
    testItemStatus.setCurrentPrice(100.0);
    testItemStatus.setStartingPrice(50.0);
    testItemStatus.setBidIncrement(10.0);
    testItemStatus.setHighestBidUser("seller");
    testItemStatus.setEndTime(Instant.now().toEpochMilli() + 300000L); // 5 minutes from now

    testBid = new Bid(testItem, bidder, 120.0);
  }

  @Test
  void createBid_Success_NoAutoBid() {
    // Arrange
    BidPostRequest request = new BidPostRequest(1L, 120.0);
    when(itemService.getItemRef(1L)).thenReturn(testItem);
    when(userService.getUserRef("bidder")).thenReturn(bidder);
    when(itemStatusService.getItemStatus(1L)).thenReturn(testItemStatus);
    when(bidService.existUserAndItem(bidder, testItem)).thenReturn(false);
    when(bidService.saveBid(any(Bid.class))).thenReturn(testBid);
    when(bidService.getAutoBidByItemId(1L)).thenReturn(Optional.empty());
    doNothing().when(itemPricesSink).publishPrice(anyLong(), anyDouble());

    // Act
    BaseObjectResponse<Bid> response = auctionService.createBid(request, "bidder");

    // Assert
    assertEquals(true, response.getStatus());
    assertEquals("Successfully created bid for an item", response.getMessage());
    assertEquals(120.0, testItemStatus.getCurrentPrice());
    assertEquals("bidder", testItemStatus.getHighestBidUser());
    verify(userService).deductBalance("bidder", 120.0);
  }

  @Test
  void createBid_BidOnOwnItem_ThrowsException() {
    // Arrange
    BidPostRequest request = new BidPostRequest(1L, 120.0);
    when(itemService.getItemRef(1L)).thenReturn(testItem);
    when(userService.getUserRef("seller")).thenReturn(seller);
    when(itemStatusService.getItemStatus(1L)).thenReturn(testItemStatus);

    // Act & Assert
    BaseException exception =
        assertThrows(
            BaseException.class,
            () -> {
              auctionService.createBid(request, "seller");
            });
    assertEquals("You can't place bid on your own item", exception.getMessage());
  }

  @Test
  void buyItemNow_Success() {
    // Arrange
    testItemStatus.setBuyItNowPrice(200.0);
    when(itemStatusService.getItemStatus(1L)).thenReturn(testItemStatus);
    when(userService.getUserByUsername("bidder")).thenReturn(bidder);
    when(itemService.getItem(1L)).thenReturn(testItem);
    when(bidService.saveBid(any(Bid.class))).thenReturn(testBid);
    doNothing().when(itemPricesSink).publishPrice(anyLong(), anyDouble());

    // Act
    BaseResponse response = auctionService.buyItemNow(1L, "bidder");

    // Assert
    assertEquals(true, response.getStatus());
    assertEquals("Successfully bought item", response.getMessage());
    assertEquals(200.0, testItemStatus.getCurrentPrice());
    assertEquals("bidder", testItemStatus.getHighestBidUser());
    verify(userService).deductBalance("bidder", 200.0);
  }

  @Test
  void createAutoBid_NewAutoBidder_Success() {
    // Arrange
    AutoBidRequest request = new AutoBidRequest(1L, 300.0);
    when(userService.getUserByUsername("bidder")).thenReturn(bidder);
    when(bidService.getAutoBidByItemId(1L)).thenReturn(Optional.empty());
    when(itemStatusService.getItemStatus(1L)).thenReturn(testItemStatus);
    when(itemService.getItem(1L)).thenReturn(testItem);

    // Act
    BaseResponse response = auctionService.createAutoBid(request, "bidder");

    // Assert
    assertEquals(true, response.getStatus());
    assertEquals("succesfully make auto bid", response.getMessage());
    verify(bidService).saveAutoBid(any(AutoBid.class));
    verify(userService).deductBalance("bidder", 300.0);
  }
}
