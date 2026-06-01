import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

function CreateAuction() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ title: '', description: '', sellerId: '', startingPrice: '' })

  const handleSubmit = (e) => {
    e.preventDefault()
    alert('Auction created! (hardcoded demo)')
    navigate('/')
  }

  return (
    <div>
      <Link to="/" className="back-link">← Back to Auctions</Link>
      <div className="card">
        <h2 style={{ marginBottom: '1rem' }}>Create Auction</h2>
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
          <button type="submit" className="btn btn-primary">Create Auction</button>
        </form>
      </div>
    </div>
  )
}

export default CreateAuction
