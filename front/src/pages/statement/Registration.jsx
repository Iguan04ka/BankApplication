import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import client from '../../api/client';
import './Registration.css';

const getApiBase = () => (process.env.NODE_ENV === 'development' ? '' : '/api');

// ── Enum options from calculator microservice ────────────────────────────────

const GENDER_OPTIONS = [
  { value: 'MALE', label: 'Мужской' },
  { value: 'FEMALE', label: 'Женский' },
  { value: 'NONBINARY', label: 'Другой' },
];

const MARITAL_OPTIONS = [
  { value: 'MARRIED', label: 'Женат/Замужем' },
  { value: 'DIVORCED', label: 'Разведён(а)' },
];

const EMP_STATUS_OPTIONS = [
  { value: 'UNEMPLOYED', label: 'Безработный' },
  { value: 'SELFEMPLOYED', label: 'Самозанятый' },
  { value: 'HIREDEMPLOYED', label: 'Наёмный работник' },
];

const POSITION_OPTIONS = [
  { value: 'JUNIOR', label: 'Младший специалист' },
  { value: 'MIDDLE', label: 'Специалист' },
  { value: 'SENIOR', label: 'Старший специалист' },
  { value: 'BOSS', label: 'Руководитель' },
];

// Map profile gender (deal) to calculator gender
function mapGender(g) {
  if (g === 'NON_BINARY') return 'NONBINARY';
  if (g === 'MALE' || g === 'FEMALE') return g;
  return '';
}

// Map profile maritalStatus (deal) to calculator marital status
function mapMarital(m) {
  if (m === 'MARRIED' || m === 'DIVORCED') return m;
  return '';
}

// Map profile employment status (deal) to calculator EmploymentStatus enum
function mapEmploymentStatus(s) {
  const map = {
    UNEMPLOYED:    'UNEMPLOYED',
    SELF_EMPLOYED: 'SELFEMPLOYED',
    EMPLOYED:      'HIREDEMPLOYED',
    // BUSINESS_OWNER has no calculator equivalent — leave blank
  };
  return map[s] || '';
}

// Map profile position (deal) to calculator Positions enum
function mapPosition(p) {
  const map = {
    WORKER:      'JUNIOR',
    MID_MANAGER: 'MIDDLE',
    TOP_MANAGER: 'SENIOR',
    OWNER:       'BOSS',
  };
  return map[p] || '';
}

const emptyForm = {
  gender: '',
  maritalStatus: '',
  dependentAmount: '',
  passportIssueDate: '',
  passportIssueBranch: '',
  accountNumber: '',
  employment: {
    employmentStatus: '',
    employerINN: '',
    salary: '',
    position: '',
    workExperienceTotal: '',
    workExperienceCurrent: '',
  },
};

function SectionCard({ title, icon, children }) {
  return (
    <div className="reg-section-card">
      <h3 className="reg-section-title">
        <span className="reg-section-icon">{icon}</span>
        {title}
      </h3>
      {children}
    </div>
  );
}

function Field({ label, children, required }) {
  return (
    <div className="reg-field">
      <label className="reg-label">
        {label}
        {required && <span className="reg-required"> *</span>}
      </label>
      {children}
    </div>
  );
}

