import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../../api/client';
import './Statement.css';

const getApiBase = () => (process.env.NODE_ENV === 'development' ? '' : '/api');

const PLACEHOLDERS = {
  amount: '500000',
  term: '24',
  firstName: 'Иван',
  lastName: 'Иванов',
  middleName: 'Иванович',
  email: 'example@mail.ru',
  birthdate: '1990-05-15',
  passportSeries: '1234',
  passportNumber: '567890',
};

const emptyForm = {
  amount: '',
  term: '',
  firstName: '',
  lastName: '',
  middleName: '',
  email: '',
  birthdate: '',
  passportSeries: '',
  passportNumber: '',
};

const formatAmount = (v) => {
  if (v == null) return '—';
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 0,
  }).format(v);
};

// Латиница, кириллица, дефис; от 2 до 30 символов
const NAME_RE = /^[a-zA-ZА-ЯЁа-яё-]{2,30}$/;
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const SERIES_RE = /^\d{4}$/;
const NUMBER_RE = /^\d{6}$/;

function validateForm(form) {
  const errors = {};

  const amount = parseFloat(form.amount);
  if (!form.amount || isNaN(amount) || amount < 20000) {
    errors.amount = 'Сумма кредита должна быть не менее 20 000 руб.';
  }

  const term = parseInt(form.term, 10);
  if (!form.term || isNaN(term) || term < 6) {
    errors.term = 'Срок кредита должен быть не менее 6 месяцев';
  }

  if (!form.lastName || !NAME_RE.test(form.lastName)) {
    errors.lastName = 'Фамилия: от 2 до 30 букв (латиница или кириллица)';
  }
  if (!form.firstName || !NAME_RE.test(form.firstName)) {
    errors.firstName = 'Имя: от 2 до 30 букв (латиница или кириллица)';
  }
  if (form.middleName && !NAME_RE.test(form.middleName)) {
    errors.middleName = 'Отчество: от 2 до 30 букв (латиница или кириллица)';
  }

  if (!form.email || !EMAIL_RE.test(form.email)) {
    errors.email = 'Введите корректный адрес электронной почты';
  }

  if (!form.birthdate) {
    errors.birthdate = 'Укажите дату рождения';
  } else if (new Date(form.birthdate) >= new Date()) {
    errors.birthdate = 'Дата рождения должна быть в прошлом';
  }

  if (!form.passportSeries || !SERIES_RE.test(form.passportSeries)) {
    errors.passportSeries = 'Серия паспорта: ровно 4 цифры';
  }
  if (!form.passportNumber || !NUMBER_RE.test(form.passportNumber)) {
    errors.passportNumber = 'Номер паспорта: ровно 6 цифр';
  }

  return errors;
}

function extractBackendError(err) {
  const data = err.response?.data;
  if (!data) return err.message || 'Произошла ошибка';

  if (typeof data === 'string') {
    try {
      const parsed = JSON.parse(data);
      if (parsed.fieldErrors) {
        const parts = Object.entries(parsed.fieldErrors).map(([f, m]) => `${f}: ${m}`);
        return `${parsed.message}: ${parts.join('; ')}`;
      }
      return parsed.message || data;
    } catch {
      return data;
    }
  }

  if (data.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    const parts = Object.entries(data.fieldErrors).map(([f, m]) => `${f}: ${m}`);
    return `${data.message}: ${parts.join('; ')}`;
  }

  return data.message || err.message || 'Произошла ошибка';
}

function FieldError({ message }) {
  if (!message) return null;
  return <div className="field-error">{message}</div>;
}

