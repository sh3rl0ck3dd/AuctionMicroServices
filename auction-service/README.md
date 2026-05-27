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
- `POST /api/auctions/{auctionId}/start`
- `POST /api/auctions/{auctionId}/end`
- `POST /api/auctions/{auctionId}/cancel`

## Lifecycle

`DRAFT -> ACTIVE -> ENDED`  
`DRAFT -> CANCELLED`  
`ACTIVE -> CANCELLED`

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

Start, end, then fail to start again:

```bash
curl -X POST http://localhost:8080/api/auctions/{auctionId}/start
curl -X POST http://localhost:8080/api/auctions/{auctionId}/end
curl -X POST http://localhost:8080/api/auctions/{auctionId}/start
```