export default function Registration() {
  const { statementId } = useParams();
  const navigate = useNavigate();
  const base = getApiBase();

  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  // Pre-fill from profile
  useEffect(() => {
    (async () => {
      try {
        const res = await client.get(`${base}/deal/client/me`);
        const p = res.data;
        setForm((prev) => ({
          ...prev,
          gender: mapGender(p.gender),
          maritalStatus: mapMarital(p.maritalStatus),
          dependentAmount: p.dependentAmount ?? '',
          passportIssueDate: p.passport?.issueDate
            ? new Date(p.passport.issueDate).toISOString().slice(0, 10)
            : '',
          passportIssueBranch: p.passport?.issueBranch || '',
          accountNumber: p.accountNumber || '',
          employment: {
            ...prev.employment,
            employmentStatus: mapEmploymentStatus(p.employment?.status),
            employerINN: p.employment?.employer_inn || '',
            salary: p.employment?.salary ?? '',
            position: mapPosition(p.employment?.position),
            workExperienceTotal: p.employment?.workExperienceTotal ?? '',
            workExperienceCurrent: p.employment?.workExperienceCurrent ?? '',
          },
        }));
      } catch {
        // Profile unavailable
      }
    })();
  }, [base]);

  const setField = (name, value) =>
    setForm((f) => ({ ...f, [name]: value }));

  const setEmployment = (name, value) =>
    setForm((f) => ({ ...f, employment: { ...f.employment, [name]: value } }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const payload = {
        gender: form.gender,
        maritalStatus: form.maritalStatus,
        dependentAmount:
          form.dependentAmount !== '' ? parseInt(form.dependentAmount, 10) : null,
        passportIssueDate: form.passportIssueDate || null,
        passportIssueBranch: form.passportIssueBranch,
        accountNumber: form.accountNumber,
        employment: {
          employmentStatus: form.employment.employmentStatus,
          employerINN: form.employment.employerINN,
          salary:
            form.employment.salary !== ''
              ? parseFloat(form.employment.salary)
              : null,
          position: form.employment.position,
          workExperienceTotal:
            form.employment.workExperienceTotal !== ''
              ? parseInt(form.employment.workExperienceTotal, 10)
              : null,
          workExperienceCurrent:
            form.employment.workExperienceCurrent !== ''
              ? parseInt(form.employment.workExperienceCurrent, 10)
              : null,
        },
      };
      await client.post(`${base}/statement/registration/${statementId}`, payload);
      setSuccess(true);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          err.message ||
          'Ошибка при отправке данных',
      );
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="reg-page">
        <div className="reg-content">
          <div className="reg-success">
            <div className="reg-success-icon">✅</div>
            <h2 className="reg-success-title">Заявка успешно подана!</h2>
            <p className="reg-success-text">
              Ваши данные переданы на рассмотрение. Вы можете следить за
              статусом заявки в личном кабинете.
            </p>
            <div className="reg-success-actions">
              <button
                className="btn btn-primary"
                onClick={() => navigate('/account')}
              >
                Перейти в личный кабинет
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="reg-page">
      <div className="reg-content">
        <div className="reg-header">
          <h2 className="reg-title">📝 Завершение оформления</h2>
          <p className="reg-subtitle">
            Заполните оставшиеся данные для завершения подачи кредитной заявки
          </p>
          <div className="reg-id">ID заявки: {statementId}</div>
        </div>

        <form onSubmit={handleSubmit}>
          {error && (
            <div className="alert alert-danger" role="alert">
              {String(error)}
            </div>
          )}

          <div className="reg-grid">
            {/* Personal data */}
            <SectionCard title="Личные данные" icon="👤">
              <Field label="Пол" required>
                <select
                  className="reg-control"
                  value={form.gender}
                  onChange={(e) => setField('gender', e.target.value)}
                  required
                >
                  <option value="">Выберите...</option>
                  {GENDER_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </Field>

              <Field label="Семейное положение" required>
                <select
                  className="reg-control"
                  value={form.maritalStatus}
                  onChange={(e) => setField('maritalStatus', e.target.value)}
                  required
                >
                  <option value="">Выберите...</option>
                  {MARITAL_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </Field>

              <Field label="Кол-во иждивенцев" required>
                <input
                  className="reg-control"
                  type="number"
                  min="0"
                  value={form.dependentAmount}
                  onChange={(e) => setField('dependentAmount', e.target.value)}
                  placeholder="0"
                  required
                />
              </Field>

              <Field label="Номер счёта" required>
                <input
                  className="reg-control"
                  type="text"
                  value={form.accountNumber}
                  onChange={(e) => setField('accountNumber', e.target.value)}
                  placeholder="12345678901234567890"
                  required
                />
              </Field>
            </SectionCard>

            {/* Passport data */}
            <SectionCard title="Паспортные данные" icon="🪪">
              <Field label="Дата выдачи паспорта" required>
                <input
                  className="reg-control"
                  type="date"
                  value={form.passportIssueDate}
                  onChange={(e) => setField('passportIssueDate', e.target.value)}
                  required
                />
              </Field>

              <Field label="Отдел выдачи" required>
                <input
                  className="reg-control"
                  type="text"
                  value={form.passportIssueBranch}
                  onChange={(e) =>
                    setField('passportIssueBranch', e.target.value)
                  }
                  placeholder="Отдел МВД №123"
                  required
                />
              </Field>
            </SectionCard>

            {/* Employment data */}
            <SectionCard title="Трудоустройство" icon="💼">
              <Field label="Статус занятости" required>
                <select
                  className="reg-control"
                  value={form.employment.employmentStatus}
                  onChange={(e) =>
                    setEmployment('employmentStatus', e.target.value)
                  }
                  required
                >
                  <option value="">Выберите...</option>
                  {EMP_STATUS_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </Field>

              <Field label="ИНН работодателя" required>
                <input
                  className="reg-control"
                  type="text"
                  minLength={10}
                  maxLength={12}
                  pattern="[0-9]{10,12}"
                  value={form.employment.employerINN}
                  onChange={(e) => setEmployment('employerINN', e.target.value)}
                  placeholder="1234567890"
                  required
                />
              </Field>

              <Field label="Зарплата (руб.)" required>
                <input
                  className="reg-control"
                  type="number"
                  min="0"
                  step="1000"
                  value={form.employment.salary}
                  onChange={(e) => setEmployment('salary', e.target.value)}
                  placeholder="50000"
                  required
                />
              </Field>

              <Field label="Должность" required>
                <select
                  className="reg-control"
                  value={form.employment.position}
                  onChange={(e) => setEmployment('position', e.target.value)}
                  required
                >
                  <option value="">Выберите...</option>
                  {POSITION_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              </Field>

              <Field label="Общий стаж (мес.)" required>
                <input
                  className="reg-control"
                  type="number"
                  min="0"
                  value={form.employment.workExperienceTotal}
                  onChange={(e) =>
                    setEmployment('workExperienceTotal', e.target.value)
                  }
                  placeholder="36"
                  required
                />
              </Field>

              <Field label="Текущий стаж (мес.)" required>
                <input
                  className="reg-control"
                  type="number"
                  min="0"
                  value={form.employment.workExperienceCurrent}
                  onChange={(e) =>
                    setEmployment('workExperienceCurrent', e.target.value)
                  }
                  placeholder="12"
                  required
                />
              </Field>
            </SectionCard>
          </div>

          <div className="reg-actions">
            <button type="submit" className="btn btn-primary btn-lg" disabled={loading}>
              {loading ? (
                <>
                  <span
                    className="spinner-border spinner-border-sm me-2"
                    role="status"
                    aria-hidden="true"
                  />
                  Отправка...
                </>
              ) : (
                'Отправить заявку'
              )}
            </button>
            <button
              type="button"
              className="btn btn-back"
              onClick={() => navigate('/statement')}
              disabled={loading}
            >
              ← Назад к заявке
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
