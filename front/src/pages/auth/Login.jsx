import React, { useState, useEffect } from 'react';
import axios from 'axios';
import client from '../../api/client';
import { useAuth } from '../../auth/AuthProvider';
import { useNavigate, Link } from 'react-router-dom';
import './AuthForm.css';

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
      navigate('/account');
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
        // redirect to account
        navigate('/account');
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
    <div className="auth-container">
      <div className="auth-form-wrapper">
        <div className="auth-form">
          <div className="auth-header">
            <h2 className="auth-title">🔐 Вход в АтласКредит</h2>
            <p className="auth-subtitle">Введите ваши учётные данные для доступа к системе заявок</p>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="login" className="form-label">Логин</label>
              <input
                id="login"
                name="login"
                type="text"
                className="form-control form-control-lg"
                value={login}
                onChange={(e) => setLogin(e.target.value)}
                placeholder="Введите логин или e-mail"
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="password" className="form-label">Пароль</label>
              <input
                id="password"
                name="password"
                type="password"
                className="form-control form-control-lg"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Введите пароль"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary btn-lg w-100 mt-3"
            >
                  {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Вход...
                </>
              ) : (
                'Войти'
              )}
            </button>
          </form>

          <div className="auth-footer">
            <p className="mb-0">Ещё нет аккаунта? <Link to="/auth/register" className="auth-link">Зарегистрироваться</Link></p>
          </div>

          {message && <div className="alert alert-success mt-3" role="alert">{message}</div>}
          {error && <div className="alert alert-danger mt-3" role="alert">{String(error)}</div>}
        </div>
      </div>
    </div>
  );
}
