package com.example.biddingservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("!mock")
public class RestAuctionClient implements AuctionClient {

  private final RestClient restClient;

  public RestAuctionClient(
      @Value("${auction-service.url}") String auctionServiceUrl,
      RestClient.Builder builder) {
    this.restClient = builder.baseUrl(auctionServiceUrl).build();
  }

  @Override
  public AuctionSummary getAuction(String auctionId) {
    try {
      AuctionResponse response =
          restClient
              .get()
              .uri("/api/auctions/{auctionId}", auctionId)
              .retrieve()
              .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                if (res.getStatusCode() == HttpStatus.NOT_FOUND) {
                  throw new ResponseStatusException(
                      HttpStatus.NOT_FOUND, "Auction not found: " + auctionId);
                }
                throw new ResponseStatusException(
                    res.getStatusCode(), "Auction service error: " + auctionId);
              })
              .body(AuctionResponse.class);

      return new AuctionSummary(response.id(), response.status(), response.startingPrice());
    } catch (ResponseStatusException e) {
      throw e;
    } catch (HttpClientErrorException.NotFound e) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Auction not found: " + auctionId);
    } catch (RestClientException e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Auction service is currently unavailable. Please try again later.");
    }
  }

  private record AuctionResponse(String id, String title, String description,
      String sellerId, java.math.BigDecimal startingPrice, String status) {}
}
