import React, { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../../api/client';
import { adminApi, formatMoney, statusColor, extractError } from './shared';

// ── Column sort for the status table ─────────────────────────────────────────

const SORT_COLS = ['status', 'count', 'pct'];

function nextDir(cur) { return cur === 'desc' ? 'asc' : 'desc'; }

function SortTh({ label, colKey, sort, onSort, style }) {
  const active = sort.col === colKey;
  return (
    <th
      style={{ cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap', ...style }}
      onClick={() => onSort(colKey)}
    >
      {label}
      <span style={{ marginLeft: 4, opacity: active ? 1 : 0.3, fontSize: '0.75rem' }}>
        {active ? (sort.dir === 'desc' ? '▼' : '▲') : '⇅'}
      </span>
    </th>
  );
}

// ─────────────────────────────────────────────────────────────────────────────

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Sort state for status table. Default: by count desc (most common first).
  const [sort, setSort] = useState({ col: 'count', dir: 'desc' });

  const navigate = useNavigate();

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const resp = await client.get(adminApi('/admin/dashboard/stats'));
      setStats(resp.data || {});
    } catch (e) {
      setError(extractError(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSort = (col) => {
    setSort((prev) =>
      prev.col === col
        ? { col, dir: nextDir(prev.dir) }
        : { col, dir: col === 'status' ? 'asc' : 'desc' }
    );
  };

  const handleStatusClick = (status) => {
    navigate(`/admin/statements?status=${encodeURIComponent(status)}`);
  };

  const byStatus = stats?.statementsByStatus || {};
  const total    = stats?.totalStatements ?? 0;
  const credits  = stats?.totalCredits ?? 0;
  const issuedAmount = stats?.totalIssuedAmount ?? 0;

  // Build rows and apply sorting
  const sortedRows = useMemo(() => {
    const entries = Object.entries(byStatus).map(([status, count]) => ({
      status,
      count: count || 0,
      pct: total > 0 ? Math.round(((count || 0) / total) * 1000) / 10 : 0,
    }));

    return [...entries].sort((a, b) => {
      let va, vb;
      if (sort.col === 'status') { va = a.status; vb = b.status; }
      else if (sort.col === 'count') { va = a.count; vb = b.count; }
      else { va = a.pct; vb = b.pct; }

      let cmp;
      if (typeof va === 'number') {
        cmp = va - vb;
      } else {
        cmp = String(va).localeCompare(String(vb), 'ru', { numeric: true });
      }
      return sort.dir === 'asc' ? cmp : -cmp;
    });
  }, [byStatus, total, sort]);

  if (loading) return <div className="admin-loading">Загрузка статистики...</div>;
  if (error)   return <div className="admin-error">{error}</div>;

  return (
    <>
      <div className="admin-page-header">
        <h2 className="admin-page-title">Главная</h2>
        <button className="admin-btn" onClick={load}>↻ Обновить</button>
      </div>

      {/* ── Top stat cards ──────────────────────────────────────────────── */}
      <div className="admin-stats">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Всего заявок</div>
          <div className="admin-stat-value">{total}</div>
          <div className="admin-stat-sub">за всё время</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Кредитов</div>
          <div className="admin-stat-value">{credits}</div>
          <div className="admin-stat-sub">оформлено всего</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Выдано на сумму</div>
          <div className="admin-stat-value">{formatMoney(issuedAmount)}</div>
          <div className="admin-stat-sub">заявки со статусом CREDIT_ISSUED</div>
        </div>
      </div>

      {/* ── Status distribution table ────────────────────────────────────── */}
      <div className="admin-card">
        <h3 style={{ margin: 0, fontSize: '1rem' }}>Заявки по статусам</h3>
        <p style={{ color: '#6b7280', fontSize: '0.85rem', margin: '0.4rem 0 1rem' }}>
          Распределение всех заявок в системе.&nbsp;
          <span style={{ color: '#0066cc', fontSize: '0.8rem' }}>
            Кликните на строку, чтобы перейти к заявкам с этим статусом.
          </span>
        </p>

        {sortedRows.length === 0 ? (
          <div className="empty">Нет данных</div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <SortTh label="Статус"    colKey="status" sort={sort} onSort={handleSort} />
                <SortTh label="Количество" colKey="count"  sort={sort} onSort={handleSort}
                        style={{ textAlign: 'right' }} />
                <SortTh label="Доля"      colKey="pct"    sort={sort} onSort={handleSort}
                        style={{ width: '40%' }} />
              </tr>
            </thead>
            <tbody>
              {sortedRows.map(({ status, count, pct }) => (
                <tr
                  key={status}
                  onClick={() => handleStatusClick(status)}
                  style={{ cursor: 'pointer' }}
                  title={`Показать заявки со статусом ${status}`}
                >
                  <td>
                    <span className={`admin-status-badge ${statusColor(status)}`}>
                      {status}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                    {count}
                  </td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <div style={{
                        flex: 1, height: 8,
                        background: '#f3f4f6', borderRadius: 4, overflow: 'hidden',
                      }}>
                        <div style={{
                          width: `${pct}%`, height: '100%', background: '#0066cc',
                        }} />
                      </div>
                      <span style={{ fontSize: '0.78rem', color: '#6b7280', width: 50 }}>
                        {pct}%
                      </span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
