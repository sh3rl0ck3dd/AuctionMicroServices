CREATE TABLE auction.watchlists (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    auction_id VARCHAR(36) NOT NULL REFERENCES auction.auctions(id),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_watchlists_user_id ON auction.watchlists(user_id);
CREATE INDEX idx_watchlists_auction_id ON auction.watchlists(auction_id);
