import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import { Navigate } from 'react-router-dom';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // On app start, try to read tokens
    const access = localStorage.getItem('accessToken');
    const refresh = localStorage.getItem('refreshToken');
    if (access) {
      setUser({ accessToken: access });
      setLoading(false);
      return;
    }
    if (refresh) {
      // try to refresh immediately to get access token
      (async () => {
        try {
          const isDev = process.env.NODE_ENV === 'development';
          const url = isDev ? '/auth/refresh' : '/api/auth/refresh';
          const resp = await axios.post(url, { refreshToken: refresh }, { headers: { 'Content-Type': 'application/json' } });
          if (resp.status === 200 && resp.data && resp.data.accessToken) {
            localStorage.setItem('accessToken', resp.data.accessToken);
            // only update refresh token if backend returned a new one
            if (resp.data.refreshToken) {
              localStorage.setItem('refreshToken', resp.data.refreshToken);
            }
            setUser({ accessToken: resp.data.accessToken });
          }
        } catch (e) {
          // ignore
        } finally {
          setLoading(false);
        }
      })();
      return; // don't set loading false here because handled in async fn
    }
    setLoading(false);
  }, []);

  const login = ({ accessToken, refreshToken }) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    setUser({ accessToken });
  };

  const logout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}

export function RequireAuth({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <div>Loading...</div>;
  if (!user) {
    return <Navigate to="/auth/login" replace />;
  }
  return children;
}

