import React, { useRef, useState, useEffect, useCallback } from 'react';
import client from '../../../api/client';
import './DocumentUploader.css';

/**
 * Универсальный компонент загрузки PDF-документов (2-НДФЛ и выписки из ЭТК).
 * Использует /deal/client/me/documents — работает только для аутентифицированных
 * пользователей. Передаваемые типы должны совпадать с enum UserDocumentType
 * на backend: NDFL_2, EMPLOYMENT_RECORD.
 *
 * Props:
 *   slots:           массив { key, label, hint }
 *   onChange(docs):  опциональный callback, вызываемый после загрузки списка и
 *                    после каждой операции. Передаёт текущий массив документов.
 *   compact:         true — без верхнего заголовка секции (для встраивания в форму)
 *   requiredKeys:    массив ключей слотов, обязательных к заполнению. Если
 *                    переданы — пустой слот подсвечивается как ошибочный после
 *                    флага showRequiredErrors=true.
 *   showRequiredErrors: показывать ли подсветку обязательных пустых слотов
 */

const getApiBase = () => (process.env.NODE_ENV === 'development' ? '' : '/api');

const DEFAULT_SLOTS = [
  {
    key: 'NDFL_2',
    label: 'Справка 2-НДФЛ',
    hint: 'Подтверждение дохода в формате PDF',
  },
  {
    key: 'EMPLOYMENT_RECORD',
    label: 'Выписка из электронной трудовой книжки',
    hint: 'СТД-Р или СТД-ПФР в формате PDF',
  },
];

const MAX_SIZE_MB = 10;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;

