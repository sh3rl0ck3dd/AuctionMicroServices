import { Routes, Route, Link } from 'react-router-dom'
import AuctionList from './pages/AuctionList'
import AuctionDetail from './pages/AuctionDetail'
import CreateAuction from './pages/CreateAuction'
import PlaceBid from './pages/PlaceBid'
import './App.css'

function App() {
  return (
    <div className="app">
      <nav className="nav">
        <Link to="/" className="nav-brand">Auction MicroServices</Link>
        <div className="nav-links">
          <Link to="/create">+ New Auction</Link>
        </div>
      </nav>
      <main className="content">
        <Routes>
          <Route path="/" element={<AuctionList />} />
          <Route path="/auction/:id" element={<AuctionDetail />} />
          <Route path="/create" element={<CreateAuction />} />
          <Route path="/auction/:id/bid" element={<PlaceBid />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
