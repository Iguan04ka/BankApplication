import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../../auth/AuthProvider';
import './Header.css';

export default function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/');
    setMenuOpen(false);
  };

  const isActive = (path) =>
    location.pathname === path || location.pathname.startsWith(path + '/');

  return (
    <nav className="header-nav">
      <div className="header-inner">
        <Link className="header-brand" to="/" onClick={() => setMenuOpen(false)}>
          <span className="brand-icon">🏦</span>
          <span className="brand-text">АтласКредит</span>
        </Link>

        <button
          className={`header-burger${menuOpen ? ' header-burger--open' : ''}`}
          type="button"
          aria-label="Меню"
          onClick={() => setMenuOpen((v) => !v)}
        >
          <span /><span /><span />
        </button>

        <ul className={`header-nav-list${menuOpen ? ' header-nav-list--open' : ''}`}>
          {user ? (
            <>
              <li>
                <Link
                  className={`header-nav-pill header-nav-pill--outline${isActive('/account') ? ' is-active' : ''}`}
                  to="/account"
                  onClick={() => setMenuOpen(false)}
                >
                  <span className="pill-icon">👤</span>
                  Личный кабинет
                </Link>
              </li>
              <li>
                <Link
                  className={`header-nav-pill header-nav-pill--outline${isActive('/statement') ? ' is-active' : ''}`}
                  to="/statement"
                  onClick={() => setMenuOpen(false)}
                >
                  <span className="pill-icon">📋</span>
                  Заявка
                </Link>
              </li>
              <li>
                <button
                  className="header-nav-pill header-nav-pill--danger"
                  onClick={handleLogout}
                >
                  <span className="pill-icon">🚪</span>
                  Выйти
                </button>
              </li>
            </>
          ) : (
            <>
              <li>
                <Link
                  className={`header-nav-pill header-nav-pill--outline${isActive('/auth/login') ? ' is-active' : ''}`}
                  to="/auth/login"
                  onClick={() => setMenuOpen(false)}
                >
                  Войти
                </Link>
              </li>
              <li>
                <Link
                  className="header-nav-pill header-nav-pill--filled"
                  to="/auth/register"
                  onClick={() => setMenuOpen(false)}
                >
                  Регистрация
                </Link>
              </li>
            </>
          )}
        </ul>
      </div>
    </nav>
  );
}
