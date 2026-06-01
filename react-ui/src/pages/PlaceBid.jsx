import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'

function PlaceBid() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState({ bidderId: '', amount: '' })

  const handleSubmit = (e) => {
    e.preventDefault()
    alert(`Bid placed on auction ${id}! (hardcoded demo)`)
    navigate(`/auction/${id}`)
  }

  return (
    <div>
      <Link to={`/auction/${id}`} className="back-link">← Back to Auction</Link>
      <div className="card">
        <h2 style={{ marginBottom: '1rem' }}>Place Bid</h2>
        <p style={{ marginBottom: '1rem', color: '#666' }}>
          Bidding on auction <strong>{id}</strong>
        </p>
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
          <button type="submit" className="btn btn-primary">Submit Bid</button>
        </form>
      </div>
    </div>
  )
}

export default PlaceBid
