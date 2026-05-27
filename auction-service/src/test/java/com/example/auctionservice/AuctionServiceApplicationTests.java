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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
  void createAuctionReturnsCreatedAuction() throws Exception {
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
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void getAuctionByIdReturnsCreatedAuction() throws Exception {
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/auctions")
                    .contentType("application/json")
                    .content(
                        """
                        {
                          "title": "Monitor",
                          "description": "24-inch monitor",
                          "sellerId": "seller-2",
                          "startingPrice": 120
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn();

    JsonNode createdAuction = objectMapper.readTree(createResult.getResponse().getContentAsString());
    String auctionId = createdAuction.get("id").asText();

    mockMvc.perform(get("/api/auctions/{auctionId}", auctionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(auctionId))
        .andExpect(jsonPath("$.title").value("Monitor"))
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void getAuctionByIdReturnsNotFoundWhenMissing() throws Exception {
    mockMvc.perform(get("/api/auctions/{auctionId}", "missing-id"))
        .andExpect(status().isNotFound());
  }

  @Test
  void listAuctionsReturnsNewestFirst() throws Exception {
    JsonNode first = createAuction("Item A", "First item", "seller-3", 10);
    Thread.sleep(5);
    JsonNode second = createAuction("Item B", "Second item", "seller-3", 20);

    mockMvc.perform(get("/api/auctions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(second.get("id").asText()))
        .andExpect(jsonPath("$[1].id").value(first.get("id").asText()));
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

  private JsonNode createAuction(String title, String description, String sellerId, int startingPrice)
      throws Exception {
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

    return objectMapper.readTree(result.getResponse().getContentAsString());
  }
}
