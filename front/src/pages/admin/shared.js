// Helpers shared across admin pages.

export const adminApi = (path) => {
  const isDev = process.env.NODE_ENV === 'development';
  return isDev ? path : `/api${path}`;
};

export function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '—';
  const n = typeof value === 'number' ? value : Number(value);
  if (Number.isNaN(n)) return String(value);
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 2,
  }).format(n);
}

export function formatDate(value) {
  if (!value) return '—';
  try {
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleString('ru-RU', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return String(value);
  }
}

export function statusColor(status) {
  if (!status) return 'neutral';
  const s = String(status).toUpperCase();
  if (s.includes('DENIED')) return 'danger';
  if (s.includes('ISSUED') || s.includes('SIGNED') || s.includes('CC_APPROVED')) return 'success';
  if (s.includes('APPROVED') || s.includes('PREAPPROVAL')) return 'info';
  if (s.includes('DOCUMENT') || s.includes('PREPARE')) return 'warning';
  return 'neutral';
}

export const APPLICATION_STATUSES = [
  'PREAPPROVAL',
  'APPROVED',
  'CC_DENIED',
  'CC_APPROVED',
  'PREPARE_DOCUMENTS',
  'DOCUMENT_CREATED',
  'CLIENT_DENIED',
  'DOCUMENT_SIGNED',
  'CREDIT_ISSUED',
];

export function extractError(e) {
  const data = e?.response?.data;
  if (data && typeof data === 'object') return data.error || data.message || JSON.stringify(data);
  if (typeof data === 'string') return data;
  return e?.message || 'Произошла ошибка';
}
