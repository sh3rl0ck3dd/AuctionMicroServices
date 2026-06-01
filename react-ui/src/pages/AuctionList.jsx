import { Link } from 'react-router-dom'

const HARDCODED_AUCTIONS = [
  { id: '1', title: 'Vintage Watch', description: 'A classic mechanical watch', sellerId: 'seller-1', startingPrice: 50, currentPrice: 75, status: 'ACTIVE' },
  { id: '2', title: 'Gaming Laptop', description: 'High-end gaming laptop', sellerId: 'seller-2', startingPrice: 500, currentPrice: 500, status: 'DRAFT' },
  { id: '3', title: 'Mountain Bike', description: 'Lightweight trail bike', sellerId: 'seller-3', startingPrice: 200, currentPrice: 320, status: 'ACTIVE' },
  { id: '4', title: 'Antique Chair', description: 'Restored wooden chair', sellerId: 'seller-4', startingPrice: 80, currentPrice: 80, status: 'ENDED' },
  { id: '5', title: 'DSLR Camera', description: 'Canon EOS with lens', sellerId: 'seller-5', startingPrice: 300, currentPrice: 300, status: 'CANCELLED' },
]

function AuctionList() {
  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Auctions</h1>
      {HARDCODED_AUCTIONS.map(auction => (
        <div className="card" key={auction.id}>
          <h3>
            <Link to={`/auction/${auction.id}`} style={{ textDecoration: 'none', color: '#1a1a2e' }}>
              {auction.title}
            </Link>
          </h3>
          <p style={{ marginBottom: '0.5rem' }}>{auction.description}</p>
          <div className="card-meta">
            <span>${auction.currentPrice}</span>
            <span className={`status-badge status-${auction.status.toLowerCase()}`}>
              {auction.status}
            </span>
            {auction.status === 'ACTIVE' && (
              <Link to={`/auction/${auction.id}/bid`} className="btn btn-primary" style={{ marginLeft: 'auto' }}>
                Place Bid
              </Link>
            )}
          </div>
        </div>
      ))}
    </div>
  )
}

export default AuctionList
