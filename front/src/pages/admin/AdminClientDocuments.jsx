import React, { useEffect, useState, useCallback } from 'react';
import client from '../../api/client';
import { adminApi, formatDate, extractError } from './shared';

/**
 * Просмотр пользовательских документов клиента из административной панели.
 * Запрашивает список через /admin/clients/{clientId}/documents и позволяет
 * открыть/скачать каждый файл через /admin/documents/{id}/content.
 */

const TYPE_LABELS = {
  NDFL_2: 'Справка 2-НДФЛ',
  EMPLOYMENT_RECORD: 'Выписка из ЭТК',
};

function formatSize(bytes) {
  if (bytes == null) return '';
  if (bytes < 1024) return `${bytes} Б`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} КБ`;
  return `${(bytes / 1024 / 1024).toFixed(2)} МБ`;
}

export default function AdminClientDocuments({ clientId }) {
  const [docs, setDocs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    if (!clientId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await client.get(adminApi(`/admin/clients/${clientId}/documents`));
      setDocs(Array.isArray(res.data) ? res.data : []);
    } catch (e) {
      setError(extractError(e));
    } finally {
      setLoading(false);
    }
  }, [clientId]);

  useEffect(() => { load(); }, [load]);

  const handleView = async (doc) => {
    try {
      const res = await client.get(
        adminApi(`/admin/documents/${doc.documentId}/content`),
        { responseType: 'blob' },
      );
      const url = window.URL.createObjectURL(
        new Blob([res.data], { type: 'application/pdf' }),
      );
      window.open(url, '_blank', 'noopener');
      setTimeout(() => window.URL.revokeObjectURL(url), 60 * 1000);
    } catch (e) {
      setError(extractError(e));
    }
  };

  const handleDownload = async (doc) => {
    try {
      const res = await client.get(
        adminApi(`/admin/documents/${doc.documentId}/content?download=true`),
        { responseType: 'blob' },
      );
      const url = window.URL.createObjectURL(
        new Blob([res.data], { type: 'application/pdf' }),
      );
      const link = document.createElement('a');
      link.href = url;
      link.download = doc.originalFileName || 'document.pdf';
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      setError(extractError(e));
    }
  };

  if (loading) return <div className="admin-loading">Загрузка документов...</div>;
  if (error)   return <div className="admin-error">{error}</div>;

  if (docs.length === 0) {
    return (
      <div style={{ fontSize: '0.88rem', color: '#6b7280', fontStyle: 'italic' }}>
        Клиент не загрузил ни одного документа.
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
      {docs.map((doc) => (
        <div
          key={doc.documentId}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '0.75rem',
            padding: '0.6rem 0.85rem',
            border: '1px solid #e5e7eb',
            borderRadius: 8,
            background: '#f9fafb',
          }}
        >
          <div style={{ minWidth: 0, flex: 1 }}>
            <div style={{ fontWeight: 600, fontSize: '0.9rem', color: '#0f172a' }}>
              {TYPE_LABELS[doc.documentType] || doc.documentType}
            </div>
            <div
              style={{
                fontSize: '0.82rem',
                color: '#475569',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
              title={doc.originalFileName}
            >
              {doc.originalFileName}
            </div>
            <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
              {formatSize(doc.fileSize)} • {formatDate(doc.uploadedAt)}
            </div>
          </div>
          <div style={{ display: 'flex', gap: '0.35rem', flexShrink: 0 }}>
            <button className="admin-btn" onClick={() => handleView(doc)}>Открыть</button>
            <button className="admin-btn" onClick={() => handleDownload(doc)}>Скачать</button>
          </div>
        </div>
      ))}
    </div>
  );
}