function formatSize(bytes) {
  if (bytes == null) return '';
  if (bytes < 1024) return `${bytes} Б`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} КБ`;
  return `${(bytes / 1024 / 1024).toFixed(2)} МБ`;
}

function formatDate(v) {
  if (!v) return '';
  try {
    return new Date(v).toLocaleString('ru-RU');
  } catch {
    return String(v);
  }
}

function extractError(err) {
  const data = err?.response?.data;
  if (!data) return err?.message || 'Ошибка операции';
  if (typeof data === 'string') return data;
  return data.message || JSON.stringify(data);
}

export default function DocumentUploader({
  slots = DEFAULT_SLOTS,
  onChange,
  compact = false,
  horizontal = false,
  requiredKeys = [],
  showRequiredErrors = false,
}) {
  const base = getApiBase();
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busySlot, setBusySlot] = useState(null);
  const [error, setError] = useState(null);
  const [info, setInfo] = useState(null);
  const fileInputs = useRef({});

  const notifyRef = useRef(onChange);
  useEffect(() => { notifyRef.current = onChange; }, [onChange]);

  const fetchDocuments = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await client.get(`${base}/deal/client/me/documents`);
      const list = Array.isArray(res.data) ? res.data : [];
      setDocuments(list);
      if (typeof notifyRef.current === 'function') notifyRef.current(list);
    } catch (e) {
      setError(extractError(e));
    } finally {
      setLoading(false);
    }
  }, [base]);

  useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  const notify = useCallback((listOverride) => {
    if (typeof notifyRef.current === 'function') {
      notifyRef.current(listOverride !== undefined ? listOverride : documents);
    }
  }, [documents]);

  const findDoc = (slotKey) =>
    documents.find((d) => d.documentType === slotKey);

  const validateFile = (file) => {
    if (!file) return 'Файл не выбран';
    if (file.type && file.type !== 'application/pdf') {
      return 'Допустимы только PDF-файлы';
    }
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      return 'Файл должен иметь расширение .pdf';
    }
    if (file.size > MAX_SIZE_BYTES) {
      return `Максимальный размер файла — ${MAX_SIZE_MB} МБ`;
    }
    return null;
  };

  const handleFile = async (slotKey, file) => {
    setError(null);
    setInfo(null);

    const validation = validateFile(file);
    if (validation) {
      setError(validation);
      return;
    }

    const existing = findDoc(slotKey);
    const formData = new FormData();
    formData.append('file', file);

    setBusySlot(slotKey);
    try {
      if (existing) {
        // Замена существующего документа
        await client.put(
          `${base}/deal/client/me/documents/${existing.documentId}`,
          formData,
          { headers: { 'Content-Type': 'multipart/form-data' } },
        );
        setInfo(`Документ заменён: ${file.name}`);
      } else {
        // Новая загрузка
        formData.append('documentType', slotKey);
        await client.post(
          `${base}/deal/client/me/documents`,
          formData,
          { headers: { 'Content-Type': 'multipart/form-data' } },
        );
        setInfo(`Документ загружен: ${file.name}`);
      }
      await fetchDocuments();
    } catch (e) {
      setError(extractError(e));
    } finally {
      setBusySlot(null);
      // сбрасываем input, чтобы можно было выбрать тот же файл повторно
      if (fileInputs.current[slotKey]) {
        fileInputs.current[slotKey].value = '';
      }
    }
  };

  const handleDelete = async (doc) => {
    if (!doc) return;
    if (!window.confirm(`Удалить документ «${doc.originalFileName}»?`)) return;
    setBusySlot(doc.documentType);
    setError(null);
    setInfo(null);
    try {
      await client.delete(`${base}/deal/client/me/documents/${doc.documentId}`);
      setInfo('Документ удалён');
      await fetchDocuments();
    } catch (e) {
      setError(extractError(e));
    } finally {
      setBusySlot(null);
    }
  };

  const handleView = async (doc) => {
    if (!doc) return;
    try {
      const res = await client.get(
        `${base}/deal/client/me/documents/${doc.documentId}/content`,
        { responseType: 'blob' },
      );
      const url = window.URL.createObjectURL(
        new Blob([res.data], { type: 'application/pdf' }),
      );
      window.open(url, '_blank', 'noopener');
      // Освобождаем объект URL через минуту — за это время браузер успеет открыть PDF
      setTimeout(() => window.URL.revokeObjectURL(url), 60 * 1000);
    } catch (e) {
      setError(extractError(e));
    }
  };

  const handleDownload = async (doc) => {
    if (!doc) return;
    try {
      const res = await client.get(
        `${base}/deal/client/me/documents/${doc.documentId}/content?download=true`,
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

  return (
    <div className={[
      'doc-uploader',
      compact ? 'doc-uploader--compact' : '',
      horizontal ? 'doc-uploader--horizontal' : '',
    ].filter(Boolean).join(' ')}>
      {!compact && (
        <div className="doc-uploader-header">
          <h3 className="doc-uploader-title">📄 Документы</h3>
          <p className="doc-uploader-subtitle">
            Загрузите PDF-файлы. Максимальный размер каждого файла — {MAX_SIZE_MB} МБ.
          </p>
        </div>
      )}

      {error && <div className="alert alert-danger doc-uploader-alert">{error}</div>}
      {info && <div className="alert alert-success doc-uploader-alert">{info}</div>}

      {loading ? (
        <div className="doc-uploader-loading">
          <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true" />
          <span>Загрузка списка...</span>
        </div>
      ) : (
        <div className={`doc-uploader-slots${horizontal ? ' doc-uploader-slots--row' : ''}`}>
          {slots.map((slot) => {
            const doc = findDoc(slot.key);
            const busy = busySlot === slot.key;
            const isRequired = requiredKeys.includes(slot.key);
            const missingRequired = isRequired && !doc && showRequiredErrors;
            return (
              <div
                key={slot.key}
                className={
                  `doc-slot${doc ? ' doc-slot--filled' : ''}` +
                  `${missingRequired ? ' doc-slot--error' : ''}`
                }
              >
                <div className="doc-slot-info">
                  <div className="doc-slot-label">
                    {slot.label}
                    {isRequired && <span className="doc-slot-required" aria-hidden="true"> *</span>}
                  </div>
                  <div className="doc-slot-hint">{slot.hint}</div>
                  {missingRequired && (
                    <div className="doc-slot-error-text">Документ обязателен для отправки заявки</div>
                  )}
                  {doc ? (
                    <div className="doc-slot-file">
                      <span className="doc-slot-file-icon" aria-hidden="true">📄</span>
                      <div className="doc-slot-file-meta">
                        <div className="doc-slot-file-name" title={doc.originalFileName}>
                          {doc.originalFileName}
                        </div>
                        <div className="doc-slot-file-sub">
                          {formatSize(doc.fileSize)} • загружен {formatDate(doc.uploadedAt)}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="doc-slot-empty">Файл не загружен</div>
                  )}
                </div>

                <div className="doc-slot-actions">
                  <input
                    ref={(el) => (fileInputs.current[slot.key] = el)}
                    type="file"
                    accept="application/pdf,.pdf"
                    style={{ display: 'none' }}
                    onChange={(e) => {
                      const f = e.target.files?.[0];
                      if (f) handleFile(slot.key, f);
                    }}
                  />
                  {doc ? (
                    <>
                      <button
                        type="button"
                        className="btn btn-outline btn-sm"
                        onClick={() => handleView(doc)}
                        disabled={busy}
                      >
                        Открыть
                      </button>
                      <button
                        type="button"
                        className="btn btn-outline btn-sm"
                        onClick={() => handleDownload(doc)}
                        disabled={busy}
                      >
                        Скачать
                      </button>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => fileInputs.current[slot.key]?.click()}
                        disabled={busy}
                      >
                        {busy ? 'Загрузка…' : 'Заменить'}
                      </button>
                      <button
                        type="button"
                        className="btn btn-danger btn-sm"
                        onClick={() => handleDelete(doc)}
                        disabled={busy}
                      >
                        Удалить
                      </button>
                    </>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => fileInputs.current[slot.key]?.click()}
                      disabled={busy}
                    >
                      {busy ? 'Загрузка…' : 'Загрузить PDF'}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
