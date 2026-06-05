package com.example.auctionservice;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuctionStore {

  private static final Logger log = LoggerFactory.getLogger(AuctionStore.class);

  private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
  private final ObjectMapper mapper;
  private final Path filePath;

  public AuctionStore(@Value("${auction-service.data.file:}") String dataFilePath) {
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
        Map<String, Auction> loaded = mapper.readValue(json, new TypeReference<Map<String, Auction>>() {});
        auctions.putAll(loaded);
        log.info("Loaded {} auctions from {}", loaded.size(), filePath);
      }
    } catch (IOException e) {
      log.error("Failed to load auctions from {}", filePath, e);
    }
  }

  public Auction put(String id, Auction auction) {
    Auction result = auctions.put(id, auction);
    saveToFile();
    return result;
  }

  public Auction get(String id) {
    return auctions.get(id);
  }

  public Collection<Auction> values() {
    return auctions.values();
  }

  private void saveToFile() {
    if (filePath == null) {
      return;
    }
    try {
      Files.createDirectories(filePath.getParent());
      String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(auctions);
      Files.writeString(filePath, json);
    } catch (IOException e) {
      log.error("Failed to save auctions to {}", filePath, e);
    }
  }
}
