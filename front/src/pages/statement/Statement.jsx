import React, { useState } from 'react';
import client from '../../api/client';

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
    <div>
      <h2>Statement form</h2>
      <form onSubmit={handleSubmit} style={{ maxWidth: 600 }}>
        <div>
          <label>Amount</label>
          <input name="amount" type="number" step="0.01" value={form.amount} onChange={handleChange} />
        </div>

        <div>
          <label>Term (months)</label>
          <input name="term" type="number" value={form.term} onChange={handleChange} />
        </div>

        <div>
          <label>First name</label>
          <input name="firstName" type="text" value={form.firstName} onChange={handleChange} />
        </div>

        <div>
          <label>Last name</label>
          <input name="lastName" type="text" value={form.lastName} onChange={handleChange} />
        </div>

        <div>
          <label>Middle name</label>
          <input name="middleName" type="text" value={form.middleName} onChange={handleChange} />
        </div>

        <div>
          <label>Email</label>
          <input name="email" type="email" value={form.email} onChange={handleChange} />
        </div>

        <div>
          <label>Birthdate</label>
          <input name="birthdate" type="date" value={form.birthdate} onChange={handleChange} />
        </div>

        <div>
          <label>Passport series</label>
          <input name="passportSeries" type="text" value={form.passportSeries} onChange={handleChange} />
        </div>

        <div>
          <label>Passport number</label>
          <input name="passportNumber" type="text" value={form.passportNumber} onChange={handleChange} />
        </div>

        <div style={{ marginTop: 10 }}>
          <button type="submit" disabled={loading}>{loading ? 'Sending...' : 'Submit'}</button>
        </div>
      </form>

      <section style={{ marginTop: 20 }}>
        <h3>Response</h3>
        {error && <pre style={{ color: 'red' }}>{String(error)}</pre>}
        {response ? (
          <pre style={{ background: '#f6f6f6', padding: 10 }}>{JSON.stringify(response, null, 2)}</pre>
        ) : (
          <div>No response yet</div>
        )}
      </section>
    </div>
  );
}
