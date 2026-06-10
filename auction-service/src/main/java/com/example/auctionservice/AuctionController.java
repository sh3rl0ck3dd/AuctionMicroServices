package com.example.auctionservice;

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
@RequestMapping("/api/auctions")
public class AuctionController {

  private final AuctionService auctionService;

  public AuctionController(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @PostMapping
  public ResponseEntity<AuctionResponse> createAuction(
      @Valid @RequestBody CreateAuctionRequest createAuctionRequest) {
    Auction auction = auctionService.createAuction(createAuctionRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(auction));
  }

  @GetMapping("/{auctionId}")
  public AuctionResponse getAuctionById(@PathVariable String auctionId) {
    return toResponse(auctionService.getAuctionById(auctionId));
  }

  @GetMapping
  public List<AuctionResponse> listAuctions() {
    return auctionService.listAuctions().stream().map(this::toResponse).toList();
  }

  @PostMapping("/{auctionId}/start")
  public AuctionResponse startAuction(
      @PathVariable String auctionId, @Valid @RequestBody StartAuctionRequest request) {
    return toResponse(auctionService.startAuction(auctionId, request.endsAt()));
  }

  @PostMapping("/{auctionId}/end")
  public AuctionResponse endAuction(@PathVariable String auctionId) {
    return toResponse(auctionService.endAuction(auctionId));
  }

  @PostMapping("/{auctionId}/cancel")
  public AuctionResponse cancelAuction(@PathVariable String auctionId) {
    return toResponse(auctionService.cancelAuction(auctionId));
  }

  @PostMapping("/{auctionId}/highest-bid")
  public AuctionResponse updateHighestBid(
      @PathVariable String auctionId, @RequestBody HighestBidRequest request) {
    return toResponse(auctionService.updateHighestBid(auctionId, request));
  }

  private AuctionResponse toResponse(Auction auction) {
    return new AuctionResponse(
        auction.id(),
        auction.title(),
        auction.description(),
        auction.sellerId(),
        auction.startingPrice(),
        auction.currentPrice(),
        auction.status(),
        auction.endsAt(),
        auction.lastBidTime());
  }
}
