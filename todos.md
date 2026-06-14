## Post-PR Review Action Items

### ~1. SqsAuctionEndConsumer: add retry on OptimisticLockingFailureException~ ✅ DONE
`auction-service/src/main/java/.../SqsAuctionEndConsumer.java` → `processMessage()`
- Added retry loop (3 attempts) around read→decide→save, mirroring `AuctionService.updateHighestBid()` pattern
- On exhaustion: logs warning and returns (message stays in queue for next poll)

### ~2. SqsAuctionEndConsumer: handle sendScheduledEnd failure after successful save~ ✅ DONE
`auction-service/src/main/java/.../SqsAuctionEndConsumer.java` → `processMessage()` extension path
- Swapped order: `sendScheduledEnd()` now called before `repo.save()` — if SQS send fails, exception propagates, message not deleted, retried on next poll

### ~3. Create custom PostgreSQL schema~ ✅ DONE
- `V0__create_schema.sql` creates `auction` schema in auction-service, `bidding` schema in bidding-service
- Updated V1-V3 migrations to use schema-qualified table names (`auction.auctions`, `bidding.bids`)
- Updated `@Table(schema = "...")` on Auction (`"auction"`) and Bid (`"bidding"`) entities
