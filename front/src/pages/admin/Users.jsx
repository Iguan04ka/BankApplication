import React, { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../../api/client';
import { adminApi, extractError } from './shared';

export default function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [allRoles, setAllRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('');
  const [editing, setEditing] = useState(null);   // user being role-edited
  const [detailUser, setDetailUser] = useState(null); // user whose detail modal is open
  const navigate = useNavigate();

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [u, r] = await Promise.all([
        client.get(adminApi('/admin/users')),
        client.get(adminApi('/admin/roles')),
      ]);
      setUsers(Array.isArray(u.data) ? u.data : []);
      setAllRoles(Array.isArray(r.data) ? r.data : []);
    } catch (e) {
      setError(extractError(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const setBlocked = async (e, sub, blocked) => {
    e.stopPropagation(); // don't open detail modal
    if (blocked && !window.confirm('Заблокировать пользователя ' + sub + '?')) return;
    try {
      const resp = await client.put(
        adminApi(`/admin/users/${encodeURIComponent(sub)}/blocked`),
        { blocked },
      );
      setUsers((list) => list.map((u) =>
        u.userKey?.sub === sub ? resp.data : u
      ));
      // keep detail modal in sync
      if (detailUser?.userKey?.sub === sub) setDetailUser(resp.data);
    } catch (e) {
      alert(extractError(e));
    }
  };

  const openRoleEdit = (e, u) => {
    e.stopPropagation();
    setEditing(u);
  };

  // navigate to statements filtered by this user
  const viewStatements = (sub) => {
    navigate(`/admin/statements?user=${encodeURIComponent(sub)}`);
  };

  const filtered = users.filter((u) => {
    if (!filter) return true;
    const q = filter.toLowerCase();
    return (
      (u.userKey?.sub || '').toLowerCase().includes(q) ||
      (u.email || '').toLowerCase().includes(q) ||
      (u.roles || []).some((r) => (r.name || '').toLowerCase().includes(q))
    );
  });

  return (
    <>
      <div className="admin-page-header">
        <h2 className="admin-page-title">Пользователи</h2>
        <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <input
            placeholder="Поиск по логину, email или роли..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            style={{ border: '1px solid #d1d5db', borderRadius: 8, padding: '0.4rem 0.7rem', fontSize: '0.88rem', minWidth: 260 }}
          />
          <button className="admin-btn" onClick={load} disabled={loading}>↻ Обновить</button>
        </div>
      </div>

      {error && <div className="admin-error">{error}</div>}

      <div className="admin-card" style={{ padding: 0 }}>
        {loading ? (
          <div className="admin-loading">Загрузка пользователей...</div>
        ) : filtered.length === 0 ? (
          <div style={{ padding: '3rem 1rem', textAlign: 'center', color: '#9ca3af' }}>
            {users.length === 0 ? 'Пользователи не найдены' : 'Нет совпадений'}
          </div>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Логин</th>
                <th>Email</th>
                <th>Роли</th>
                <th>2FA</th>
                <th>Статус</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((u) => {
                const sub = u.userKey?.sub;
                const roles = u.roles || [];
                return (
                  <tr
                    key={sub}
                    style={{ cursor: 'pointer' }}
                    onClick={() => setDetailUser(u)}
                    title="Нажмите для просмотра деталей"
                  >
                    <td>
                      <div style={{ fontWeight: 500 }}>{sub}</div>
                      <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>
                        {u.userKey?.systemCode || ''}
                      </div>
                    </td>
                    <td style={{ fontSize: '0.85rem' }}>{u.email || '—'}</td>
                    <td>
                      <div style={{ display: 'flex', gap: '0.3rem', flexWrap: 'wrap' }}>
                        {roles.length === 0 && <span style={{ color: '#9ca3af' }}>—</span>}
                        {roles.map((r) => (
                          <span key={r.id || r.name}
                            className={`admin-status-badge ${r.name === 'admin' ? 'info' : 'neutral'}`}>
                            {r.name}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td>
                      {u.twoFactorEnabled
                        ? <span className="admin-status-badge success">вкл.</span>
                        : <span className="admin-status-badge neutral">выкл.</span>}
                    </td>
                    <td>
                      {u.blocked
                        ? <span className="admin-status-badge danger">заблокирован</span>
                        : <span className="admin-status-badge success">активен</span>}
                    </td>
                    <td style={{ textAlign: 'right' }} onClick={(e) => e.stopPropagation()}>
                      <div style={{ display: 'inline-flex', gap: '0.4rem' }}>
                        <button className="admin-btn sm" onClick={(e) => openRoleEdit(e, u)}>
                          Роли
                        </button>
                        {u.blocked ? (
                          <button className="admin-btn success sm" onClick={(e) => setBlocked(e, sub, false)}>
                            Разблокировать
                          </button>
                        ) : (
                          <button className="admin-btn danger sm" onClick={(e) => setBlocked(e, sub, true)}>
                            Заблокировать
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {detailUser && (
        <UserDetailModal
          user={detailUser}
          onClose={() => setDetailUser(null)}
          onBlock={(sub, blocked) => setBlocked({ stopPropagation: () => {} }, sub, blocked)}
          onEditRoles={() => { setEditing(detailUser); setDetailUser(null); }}
          onViewStatements={(sub) => { setDetailUser(null); viewStatements(sub); }}
        />
      )}

      {editing && (
        <RoleEditModal
          user={editing}
          allRoles={allRoles}
          onClose={() => setEditing(null)}
          onSaved={(updated) => {
            setUsers((list) => list.map((u) =>
              u.userKey?.sub === updated.userKey?.sub ? updated : u
            ));
            setEditing(null);
          }}
        />
      )}
    </>
  );
}

// ── User detail modal ─────────────────────────────────────────────────────────

function UserDetailModal({ user, onClose, onBlock, onEditRoles, onViewStatements }) {
  const sub = user.userKey?.sub;
  const roles = user.roles || [];
  const isAdmin = roles.some((r) => (r.name || '').toLowerCase() === 'admin');

  return (
    <div className="admin-modal-backdrop" onClick={onClose}>
      <div className="admin-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 540 }}>
        <div className="admin-modal-header">
          <h3 className="admin-modal-title">Пользователь</h3>
          <button className="admin-modal-close" onClick={onClose}>×</button>
        </div>

        <div className="admin-modal-body">
          {/* Avatar + name */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem' }}>
            <div style={{
              width: 56, height: 56, borderRadius: '50%',
              background: isAdmin
                ? 'linear-gradient(135deg, #7c3aed, #4f46e5)'
                : 'linear-gradient(135deg, #0066cc, #003d7a)',
              color: '#fff', display: 'flex', alignItems: 'center',
              justifyContent: 'center', fontWeight: 700, fontSize: '1.4rem',
            }}>
              {(sub || 'U').slice(0, 1).toUpperCase()}
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '1.1rem' }}>{sub}</div>
              <div style={{ color: '#6b7280', fontSize: '0.82rem' }}>{user.email || 'email не указан'}</div>
              {roles.map((r) => (
                <span key={r.id || r.name}
                  className={`admin-status-badge ${r.name === 'admin' ? 'info' : 'neutral'}`}
                  style={{ marginRight: 4, marginTop: 4 }}>
                  {r.name}
                </span>
              ))}
            </div>
          </div>

          <div className="admin-detail-grid">
            <div className="admin-detail-row">
              <div className="admin-detail-label">Логин (sub)</div>
              <div className="admin-detail-value" style={{ fontFamily: 'monospace' }}>{sub || '—'}</div>
            </div>
            <div className="admin-detail-row">
              <div className="admin-detail-label">Email</div>
              <div className="admin-detail-value">{user.email || '—'}</div>
            </div>
            <div className="admin-detail-row">
              <div className="admin-detail-label">Системный код</div>
              <div className="admin-detail-value">{user.userKey?.systemCode || '—'}</div>
            </div>
            <div className="admin-detail-row">
              <div className="admin-detail-label">2FA</div>
              <div className="admin-detail-value">
                {user.twoFactorEnabled
                  ? <span className="admin-status-badge success">включена</span>
                  : <span className="admin-status-badge neutral">выключена</span>}
              </div>
            </div>
            <div className="admin-detail-row">
              <div className="admin-detail-label">Статус</div>
              <div className="admin-detail-value">
                {user.blocked
                  ? <span className="admin-status-badge danger">заблокирован</span>
                  : <span className="admin-status-badge success">активен</span>}
              </div>
            </div>
            <div className="admin-detail-row">
              <div className="admin-detail-label">Роли</div>
              <div className="admin-detail-value">
                {roles.length === 0 ? '—' : roles.map((r) => r.name).join(', ')}
              </div>
            </div>
          </div>
        </div>

        <div className="admin-modal-footer" style={{ justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            <button className="admin-btn sm" onClick={onEditRoles}>Изменить роли</button>
            <button className="admin-btn sm" onClick={() => onViewStatements(sub)}>
              📄 Заявки
            </button>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem' }}>
            {user.blocked ? (
              <button className="admin-btn success sm" onClick={() => { onBlock(sub, false); onClose(); }}>
                Разблокировать
              </button>
            ) : (
              <button className="admin-btn danger sm" onClick={() => { onBlock(sub, true); onClose(); }}>
                Заблокировать
              </button>
            )}
            <button className="admin-btn" onClick={onClose}>Закрыть</button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Role edit modal ───────────────────────────────────────────────────────────

function RoleEditModal({ user, allRoles, onClose, onSaved }) {
  const [selected, setSelected] = useState(
    new Set((user.roles || []).map((r) => r.name))
  );
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const toggle = (name) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name); else next.add(name);
      return next;
    });
  };

  const save = async () => {
    if (selected.size === 0) { setError('Выберите хотя бы одну роль'); return; }
    setSaving(true);
    setError(null);
    try {
      const sub = user.userKey?.sub;
      const resp = await client.put(
        adminApi(`/admin/users/${encodeURIComponent(sub)}/roles`),
        { roleNames: Array.from(selected) },
      );
      onSaved(resp.data);
    } catch (e) {
      setError(extractError(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="admin-modal-backdrop" onClick={onClose}>
      <div className="admin-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 480 }}>
        <div className="admin-modal-header">
          <h3 className="admin-modal-title">Роли пользователя {user.userKey?.sub}</h3>
          <button className="admin-modal-close" onClick={onClose}>×</button>
        </div>
        <div className="admin-modal-body">
          {error && <div className="admin-error">{error}</div>}
          <p style={{ color: '#6b7280', fontSize: '0.85rem', margin: '0 0 0.85rem' }}>
            Выберите все роли, которые должны быть у пользователя.
          </p>
          <div className="admin-roles-grid">
            {allRoles.length === 0
              ? <div style={{ color: '#9ca3af' }}>Роли не загружены</div>
              : allRoles.map((r) => {
                  const active = selected.has(r.name);
                  return (
                    <label key={r.id || r.name} className={`admin-role-chip ${active ? 'active' : ''}`}>
                      <input type="checkbox" checked={active} onChange={() => toggle(r.name)} />
                      {r.name}
                    </label>
                  );
                })}
          </div>
          {selected.has('admin') && (
            <div style={{ marginTop: '0.85rem', color: '#6b7280', fontSize: '0.8rem' }}>
              ℹ️ При выдаче роли admin двухфакторная аутентификация будет принудительно включена.
            </div>
          )}
        </div>
        <div className="admin-modal-footer">
          <button className="admin-btn" onClick={onClose} disabled={saving}>Отмена</button>
          <button className="admin-btn primary" onClick={save} disabled={saving}>
            {saving ? 'Сохранение...' : 'Сохранить'}
          </button>
        </div>
      </div>
    </div>
  );
}
