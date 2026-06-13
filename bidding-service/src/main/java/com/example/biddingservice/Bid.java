package com.example.biddingservice;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bids")
@Access(AccessType.FIELD)
public class Bid {

  @Id
  @Column(name = "id", length = 36)
  private String id;

  @Column(name = "auction_id", nullable = false, length = 36)
  private String auctionId;

  @Column(name = "bidder_id", nullable = false)
  private String bidderId;

  @Column(name = "amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private BidStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Bid() {
  }

  public Bid(
      String id,
      String auctionId,
      String bidderId,
      BigDecimal amount,
      BidStatus status,
      Instant createdAt) {
    this.id = id;
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.amount = amount;
    this.status = status;
    this.createdAt = createdAt;
  }

  public String id() { return id; }
  public String auctionId() { return auctionId; }
  public String bidderId() { return bidderId; }
  public BigDecimal amount() { return amount; }
  public BidStatus status() { return status; }
  public Instant createdAt() { return createdAt; }

  public Bid withStatus(BidStatus newStatus) {
    return new Bid(id, auctionId, bidderId, amount, newStatus, createdAt);
  }
}
