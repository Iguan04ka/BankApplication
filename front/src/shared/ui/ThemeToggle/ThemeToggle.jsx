import React from 'react';
import { useTheme } from '../../../context/ThemeContext';
import './ThemeToggle.css';

/**
 * Theme toggle button.
 * variant="pill"   – fits into the public Header nav
 * variant="icon"   – compact icon-only button for the admin topbar
 */
export default function ThemeToggle({ variant = 'pill' }) {
  const { isDark, toggleTheme } = useTheme();

  if (variant === 'icon') {
    return (
      <button
        className="theme-toggle-icon"
        onClick={toggleTheme}
        title={isDark ? 'Включить светлую тему' : 'Включить тёмную тему'}
        aria-label={isDark ? 'Светлая тема' : 'Тёмная тема'}
      >
        {isDark ? '☀️' : '🌙'}
      </button>
    );
  }

  return (
    <button
      className="theme-toggle-pill"
      onClick={toggleTheme}
      title={isDark ? 'Включить светлую тему' : 'Включить тёмную тему'}
    >
      <span className="theme-toggle-icon-inner">{isDark ? '☀️' : '🌙'}</span>
      <span className="theme-toggle-label">{isDark ? 'Светлая' : 'Тёмная'}</span>
    </button>
  );
}
