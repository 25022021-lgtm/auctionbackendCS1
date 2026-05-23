package com.auction.auctionorchestration;

import com.auction.auctionorchestration.dto.BidPostRequest;
import com.auction.auctionorchestration.dto.BidPostResponse;
import com.auction.auth.jwtools.UserDetailsImpl;
import com.auction.bids.Bid;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.common.ItemPricesSink;
import com.auction.common.jointdata.BidAndItem;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/** REST controller for auction operations: bidding, viewing bids/wins, and price streaming. */
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping
public class AuctionController {

  private final AuctionService auctionService;
  private final ItemPricesSink itemsPricesSinks;

  public AuctionController(AuctionService auctionService, ItemPricesSink itemPricesSink) {
    this.auctionService = auctionService;
    this.itemsPricesSinks = itemPricesSink;
  }

  @PostMapping("/bid")
  public ResponseEntity<BidPostResponse> makeBid(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
      @Valid @RequestBody BidPostRequest request) {
    BidPostResponse response =
        auctionService.createBid(request, userDetailsImpl.getUsername());
    return ResponseEntity.ok().body(response);
  }

  @GetMapping("/me/bids")
  public ResponseEntity<BaseObjectResponse<Page<Bid>>> bids(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
      @Min(0) @RequestParam(defaultValue = "0") int page,
      @Min(1) @Max(20) @RequestParam(defaultValue = "10") int size) {
    BaseObjectResponse<Page<Bid>> response =
        auctionService.getMyCurrentBids(userDetailsImpl.getUsername(), page, size);
    return ResponseEntity.ok().body(response);
  }

  @GetMapping("/me/wins")
  public ResponseEntity<BaseObjectResponse<List<BidAndItem>>> getWins(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl) {
    BaseObjectResponse<List<BidAndItem>> response =
        auctionService.getMyWinnings(userDetailsImpl.getUsername());
    return ResponseEntity.ok().body(response);
  }

  @GetMapping(value = "/items/stream/{itemId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<Double> streamPrice(@PathVariable Long itemId) {
    return itemsPricesSinks.getPriceSink(itemId);
  }

  @PostMapping("/buy-now/{itemId}")
  public ResponseEntity<BaseResponse> buyNow(
      @AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
      @PathVariable Long itemId) {
    BaseResponse response =
        auctionService.buyItemNow(itemId, userDetailsImpl.getUsername());
    return ResponseEntity.ok().body(response);
  }
}
