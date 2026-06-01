package com.example.biddingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.web.client.HttpClientErrorException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class BiddingServiceApplicationTests {

  @LocalServerPort
  private int port;

  private final RestTemplate rest = new RestTemplate();

  private String bidsUrl(String auctionId) {
    return "http://localhost:" + port + "/api/auctions/" + auctionId + "/bids";
  }

  private HttpEntity<String> jsonRequest(String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }

  @Test
  void contextLoads() {}

  @Test
  void placeBidReturnsCreatedBid() {
    var request = jsonRequest("""
        {"bidderId": "bidder-1", "amount": 100}
        """);

    @SuppressWarnings("rawtypes")
    ResponseEntity<BidResponse> response =
        rest.postForEntity(bidsUrl("auction-1"), request, BidResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isNotBlank();
    assertThat(response.getBody().auctionId()).isEqualTo("auction-1");
    assertThat(response.getBody().bidderId()).isEqualTo("bidder-1");
    assertThat(response.getBody().amount()).isEqualByComparingTo("100");
    assertThat(response.getBody().status()).isEqualTo(BidStatus.ACTIVE);
  }

  @Test
  void getBidsForAuctionReturnsAllBids() throws Exception {
    rest.postForEntity(
        bidsUrl("auction-2"),
        jsonRequest("""
            {"bidderId": "bidder-a", "amount": 50}
            """),
        BidResponse.class);

    // Small delay to ensure distinct ordering if needed
    Thread.sleep(5);

    rest.postForEntity(
        bidsUrl("auction-2"),
        jsonRequest("""
            {"bidderId": "bidder-b", "amount": 75}
            """),
        BidResponse.class);

    @SuppressWarnings("rawtypes")
    ResponseEntity<BidResponse[]> response =
        rest.getForEntity(bidsUrl("auction-2"), BidResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
  }

  @Test
  void getBidsForAuctionReturnsEmptyListWhenNoBids() {
    @SuppressWarnings("rawtypes")
    ResponseEntity<BidResponse[]> response =
        rest.getForEntity(bidsUrl("nonexistent"), BidResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEmpty();
  }

  @Test
  void placeBidReturnsBadRequestForMissingBidder() {
    var request = jsonRequest("""
        {"bidderId": "", "amount": 100}
        """);

    assertThatThrownBy(
            () -> rest.postForEntity(bidsUrl("auction-3"), request, String.class))
        .isInstanceOf(HttpClientErrorException.BadRequest.class);
  }

  @Test
  void placeBidReturnsBadRequestForZeroAmount() {
    var request = jsonRequest("""
        {"bidderId": "bidder-1", "amount": 0}
        """);

    assertThatThrownBy(
            () -> rest.postForEntity(bidsUrl("auction-4"), request, String.class))
        .isInstanceOf(HttpClientErrorException.BadRequest.class);
  }
}
