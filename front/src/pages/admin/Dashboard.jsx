import React, { useEffect, useState } from 'react';
import client from '../../api/client';
import { adminApi, formatMoney } from './shared';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

  useEffect(() => { load(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, []);

  if (loading) return <div className="admin-loading">Загрузка статистики...</div>;
  if (error) return <div className="admin-error">{error}</div>;

  const byStatus = (stats?.statementsByStatus) || {};
  const total = stats?.totalStatements ?? 0;
  const credits = stats?.totalCredits ?? 0;
  const issuedAmount = stats?.totalIssuedAmount ?? 0;

  // Sort statuses by count desc for nicer display
  const statusEntries = Object.entries(byStatus).sort((a, b) => (b[1] || 0) - (a[1] || 0));

  return (
    <>
      <div className="admin-page-header">
        <h2 className="admin-page-title">Главная</h2>
        <button className="admin-btn" onClick={load}>↻ Обновить</button>
      </div>

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
          <div className="admin-stat-sub">только статус ISSUED</div>
        </div>
      </div>

      <div className="admin-card">
        <h3 style={{ margin: 0, fontSize: '1rem' }}>Заявки по статусам</h3>
        <p style={{ color: '#6b7280', fontSize: '0.85rem', margin: '0.4rem 0 1rem' }}>
          Распределение всех заявок в системе.
        </p>

        {statusEntries.length === 0 ? (
          <div className="empty">Нет данных</div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Статус</th>
                <th style={{ textAlign: 'right' }}>Количество</th>
                <th style={{ width: '40%' }}>Доля</th>
              </tr>
            </thead>
            <tbody>
              {statusEntries.map(([status, count]) => {
                const pct = total > 0 ? Math.round(((count || 0) / total) * 1000) / 10 : 0;
                return (
                  <tr key={status}>
                    <td>
                      <span className={`admin-status-badge ${statusColorClass(status)}`}>
                        {status}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                      {count}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <div style={{
                          flex: 1,
                          height: 8,
                          background: '#f3f4f6',
                          borderRadius: 4,
                          overflow: 'hidden',
                        }}>
                          <div style={{
                            width: `${pct}%`,
                            height: '100%',
                            background: '#0066cc',
                          }} />
                        </div>
                        <span style={{ fontSize: '0.78rem', color: '#6b7280', width: 50 }}>
                          {pct}%
                        </span>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}

function statusColorClass(status) {
  if (!status) return 'neutral';
  const s = String(status).toUpperCase();
  if (s.includes('DENIED')) return 'danger';
  if (s.includes('ISSUED') || s.includes('SIGNED') || s.includes('CC_APPROVED')) return 'success';
  if (s.includes('APPROVED') || s.includes('PREAPPROVAL')) return 'info';
  if (s.includes('DOCUMENT') || s.includes('PREPARE')) return 'warning';
  return 'neutral';
}

function extractError(e) {
  const data = e?.response?.data;
  if (data && typeof data === 'object') return data.error || data.message || JSON.stringify(data);
  if (typeof data === 'string') return data;
  return e?.message || 'Не удалось загрузить данные';
}
