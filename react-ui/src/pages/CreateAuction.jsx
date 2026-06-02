import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

function CreateAuction() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', description: '', sellerId: '', startingPrice: '' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError(null)

    fetch('/api/auctions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: form.title,
        description: form.description,
        sellerId: form.sellerId,
        startingPrice: Number(form.startingPrice),
      }),
    })
      .then(res => {
        if (!res.ok) throw new Error('Failed to create auction')
        return res.json()
      })
      .then(() => navigate('/'))
      .catch(err => {
        setError(err.message)
        setSubmitting(false)
      })
  }

  return (
    <div>
      <Link to="/" className="back-link">← Back to Auctions</Link>
      <div className="card">
        <h2 style={{ marginBottom: '1rem' }}>Create Auction</h2>
        {error && <p style={{ color: 'red', marginBottom: '1rem' }}>Error: {error}</p>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Title</label>
            <input
              type="text"
              value={form.title}
              onChange={e => setForm({ ...form, title: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea
              value={form.description}
              onChange={e => setForm({ ...form, description: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Seller ID</label>
            <input
              type="text"
              value={form.sellerId}
              onChange={e => setForm({ ...form, sellerId: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label>Starting Price ($)</label>
            <input
              type="number"
              min="1"
              value={form.startingPrice}
              onChange={e => setForm({ ...form, startingPrice: e.target.value })}
              required
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creating...' : 'Create Auction'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default CreateAuction
