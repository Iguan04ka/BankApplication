import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useParams, Link, useNavigate } from 'react-router-dom';
import './AuthForm.css';

const getApiBase = () => (process.env.NODE_ENV === 'development' ? '' : '/api');

export default function ResetPassword() {
  const { token } = useParams();
  const navigate = useNavigate();
  const base = getApiBase();

  const [tokenValid, setTokenValid] = useState(null); // null = checking, true/false otherwise
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await axios.get(`${base}/auth/reset-password/${token}/valid`);
        if (!cancelled) setTokenValid(!!res.data?.valid);
      } catch {
        if (!cancelled) setTokenValid(false);
      }
    })();
    return () => { cancelled = true; };
  }, [base, token]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (password.length < 6) {
      setError('Пароль должен быть не короче 6 символов.');
      return;
    }
    if (password !== confirm) {
      setError('Пароли не совпадают.');
      return;
    }

    setLoading(true);
    try {
      await axios.post(
        `${base}/auth/reset-password`,
        { token, newPassword: password },
        { headers: { 'Content-Type': 'application/json' } },
      );
      setSuccess(true);
      setTimeout(() => navigate('/auth/login'), 2500);
    } catch (err) {
      setError(
        err.response?.data
          ? (typeof err.response.data === 'string' ? err.response.data : JSON.stringify(err.response.data))
          : err.message || 'Не удалось сменить пароль.',
      );
    } finally {
      setLoading(false);
    }
  };

  if (tokenValid === null) {
    return (
      <div className="auth-container">
        <div className="auth-form-wrapper">
          <div className="auth-form">
            <div className="auth-header">
              <p className="auth-subtitle">Проверка ссылки…</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (tokenValid === false) {
    return (
      <div className="auth-container">
        <div className="auth-form-wrapper">
          <div className="auth-form">
            <div className="auth-header">
              <h2 className="auth-title">⚠️ Ссылка недействительна</h2>
              <p className="auth-subtitle">
                Эта ссылка для восстановления пароля устарела или уже была использована.
                Запросите новую, чтобы продолжить.
              </p>
            </div>

            <div className="auth-footer">
              <p className="mb-1">
                <Link to="/auth/forgot-password" className="auth-link">Запросить новую ссылку</Link>
              </p>
              <p className="mb-0">
                <Link to="/auth/login" className="auth-link">← Вернуться ко входу</Link>
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="auth-container">
        <div className="auth-form-wrapper">
          <div className="auth-form">
            <div className="auth-header">
              <h2 className="auth-title">✅ Пароль обновлён</h2>
              <p className="auth-subtitle">
                Пароль успешно изменён. Сейчас вы будете перенаправлены на страницу входа.
              </p>
            </div>

            <div className="auth-footer">
              <p className="mb-0">
                <Link to="/auth/login" className="auth-link">Перейти ко входу</Link>
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
            <h2 className="auth-title">🔐 Новый пароль</h2>
            <p className="auth-subtitle">Придумайте новый пароль для вашего аккаунта</p>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="password" className="form-label">Новый пароль</label>
              <input
                id="password"
                name="password"
                type="password"
                className="form-control form-control-lg"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Минимум 6 символов"
                required
                minLength={6}
                autoFocus
              />
            </div>

            <div className="form-group">
              <label htmlFor="confirm" className="form-label">Повторите пароль</label>
              <input
                id="confirm"
                name="confirm"
                type="password"
                className="form-control form-control-lg"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                placeholder="Повторите новый пароль"
                required
                minLength={6}
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
                  Сохранение...
                </>
              ) : (
                'Сменить пароль'
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
