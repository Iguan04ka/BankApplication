import React, { useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import './AuthForm.css';

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
        setMessage('Registration successful! You can now login.');
        setLogin('');
        setPassword('');
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
    <div className="auth-container">
      <div className="auth-form-wrapper">
            <div className="auth-form">
              <div className="auth-header">
                <h2 className="auth-title">✨ Регистрация в АтласКредит</h2>
                <p className="auth-subtitle">Создайте аккаунт для работы с кредитными заявками</p>
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
                placeholder="Придумайте надёжный пароль"
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
                  Создание...
                </>
              ) : (
                'Зарегистрироваться'
              )}
            </button>
          </form>

          <div className="auth-footer">
            <p className="mb-0">Уже есть аккаунт? <Link to="/auth/login" className="auth-link">Войти</Link></p>
          </div>

          {message && <div className="alert alert-success mt-3" role="alert">{message}</div>}
          {error && <div className="alert alert-danger mt-3" role="alert">{String(error)}</div>}
        </div>
      </div>
    </div>
  );
}
