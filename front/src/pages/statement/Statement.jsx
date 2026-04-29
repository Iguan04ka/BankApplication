import React, { useState } from 'react';
import client from '../../api/client';
import './Statement.css';

const initial = {
  amount: 500000.0,
  term: 24,
  firstName: 'Иван',
  lastName: 'Иванов',
  middleName: 'Иванович',
  email: 'forwf2000@mail.ru',
  birthdate: '1990-05-15',
  passportSeries: '1234',
  passportNumber: '567890',
};

export default function Statement() {
  const [form, setForm] = useState(initial);
  const [response, setResponse] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const getEndpoint = () => {
    const isDev = process.env.NODE_ENV === 'development';
    return isDev ? '/statement' : '/api/statement';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResponse(null);
    try {
      const endpoint = getEndpoint();
      const payload = {
        ...form,
        amount: typeof form.amount === 'string' ? parseFloat(form.amount) : form.amount,
        term: typeof form.term === 'string' ? parseInt(form.term, 10) : form.term,
      };

      const token = localStorage.getItem('accessToken');
      const headers = {
        login: '123',
      };
      if (token) headers.Authorization = `Bearer ${token}`;

      console.debug('Statement: sending request', { endpoint, headers, payload });

      const res = await client.post(endpoint, payload, { headers });
      setResponse(res.data);
    } catch (err) {
      setError(err.message || 'Request failed');
      if (err.response && err.response.data) setResponse(err.response.data);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="statement-container">
      <div className="statement-content">
        <div className="statement-header">
          <h2 className="statement-title">📋 Заявка на кредит</h2>
          <p className="statement-subtitle">Заполните данные клиента для предварительного расчёта и отправки на обработку</p>
        </div>

        <form onSubmit={handleSubmit} className="statement-form">
          <div className="form-row">
            <div className="form-col">
              <div className="form-group">
                <label htmlFor="amount" className="form-label">Сумма кредита *</label>
                <input
                  id="amount"
                  name="amount"
                  type="number"
                  step="0.01"
                  className="form-control form-control-lg"
                  value={form.amount}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="form-col">
              <div className="form-group">
                <label htmlFor="term" className="form-label">Срок (месяцев) *</label>
                <input
                  id="term"
                  name="term"
                  type="number"
                  className="form-control form-control-lg"
                  value={form.term}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>
          </div>

          <div className="form-row">
            <div className="form-col">
              <div className="form-group">
                <label htmlFor="firstName" className="form-label">Имя *</label>
                <input
                  id="firstName"
                  name="firstName"
                  type="text"
                  className="form-control form-control-lg"
                  value={form.firstName}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="form-col">
              <div className="form-group">
                <label htmlFor="lastName" className="form-label">Фамилия *</label>
                <input
                  id="lastName"
                  name="lastName"
                  type="text"
                  className="form-control form-control-lg"
                  value={form.lastName}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>
          </div>

          <div className="form-row">
            <div className="form-col">
              <div className="form-group">
                <label htmlFor="middleName" className="form-label">Отчество *</label>
                <input
                  id="middleName"
                  name="middleName"
                  type="text"
                  className="form-control form-control-lg"
                  value={form.middleName}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="form-col">
              <div className="form-group">
                <label htmlFor="email" className="form-label">Электронная почта *</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  className="form-control form-control-lg"
                  value={form.email}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>
          </div>

          <div className="form-row">
            <div className="form-col">
              <div className="form-group">
                <label htmlFor="birthdate" className="form-label">Дата рождения *</label>
                <input
                  id="birthdate"
                  name="birthdate"
                  type="date"
                  className="form-control form-control-lg"
                  value={form.birthdate}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div className="form-col">
              <div className="form-group">
                <label htmlFor="passportSeries" className="form-label">Серия паспорта *</label>
                <input
                  id="passportSeries"
                  name="passportSeries"
                  type="text"
                  className="form-control form-control-lg"
                  value={form.passportSeries}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>
          </div>

          <div className="form-row">
            <div className="form-col">
              <div className="form-group">
                <label htmlFor="passportNumber" className="form-label">Номер паспорта *</label>
                <input
                  id="passportNumber"
                  name="passportNumber"
                  type="text"
                  className="form-control form-control-lg"
                  value={form.passportNumber}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>
          </div>

          <div className="form-actions">
            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary btn-lg"
            >
              {loading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Отправка...
                </>
              ) : (
                'Подать заявку'
              )}
            </button>
          </div>
        </form>

        <section className="statement-response">
          <h3 className="response-title">Ответ сервера</h3>
          {error && (
            <div className="alert alert-danger" role="alert">
              <strong>Ошибка:</strong> {String(error)}
            </div>
          )}
          {response ? (
            <div className="response-content">
              <pre>{JSON.stringify(response, null, 2)}</pre>
            </div>
          ) : (
            <div className="alert alert-info" role="alert">
              Ответа пока нет. Отправьте заявку, чтобы увидеть результат.
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
