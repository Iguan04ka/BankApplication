import React, { useState, useEffect } from 'react';
import axios from 'axios';
import client from '../../api/client';
import { useAuth } from '../../auth/AuthProvider';
import { useNavigate } from 'react-router-dom';

export default function Login() {
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (auth && auth.user) {
      navigate('/statement');
    }
  }, [auth, navigate]);

  const getEndpoint = () => {
    const isDev = process.env.NODE_ENV === 'development';
    return isDev ? '/auth/login' : '/api/auth/login';
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
      if (res.status === 200 && res.data && res.data.accessToken) {
        // save tokens via auth context
        auth.login({ accessToken: res.data.accessToken, refreshToken: res.data.refreshToken });
        // ensure axios and client have Authorization header set immediately
        const token = res.data.accessToken;
        axios.defaults.headers.common.Authorization = `Bearer ${token}`;
        client.defaults.headers = client.defaults.headers || {};
        client.defaults.headers.common = client.defaults.headers.common || {};
        client.defaults.headers.common.Authorization = `Bearer ${token}`;
        // redirect to statement
        navigate('/statement');
      } else {
        setError('Unexpected response: ' + JSON.stringify(res.data));
      }
    } catch (err) {
      setError(err.response?.data ? JSON.stringify(err.response.data) : err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 480 }}>
      <h2>Login</h2>
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
          <button type="submit" disabled={loading}>{loading ? 'Sending...' : 'Login'}</button>
        </div>
      </form>

      <div style={{ marginTop: 12 }}>
        {message && <div style={{ color: 'green' }}>{message}</div>}
        {error && <div style={{ color: 'red' }}>{String(error)}</div>}
      </div>
    </div>
  );
}
