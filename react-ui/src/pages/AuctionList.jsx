import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'

function AuctionList() {
  const [auctions, setAuctions] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetch('/api/auctions')
      .then(res => {
        if (!res.ok) throw new Error('Failed to load auctions')
        return res.json()
      })
      .then(data => setAuctions(data))
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Loading auctions...</p>

  if (error) return <div className="card"><p style={{ color: 'red' }}>Error: {error}</p></div>

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem' }}>Auctions</h1>
      {auctions.length === 0 ? (
        <p style={{ color: '#666' }}>No auctions yet. Create one!</p>
      ) : (
        auctions.map(auction => (
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
        ))
      )}
    </div>
  )
}

export default AuctionList
