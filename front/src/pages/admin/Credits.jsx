import React, { useEffect, useState, useCallback, useMemo } from 'react';
import client from '../../api/client';
import { adminApi, formatMoney, statusColor, extractError } from './shared';
import { StatementDetailModal } from './Statements';

// ── shared sort helpers (same pattern as Statements) ─────────────────────────

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
    let va = resolveCredit(a, col);
    let vb = resolveCredit(b, col);
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

function resolveCredit(item, col) {
  switch (col) {
    case 'amount':   return parseFloat(item.amount ?? 0);
    case 'term':     return parseInt(item.term ?? 0, 10);
    case 'rate':     return parseFloat(item.rate ?? 0);
    case 'payment':  return parseFloat(item.monthlyPayment ?? 0);
    case 'psk':      return parseFloat(item.psk ?? 0);
    case 'status':   return item.creditStatus || '';
    default:         return '';
  }
}

// ─────────────────────────────────────────────────────────────────────────────

export default function AdminCredits() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const sort = useSortState('amount', 'desc');

  // Which credit's schedule/detail modal is open
  const [openSchedule, setOpenSchedule] = useState(null);

  // Statement modal opened from credit
  const [statementId, setStatementId] = useState(null);
  const [statementLoading, setStatementLoading] = useState(false);
  const [statementError, setStatementError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const resp = await client.get(adminApi('/admin/credits'));
      setItems(Array.isArray(resp.data) ? resp.data : []);
    } catch (e) {
      setError(extractError(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const displayed = useMemo(
    () => sortItems(items, sort.col, sort.dir),
    [items, sort.col, sort.dir]
  );

  const openStatement = async (creditId) => {
    setStatementError(null);
    setStatementLoading(true);
    try {
      const resp = await client.get(adminApi(`/admin/credits/${creditId}/statement`));
      const id = resp.data?.statementId;
      if (!id) throw new Error('statementId не найден в ответе');
      setStatementId(id);
    } catch (e) {
      setStatementError(extractError(e));
    } finally {
      setStatementLoading(false);
    }
  };

  return (
    <>
      <div className="admin-page-header">
        <h2 className="admin-page-title">Кредиты</h2>
        <button className="admin-btn" onClick={load} disabled={loading}>↻ Обновить</button>
      </div>

      {error && <div className="admin-error">{error}</div>}
      {statementError && (
        <div className="admin-error" style={{ marginBottom: '1rem' }}>
          {statementError}
          <button style={{ marginLeft: 8, background: 'none', border: 'none', cursor: 'pointer', color: '#991b1b' }}
            onClick={() => setStatementError(null)}>✕</button>
        </div>
      )}

      <div className="admin-card" style={{ padding: 0 }}>
        {loading ? (
          <div className="admin-loading">Загрузка кредитов...</div>
        ) : displayed.length === 0 ? (
          <div style={{ padding: '3rem 1rem', textAlign: 'center', color: '#9ca3af' }}>
            Кредиты не найдены
          </div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <SortTh label="Сумма" colKey="amount" sort={sort} />
                <SortTh label="Срок" colKey="term" sort={sort} />
                <SortTh label="Ставка" colKey="rate" sort={sort} />
                <SortTh label="Платёж" colKey="payment" sort={sort} />
                <SortTh label="ПСК" colKey="psk" sort={sort} />
                <SortTh label="Статус" colKey="status" sort={sort} />
                <th></th>
              </tr>
            </thead>
            <tbody>
              {displayed.map((c) => (
                <tr key={c.creditId}>
                  <td style={{ fontFamily: 'monospace', fontSize: '0.78rem', color: '#6b7280' }}>
                    {String(c.creditId || '').slice(0, 8)}…
                  </td>
                  <td>{formatMoney(c.amount)}</td>
                  <td>{c.term ?? '—'} мес.</td>
                  <td>{c.rate ?? '—'}%</td>
                  <td>{formatMoney(c.monthlyPayment)}</td>
                  <td>{c.psk ?? '—'}</td>
                  <td>
                    <span className={`admin-status-badge ${statusColor(c.creditStatus)}`}>
                      {c.creditStatus || '—'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    <div style={{ display: 'inline-flex', gap: '0.4rem' }}>
                      <button className="admin-btn sm" onClick={() => setOpenSchedule(c.creditId)}>
                        График
                      </button>
                      <button
                        className="admin-btn sm primary"
                        disabled={statementLoading}
                        onClick={() => openStatement(c.creditId)}
                        title="Открыть заявку по этому кредиту"
                      >
                        {statementLoading ? '...' : '→ Заявка'}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {openSchedule && (
        <ScheduleModal
          creditId={openSchedule}
          onClose={() => setOpenSchedule(null)}
          onOpenStatement={(cid) => { setOpenSchedule(null); openStatement(cid); }}
        />
      )}

      {statementId && (
        <StatementDetailModal
          statementId={statementId}
          onClose={() => setStatementId(null)}
          onChanged={() => setStatementId(null)}
          hideActions
        />
      )}
    </>
  );
}

// ── Schedule / credit detail modal ───────────────────────────────────────────

function ScheduleModal({ creditId, onClose, onOpenStatement }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const resp = await client.get(adminApi(`/admin/credits/${creditId}`));
        if (alive) setData(resp.data);
      } catch (e) {
        if (alive) setError(extractError(e));
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => { alive = false; };
  }, [creditId]);

  let schedule = data?.paymentSchedule;
  if (typeof schedule === 'string') {
    try { schedule = JSON.parse(schedule); } catch { /* keep */ }
  }
  const rows = Array.isArray(schedule) ? schedule : [];

  return (
    <div className="admin-modal-backdrop" onClick={onClose}>
      <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
        <div className="admin-modal-header">
          <h3 className="admin-modal-title">График платежей</h3>
          <button className="admin-modal-close" onClick={onClose}>×</button>
        </div>

        <div className="admin-modal-body">
          {loading && <div className="admin-loading">Загрузка...</div>}
          {error && <div className="admin-error">{error}</div>}

          {!loading && data && <>
            <div className="admin-detail-grid" style={{ marginBottom: '1rem' }}>
              <div className="admin-detail-row">
                <div className="admin-detail-label">Сумма</div>
                <div className="admin-detail-value">{formatMoney(data.amount)}</div>
              </div>
              <div className="admin-detail-row">
                <div className="admin-detail-label">Срок</div>
                <div className="admin-detail-value">{data.term ?? '—'} мес.</div>
              </div>
              <div className="admin-detail-row">
                <div className="admin-detail-label">Ставка</div>
                <div className="admin-detail-value">{data.rate ?? '—'}%</div>
              </div>
              <div className="admin-detail-row">
                <div className="admin-detail-label">Ежемес. платёж</div>
                <div className="admin-detail-value">{formatMoney(data.monthlyPayment)}</div>
              </div>
              <div className="admin-detail-row">
                <div className="admin-detail-label">ПСК</div>
                <div className="admin-detail-value">{data.psk ?? '—'}</div>
              </div>
              <div className="admin-detail-row">
                <div className="admin-detail-label">Статус</div>
                <div className="admin-detail-value">
                  <span className={`admin-status-badge ${statusColor(data.creditStatus)}`}>
                    {data.creditStatus || '—'}
                  </span>
                </div>
              </div>
            </div>

            <h4 style={{ fontSize: '0.85rem', textTransform: 'uppercase', color: '#6b7280', letterSpacing: '0.05em', margin: '0 0 0.6rem' }}>
              Платежи
            </h4>
            {rows.length === 0 ? (
              <div style={{ color: '#9ca3af' }}>График платежей отсутствует</div>
            ) : (
              <table className="admin-table">
                <thead>
                  <tr>
                    <th>№</th>
                    <th>Дата</th>
                    <th>Платёж</th>
                    <th>Проценты</th>
                    <th>Долг</th>
                    <th>Остаток</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r, i) => (
                    <tr key={i}>
                      <td>{r.number ?? i + 1}</td>
                      <td style={{ fontSize: '0.82rem' }}>{r.date || '—'}</td>
                      <td>{formatMoney(r.totalPayment ?? r.total ?? r.payment)}</td>
                      <td>{formatMoney(r.interestPayment ?? r.interest)}</td>
                      <td>{formatMoney(r.debtPayment ?? r.principal)}</td>
                      <td>{formatMoney(r.remainingDebt ?? r.balance)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>}
        </div>

        <div className="admin-modal-footer" style={{ justifyContent: 'space-between' }}>
          <button
            className="admin-btn primary"
            onClick={() => onOpenStatement(creditId)}
          >
            → Открыть заявку
          </button>
          <button className="admin-btn" onClick={onClose}>Закрыть</button>
        </div>
      </div>
    </div>
  );
}
