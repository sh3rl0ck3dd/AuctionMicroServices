import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'

function AuctionDetail() {
  const { id } = useParams()
  const [auction, setAuction] = useState(null)
  const [bids, setBids] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    Promise.all([
      fetch(`/api/auctions/${id}`).then(res => {
        if (!res.ok) throw new Error('Auction not found')
        return res.json()
      }),
      fetch(`/api/auctions/${id}/bids`).then(res => {
        if (!res.ok) return []
        return res.json()
      }),
    ])
      .then(([auctionData, bidData]) => {
        setAuction(auctionData)
        setBids(Array.isArray(bidData) ? bidData : [])
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (loading || error || !auction) return

    const source = new EventSource(`/api/notifications/auctions/${id}/stream`)

    source.onmessage = (event) => {
      let update
      try {
        update = JSON.parse(event.data)
      } catch {
        return
      }
      const { type, data } = update
      console.log(type, data)

      if (type === 'bid.accepted') {
        setBids(prev => [
          { id: data.bidId, bidderId: data.bidderId, amount: data.amount, status: data.status },
          ...prev.map(b => b.status === 'ACTIVE' ? { ...b, status: 'OUTBID' } : b),
        ])
        setAuction(prev => prev ? { ...prev, currentPrice: data.amount } : prev)
      } else if (type === 'bid.rejected') {
        // no UI change
      } else if (type.startsWith('auction.')) {
        fetch(`/api/auctions/${id}`)
          .then(res => res.ok ? res.json() : null)
          .then(updated => { if (updated) setAuction(updated) })
          .catch(() => {})
      }
    }

    source.onerror = () => {}

    return () => source.close()
  }, [id, loading, error, auction])

  if (loading) return <p>Loading auction...</p>

  if (error) return <div className="card"><p style={{ color: 'red' }}>Error: {error}</p></div>

  if (!auction) return <div className="card"><p>Auction not found.</p></div>

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
      {bids.length === 0 ? (
        <p style={{ color: '#666' }}>No bids yet.</p>
      ) : (
        bids.map(bid => (
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
