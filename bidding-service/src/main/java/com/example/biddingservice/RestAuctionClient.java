package com.example.biddingservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("!mock")
public class RestAuctionClient implements AuctionClient {

  private static final Logger log = LoggerFactory.getLogger(RestAuctionClient.class);

  private final RestClient restClient;

  public RestAuctionClient(
      @Value("${auction-service.url}") String auctionServiceUrl) {
    this.restClient = RestClient.builder().baseUrl(auctionServiceUrl).build();
  }

  @Override
  public AuctionSummary getAuction(String auctionId) {
    log.info("GET /api/auctions/{} — fetching auction from auction-service", auctionId);
    try {
      AuctionResponse response =
          restClient
              .get()
              .uri("/api/auctions/{auctionId}", auctionId)
              .retrieve()
              .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                if (res.getStatusCode() == HttpStatus.NOT_FOUND) {
                  log.warn("Auction {} not found via auction-service", auctionId);
                  throw new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Auction not found: " + auctionId);
                }
                log.warn("Auction service returned {} for auction {}", res.getStatusCode(), auctionId);
                throw new ResponseStatusException(
                    res.getStatusCode(), "Auction service error: " + auctionId);
              })
              .body(AuctionResponse.class);

      log.info("Fetched auction {}: status={}", auctionId, response.status());
      return new AuctionSummary(response.id(), response.status(), response.startingPrice());
    } catch (ResponseStatusException e) {
      throw e;
    } catch (HttpClientErrorException.NotFound e) {
      log.warn("Auction {} not found (HttpClientErrorException)", auctionId);
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Auction not found: " + auctionId);
    } catch (RestClientException e) {
      log.error("Auction service unavailable while fetching auction {}: {}", auctionId, e.getMessage());
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Auction service is currently unavailable. Please try again later.");
    }
  }

  @Override
  public void updateHighestBid(String auctionId, String bidId, String bidderId,
      java.math.BigDecimal amount) {
    log.info("POST /api/auctions/{}/highest-bid — amount={} bidder={}", auctionId, amount, bidderId);
    try {
      restClient
          .post()
          .uri("/api/auctions/{auctionId}/highest-bid", auctionId)
          .contentType(MediaType.APPLICATION_JSON)
          .body(new HighestBidBody(bidId, bidderId, amount))
          .retrieve()
          .toBodilessEntity();
      log.info("Successfully updated highest bid for auction {}", auctionId);
    } catch (HttpClientErrorException.NotFound e) {
      log.warn("Auction {} not found while updating highest bid", auctionId);
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Auction not found: " + auctionId);
    } catch (RestClientException e) {
      log.error("Auction service unavailable while updating bid for auction {}: {}", auctionId, e.getMessage());
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Auction service is currently unavailable. Please try again later.");
    }
  }

  private record AuctionResponse(String id, String title, String description,
      String sellerId, java.math.BigDecimal startingPrice, String status) {}

  private record HighestBidBody(String bidId, String bidderId, java.math.BigDecimal amount) {}
}
