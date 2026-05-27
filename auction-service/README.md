# auction-service

Spring Boot scaffold service for auction workflows.

## Run

```bash
./gradlew bootRun
```

Or with a local Maven install:

```bash
./gradlew bootRun
```

## Test

```bash
./gradlew test
```

## Endpoint

- `GET /api/health`
- `POST /api/auctions`
- `GET /api/auctions/{auctionId}`
- `GET /api/auctions`

## Example

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

Fetch by ID:

```bash
curl http://localhost:8080/api/auctions/{auctionId}
```
