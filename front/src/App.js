import React from 'react';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import './App.css';
import StatementPage from './pages/statement/Statement';
import LoginPage from './pages/auth/Login.jsx';
import RegisterPage from './pages/auth/Register.jsx';
import { AuthProvider, RequireAuth } from './auth/AuthProvider';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="App">
          <nav style={{ padding: 10, borderBottom: '1px solid #ccc' }}>
            <Link to="/">Home</Link> | <Link to="/statement">Statement</Link> | <Link to="/auth/login">Login</Link> | <Link to="/auth/register">Register</Link>
          </nav>
          <main style={{ padding: 10 }}>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/statement" element={<RequireAuth><StatementPage /></RequireAuth>} />
              <Route path="/auth/login" element={<LoginPage />} />
              <Route path="/auth/register" element={<RegisterPage />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}

function Home() {
  return (
    <div>
      <h2>Front application</h2>
      <p>Use <Link to="/statement">/statement</Link> to open the form.</p>
      <p>Use <Link to="/auth/login">/auth/login</Link> and <Link to="/auth/register">/auth/register</Link> for authentication.</p>
    </div>
  );
}

export default App;
