import React, { useState, useEffect } from 'react';
import axios from 'axios';
import client from '../../api/client';
import { useAuth } from '../../auth/AuthProvider';
import { useNavigate, Link } from 'react-router-dom';
import OtpInput from '../../shared/ui/OtpInput/OtpInput';
import './AuthForm.css';

const getEndpoint = (path) => {
  const isDev = process.env.NODE_ENV === 'development';
  return isDev ? path : `/api${path}`;
};

export default function Login() {
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  // 2FA stage
  const [stage, setStage] = useState('credentials'); // 'credentials' | 'twofa'
  const [twoFa, setTwoFa] = useState({ sub: '', email: '', code: '' });
  const [twoFaLoading, setTwoFaLoading] = useState(false);
  const [twoFaError, setTwoFaError] = useState(null);
  const [resendInfo, setResendInfo] = useState(null);

  const auth = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (auth && auth.user) {
      navigate(auth.isAdmin ? '/admin' : '/account');
    }
  }, [auth, navigate]);

  const finishLogin = (data, fallbackSub) => {
    const roles = Array.isArray(data.roles) ? data.roles : [];
    auth.login({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      roles,
      sub: data.sub || fallbackSub || null,
    });
    const token = data.accessToken;
    axios.defaults.headers.common.Authorization = `Bearer ${token}`;
    client.defaults.headers = client.defaults.headers || {};
    client.defaults.headers.common = client.defaults.headers.common || {};
    client.defaults.headers.common.Authorization = `Bearer ${token}`;
    const isAdmin = roles.some((r) => String(r).toLowerCase() === 'admin');
    navigate(isAdmin ? '/admin' : '/account');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);
    setError(null);
    try {
      const res = await axios.post(
        getEndpoint('/auth/login'),
        { sub: login, password },
        { headers: { 'Content-Type': 'application/json' } },
      );
      if (res.status === 200 && res.data) {
        if (res.data.requires2FA) {
          setTwoFa({
            sub: res.data.sub || login,
            email: res.data.email || '',
            code: '',
          });
          setStage('twofa');
        } else if (res.data.accessToken) {
          finishLogin(res.data, login);
        } else {
          setError('Unexpected response: ' + JSON.stringify(res.data));
        }
      } else {
        setError('Unexpected response: ' + JSON.stringify(res.data));
      }
    } catch (err) {
      const status = err.response?.status;
      const data = err.response?.data;
      const serverMsg =
        (data && typeof data === 'object' && (data.error || data.message)) ||
        (typeof data === 'string' ? data : null);
      if (status === 401) {
        if (serverMsg && /blocked/i.test(serverMsg)) {
          setError('Ваша учётная запись заблокирована. Обратитесь в поддержку.');
        } else {
          setError(serverMsg || 'Неверный логин или пароль');
        }
      } else {
        setError(serverMsg || err.message || 'Не удалось войти');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleVerify2FA = async (e) => {
    e.preventDefault();
    setTwoFaError(null);
    if (twoFa.code.length !== 6) {
      setTwoFaError('Введите 6-значный код');
      return;
    }
    setTwoFaLoading(true);
    try {
      const res = await axios.post(
        getEndpoint('/auth/2fa/verify'),
        { sub: twoFa.sub, code: twoFa.code },
        { headers: { 'Content-Type': 'application/json' } },
      );
      if (res.status === 200 && res.data?.accessToken) {
        finishLogin(res.data, twoFa.sub);
      } else {
        setTwoFaError('Не удалось подтвердить код');
      }
    } catch (err) {
      const status = err.response?.status;
      const data = err.response?.data;
      const serverMsg =
        (data && typeof data === 'object' && (data.error || data.message)) ||
        (typeof data === 'string' ? data : null);
      if (status === 401) {
        setTwoFaError(serverMsg || 'Неверный или просроченный код');
      } else {
        setTwoFaError(serverMsg || err.message || 'Не удалось подтвердить код');
      }
    } finally {
      setTwoFaLoading(false);
    }
  };

  const handleResend = async () => {
    setResendInfo(null);
    setTwoFaError(null);
    try {
      await axios.post(
        getEndpoint('/auth/2fa/resend'),
        { sub: twoFa.sub },
        { headers: { 'Content-Type': 'application/json' } },
      );
      setResendInfo('Новый код отправлен на почту.');
    } catch {
      setResendInfo('Не удалось отправить код повторно. Попробуйте позже.');
    }
  };

  const cancel2FA = () => {
    setStage('credentials');
    setTwoFa({ sub: '', email: '', code: '' });
    setTwoFaError(null);
    setResendInfo(null);
    setPassword('');
  };

  if (stage === 'twofa') {
    return (
      <div className="auth-container">
        <div className="auth-form-wrapper">
          <div className="auth-form">
            <div className="auth-header">
              <h2 className="auth-title">🔐 Подтверждение входа</h2>
              <p className="auth-subtitle">
                Мы отправили 6-значный код на адрес{' '}
                <strong>{twoFa.email || 'вашу почту'}</strong>. Введите его, чтобы продолжить.
              </p>
            </div>

            <form onSubmit={handleVerify2FA}>
              {twoFaError && (
                <div className="alert alert-danger" role="alert">{twoFaError}</div>
              )}
              {resendInfo && (
                <div className="alert alert-success" role="alert">{resendInfo}</div>
              )}

              <OtpInput
                value={twoFa.code}
                onChange={(c) => setTwoFa((s) => ({ ...s, code: c }))}
                disabled={twoFaLoading}
              />

              <div style={{ textAlign: 'center', fontSize: '0.82rem', color: '#0066cc99', marginBottom: '1rem' }}>
                Код действителен 5 минут с момента отправки.
              </div>

              <button
                type="submit"
                disabled={twoFaLoading || twoFa.code.length !== 6}
                className="btn btn-primary btn-lg w-100"
              >
                {twoFaLoading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                    Проверка...
                  </>
                ) : (
                  'Войти'
                )}
              </button>

              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.85rem' }}>
                <button type="button" className="auth-link" style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                        onClick={handleResend} disabled={twoFaLoading}>
                  Отправить код повторно
                </button>
                <button type="button" className="auth-link" style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                        onClick={cancel2FA} disabled={twoFaLoading}>
                  ← Назад
                </button>
              </div>
            </form>
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
            <p className="mb-1">
              <Link to="/auth/forgot-password" className="auth-link">Забыли пароль?</Link>
            </p>
            <p className="mb-0">Ещё нет аккаунта? <Link to="/auth/register" className="auth-link">Зарегистрироваться</Link></p>
          </div>

          {message && <div className="alert alert-success mt-3" role="alert">{message}</div>}
          {error && <div className="alert alert-danger mt-3" role="alert">{String(error)}</div>}
        </div>
      </div>
    </div>
  );
}
