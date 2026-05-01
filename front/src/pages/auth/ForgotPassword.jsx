import React, { useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import './AuthForm.css';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const getEndpoint = () => {
    const isDev = process.env.NODE_ENV === 'development';
    return isDev ? '/auth/forgot-password' : '/api/auth/forgot-password';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await axios.post(
        getEndpoint(),
        { email },
        { headers: { 'Content-Type': 'application/json' } },
      );
      setSubmitted(true);
    } catch (err) {
      // Backend always 200; show neutral error message only for connection issues
      setError('Не удалось отправить запрос. Попробуйте позже.');
    } finally {
      setLoading(false);
    }
  };

  if (submitted) {
    return (
      <div className="auth-container">
        <div className="auth-form-wrapper">
          <div className="auth-form">
            <div className="auth-header">
              <h2 className="auth-title">📨 Письмо отправлено</h2>
              <p className="auth-subtitle">
                Если на адрес <strong>{email}</strong> зарегистрирован аккаунт,
                мы отправили на него письмо с ссылкой для смены пароля.
                Ссылка действительна 30 минут.
              </p>
            </div>

            <div className="auth-footer">
              <p className="mb-0">
                <Link to="/auth/login" className="auth-link">← Вернуться ко входу</Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-container">
      <div className="auth-form-wrapper">
        <div className="auth-form">
          <div className="auth-header">
            <h2 className="auth-title">🔑 Восстановление пароля</h2>
            <p className="auth-subtitle">
              Укажите email, который вы использовали при регистрации.
              Мы отправим на него ссылку для смены пароля.
            </p>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="email" className="form-label">Электронная почта</label>
              <input
                id="email"
                name="email"
                type="email"
                className="form-control form-control-lg"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="example@mail.ru"
                required
                autoFocus
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
                  Отправка...
                </>
              ) : (
                'Отправить ссылку'
              )}
            </button>
          </form>

          <div className="auth-footer">
            <p className="mb-0">
              <Link to="/auth/login" className="auth-link">← Вернуться ко входу</Link>
            </p>
          </div>

          {error && <div className="alert alert-danger mt-3" role="alert">{String(error)}</div>}
        </div>
      </div>
    </div>
  );
}
