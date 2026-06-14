CREATE TABLE bidding.bids (
    id VARCHAR(36) PRIMARY KEY,
    auction_id VARCHAR(36) NOT NULL,
    bidder_id VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_bids_auction_id ON bidding.bids(auction_id);
