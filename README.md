# Auction Learning Platform

Monorepo for learning and iterating on auction-based microservices.

## Current Status

Only `auction-service` is implemented and runnable right now.

## Repository Layout

- `auction-service/` - active Spring Boot service
- `bidding-service/` - placeholder
- `notification-service/` - placeholder
- `react-ui/` - placeholder
- `scripts/` - helper scripts
- `docs/` - architecture and notes

## Quick Start (auction-service)

```bash
cd auction-service
./gradlew bootRun
```

Health check:

```bash
curl http://localhost:8080/api/health
```

Create auction:

```bash
curl -X POST http://localhost:8080/api/auctions \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Mechanical Keyboard",
    "description": "Used keyboard",
    "sellerId": "seller-1",
    "startingPrice": 50
  }'
```
