package com.example.auctionservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuctionServiceApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void healthEndpointReturnsUp() throws Exception {
    mockMvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.service").value("auction-service"));
  }

  @Test
  void createAuctionReturnsCreatedAuctionInDraftState() throws Exception {
    mockMvc.perform(
            post("/api/auctions")
                .contentType("application/json")
                .content(
                    """
                    {
                      "title": "Mechanical Keyboard",
                      "description": "Used keyboard",
                      "sellerId": "seller-1",
                      "startingPrice": 50
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.title").value("Mechanical Keyboard"))
        .andExpect(jsonPath("$.description").value("Used keyboard"))
        .andExpect(jsonPath("$.sellerId").value("seller-1"))
        .andExpect(jsonPath("$.startingPrice").value(50))
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  @Test
  void getAuctionByIdReturnsCreatedAuction() throws Exception {
    String auctionId = createAuctionAndGetId("Monitor", "24-inch monitor", "seller-2", 120);

    mockMvc.perform(get("/api/auctions/{auctionId}", auctionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(auctionId))
        .andExpect(jsonPath("$.title").value("Monitor"))
        .andExpect(jsonPath("$.status").value("DRAFT"));
  }

  @Test
  void getAuctionByIdReturnsNotFoundWhenMissing() throws Exception {
    mockMvc.perform(get("/api/auctions/{auctionId}", "missing-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void listAuctionsReturnsNewestFirst() throws Exception {
    String first = createAuctionAndGetId("Item A", "First item", "seller-3", 10);
    Thread.sleep(5);
    String second = createAuctionAndGetId("Item B", "Second item", "seller-3", 20);

    mockMvc.perform(get("/api/auctions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(second))
        .andExpect(jsonPath("$[1].id").value(first));
  }

  @Test
  void createAuctionReturnsBadRequestForInvalidPayload() throws Exception {
    mockMvc.perform(
            post("/api/auctions")
                .contentType("application/json")
                .content(
                    """
                    {
                      "title": "",
                      "description": "desc",
                      "sellerId": "seller-4",
                      "startingPrice": 0
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void startFromDraftAndEndFromActiveSucceed() throws Exception {
    String auctionId = createAuctionAndGetId("Desk", "Wooden desk", "seller-5", 75);

    transition(auctionId, "start")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    transition(auctionId, "end")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ENDED"));
  }

  @Test
  void secondStartAfterEndedFailsWithClearError() throws Exception {
    String auctionId = createAuctionAndGetId("Chair", "Office chair", "seller-6", 40);

    transition(auctionId, "start").andExpect(status().isOk());
    transition(auctionId, "end").andExpect(status().isOk());

    transition(auctionId, "start")
        .andExpect(status().isConflict())
        .andExpect(content().string(containsString("Cannot start auction in ENDED state")));
  }

  @Test
  void cancelWorksFromDraftAndActive() throws Exception {
    String draftAuctionId = createAuctionAndGetId("Lamp", "Desk lamp", "seller-7", 15);
    transition(draftAuctionId, "cancel")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));

    String activeAuctionId = createAuctionAndGetId("Tablet", "Android tablet", "seller-8", 90);
    transition(activeAuctionId, "start").andExpect(status().isOk());
    transition(activeAuctionId, "cancel")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
  }

  @Test
  void invalidTransitionFromDraftToEndFails() throws Exception {
    String auctionId = createAuctionAndGetId("Phone", "Used phone", "seller-9", 55);

    transition(auctionId, "end")
        .andExpect(status().isConflict())
        .andExpect(content().string(containsString("Cannot end auction in DRAFT state")));
  }

  @Test
  void transitionFailsWithNotFoundForMissingAuction() throws Exception {
    transition("missing-id", "start").andExpect(status().isNotFound());
  }

  private String createAuctionAndGetId(
      String title, String description, String sellerId, int startingPrice) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auctions")
                    .contentType("application/json")
                    .content(
                        """
                        {
                          "title": "%s",
                          "description": "%s",
                          "sellerId": "%s",
                          "startingPrice": %d
                        }
                        """
                            .formatted(title, description, sellerId, startingPrice)))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
    return created.get("id").asText();
  }

  private org.springframework.test.web.servlet.ResultActions transition(String auctionId, String action)
      throws Exception {
    return mockMvc.perform(post("/api/auctions/{auctionId}/{action}", auctionId, action));
  }
}
