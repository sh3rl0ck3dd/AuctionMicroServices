package com.example.biddingservice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, String> {

  List<Bid> findByAuctionIdOrderByCreatedAtAsc(String auctionId);
}
