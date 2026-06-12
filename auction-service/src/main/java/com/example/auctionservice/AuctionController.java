package com.example.auctionservice;

import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

  private final AuctionService auctionService;

  public AuctionController(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  @PostMapping
  public ResponseEntity<AuctionResponse> createAuction(
      @Valid @RequestBody CreateAuctionRequest createAuctionRequest) {
    log.info("POST /api/auctions — create auction: title='{}'", createAuctionRequest.title());
    Auction auction = auctionService.createAuction(createAuctionRequest);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(auction));
  }

  @GetMapping("/{auctionId}")
  public AuctionResponse getAuctionById(@PathVariable String auctionId) {
    log.info("GET /api/auctions/{}", auctionId);
    return toResponse(auctionService.getAuctionById(auctionId));
  }

  @GetMapping
  public List<AuctionResponse> listAuctions() {
    log.info("GET /api/auctions");
    return auctionService.listAuctions().stream().map(this::toResponse).toList();
  }

  @PostMapping("/{auctionId}/start")
  public AuctionResponse startAuction(
      @PathVariable String auctionId, @Valid @RequestBody StartAuctionRequest request) {
    log.info("POST /api/auctions/{}/start — endsAt={}", auctionId, request.endsAt());
    return toResponse(auctionService.startAuction(auctionId, request.endsAt()));
  }

  @PostMapping("/{auctionId}/end")
  public AuctionResponse endAuction(@PathVariable String auctionId) {
    log.info("POST /api/auctions/{}/end", auctionId);
    return toResponse(auctionService.endAuction(auctionId));
  }

  @PostMapping("/{auctionId}/cancel")
  public AuctionResponse cancelAuction(@PathVariable String auctionId) {
    log.info("POST /api/auctions/{}/cancel", auctionId);
    return toResponse(auctionService.cancelAuction(auctionId));
  }

  @PostMapping("/{auctionId}/highest-bid")
  public AuctionResponse updateHighestBid(
      @PathVariable String auctionId, @RequestBody HighestBidRequest request) {
    log.info("POST /api/auctions/{}/highest-bid — amount={} bidder={}", auctionId, request.amount(), request.bidderId());
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
