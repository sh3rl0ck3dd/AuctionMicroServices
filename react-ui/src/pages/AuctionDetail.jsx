import { useParams, Link } from 'react-router-dom'

const HARDCODED_BIDS = [
  { id: 'bid-1', bidderId: 'user-1', amount: 60, status: 'OUTBID', createdAt: '2026-05-30T10:00:00Z' },
  { id: 'bid-2', bidderId: 'user-2', amount: 75, status: 'ACTIVE', createdAt: '2026-05-30T11:00:00Z' },
]

const HARDCODED_AUCTION = {
  id: '1',
  title: 'Vintage Watch',
  description: 'A classic mechanical watch from the 1960s. Fully functional and in great condition.',
  sellerId: 'seller-1',
  startingPrice: 50,
  currentPrice: 75,
  status: 'ACTIVE',
}

function AuctionDetail() {
  const { id } = useParams()
  const auction = HARDCODED_AUCTION

  return (
    <div>
      <Link to="/" className="back-link">← Back to Auctions</Link>
      <div className="card">
        <h2>{auction.title}</h2>
        <span className={`status-badge status-${auction.status.toLowerCase()}`} style={{ margin: '0.5rem 0', display: 'inline-block' }}>
          {auction.status}
        </span>
        <p style={{ margin: '0.75rem 0' }}>{auction.description}</p>
        <div className="card-meta" style={{ marginBottom: '0.5rem' }}>
          <span><strong>Seller:</strong> {auction.sellerId}</span>
          <span><strong>Starting:</strong> ${auction.startingPrice}</span>
          <span><strong>Current:</strong> ${auction.currentPrice}</span>
        </div>
        {auction.status === 'ACTIVE' && (
          <Link to={`/auction/${id}/bid`} className="btn btn-primary" style={{ marginTop: '0.5rem', display: 'inline-block' }}>
            Place Bid
          </Link>
        )}
      </div>

      <h3 style={{ margin: '1.5rem 0 0.75rem' }}>Bids</h3>
      {HARDCODED_BIDS.length === 0 ? (
        <p style={{ color: '#666' }}>No bids yet.</p>
      ) : (
        HARDCODED_BIDS.map(bid => (
          <div className="card" key={bid.id} style={{ opacity: bid.status === 'OUTBID' ? 0.6 : 1 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <strong>{bid.bidderId}</strong>
                <span className={`status-badge ${bid.status === 'ACTIVE' ? 'status-active' : 'status-ended'}`} style={{ marginLeft: '0.75rem' }}>
                  {bid.status}
                </span>
              </div>
              <strong>${bid.amount}</strong>
            </div>
          </div>
        ))
      )}
    </div>
  )
}

export default AuctionDetail
