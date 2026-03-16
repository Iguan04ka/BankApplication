import React from 'react';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import './App.css';
import StatementPage from './pages/statement/Statement';

function App() {
  return (
    <BrowserRouter>
      <div className="App">
        <nav style={{ padding: 10, borderBottom: '1px solid #ccc' }}>
          <Link to="/">Home</Link> | <Link to="/statement">Statement</Link>
        </nav>
        <main style={{ padding: 10 }}>
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/statement" element={<StatementPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}

function Home() {
  return (
    <div>
      <h2>Front application</h2>
      <p>Use <Link to="/statement">/statement</Link> to open the form.</p>
    </div>
  );
}

export default App;
