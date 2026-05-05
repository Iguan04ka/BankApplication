import React, { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthProvider';
import ThemeToggle from '../../shared/ui/ThemeToggle/ThemeToggle';
import './AdminLayout.css';

export default function AdminLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/auth/login', { replace: true });
  };

  const initials = (user?.sub || 'A').slice(0, 2).toUpperCase();

  return (
    <div className={`admin-shell ${collapsed ? 'collapsed' : ''}`}>
      <aside className="admin-sidebar">
        <div className="admin-brand">
          <span className="admin-brand-mark">⚡</span>
          {!collapsed && <span className="admin-brand-text">АтласКредит<br /><small>Admin</small></span>}
        </div>

        <nav className="admin-nav">
          <NavLink to="/admin/dashboard" className="admin-nav-link">
            <span className="admin-nav-icon">📊</span>
            {!collapsed && <span>Дашборд</span>}
          </NavLink>
          <NavLink to="/admin/statements" className="admin-nav-link">
            <span className="admin-nav-icon">📄</span>
            {!collapsed && <span>Заявки</span>}
          </NavLink>
          <NavLink to="/admin/users" className="admin-nav-link">
            <span className="admin-nav-icon">👥</span>
            {!collapsed && <span>Пользователи</span>}
          </NavLink>
          <NavLink to="/admin/credits" className="admin-nav-link">
            <span className="admin-nav-icon">💳</span>
            {!collapsed && <span>Кредиты</span>}
          </NavLink>
        </nav>

        <button
          className="admin-collapse-btn"
          onClick={() => setCollapsed((c) => !c)}
          title={collapsed ? 'Развернуть' : 'Свернуть'}
        >
          {collapsed ? '›' : '‹'}
        </button>
      </aside>

      <div className="admin-main">
        <header className="admin-topbar">
          <div className="admin-topbar-left">
            <h1 className="admin-topbar-title">Панель администратора</h1>
          </div>
          <div className="admin-topbar-right">
            <ThemeToggle variant="icon" />
            <div className="admin-user-chip">
              <div className="admin-avatar">{initials}</div>
              <div className="admin-user-meta">
                <div className="admin-user-name">{user?.sub || 'Администратор'}</div>
                <div className="admin-user-role">
                  {(user?.roles || []).join(', ') || 'admin'}
                </div>
              </div>
            </div>
            <button className="admin-logout-btn" onClick={handleLogout}>
              Выйти
            </button>
          </div>
        </header>

        <main className="admin-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
