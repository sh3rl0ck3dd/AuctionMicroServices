CREATE TABLE auction.auctions (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    seller_id VARCHAR(255) NOT NULL,
    starting_price NUMERIC(19, 4) NOT NULL,
    current_price NUMERIC(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    last_bid_time TIMESTAMPTZ
);