export default function Statement() {
  const [form, setForm] = useState(emptyForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [offers, setOffers] = useState(null);
  const [loading, setLoading] = useState(false);
  const [selectLoading, setSelectLoading] = useState(null);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const base = getApiBase();

  // Pre-fill form from user profile
  useEffect(() => {
    (async () => {
      try {
        const res = await client.get(`${base}/deal/client/me`);
        const p = res.data;
        setForm((prev) => ({
          ...prev,
          firstName: p.firstName || '',
          lastName: p.lastName || '',
          middleName: p.middleName || '',
          email: p.email || '',
          birthdate: p.birthDate
            ? new Date(p.birthDate).toISOString().slice(0, 10)
            : '',
          passportSeries: p.passport?.series || '',
          passportNumber: p.passport?.number || '',
        }));
      } catch {
        // Profile unavailable — user fills manually
      }
    })();
  }, [base]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    const errors = validateForm(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});

    setLoading(true);
    setOffers(null);
    try {
      const payload = {
        ...form,
        amount: parseFloat(form.amount),
        term: parseInt(form.term, 10),
      };
      const res = await client.post(`${base}/statement`, payload);
      setOffers(res.data);
    } catch (err) {
      setError(extractBackendError(err));
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
      setError(extractBackendError(err));
      setSelectLoading(null);
    }
  };

  return (
    <div className="statement-container">
      <div className="statement-content">
        <div className="statement-header">
          <h2 className="statement-title">📋 Заявка на кредит</h2>
          <p className="statement-subtitle">
            Заполните данные для предварительного расчёта кредитных предложений
          </p>
        </div>

        {/* ── Application Form ── */}
        {!offers && (
          <form onSubmit={handleSubmit} className="statement-form" noValidate>
            <div className="form-row">
              <div className="form-col">
                <div className="form-group">
                  <label htmlFor="amount" className="form-label">
                    Сумма кредита (руб.) *
                  </label>
                  <input
                    id="amount"
                    name="amount"
                    type="number"
                    step="1000"
                    min="20000"
                    className={`form-control form-control-lg${fieldErrors.amount ? ' is-invalid' : ''}`}
                    value={form.amount}
                    onChange={handleChange}
                    placeholder={PLACEHOLDERS.amount}
                  />
                  <FieldError message={fieldErrors.amount} />
                </div>
              </div>
              <div className="form-col">
                <div className="form-group">
                  <label htmlFor="term" className="form-label">
                    Срок (месяцев) *
                  </label>
                  <input
                    id="term"
                    name="term"
                    type="number"
                    min="6"
                    className={`form-control form-control-lg${fieldErrors.term ? ' is-invalid' : ''}`}
                    value={form.term}
                    onChange={handleChange}
                    placeholder={PLACEHOLDERS.term}
                  />
                  <FieldError message={fieldErrors.term} />
                </div>
              </div>
            </div>

            <div className="form-row">
              <div className="form-col">
                <div className="form-group">
                  <label htmlFor="lastName" className="form-label">
                    Фамилия *
                  </label>
                  <input
                    id="lastName"
                    name="lastName"
                    type="text"
                    className={`form-control form-control-lg${fieldErrors.lastName ? ' is-invalid' : ''}`}
                    value={form.lastName}
                    onChange={handleChange}
                    placeholder={PLACEHOLDERS.lastName}
                  />
                  <FieldError message={fieldErrors.lastName} />
                </div>
              </div>
              <div className="form-col">
                <div className="form-group">
                  <label htmlFor="firstName" className="form-label">
                    Имя *
                  </label>
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    className={`form-control form-control-lg${fieldErrors.firstName ? ' is-invalid' : ''}`}
                    value={form.firstName}
                    onChange={handleChange}
                    placeholder={PLACEHOLDERS.firstName}
                  />
                  <FieldError message={fieldErrors.firstName} />
                </div>
              </div>
            </div>

            <div className="form-row">
              <div className="form-col">
                <div className="form-group">
                  <label htmlFor="middleName" className="form-label">
                    Отчество <span className="form-hint">(необязательно)</span>
                  </label>
                  <input
                    id="middleName"
                    name="middleName"
                    type="text"
                    className={`form-control form-control-lg${fieldErrors.middleName ? ' is-invalid' : ''}`}
                    value={form.middleName}
                    onChange={handleChange}
                    placeholder={PLACEHOLDERS.middleName}
                  />
                  <FieldError message={fieldErrors.middleName} />
                </div>
              </div>
              <div className="form-col">
                <div className="form-group">
                  <label htmlFor="email" className="form-label">
                    Электронная почта *
                  </label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    className={`form-control form-control-lg${fieldErrors.email ? ' is-invalid' : ''}`}
                    value={form.email}
                    onChange={handleChange}
                    placeholder={PLACEHOLDERS.email}
                  />
                  <FieldError message={fieldErrors.email} />
                </div>
              </div>
            </div>

            <div className="form-row">
              <div className="form-col">
                <div className="form-group">
                  <label htmlFor="birthdate" className="form-label">
                    Дата рождения *
                  </label>
                  <input
                    id="birthdate"
                    name="birthdate"
                    type="date"
                    className={`form-control form-control-lg${fieldErrors.birthdate ? ' is-invalid' : ''}`}
                    value={form.birthdate}
                    onChange={handleChange}
                  />
                  <FieldError message={fieldErrors.birthdate} />
                </div>
              </div>
              <div className="form-col">
                <div className="form-group">
                  <label htmlFor="passportSeries" className="form-label">
                    Серия паспорта *
                  </label>
                  <input
                    id="passportSeries"
                    name="passportSeries"
                    type="text"
                    maxLength={4}
                    className={`form-control form-control-lg${fieldErrors.passportSeries ? ' is-invalid' : ''}`}
                    value={form.passportSeries}
                    onChange={handleChange}
                    placeholder={PLACEHOLDERS.passportSeries}
                  />
                  <FieldError message={fieldErrors.passportSeries} />
                </div>
              </div>
            </div>

            <div className="form-row">
              <div className="form-col">
                <div className="form-group">
                  <label htmlFor="passportNumber" className="form-label">
                    Номер паспорта *
                  </label>
                  <input
                    id="passportNumber"
                    name="passportNumber"
                    type="text"
                    maxLength={6}
                    className={`form-control form-control-lg${fieldErrors.passportNumber ? ' is-invalid' : ''}`}
                    value={form.passportNumber}
                    onChange={handleChange}
                    placeholder={PLACEHOLDERS.passportNumber}
                  />
                  <FieldError message={fieldErrors.passportNumber} />
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
                type="submit"
                disabled={loading}
                className="btn btn-primary btn-lg"
              >
                {loading ? (
                  <>
                    <span
                      className="spinner-border spinner-border-sm me-2"
                      role="status"
                      aria-hidden="true"
                    />
                    Отправка...
                  </>
                ) : (
                  'Подать заявку'
                )}
              </button>
            </div>
          </form>
        )}

        {/* ── Offers ── */}
        {offers && (
          <div className="offers-section">
            <div className="offers-header">
              <h3 className="offers-title">Доступные предложения</h3>
              <p className="offers-subtitle">
                Выберите подходящее кредитное предложение, нажав на карточку
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
                      {offer.isSalaryClient ? '✓ Зарп. клиент' : '✗ Не зарп. клиент'}
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

            <div className="offers-back">
              <button
                className="btn btn-back"
                onClick={() => {
                  setOffers(null);
                  setError(null);
                }}
              >
                ← Вернуться к форме
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
