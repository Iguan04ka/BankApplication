import React, { useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import client from '../../api/client';
import './Statement.css';

const getApiBase = () => (process.env.NODE_ENV === 'development' ? '' : '/api');

const formatAmount = (v) => {
  if (v == null) return '—';
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 0,
  }).format(v);
};

export default function StatementResume() {
  const { statementId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const base = getApiBase();

  // Pre-fill from account page navigation state when available
  const stateAmount = location.state?.requestedAmount;
  const stateTerm   = location.state?.requestedTerm;

  const [amount, setAmount] = useState(stateAmount != null ? String(stateAmount) : '');
  const [term,   setTerm]   = useState(stateTerm   != null ? String(stateTerm)   : '');

  const [offers,       setOffers]       = useState(null);
  const [loading,      setLoading]      = useState(false);
  const [error,        setError]        = useState(null);
  const [selectLoading, setSelectLoading] = useState(null);

  const fetchOffers = async () => {
    if (!amount || !term) {
      setError('Укажите сумму кредита и срок');
      return;
    }
    setLoading(true);
    setError(null);
    setOffers(null);
    try {
      const params = new URLSearchParams({ amount, term });
      const res = await client.get(
        `${base}/deal/client/me/statements/${statementId}/offers?${params}`,
      );
      const data = res.data;
      setOffers(Array.isArray(data) ? data : Object.values(data));
    } catch (err) {
      setError(
        err.response?.data?.message ||
          err.message ||
          'Не удалось загрузить предложения',
      );
    } finally {
      setLoading(false);
    }
  };

  const handleSelectOffer = async (offer) => {
    setSelectLoading(offer.statementId);
    setError(null);
    try {
      await client.post(`${base}/statement/select`, offer);
      navigate(`/statement/registration/${offer.statementId}`);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          err.message ||
          'Ошибка при выборе предложения',
      );
      setSelectLoading(null);
    }
  };

  return (
    <div className="statement-container">
      <div className="statement-content">
        <div className="statement-header">
          <h2 className="statement-title">📋 Выбор кредитного предложения</h2>
          <p className="statement-subtitle">
            Укажите параметры кредита, чтобы получить доступные предложения
          </p>
          <div className="statement-id-badge">ID заявки: {statementId}</div>
        </div>

        {/* ── Params form ── */}
        {!offers && (
          <div className="statement-form">
            <div className="form-row">
              <div className="form-col">
                <div className="form-group">
                  <label className="form-label">Сумма кредита (руб.) *</label>
                  <input
                    type="number"
                    className="form-control form-control-lg"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="500000"
                    min="10000"
                    step="1000"
                    required
                  />
                </div>
              </div>
              <div className="form-col">
                <div className="form-group">
                  <label className="form-label">Срок (месяцев) *</label>
                  <input
                    type="number"
                    className="form-control form-control-lg"
                    value={term}
                    onChange={(e) => setTerm(e.target.value)}
                    placeholder="24"
                    min="6"
                    required
                  />
                </div>
              </div>
            </div>

            {error && (
              <div className="alert alert-danger mt-3" role="alert">
                {String(error)}
              </div>
            )}

            <div className="form-actions">
              <button
                className="btn btn-primary btn-lg"
                onClick={fetchOffers}
                disabled={loading}
              >
                {loading ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                    Загрузка...
                  </>
                ) : (
                  'Получить предложения'
                )}
              </button>
              <button
                className="btn btn-back"
                onClick={() => navigate('/account')}
                disabled={loading}
              >
                ← Вернуться в кабинет
              </button>
            </div>
          </div>
        )}

        {/* ── Offers ── */}
        {offers && (
          <>
            <div className="offers-section">
              <div className="offers-header">
                <h3 className="offers-title">Доступные предложения</h3>
                <p className="offers-subtitle">
                  Нажмите на карточку, чтобы выбрать предложение
                </p>
              </div>

              {error && (
                <div className="alert alert-danger" role="alert">
                  {String(error)}
                </div>
              )}

              <div className="offers-grid">
                {offers.map((offer, i) => (
                  <div
                    key={offer.statementId || i}
                    className={`offer-card${selectLoading === offer.statementId ? ' offer-card--loading' : ''}`}
                    onClick={() => !selectLoading && handleSelectOffer(offer)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) =>
                      e.key === 'Enter' && !selectLoading && handleSelectOffer(offer)
                    }
                  >
                    <div className="offer-index">Предложение {i + 1}</div>
                    <div className="offer-rate">{offer.rate}%</div>
                    <div className="offer-rate-label">годовых</div>

                    <div className="offer-details">
                      <div className="offer-row">
                        <span className="offer-row-label">Ежемесячный платёж</span>
                        <strong className="offer-row-value">
                          {formatAmount(offer.monthlyPayment)}
                        </strong>
                      </div>
                      <div className="offer-row">
                        <span className="offer-row-label">Запрошенная сумма</span>
                        <strong className="offer-row-value">
                          {formatAmount(offer.requestedAmount)}
                        </strong>
                      </div>
                      <div className="offer-row">
                        <span className="offer-row-label">Итого к выплате</span>
                        <strong className="offer-row-value">
                          {formatAmount(offer.totalAmount)}
                        </strong>
                      </div>
                      <div className="offer-row">
                        <span className="offer-row-label">Срок</span>
                        <strong className="offer-row-value">{offer.term} мес.</strong>
                      </div>
                    </div>

                    <div className="offer-badges">
                      <span
                        className={`offer-badge ${offer.isInsuranceEnabled ? 'offer-badge--yes' : 'offer-badge--no'}`}
                      >
                        {offer.isInsuranceEnabled ? '✓ Страховка' : '✗ Без страховки'}
                      </span>
                      <span
                        className={`offer-badge ${offer.isSalaryClient ? 'offer-badge--yes' : 'offer-badge--no'}`}
                      >
                        {offer.isSalaryClient
                          ? '✓ Зарп. клиент'
                          : '✗ Не зарп. клиент'}
                      </span>
                    </div>

                    <button
                      className="btn btn-primary offer-select-btn"
                      disabled={!!selectLoading}
                      onClick={(e) => {
                        e.stopPropagation();
                        if (!selectLoading) handleSelectOffer(offer);
                      }}
                    >
                      {selectLoading === offer.statementId ? (
                        <>
                          <span
                            className="spinner-border spinner-border-sm me-2"
                            role="status"
                            aria-hidden="true"
                          />
                          Выбор...
                        </>
                      ) : (
                        'Выбрать'
                      )}
                    </button>
                  </div>
                ))}
              </div>
            </div>

            <div className="offers-back">
              <button
                className="btn btn-back"
                onClick={() => { setOffers(null); setError(null); }}
              >
                ← Изменить параметры
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
