import React, { createContext, useContext, useState, useEffect } from 'react';

const ThemeContext = createContext(null);

const STORAGE_KEY = 'theme';
const DARK = 'dark';
const LIGHT = 'light';

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) === DARK ? DARK : LIGHT;
    } catch {
      return LIGHT;
    }
  });

  // Keep <html data-theme="..."> in sync
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch { /* ignore quota errors */ }
  }, [theme]);

  const toggleTheme = () =>
    setTheme((t) => (t === DARK ? LIGHT : DARK));

  const isDark = theme === DARK;

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, isDark }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}
