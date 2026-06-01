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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("mock")
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

  @Test
  void placeBidOnActiveAuctionSucceeds() {
    var request = jsonRequest("""
        {"bidderId": "bidder-5", "amount": 500}
        """);

    @SuppressWarnings("rawtypes")
    ResponseEntity<BidResponse> response =
        rest.postForEntity(bidsUrl("auction-active"), request, BidResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(BidStatus.ACTIVE);
    assertThat(response.getBody().amount()).isEqualByComparingTo("500");
  }

  @Test
  void placeBidOnEndedAuctionFails() {
    var request = jsonRequest("""
        {"bidderId": "bidder-6", "amount": 100}
        """);

    assertThatThrownBy(
            () -> rest.postForEntity(bidsUrl("auction-ended"), request, String.class))
        .isInstanceOf(HttpClientErrorException.Conflict.class);
  }

  @Test
  void placeBidLowerThanHighestFails() {
    var first = jsonRequest("""
        {"bidderId": "bidder-7", "amount": 50}
        """);
    rest.postForEntity(bidsUrl("auction-5"), first, BidResponse.class);

    var second = jsonRequest("""
        {"bidderId": "bidder-8", "amount": 30}
        """);

    assertThatThrownBy(
            () -> rest.postForEntity(bidsUrl("auction-5"), second, String.class))
        .isInstanceOf(HttpClientErrorException.BadRequest.class);
  }

  @Test
  void placeBidHigherThanHighestSucceedsAndOutbidsPrevious() {
    var first = jsonRequest("""
        {"bidderId": "bidder-9", "amount": 50}
        """);
    @SuppressWarnings("rawtypes")
    ResponseEntity<BidResponse> firstResp =
        rest.postForEntity(bidsUrl("auction-6"), first, BidResponse.class);
    assertThat(firstResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    var second = jsonRequest("""
        {"bidderId": "bidder-10", "amount": 200}
        """);
    @SuppressWarnings("rawtypes")
    ResponseEntity<BidResponse> secondResp =
        rest.postForEntity(bidsUrl("auction-6"), second, BidResponse.class);

    assertThat(secondResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(secondResp.getBody()).isNotNull();
    assertThat(secondResp.getBody().status()).isEqualTo(BidStatus.ACTIVE);
    assertThat(secondResp.getBody().amount()).isEqualByComparingTo("200");

    @SuppressWarnings("rawtypes")
    ResponseEntity<BidResponse[]> allBids =
        rest.getForEntity(bidsUrl("auction-6"), BidResponse[].class);

    assertThat(allBids.getBody()).hasSize(2);
    BidResponse firstBid = allBids.getBody()[0];
    assertThat(firstBid.status()).isEqualTo(BidStatus.OUTBID);
    BidResponse secondBid = allBids.getBody()[1];
    assertThat(secondBid.status()).isEqualTo(BidStatus.ACTIVE);
  }

  @Test
  void concurrentBidsOnSameAuctionProcessCorrectly() throws Exception {
    int threadCount = 20;
    String auctionId = "auction-concurrent";
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Future<BidResponse>> futures = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      final BigDecimal amount = new BigDecimal(10 + i * 10);
      final String bidderId = "bidder-" + i;
      futures.add(executor.submit(() -> {
        var request = jsonRequest("""
            {"bidderId": "%s", "amount": %s}
            """.formatted(bidderId, amount));
        try {
          @SuppressWarnings("rawtypes")
          ResponseEntity<BidResponse> resp =
              rest.postForEntity(bidsUrl(auctionId), request, BidResponse.class);
          return resp.getBody();
        } catch (HttpClientErrorException e) {
          return null;
        }
      }));
    }

    List<BidResponse> successfulBids = new ArrayList<>();
    for (Future<BidResponse> future : futures) {
      BidResponse result = future.get();
      if (result != null) {
        successfulBids.add(result);
      }
    }
    executor.shutdown();

    assertThat(successfulBids).isNotEmpty();

    BigDecimal maxAmount = successfulBids.stream()
        .map(BidResponse::amount)
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);
    assertThat(maxAmount).isEqualByComparingTo("200");

    @SuppressWarnings("rawtypes")
    ResponseEntity<BidResponse[]> allBidsResp =
        rest.getForEntity(bidsUrl(auctionId), BidResponse[].class);

    assertThat(allBidsResp.getBody()).isNotNull();
    assertThat(allBidsResp.getBody().length).isEqualTo(successfulBids.size());

    for (BidResponse bid : allBidsResp.getBody()) {
      if (bid.status() == BidStatus.ACTIVE) {
        assertThat(bid.amount()).isEqualByComparingTo(maxAmount);
      }
    }
  }
}
