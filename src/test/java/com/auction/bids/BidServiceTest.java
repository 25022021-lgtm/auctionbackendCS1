package com.auction.bids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auction.common.BaseObjectResponse;
import com.auction.items.Item;
import com.auction.users.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

  @Mock private BidRepository bidRepository;
  @Mock private AutoBidRepository autoBidRepository;

  @InjectMocks private BidService bidService;

  private User testUser;
  private Item testItem;
  private Bid testBid;
  private AutoBid testAutoBid;

  @BeforeEach
  void setUp() {
    testUser = new User("testuser", "Test User", "password", 100.0);
    testItem = new Item(testUser, "Test Item", "Description");
    testBid = new Bid(testItem, testUser, 15.0);
    // Use the constructor to create the AutoBid object
    testAutoBid = new AutoBid(1L, testUser, 200.0, 150.0);
  }

  @Test
  void getBidsOnItem_Success() {
    // Arrange
    Long itemId = 1L;
    Page<Bid> page = new PageImpl<>(List.of(testBid));
    when(bidRepository.findItemBidHistory(any(Pageable.class), any(Long.class))).thenReturn(page);

    // Act
    BaseObjectResponse<Page<Bid>> response = bidService.getBidsOnItem(itemId, 0, 10);

    // Assert
    assertTrue(response.getStatus());
    assertEquals("Succesfully get all bids", response.getMessage());
    assertEquals(1, response.getEntity().getTotalElements());
  }

  @Test
  void saveBid_Success() {
    // Arrange
    when(bidRepository.save(any(Bid.class))).thenReturn(testBid);

    // Act
    Bid result = bidService.saveBid(testBid);

    // Assert
    assertEquals(testBid, result);
    verify(bidRepository).save(testBid);
  }

  @Test
  void getAutoBidByItemId_Success() {
    // Arrange
    Long itemId = 1L;
    when(autoBidRepository.findByItemId(itemId)).thenReturn(Optional.of(testAutoBid));

    // Act
    Optional<AutoBid> result = bidService.getAutoBidByItemId(itemId);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(testAutoBid, result.get());
  }

  @Test
  void saveAutoBid_Success() {
    // Arrange
    when(autoBidRepository.save(any(AutoBid.class))).thenReturn(testAutoBid);

    // Act
    bidService.saveAutoBid(testAutoBid);

    // Assert
    verify(autoBidRepository).save(testAutoBid);
  }

  @Test
  void deleteAutoBid_Success() {
    // Act
    bidService.deleteAutoBid(testAutoBid);

    // Assert
    verify(autoBidRepository).delete(testAutoBid);
  }
}
