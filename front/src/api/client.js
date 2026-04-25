import axios from 'axios';

// Simple axios client with refresh token handling.
// Assumptions:
// - Backend exposes POST /auth/refresh which accepts { refreshToken } and returns { accessToken, refreshToken }
// - Tokens are stored in localStorage under keys 'accessToken' and 'refreshToken'

const API_BASE = ''; // use relative paths by default; when in docker nginx proxies / to gateway

const client = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let subscribers = [];

function onRefreshed(token) {
  subscribers.forEach((cb) => cb(token));
  subscribers = [];
}

function addSubscriber(cb) {
  subscribers.push(cb);
}

async function refreshToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) throw new Error('No refresh token');

  // Try to refresh tokens (use same environment detection as other modules)
  const isDev = process.env.NODE_ENV === 'development';
  const url = isDev ? '/auth/refresh' : '/api/auth/refresh';

  const resp = await axios.post(url, { refreshToken }, { headers: { 'Content-Type': 'application/json' } });
  if (resp.status === 200 && resp.data && resp.data.accessToken) {
    const { accessToken, refreshToken: newRefresh } = resp.data;
    // always update access token
    localStorage.setItem('accessToken', accessToken);
    // update refresh token only if backend returned a new one
    if (newRefresh) {
      localStorage.setItem('refreshToken', newRefresh);
    }
    return accessToken;
  }
  throw new Error('Failed to refresh token');
}

// Request interceptor to add Authorization header when accessToken exists
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => Promise.reject(error));

// Response interceptor to handle 401 and try refresh once
client.interceptors.response.use((res) => res, async (error) => {
  const { config, response } = error;
  if (!config) return Promise.reject(error);

  // If response status is 401, try refresh
  if (response && response.status === 401 && !config._retry) {
    if (isRefreshing) {
      // queue the request
      return new Promise((resolve, reject) => {
        addSubscriber((token) => {
          if (token) {
            config.headers = config.headers || {};
            config.headers.Authorization = `Bearer ${token}`;
            resolve(client(config));
          } else {
            reject(error);
          }
        });
      });
    }

    config._retry = true;
    isRefreshing = true;
    try {
      const newToken = await refreshToken();
      onRefreshed(newToken);
      isRefreshing = false;
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${newToken}`;
      return client(config);
    } catch (e) {
      isRefreshing = false;
      onRefreshed(null);
      // Clear tokens and redirect to login
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      if (typeof window !== 'undefined') {
        window.location.href = '/auth/login';
      }
      return Promise.reject(e);
    }
  }

  return Promise.reject(error);
});

export default client;

