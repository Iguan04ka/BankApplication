import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { Navigate, useLocation } from 'react-router-dom';

const AuthContext = createContext(null);

const ROLES_KEY = 'roles';
const SUB_KEY = 'sub';

const isDev = () => process.env.NODE_ENV === 'development';
const apiPrefix = () => (isDev() ? '' : '/api');

function readRoles() {
  try {
    const raw = localStorage.getItem(ROLES_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function writeRoles(roles) {
  if (!roles) {
    localStorage.removeItem(ROLES_KEY);
    return;
  }
  localStorage.setItem(ROLES_KEY, JSON.stringify(roles));
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Hydrates roles + sub from /auth/me using the current access token.
  // Resolves with the loaded user, or null on failure.
  const hydrateMe = useCallback(async (accessToken) => {
    try {
      const url = `${apiPrefix()}/auth/me`;
      const resp = await axios.get(url, {
        headers: { Authorization: `Bearer ${accessToken}` },
      });
      if (resp.status === 200 && resp.data) {
        const roles = Array.isArray(resp.data.roles) ? resp.data.roles : [];
        const sub = resp.data.sub || null;
        writeRoles(roles);
        if (sub) localStorage.setItem(SUB_KEY, sub); else localStorage.removeItem(SUB_KEY);
        const u = { accessToken, sub, roles };
        setUser(u);
        return u;
      }
    } catch {
      // ignore
    }
    return null;
  }, []);

  useEffect(() => {
    const access = localStorage.getItem('accessToken');
    const refresh = localStorage.getItem('refreshToken');

    const storedRoles = readRoles();
    const storedSub = localStorage.getItem(SUB_KEY);

    if (access) {
      // Set immediately from cache so first render isn't blank, then refresh from /auth/me.
      setUser({ accessToken: access, sub: storedSub, roles: storedRoles });
      hydrateMe(access).finally(() => setLoading(false));
      return;
    }

    if (refresh) {
      (async () => {
        try {
          const url = `${apiPrefix()}/auth/refresh`;
          const resp = await axios.post(url, { refreshToken: refresh }, {
            headers: { 'Content-Type': 'application/json' },
          });
          if (resp.status === 200 && resp.data?.accessToken) {
            localStorage.setItem('accessToken', resp.data.accessToken);
            if (resp.data.refreshToken) {
              localStorage.setItem('refreshToken', resp.data.refreshToken);
            }
            await hydrateMe(resp.data.accessToken);
          }
        } catch {
          // refresh failed — stay logged out
        } finally {
          setLoading(false);
        }
      })();
      return;
    }
    setLoading(false);
  }, [hydrateMe]);

  // Called by Login on success. roles is a string[] returned by /auth/login.
  const login = ({ accessToken, refreshToken, roles, sub }) => {
    localStorage.setItem('accessToken', accessToken);
    if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
    const safeRoles = Array.isArray(roles) ? roles : [];
    writeRoles(safeRoles);
    if (sub) localStorage.setItem(SUB_KEY, sub);
    setUser({ accessToken, sub: sub || null, roles: safeRoles });
  };

  const logout = async () => {
    const refresh = localStorage.getItem('refreshToken');
    // Best-effort server-side logout.
    if (refresh) {
      try {
        await axios.post(`${apiPrefix()}/auth/logout`, { refreshToken: refresh }, {
          headers: { 'Content-Type': 'application/json' },
        });
      } catch {
        // ignore — we still clear local state
      }
    }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem(ROLES_KEY);
    localStorage.removeItem(SUB_KEY);
    setUser(null);
  };

  const isAdmin = !!user && Array.isArray(user.roles) &&
    user.roles.some((r) => String(r).toLowerCase() === 'admin');

  return (
    <AuthContext.Provider value={{ user, login, logout, loading, isAdmin }}>
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

export function RequireAdmin({ children }) {
  const { user, loading, isAdmin } = useAuth();
  const location = useLocation();
  if (loading) return <div>Loading...</div>;
  if (!user) {
    return <Navigate to="/auth/login" replace state={{ from: location }} />;
  }
  if (!isAdmin) {
    // logged in but not admin → bounce to user account
    return <Navigate to="/account" replace />;
  }
  return children;
}
