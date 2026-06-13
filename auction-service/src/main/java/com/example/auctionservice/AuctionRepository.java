package com.example.auctionservice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, String> {

  List<Auction> findByStatus(AuctionStatus status);
}
