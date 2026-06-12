package com.example.biddingservice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BidStore {

  private static final Logger log = LoggerFactory.getLogger(BidStore.class);

  private final Map<String, List<Bid>> bidsByAuctionId = new ConcurrentHashMap<>();
  private final ObjectMapper mapper;
  private final Path filePath;

  public BidStore(@Value("${bidding-service.data.file:}") String dataFilePath) {
    this.mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    if (dataFilePath != null && !dataFilePath.isBlank()) {
      this.filePath = Paths.get(dataFilePath);
    } else {
      this.filePath = null;
    }
  }

  @PostConstruct
  void loadFromFile() {
    if (filePath == null) {
      return;
    }
    try {
      if (Files.exists(filePath)) {
        String json = Files.readString(filePath);
        Map<String, List<Bid>> loaded = mapper.readValue(json, new TypeReference<Map<String, List<Bid>>>() {});
        bidsByAuctionId.putAll(loaded);
        log.info("Loaded bids for {} auctions from {}", loaded.size(), filePath);
      }
    } catch (IOException e) {
      log.error("Failed to load bids from {}", filePath, e);
    }
  }

  public List<Bid> get(String auctionId) {
    return bidsByAuctionId.getOrDefault(auctionId, List.of());
  }

  public List<Bid> putIfAbsent(String auctionId, List<Bid> bids) {
    List<Bid> result = bidsByAuctionId.putIfAbsent(auctionId, bids);
    saveToFile();
    return result;
  }

  public void put(String auctionId, List<Bid> bids) {
    bidsByAuctionId.put(auctionId, bids);
    saveToFile();
  }

  public void addBid(String auctionId, Bid bid) {
    bidsByAuctionId.computeIfAbsent(auctionId, k -> new ArrayList<>()).add(bid);
    saveToFile();
  }

  public Collection<List<Bid>> values() {
    return bidsByAuctionId.values();
  }

  private void saveToFile() {
    if (filePath == null) {
      return;
    }
    try {
      Files.createDirectories(filePath.getParent());
      String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(bidsByAuctionId);
      Files.writeString(filePath, json);
      log.info("Saved bids to {}", filePath);
    } catch (IOException e) {
      log.error("Failed to save bids to {}", filePath, e);
    }
  }
}
