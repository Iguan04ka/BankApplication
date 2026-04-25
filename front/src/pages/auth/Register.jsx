import React, { useState } from 'react';
import axios from 'axios';

export default function Register() {
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const getEndpoint = () => {
    const isDev = process.env.NODE_ENV === 'development';
    return isDev ? '/auth/register' : '/api/auth/register';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);
    setError(null);
    try {
      const payload = { sub: login, password };
      const endpoint = getEndpoint();
      const res = await axios.post(endpoint, payload, {
        headers: { 'Content-Type': 'application/json' },
      });
      if (res.status === 200) {
        setMessage('Регистрация прошла успешно');
      } else {
        setError('Unexpected response status: ' + res.status);
      }
    } catch (err) {
      setError(err.response?.data ? JSON.stringify(err.response.data) : err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 480 }}>
      <h2>Register</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Login</label>
          <input name="login" value={login} onChange={(e) => setLogin(e.target.value)} />
        </div>
        <div>
          <label>Password</label>
          <input name="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <div style={{ marginTop: 10 }}>
          <button type="submit" disabled={loading}>{loading ? 'Sending...' : 'Register'}</button>
        </div>
      </form>

      <div style={{ marginTop: 12 }}>
        {message && <div style={{ color: 'green' }}>{message}</div>}
        {error && <div style={{ color: 'red' }}>{String(error)}</div>}
      </div>
    </div>
  );
}
