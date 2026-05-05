import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import client from '../../api/client';
import './Confirmation.css';

const getApiBase = () => (process.env.NODE_ENV === 'development' ? '' : '/api');

const CODE_LENGTH = 6;
const RESEND_COOLDOWN = 60; // seconds

export default function Confirmation() {
  const { statementId } = useParams();
  const navigate = useNavigate();
  const base = getApiBase();

  const [digits, setDigits] = useState(Array(CODE_LENGTH).fill(''));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  // Resend state
  const [resendLoading, setResendLoading] = useState(false);
  const [resendError, setResendError] = useState(null);
  const [resendSuccess, setResendSuccess] = useState(false);
  const [cooldown, setCooldown] = useState(0);
  const cooldownRef = useRef(null);

  const inputsRef = useRef([]);

  useEffect(() => {
    inputsRef.current[0]?.focus();
  }, []);

  // Cooldown timer
  const startCooldown = useCallback(() => {
    setCooldown(RESEND_COOLDOWN);
    cooldownRef.current = setInterval(() => {
      setCooldown((prev) => {
        if (prev <= 1) {
          clearInterval(cooldownRef.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  useEffect(() => {
    return () => {
      if (cooldownRef.current) clearInterval(cooldownRef.current);
    };
  }, []);

  const updateDigit = (index, value) => {
    const cleaned = value.replace(/\D/g, '');
    if (cleaned.length <= 1) {
      const next = [...digits];
      next[index] = cleaned;
      setDigits(next);
      if (cleaned && index < CODE_LENGTH - 1) {
        inputsRef.current[index + 1]?.focus();
      }
    } else {
      // pasted multiple digits — distribute
      const chars = cleaned.slice(0, CODE_LENGTH - index).split('');
      const next = [...digits];
      chars.forEach((ch, i) => {
        next[index + i] = ch;
      });
      setDigits(next);
      const lastFilled = Math.min(index + chars.length, CODE_LENGTH - 1);
      inputsRef.current[lastFilled]?.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputsRef.current[index - 1]?.focus();
    }
    if (e.key === 'ArrowLeft' && index > 0) {
      inputsRef.current[index - 1]?.focus();
    }
    if (e.key === 'ArrowRight' && index < CODE_LENGTH - 1) {
      inputsRef.current[index + 1]?.focus();
    }
  };

  const handlePaste = (e) => {
    e.preventDefault();
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, CODE_LENGTH);
    if (!pasted) return;
    const next = Array(CODE_LENGTH).fill('');
    pasted.split('').forEach((ch, i) => {
      next[i] = ch;
    });
    setDigits(next);
    const lastFilled = Math.min(pasted.length, CODE_LENGTH) - 1;
    inputsRef.current[lastFilled]?.focus();
  };

  const code = digits.join('');
  const ready = code.length === CODE_LENGTH;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!ready) return;
    setLoading(true);
    setError(null);
    try {
      await client.post(
        `${base}/statement/registration/${statementId}/verify`,
        { code },
      );
      setSuccess(true);
    } catch (err) {
      const status = err.response?.status;
      let msg;
      if (status === 401) {
        msg = 'Неверный код подтверждения. Проверьте правильность ввода.';
      } else if (status === 410) {
        msg = 'Срок действия кода истёк. Запросите новый код ниже.';
      } else if (status === 400) {
        msg = err.response?.data?.message || 'Код не был выпущен для этой заявки.';
      } else {
        msg = err.response?.data?.message || err.message || 'Не удалось проверить код.';
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (cooldown > 0 || resendLoading) return;
    setResendLoading(true);
    setResendError(null);
    setResendSuccess(false);
    try {
      await client.post(`${base}/statement/registration/${statementId}/resend-code`);
      setResendSuccess(true);
      setError(null);
      setDigits(Array(CODE_LENGTH).fill(''));
      inputsRef.current[0]?.focus();
      startCooldown();
    } catch (err) {
      const status = err.response?.status;
      let msg;
      if (status === 409) {
        msg = 'Невозможно запросить код: заявка уже подтверждена или отменена.';
      } else {
        msg = err.response?.data?.message || err.message || 'Не удалось отправить новый код.';
      }
      setResendError(msg);
    } finally {
      setResendLoading(false);
    }
  };

  if (success) {
    return (
      <div className="conf-page">
        <div className="conf-content">
          <div className="conf-success">
            <div className="conf-success-icon">🎉</div>
            <h2 className="conf-success-title">Кредит успешно оформлен!</h2>
            <p className="conf-success-text">
              Код подтверждения принят. Вы можете отслеживать статус заявки в
              личном кабинете.
            </p>
            <div className="conf-success-actions">
              <button
                className="btn btn-primary"
                onClick={() => navigate('/account')}
              >
                Перейти в личный кабинет
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="conf-page">
      <div className="conf-content">
        <div className="conf-card">
          <div className="conf-header">
            <div className="conf-icon">📨</div>
            <h2 className="conf-title">Подтверждение оформления</h2>
            <p className="conf-subtitle">
              Мы отправили одноразовый код на вашу электронную почту. Введите
              его ниже, чтобы окончательно оформить кредит.
            </p>
            <div className="conf-id">ID заявки: {statementId}</div>
          </div>

          <form onSubmit={handleSubmit} className="conf-form">
            {error && (
              <div className="alert alert-danger" role="alert">
                {String(error)}
              </div>
            )}

            <div className="conf-otp-row" onPaste={handlePaste}>
              {digits.map((d, i) => (
                <input
                  key={i}
                  ref={(el) => (inputsRef.current[i] = el)}
                  className="conf-otp-input"
                  type="text"
                  inputMode="numeric"
                  pattern="[0-9]*"
                  maxLength={CODE_LENGTH}
                  value={d}
                  onChange={(e) => updateDigit(i, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(i, e)}
                  autoComplete="one-time-code"
                  aria-label={`Цифра ${i + 1}`}
                />
              ))}
            </div>

            <div className="conf-hint">
              Код действителен 5 минут с момента отправки.
            </div>

            <div className="conf-actions">
              <button
                type="submit"
                className="btn btn-primary btn-lg"
                disabled={loading || !ready}
              >
                {loading ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                    Проверка...
                  </>
                ) : (
                  'Подтвердить'
                )}
              </button>
              <button
                type="button"
                className="btn btn-back"
                onClick={() => navigate('/account')}
                disabled={loading}
              >
                ← В личный кабинет
              </button>
            </div>
          </form>

          {/* Resend section */}
          <div className="conf-resend">
            <p className="conf-resend-hint">
              Не получили письмо или код истёк?
            </p>
            {resendSuccess && (
              <div className="alert alert-success conf-resend-msg" role="alert">
                Новый код отправлен на вашу почту.
              </div>
            )}
            {resendError && (
              <div className="alert alert-danger conf-resend-msg" role="alert">
                {String(resendError)}
              </div>
            )}
            <button
              type="button"
              className="btn conf-resend-btn"
              onClick={handleResend}
              disabled={cooldown > 0 || resendLoading}
            >
              {resendLoading ? (
                <>
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    role="status"
                    aria-hidden="true"
                  />
                  Отправка...
                </>
              ) : cooldown > 0 ? (
                `Запросить новый код (${cooldown} с)`
              ) : (
                'Запросить новый код'
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
