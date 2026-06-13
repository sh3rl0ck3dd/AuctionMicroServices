package com.example.auctionservice;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "auctions")
@Access(AccessType.FIELD)
public class Auction {

  @Id
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description")
  private String description;

  @Column(name = "seller_id", nullable = false)
  private String sellerId;

  @Column(name = "starting_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal startingPrice;

  @Column(name = "current_price", nullable = false, precision = 19, scale = 4)
  private BigDecimal currentPrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AuctionStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "ends_at")
  private Instant endsAt;

  @Column(name = "last_bid_time")
  private Instant lastBidTime;

  @Version
  @Column(name = "version")
  private Long version;

  protected Auction() {
  }

  public Auction(
      String id,
      String title,
      String description,
      String sellerId,
      BigDecimal startingPrice,
      BigDecimal currentPrice,
      AuctionStatus status,
      Instant createdAt,
      Instant endsAt,
      Instant lastBidTime,
      Long version) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.sellerId = sellerId;
    this.startingPrice = startingPrice;
    this.currentPrice = currentPrice;
    this.status = status;
    this.createdAt = createdAt;
    this.endsAt = endsAt;
    this.lastBidTime = lastBidTime;
    this.version = version;
  }

  public String id() { return id; }
  public String title() { return title; }
  public String description() { return description; }
  public String sellerId() { return sellerId; }
  public BigDecimal startingPrice() { return startingPrice; }
  public BigDecimal currentPrice() { return currentPrice; }
  public AuctionStatus status() { return status; }
  public Instant createdAt() { return createdAt; }
  public Instant endsAt() { return endsAt; }
  public Instant lastBidTime() { return lastBidTime; }
  public Long version() { return version; }

  public Auction withStatus(AuctionStatus newStatus) {
    return new Auction(id, title, description, sellerId, startingPrice, currentPrice, newStatus, createdAt, endsAt, lastBidTime, version);
  }

  public Auction withCurrentPrice(BigDecimal newPrice) {
    return new Auction(id, title, description, sellerId, startingPrice, newPrice, status, createdAt, endsAt, lastBidTime, version);
  }

  public Auction withEndsAt(Instant newEndsAt) {
    return new Auction(id, title, description, sellerId, startingPrice, currentPrice, status, createdAt, newEndsAt, lastBidTime, version);
  }

  public Auction withLastBidTime(Instant newLastBidTime) {
    return new Auction(id, title, description, sellerId, startingPrice, currentPrice, status, createdAt, endsAt, newLastBidTime, version);
  }
}
