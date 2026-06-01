package com.example.auctionservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuctionServiceApplicationTests {

  @LocalServerPort
  private int port;

  private final RestTemplate rest = new RestTemplate();

  private String auctionsUrl() {
    return "http://localhost:" + port + "/api/auctions";
  }

  private String auctionUrl(String auctionId) {
    return "http://localhost:" + port + "/api/auctions/" + auctionId;
  }

  private String transitionUrl(String auctionId, String action) {
    return "http://localhost:" + port + "/api/auctions/" + auctionId + "/" + action;
  }

  private HttpEntity<String> jsonRequest(String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }

  @Test
  void healthEndpointReturnsUp() {
    @SuppressWarnings("rawtypes")
    ResponseEntity<String> response =
        rest.getForEntity("http://localhost:" + port + "/api/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("UP");
  }

  @Test
  void createAuctionReturnsCreatedAuctionInDraftState() {
    var request = jsonRequest("""
        {"title": "Mechanical Keyboard", "description": "Used keyboard", "sellerId": "seller-1", "startingPrice": 50}
        """);

    @SuppressWarnings("rawtypes")
    ResponseEntity<AuctionResponse> response =
        rest.postForEntity(auctionsUrl(), request, AuctionResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isNotBlank();
    assertThat(response.getBody().title()).isEqualTo("Mechanical Keyboard");
    assertThat(response.getBody().description()).isEqualTo("Used keyboard");
    assertThat(response.getBody().sellerId()).isEqualTo("seller-1");
    assertThat(response.getBody().startingPrice()).isEqualByComparingTo("50");
    assertThat(response.getBody().status()).isEqualTo(AuctionStatus.DRAFT);
  }

  @Test
  void getAuctionByIdReturnsCreatedAuction() {
    String auctionId = createAuction("Monitor", "24-inch monitor", "seller-2", 120);

    @SuppressWarnings("rawtypes")
    ResponseEntity<AuctionResponse> response =
        rest.getForEntity(auctionUrl(auctionId), AuctionResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(auctionId);
    assertThat(response.getBody().title()).isEqualTo("Monitor");
    assertThat(response.getBody().status()).isEqualTo(AuctionStatus.DRAFT);
  }

  @Test
  void getAuctionByIdReturnsNotFoundWhenMissing() {
    assertThatThrownBy(
            () -> rest.getForEntity(auctionUrl("missing-id"), AuctionResponse.class))
        .isInstanceOf(HttpClientErrorException.NotFound.class);
  }

  @Test
  void listAuctionsReturnsNewestFirst() throws Exception {
    String first = createAuction("Item A", "First item", "seller-3", 10);
    Thread.sleep(5);
    String second = createAuction("Item B", "Second item", "seller-3", 20);

    @SuppressWarnings("rawtypes")
    ResponseEntity<AuctionResponse[]> response =
        rest.getForEntity(auctionsUrl(), AuctionResponse[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
    assertThat(response.getBody()[0].id()).isEqualTo(second);
    assertThat(response.getBody()[1].id()).isEqualTo(first);
  }

  @Test
  void createAuctionReturnsBadRequestForInvalidPayload() {
    var request = jsonRequest("""
        {"title": "", "description": "desc", "sellerId": "seller-4", "startingPrice": 0}
        """);

    assertThatThrownBy(
            () -> rest.postForEntity(auctionsUrl(), request, AuctionResponse.class))
        .isInstanceOf(HttpClientErrorException.BadRequest.class);
  }

  @Test
  void startFromDraftAndEndFromActiveSucceed() {
    String auctionId = createAuction("Desk", "Wooden desk", "seller-5", 75);

    @SuppressWarnings("rawtypes")
    ResponseEntity<AuctionResponse> started =
        rest.postForEntity(transitionUrl(auctionId, "start"), null, AuctionResponse.class);
    assertThat(started.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(started.getBody().status()).isEqualTo(AuctionStatus.ACTIVE);

    @SuppressWarnings("rawtypes")
    ResponseEntity<AuctionResponse> ended =
        rest.postForEntity(transitionUrl(auctionId, "end"), null, AuctionResponse.class);
    assertThat(ended.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(ended.getBody().status()).isEqualTo(AuctionStatus.ENDED);
  }

  @Test
  void secondStartAfterEndedFailsWithClearError() {
    String auctionId = createAuction("Chair", "Office chair", "seller-6", 40);

    rest.postForEntity(transitionUrl(auctionId, "start"), null, AuctionResponse.class);
    rest.postForEntity(transitionUrl(auctionId, "end"), null, AuctionResponse.class);

    assertThatThrownBy(
            () -> rest.postForEntity(transitionUrl(auctionId, "start"), null, String.class))
        .isInstanceOf(HttpClientErrorException.Conflict.class);
  }

  @Test
  void cancelWorksFromDraftAndActive() {
    String draftId = createAuction("Lamp", "Desk lamp", "seller-7", 15);

    @SuppressWarnings("rawtypes")
    ResponseEntity<AuctionResponse> cancelled =
        rest.postForEntity(transitionUrl(draftId, "cancel"), null, AuctionResponse.class);
    assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelled.getBody().status()).isEqualTo(AuctionStatus.CANCELLED);

    String activeId = createAuction("Tablet", "Android tablet", "seller-8", 90);
    rest.postForEntity(transitionUrl(activeId, "start"), null, AuctionResponse.class);

    @SuppressWarnings("rawtypes")
    ResponseEntity<AuctionResponse> cancelled2 =
        rest.postForEntity(transitionUrl(activeId, "cancel"), null, AuctionResponse.class);
    assertThat(cancelled2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(cancelled2.getBody().status()).isEqualTo(AuctionStatus.CANCELLED);
  }

  @Test
  void invalidTransitionFromDraftToEndFails() {
    String auctionId = createAuction("Phone", "Used phone", "seller-9", 55);

    assertThatThrownBy(
            () -> rest.postForEntity(transitionUrl(auctionId, "end"), null, String.class))
        .isInstanceOf(HttpClientErrorException.Conflict.class);
  }

  @Test
  void transitionFailsWithNotFoundForMissingAuction() {
    assertThatThrownBy(
            () -> rest.postForEntity(transitionUrl("missing-id", "start"), null, String.class))
        .isInstanceOf(HttpClientErrorException.NotFound.class);
  }

  @Test
  void highestBidUpdatesCurrentPrice() {
    String auctionId = createAuction("Laptop", "Gaming laptop", "seller-10", 50);

    rest.postForEntity(transitionUrl(auctionId, "start"), null, AuctionResponse.class);

    var highestBidRequest = jsonRequest("""
        {"bidId": "bid-1", "bidderId": "user-1", "amount": 80}
        """);

    @SuppressWarnings("rawtypes")
    ResponseEntity<AuctionResponse> updateResp =
        rest.postForEntity(highestBidUrl(auctionId), highestBidRequest, AuctionResponse.class);

    assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResp.getBody().currentPrice()).isEqualByComparingTo("80");

    @SuppressWarnings("rawtypes")
    ResponseEntity<AuctionResponse> getResp =
        rest.getForEntity(auctionUrl(auctionId), AuctionResponse.class);

    assertThat(getResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResp.getBody().currentPrice()).isEqualByComparingTo("80");
  }

  private String highestBidUrl(String auctionId) {
    return "http://localhost:" + port + "/api/auctions/" + auctionId + "/highest-bid";
  }

  private String createAuction(String title, String description, String sellerId, int startingPrice) {
    var request = jsonRequest("""
        {"title": "%s", "description": "%s", "sellerId": "%s", "startingPrice": %d}
        """.formatted(title, description, sellerId, startingPrice));

    ResponseEntity<AuctionResponse> response =
        rest.postForEntity(auctionsUrl(), request, AuctionResponse.class);
    return response.getBody().id();
  }
}
