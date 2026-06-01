package com.example.biddingservice;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
public class BiddingController {

  private final BiddingService biddingService;

  public BiddingController(BiddingService biddingService) {
    this.biddingService = biddingService;
  }

  @PostMapping
  public ResponseEntity<BidResponse> placeBid(
      @PathVariable String auctionId, @Valid @RequestBody PlaceBidRequest request) {
    Bid bid = biddingService.placeBid(auctionId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(bid));
  }

  @GetMapping
  public List<BidResponse> getBidsForAuction(@PathVariable String auctionId) {
    return biddingService.getBidsForAuction(auctionId).stream().map(this::toResponse).toList();
  }

  private BidResponse toResponse(Bid bid) {
    return new BidResponse(
        bid.id(),
        bid.auctionId(),
        bid.bidderId(),
        bid.amount(),
        bid.status(),
        bid.createdAt());
  }
}
