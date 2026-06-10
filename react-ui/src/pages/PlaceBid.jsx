import { useState, useEffect } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'

function PlaceBid() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState({ bidderId: '', amount: '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [auctionTitle, setAuctionTitle] = useState(null)

  useEffect(() => {
    fetch(`/api/auctions/${id}`)
      .then(res => {
        if (!res.ok) throw new Error('Failed to load auction')
        return res.json()
      })
      .then(auction => setAuctionTitle(auction.title))
      .catch(() => setAuctionTitle(null))
  }, [id])

  const handleSubmit = (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)

    fetch(`/api/auctions/${id}/bids`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        bidderId: form.bidderId,
        amount: Number(form.amount),
      }),
    })
      .then(res => {
        if (!res.ok) {
          return res.text().then(body => {
            let msg = 'Bid failed'
            try {
              const parsed = JSON.parse(body)
              msg = parsed.message || msg
            } catch (_) {
              msg = body || msg
            }
            throw new Error(msg)
          })
        }
        return res.json()
      })
      .then(() => navigate(`/auction/${id}`))
      .catch(err => {
        setError(err.message)
        setSubmitting(false)
      })
  }

  return (
    <div>
      <Link to={`/auction/${id}`} className="back-link">← Back to Auction</Link>
      <div className="card">
        <h2 style={{ marginBottom: '1rem' }}>Place Bid</h2>
        <p style={{ marginBottom: '1rem', color: '#666' }}>
          Bidding on auction <strong>{auctionTitle || id}</strong>
        </p>
        {error && <p style={{ color: 'red', marginBottom: '1rem' }}>Error: {error}</p>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Bidder ID</label>
            <input
              type="text"
              value={form.bidderId}
              onChange={e => setForm({ ...form, bidderId: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Bid Amount ($)</label>
            <input
              type="number"
              min="1"
              value={form.amount}
              onChange={e => setForm({ ...form, amount: e.target.value })}
              required
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit Bid'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default PlaceBid
