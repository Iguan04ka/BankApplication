import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import client from '../../api/client';
import AdminClientDocuments from './AdminClientDocuments';
import {
  adminApi,
  formatMoney,
  formatDate,
  statusColor,
  APPLICATION_STATUSES,
  extractError,
} from './shared';

// ── sorting hook ──────────────────────────────────────────────────────────────
function useSortState(defaultCol = null, defaultDir = 'asc') {
  const [col, setCol] = useState(defaultCol);
  const [dir, setDir] = useState(defaultDir);

  const toggle = (newCol) => {
    setCol((prev) => {
      if (prev === newCol) {
        setDir((d) => (d === 'asc' ? 'desc' : 'asc'));
        return newCol;
      }
      setDir('asc');
      return newCol;
    });
  };

  return { col, dir, toggle };
}

function SortTh({ label, colKey, sort, style }) {
  const active = sort.col === colKey;
  return (
    <th
      style={{ cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap', ...style }}
      onClick={() => sort.toggle(colKey)}
    >
      {label}
      <span style={{ marginLeft: 4, opacity: active ? 1 : 0.3, fontSize: '0.75rem' }}>
        {active ? (sort.dir === 'asc' ? '▲' : '▼') : '⇅'}
      </span>
    </th>
  );
}

function sortItems(items, col, dir) {
  if (!col) return items;
  return [...items].sort((a, b) => {
    let va = resolve(a, col);
    let vb = resolve(b, col);
    if (va === null || va === undefined) va = '';
    if (vb === null || vb === undefined) vb = '';
    let cmp = 0;
    if (typeof va === 'number' && typeof vb === 'number') {
      cmp = va - vb;
    } else {
      cmp = String(va).localeCompare(String(vb), 'ru', { numeric: true });
    }
    return dir === 'asc' ? cmp : -cmp;
  });
}

function resolve(item, col) {
  switch (col) {
    case 'client':   return item.clientFullName || item.clientUserSub || '';
    case 'userSub':  return item.clientUserSub || '';
    case 'amount':   return parseFloat(item.requestedAmount ?? item.creditAmount ?? 0);
    case 'term':     return parseInt(item.requestedTerm ?? item.creditTerm ?? 0, 10);
    case 'status':   return item.status || '';
    case 'date':     return item.creationDate ? new Date(item.creationDate).getTime() : 0;
    default:         return '';
  }
}

// ─────────────────────────────────────────────────────────────────────────────

export default function AdminStatements() {
  const [searchParams] = useSearchParams();

  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // server-side filters; pre-fill status from ?status= (e.g. from Dashboard click)
  const [status, setStatus] = useState(() => searchParams.get('status') || '');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  // client-side filter — user login; pre-fill from ?user= (e.g. from Users page)
  const [userFilter, setUserFilter] = useState(() => searchParams.get('user') || '');

  // sorting
  const sort = useSortState('date', 'desc');

  // detail modal
  const [openId, setOpenId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {};
      if (status) params.status = status;
      if (from) params.fromMillis = new Date(from + 'T00:00:00').getTime();
      if (to)   params.toMillis   = new Date(to   + 'T23:59:59.999').getTime();

      const resp = await client.get(adminApi('/admin/statements'), { params });
      setItems(Array.isArray(resp.data) ? resp.data : []);
    } catch (e) {
      setError(extractError(e));
    } finally {
      setLoading(false);
    }
  }, [status, from, to]);

  useEffect(() => { load(); }, [load]);

  const reset = () => { setStatus(''); setFrom(''); setTo(''); setUserFilter(''); };

  // apply client-side filter by user login, then sort
  const displayed = useMemo(() => {
    let list = items;
    if (userFilter.trim()) {
      const q = userFilter.trim().toLowerCase();
      list = list.filter((it) =>
        (it.clientUserSub || '').toLowerCase().includes(q) ||
        (it.clientFullName || '').toLowerCase().includes(q) ||
        (it.clientEmail || '').toLowerCase().includes(q)
      );
    }
    return sortItems(list, sort.col, sort.dir);
  }, [items, userFilter, sort.col, sort.dir]);

  return (
    <>
      <div className="admin-page-header">
        <h2 className="admin-page-title">Заявки</h2>
        <button className="admin-btn" onClick={load} disabled={loading}>↻ Обновить</button>
      </div>

      <div className="admin-filters">
        <div className="admin-filter-group">
          <label>Статус</label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Все</option>
            {APPLICATION_STATUSES.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </div>
        <div className="admin-filter-group">
          <label>Логин пользователя</label>
          <input
            type="text"
            placeholder="Введите логин..."
            value={userFilter}
            onChange={(e) => setUserFilter(e.target.value)}
          />
        </div>
        <div className="admin-filter-group">
          <label>С даты</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="admin-filter-group">
          <label>По дату</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <button className="admin-btn" onClick={reset}>Сбросить</button>
      </div>

      {error && <div className="admin-error">{error}</div>}

      <div className="admin-card" style={{ padding: 0 }}>
        {loading ? (
          <div className="admin-loading">Загрузка заявок...</div>
        ) : displayed.length === 0 ? (
          <div className="empty" style={{ padding: '3rem 1rem', textAlign: 'center', color: '#9ca3af' }}>
            {items.length === 0 ? 'Заявки не найдены' : 'Нет заявок по фильтру'}
          </div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID заявки</th>
                <SortTh label="Клиент" colKey="client" sort={sort} />
                <SortTh label="Логин" colKey="userSub" sort={sort} />
                <SortTh label="Сумма" colKey="amount" sort={sort} />
                <SortTh label="Срок" colKey="term" sort={sort} />
                <SortTh label="Статус" colKey="status" sort={sort} />
                <SortTh label="Создана" colKey="date" sort={sort} />
                <th></th>
              </tr>
            </thead>
            <tbody>
              {displayed.map((it) => (
                <tr key={it.statementId}>
                  <td style={{ fontFamily: 'monospace', fontSize: '0.78rem', color: '#6b7280' }}>
                    {String(it.statementId || '').slice(0, 8)}…
                  </td>
                  <td>
                    <div style={{ fontWeight: 500 }}>{it.clientFullName || '—'}</div>
                    <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>{it.clientEmail || ''}</div>
                  </td>
                  <td style={{ fontSize: '0.82rem', color: '#374151' }}>
                    {it.clientUserSub || '—'}
                  </td>
                  <td>{formatMoney(it.requestedAmount ?? it.creditAmount)}</td>
                  <td>{it.requestedTerm || it.creditTerm || '—'} мес.</td>
                  <td>
                    <span className={`admin-status-badge ${statusColor(it.status)}`}>
                      {it.status || '—'}
                    </span>
                  </td>
                  <td style={{ color: '#6b7280', fontSize: '0.82rem' }}>
                    {formatDate(it.creationDate)}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <button className="admin-btn sm" onClick={() => setOpenId(it.statementId)}>
                      Открыть
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {openId && (
        <StatementDetailModal
          statementId={openId}
          onClose={() => setOpenId(null)}
          onChanged={() => { setOpenId(null); load(); }}
        />
      )}
    </>
  );
}

// ── Detail modal ─────────────────────────────────────────────────────────────

export function StatementDetailModal({ statementId, onClose, onChanged, hideActions }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [savingStatus, setSavingStatus] = useState(false);
  const [newStatus, setNewStatus] = useState('');

  useEffect(() => {
    let alive = true;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const resp = await client.get(adminApi(`/admin/statements/${statementId}`));
        if (alive) { setData(resp.data); setNewStatus(resp.data?.status || ''); }
      } catch (e) {
        if (alive) setError(extractError(e));
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => { alive = false; };
  }, [statementId]);

  const updateStatus = async (target) => {
    setSavingStatus(true);
    setError(null);
    try {
      const resp = await client.put(
        adminApi(`/admin/statements/${statementId}/status`),
        { status: target },
      );
      setData(resp.data);
      setNewStatus(resp.data?.status || target);
    } catch (e) {
      setError(extractError(e));
    } finally {
      setSavingStatus(false);
    }
  };

  return (
    <div className="admin-modal-backdrop" onClick={onClose}>
      <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
        <div className="admin-modal-header">
          <h3 className="admin-modal-title">Заявка</h3>
          <button className="admin-modal-close" onClick={onClose}>×</button>
        </div>

        <div className="admin-modal-body">
          {loading && <div className="admin-loading">Загрузка...</div>}
          {error && <div className="admin-error">{error}</div>}

          {!loading && data && (
            <>
              <div style={{ marginBottom: '1rem' }}>
                <span className={`admin-status-badge ${statusColor(data.status)}`}>{data.status || '—'}</span>
                <span style={{ marginLeft: '0.5rem', color: '#6b7280', fontSize: '0.82rem', fontFamily: 'monospace' }}>
                  {data.statementId}
                </span>
              </div>

              <SectionTitle>Клиент</SectionTitle>
              <div className="admin-detail-grid">
                <Detail label="ФИО" value={
                  data.client
                    ? [data.client.lastName, data.client.firstName, data.client.middleName].filter(Boolean).join(' ') || '—'
                    : '—'
                } />
                <Detail label="Email" value={data.client?.email} />
                <Detail label="Логин" value={data.clientUserSub} />
                <Detail label="ID клиента" value={data.clientId} mono />
              </div>

              <SectionTitle>Заявка</SectionTitle>
              <div className="admin-detail-grid">
                <Detail label="Сумма" value={formatMoney(data.requestedAmount)} />
                <Detail label="Срок" value={data.requestedTerm ? `${data.requestedTerm} мес.` : null} />
                <Detail label="Создана" value={formatDate(data.creationDate)} />
                <Detail label="Подписана" value={formatDate(data.signDate)} />
              </div>

              {data.clientId && <>
                <SectionTitle>Документы клиента</SectionTitle>
                <AdminClientDocuments clientId={data.clientId} />
              </>}

              <SectionTitle>Автоматическая проверка документов</SectionTitle>
              <ValidationResultSection statementId={statementId} />


              {data.credit && <>
                <SectionTitle>Кредит</SectionTitle>
                <div className="admin-detail-grid">
                  <Detail label="Сумма" value={formatMoney(data.credit.amount)} />
                  <Detail label="Срок" value={data.credit.term ? `${data.credit.term} мес.` : null} />
                  <Detail label="Ставка" value={data.credit.rate != null ? `${data.credit.rate}%` : null} />
                  <Detail label="Ежемес. платёж" value={formatMoney(data.credit.monthlyPayment)} />
                  <Detail label="ПСК" value={data.credit.psk} />
                  <Detail label="Статус кредита"
                    value={<span className={`admin-status-badge ${statusColor(data.credit.creditStatus)}`}>{data.credit.creditStatus || '—'}</span>}
                  />
                  <Detail label="Страховка" value={data.credit.isInsuranceEnabled != null ? (data.credit.isInsuranceEnabled ? 'Да' : 'Нет') : null} />
                  <Detail label="Зарплатный клиент" value={data.credit.isSalaryClient != null ? (data.credit.isSalaryClient ? 'Да' : 'Нет') : null} />
                </div>
              </>}

              {!hideActions && <>
                <SectionTitle>Действия</SectionTitle>

                {/* Prominent "Issue credit" button — only for DOCUMENT_SIGNED */}
                {data.status === 'DOCUMENT_SIGNED' && (
                  <div style={{
                    marginBottom: '0.85rem',
                    padding: '0.85rem 1rem',
                    background: '#f0fdf4',
                    border: '1px solid #86efac',
                    borderRadius: 10,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.75rem',
                    flexWrap: 'wrap',
                  }}>
                    <span style={{ fontSize: '1.1rem' }}>✅</span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div style={{ fontWeight: 600, fontSize: '0.92rem', color: '#166534' }}>
                        Документы подписаны
                      </div>
                      <div style={{ fontSize: '0.8rem', color: '#4ade80', color: '#16a34a' }}>
                        Заявка готова к выдаче кредита
                      </div>
                    </div>
                    <button
                      className="admin-btn primary"
                      disabled={savingStatus}
                      style={{ background: 'linear-gradient(135deg,#16a34a,#15803d)', fontSize: '0.95rem', padding: '0.6rem 1.4rem', whiteSpace: 'nowrap' }}
                      onClick={() => updateStatus('CREDIT_ISSUED')}
                    >
                      {savingStatus ? 'Выдача...' : '💳 Выдать кредит'}
                    </button>
                  </div>
                )}

                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
                  <select
                    value={newStatus}
                    onChange={(e) => setNewStatus(e.target.value)}
                    style={{ border: '1px solid #d1d5db', borderRadius: 8, padding: '0.4rem 0.65rem', fontSize: '0.88rem' }}
                  >
                    {APPLICATION_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                  <button className="admin-btn primary" disabled={savingStatus || !newStatus || newStatus === data.status}
                    onClick={() => updateStatus(newStatus)}>Применить статус</button>
                  <button className="admin-btn success" disabled={savingStatus} onClick={() => updateStatus('APPROVED')}>Одобрить</button>
                  <button className="admin-btn danger" disabled={savingStatus} onClick={() => updateStatus('CC_DENIED')}>Отклонить</button>
                </div>
              </>}
            </>
          )}
        </div>

        <div className="admin-modal-footer">
          <button className="admin-btn" onClick={onChanged || onClose}>Закрыть{!hideActions && ' и обновить'}</button>
        </div>
      </div>
    </div>
  );
}

// ── Validation result section ─────────────────────────────────────────────────

// Человекочитаемые подписи для типов ошибок, возвращаемых бэкендом.
const VALIDATION_ERROR_LABELS = {
  FIO_MISMATCH:             'Несовпадение ФИО',
  BIRTH_DATE_MISMATCH:      'Несовпадение даты рождения',
  PASSPORT_MISMATCH:        'Несовпадение паспортных данных',
  EMPLOYER_INN_MISMATCH:    'Несовпадение ИНН работодателя',
  SALARY_MISMATCH:          'Несовпадение зарплаты',
  WORK_EXPERIENCE_MISMATCH: 'Несовпадение стажа',
  FIELD_NOT_FOUND:          'Поле не найдено в документе',
  DOCUMENT_MISSING:         'Документ не загружен',
  PDF_PARSE_ERROR:          'Ошибка чтения PDF',
  UNEMPLOYED:               'Клиент не трудоустроен',
  INTERNAL_ERROR:           'Внутренняя ошибка',
};

/**
 * Карточка с результатом автоматической валидации PDF-документов по заявке.
 * При первом рендере подтягивает последний результат через GET
 * /admin/documents/{statementId}/validation-result.
 *
 * Если результата ещё нет (404) — предлагает запустить проверку вручную.
 * Кнопка «Перезапустить» отправляет POST /admin/documents/{statementId}/validate
 * и обновляет показанный результат свежими данными.
 */
function ValidationResultSection({ statementId }) {
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notFound, setNotFound] = useState(false);
  const [revalidating, setRevalidating] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    setNotFound(false);
    try {
      const resp = await client.get(adminApi(`/admin/documents/${statementId}/validation-result`));
      setResult(resp.data);
    } catch (e) {
      if (e?.response?.status === 404) {
        setNotFound(true);
      } else {
        setError(extractError(e));
      }
    } finally {
      setLoading(false);
    }
  }, [statementId]);

  useEffect(() => { load(); }, [load]);

  const revalidate = async () => {
    setRevalidating(true);
    setError(null);
    try {
      const resp = await client.post(adminApi(`/admin/documents/${statementId}/validate`));
      setResult(resp.data);
      setNotFound(false);
    } catch (e) {
      setError(extractError(e));
    } finally {
      setRevalidating(false);
    }
  };

  if (loading) return <div className="admin-loading">Загрузка результата проверки...</div>;

  if (error) {
    return (
      <div className="admin-error" style={{ marginBottom: '0.5rem' }}>
        {error}
        <div style={{ marginTop: '0.5rem' }}>
          <button className="admin-btn sm" onClick={load}>↻ Повторить</button>
        </div>
      </div>
    );
  }

  if (notFound || !result) {
    return (
      <div style={{
        padding: '0.85rem 1rem',
        background: '#f9fafb',
        border: '1px dashed #d1d5db',
        borderRadius: 8,
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem',
        flexWrap: 'wrap',
      }}>
        <span style={{ fontSize: '0.88rem', color: '#6b7280' }}>
          Автоматическая проверка ещё не запускалась для этой заявки.
        </span>
        <button className="admin-btn sm" disabled={revalidating} onClick={revalidate}>
          {revalidating ? 'Запуск...' : '▶ Запустить проверку'}
        </button>
      </div>
    );
  }

  const isSuccess = result.success === true;
  const errors = Array.isArray(result.errors) ? result.errors : [];

  return (
    <div>
      <div style={{
        padding: '0.85rem 1rem',
        background: isSuccess ? '#f0fdf4' : '#fef3c7',
        border: `1px solid ${isSuccess ? '#86efac' : '#fcd34d'}`,
        borderRadius: 10,
        marginBottom: '0.75rem',
        display: 'flex',
        alignItems: 'center',
        gap: '0.75rem',
        flexWrap: 'wrap',
      }}>
        <span style={{ fontSize: '1.2rem' }}>{isSuccess ? '✅' : '⚠️'}</span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontWeight: 600, color: isSuccess ? '#166534' : '#92400e' }}>
            {isSuccess
              ? 'Автоматическая проверка пройдена'
              : `Найдено несовпадений: ${errors.length}`}
          </div>
          <div style={{ fontSize: '0.78rem', color: '#6b7280', marginTop: 2 }}>
            Проверено: {formatDate(result.validatedAt)} · Итоговый статус: {result.finalStatus || '—'}
          </div>
        </div>
        <button className="admin-btn sm" disabled={revalidating} onClick={revalidate}>
          {revalidating ? 'Запуск...' : '↻ Перезапустить'}
        </button>
      </div>

      {!isSuccess && errors.length > 0 && (
        <div style={{ overflowX: 'auto', border: '1px solid #e5e7eb', borderRadius: 8 }}>
          <table className="admin-table" style={{ margin: 0 }}>
            <thead>
              <tr>
                <th style={{ width: '24%' }}>Тип</th>
                <th style={{ width: '20%' }}>Поле</th>
                <th style={{ width: '18%' }}>Ожидалось</th>
                <th style={{ width: '18%' }}>Фактически</th>
                <th>Сообщение</th>
              </tr>
            </thead>
            <tbody>
              {errors.map((err, idx) => (
                <tr key={idx}>
                  <td style={{ fontSize: '0.82rem', color: '#b45309', whiteSpace: 'nowrap' }}>
                    {VALIDATION_ERROR_LABELS[err.errorType] || err.errorType || '—'}
                  </td>
                  <td style={{ fontFamily: 'monospace', fontSize: '0.78rem', color: '#374151' }}>
                    {err.field || '—'}
                  </td>
                  <td style={{ fontSize: '0.82rem' }}>{err.expected ?? '—'}</td>
                  <td style={{ fontSize: '0.82rem' }}>{err.actual ?? '—'}</td>
                  <td style={{ fontSize: '0.82rem', color: '#4b5563' }}>{err.message || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function SectionTitle({ children }) {
  return (
    <h4 style={{ fontSize: '0.85rem', textTransform: 'uppercase', color: '#6b7280', letterSpacing: '0.05em', margin: '1rem 0 0.6rem' }}>
      {children}
    </h4>
  );
}

function Detail({ label, value, mono }) {
  return (
    <div className="admin-detail-row">
      <div className="admin-detail-label">{label}</div>
      <div className="admin-detail-value" style={mono ? { fontFamily: 'monospace', fontSize: '0.8rem' } : {}}>
        {value ?? '—'}
      </div>
    </div>
  );
}
