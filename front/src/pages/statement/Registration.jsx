import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import client from '../../api/client';
import DocumentUploader from '../../shared/ui/DocumentUploader/DocumentUploader';
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

const INN_RE = /^\d{10,12}$/;
const ACCOUNT_RE = /^\d{20}$/;

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

// Map profile employment status to calculator EmploymentStatus enum.
// Поддерживаются оба формата: Account/deal-формат (EMPLOYED, SELF_EMPLOYED…)
// и калькуляторный (HIREDEMPLOYED, SELFEMPLOYED…) — на случай, если данные
// уже были сохранены после предыдущего прохождения регистрации.
function mapEmploymentStatus(s) {
  const map = {
    // Deal / Account формат → calculator формат
    UNEMPLOYED:     'UNEMPLOYED',
    SELF_EMPLOYED:  'SELFEMPLOYED',
    EMPLOYED:       'HIREDEMPLOYED',
    BUSINESS_OWNER: 'SELFEMPLOYED',
    // Уже в calculator-формате (identity, сохранено после регистрации)
    SELFEMPLOYED:   'SELFEMPLOYED',
    HIREDEMPLOYED:  'HIREDEMPLOYED',
  };
  return map[s] || '';
}

// Map profile position to calculator Positions enum.
function mapPosition(p) {
  const map = {
    // Deal / Account формат → calculator формат
    WORKER:      'JUNIOR',
    MID_MANAGER: 'MIDDLE',
    TOP_MANAGER: 'SENIOR',
    OWNER:       'BOSS',
    // Уже в calculator-формате (identity)
    JUNIOR:      'JUNIOR',
    MIDDLE:      'MIDDLE',
    SENIOR:      'SENIOR',
    BOSS:        'BOSS',
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

function validateForm(form) {
  const errors = {};

  if (!form.gender) {
    errors.gender = 'Выберите пол';
  }

  if (!form.maritalStatus) {
    errors.maritalStatus = 'Выберите семейное положение';
  }

  const dep = parseInt(form.dependentAmount, 10);
  if (form.dependentAmount === '' || isNaN(dep) || dep < 0) {
    errors.dependentAmount = 'Укажите количество иждивенцев (0 или более)';
  }

  if (!form.passportIssueDate) {
    errors.passportIssueDate = 'Укажите дату выдачи паспорта';
  } else if (new Date(form.passportIssueDate) >= new Date()) {
    errors.passportIssueDate = 'Дата выдачи паспорта должна быть в прошлом';
  }

  if (!form.passportIssueBranch || form.passportIssueBranch.trim().length < 5) {
    errors.passportIssueBranch = 'Отдел выдачи: не менее 5 символов';
  } else if (form.passportIssueBranch.length > 50) {
    errors.passportIssueBranch = 'Отдел выдачи: не более 50 символов';
  }

  if (!form.accountNumber || !ACCOUNT_RE.test(form.accountNumber)) {
    errors.accountNumber = 'Номер счёта: ровно 20 цифр';
  }

  if (!form.employment.employmentStatus) {
    errors.employmentStatus = 'Выберите статус занятости';
  } else if (form.employment.employmentStatus === 'UNEMPLOYED') {
    errors.employmentStatus = 'Кредиты безработным не предоставляются';
  }

  if (!form.employment.employerINN || !INN_RE.test(form.employment.employerINN)) {
    errors.employerINN = 'ИНН работодателя: от 10 до 12 цифр';
  }

  const salary = parseFloat(form.employment.salary);
  if (!form.employment.salary || isNaN(salary) || salary <= 0) {
    errors.salary = 'Укажите зарплату (больше 0)';
  }

  if (!form.employment.position) {
    errors.position = 'Выберите должность';
  }

  const totalExp = parseInt(form.employment.workExperienceTotal, 10);
  if (form.employment.workExperienceTotal === '' || isNaN(totalExp) || totalExp < 0) {
    errors.workExperienceTotal = 'Укажите общий стаж (0 или более месяцев)';
  } else if (totalExp < 18) {
    errors.workExperienceTotal = 'Общий трудовой стаж — не менее 18 месяцев';
  }

  const currExp = parseInt(form.employment.workExperienceCurrent, 10);
  if (form.employment.workExperienceCurrent === '' || isNaN(currExp) || currExp < 0) {
    errors.workExperienceCurrent = 'Укажите текущий стаж (0 или более месяцев)';
  } else if (currExp < 3) {
    errors.workExperienceCurrent = 'Текущий стаж на последнем месте — не менее 3 месяцев';
  }

  return errors;
}

function extractBackendError(err) {
  const data = err.response?.data;
  if (!data) return err.message || 'Произошла ошибка';

  if (typeof data === 'string') {
    try {
      const parsed = JSON.parse(data);
      if (parsed.fieldErrors) {
        const parts = Object.entries(parsed.fieldErrors).map(([f, m]) => `${f}: ${m}`);
        return `${parsed.message}: ${parts.join('; ')}`;
      }
      return parsed.message || data;
    } catch {
      return data;
    }
  }

  if (data.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    const parts = Object.entries(data.fieldErrors).map(([f, m]) => `${f}: ${m}`);
    return `${data.message}: ${parts.join('; ')}`;
  }

  return data.message || err.message || 'Произошла ошибка';
}

function FieldError({ message }) {
  if (!message) return null;
  return <div className="field-error">{message}</div>;
}

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
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Состояние загруженных пользовательских документов (2-НДФЛ и ЭТК).
  // Обновляется компонентом DocumentUploader через onChange и используется для
  // блокировки отправки заявки, если хотя бы один обязательный документ
  // отсутствует.
  const [uploadedDocs, setUploadedDocs] = useState([]);
  const [showDocsError, setShowDocsError] = useState(false);

  const hasDoc = (type) => uploadedDocs.some((d) => d.documentType === type);
  const missingNdfl = !hasDoc('NDFL_2');
  const missingEmployment = !hasDoc('EMPLOYMENT_RECORD');

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

  const setField = (name, value) => {
    setForm((f) => ({ ...f, [name]: value }));
    if (fieldErrors[name]) {
      setFieldErrors((prev) => ({ ...prev, [name]: null }));
    }
  };

  const setEmployment = (name, value) => {
    setForm((f) => ({ ...f, employment: { ...f.employment, [name]: value } }));
    const errorKey = name === 'employmentStatus' ? 'employmentStatus'
      : name === 'employerINN' ? 'employerINN'
      : name === 'salary' ? 'salary'
      : name === 'position' ? 'position'
      : name === 'workExperienceTotal' ? 'workExperienceTotal'
      : name === 'workExperienceCurrent' ? 'workExperienceCurrent'
      : null;
    if (errorKey && fieldErrors[errorKey]) {
      setFieldErrors((prev) => ({ ...prev, [errorKey]: null }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    const errors = validateForm(form);
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      // Scroll to first error
      const firstErrorEl = document.querySelector('.field-error');
      if (firstErrorEl) firstErrorEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }
    setFieldErrors({});

    // Документы 2-НДФЛ и выписки из ЭТК обязательны для отправки заявки.
    if (missingNdfl || missingEmployment) {
      setShowDocsError(true);
      setError('Загрузите обязательные документы: справку 2-НДФЛ и выписку из электронной трудовой книжки.');
      const el = document.querySelector('.doc-slot--error');
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }
    setShowDocsError(false);

    setLoading(true);
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
      navigate(`/statement/confirmation/${statementId}`);
      return;
    } catch (err) {
      setError(extractBackendError(err));
    } finally {
      setLoading(false);
    }
  };

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

        <form onSubmit={handleSubmit} noValidate>
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
                  className={`reg-control${fieldErrors.gender ? ' is-invalid' : ''}`}
                  value={form.gender}
                  onChange={(e) => setField('gender', e.target.value)}
                >
                  <option value="">Выберите...</option>
                  {GENDER_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
                <FieldError message={fieldErrors.gender} />
              </Field>

              <Field label="Семейное положение" required>
                <select
                  className={`reg-control${fieldErrors.maritalStatus ? ' is-invalid' : ''}`}
                  value={form.maritalStatus}
                  onChange={(e) => setField('maritalStatus', e.target.value)}
                >
                  <option value="">Выберите...</option>
                  {MARITAL_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
                <FieldError message={fieldErrors.maritalStatus} />
              </Field>

              <Field label="Кол-во иждивенцев" required>
                <input
                  className={`reg-control${fieldErrors.dependentAmount ? ' is-invalid' : ''}`}
                  type="number"
                  min="0"
                  value={form.dependentAmount}
                  onChange={(e) => setField('dependentAmount', e.target.value)}
                  placeholder="0"
                />
                <FieldError message={fieldErrors.dependentAmount} />
              </Field>

              <Field label="Номер счёта" required>
                <input
                  className={`reg-control${fieldErrors.accountNumber ? ' is-invalid' : ''}`}
                  type="text"
                  maxLength={20}
                  value={form.accountNumber}
                  onChange={(e) => setField('accountNumber', e.target.value)}
                  placeholder="12345678901234567890"
                />
                <FieldError message={fieldErrors.accountNumber} />
              </Field>
            </SectionCard>

            {/* Passport data */}
            <SectionCard title="Паспортные данные" icon="🪪">
              <Field label="Дата выдачи паспорта" required>
                <input
                  className={`reg-control${fieldErrors.passportIssueDate ? ' is-invalid' : ''}`}
                  type="date"
                  value={form.passportIssueDate}
                  onChange={(e) => setField('passportIssueDate', e.target.value)}
                />
                <FieldError message={fieldErrors.passportIssueDate} />
              </Field>

              <Field label="Отдел выдачи" required>
                <input
                  className={`reg-control${fieldErrors.passportIssueBranch ? ' is-invalid' : ''}`}
                  type="text"
                  value={form.passportIssueBranch}
                  onChange={(e) => setField('passportIssueBranch', e.target.value)}
                  placeholder="Отдел МВД №123"
                />
                <FieldError message={fieldErrors.passportIssueBranch} />
              </Field>
            </SectionCard>

            {/* Employment data */}
            <SectionCard title="Трудоустройство" icon="💼">
              <Field label="Статус занятости" required>
                <select
                  className={`reg-control${fieldErrors.employmentStatus ? ' is-invalid' : ''}`}
                  value={form.employment.employmentStatus}
                  onChange={(e) => setEmployment('employmentStatus', e.target.value)}
                >
                  <option value="">Выберите...</option>
                  {EMP_STATUS_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
                <FieldError message={fieldErrors.employmentStatus} />
              </Field>

              <Field label="ИНН работодателя" required>
                <input
                  className={`reg-control${fieldErrors.employerINN ? ' is-invalid' : ''}`}
                  type="text"
                  minLength={10}
                  maxLength={12}
                  value={form.employment.employerINN}
                  onChange={(e) => setEmployment('employerINN', e.target.value)}
                  placeholder="1234567890"
                />
                <FieldError message={fieldErrors.employerINN} />
              </Field>

              <Field label="Зарплата (руб.)" required>
                <input
                  className={`reg-control${fieldErrors.salary ? ' is-invalid' : ''}`}
                  type="number"
                  min="0"
                  step="1000"
                  value={form.employment.salary}
                  onChange={(e) => setEmployment('salary', e.target.value)}
                  placeholder="50000"
                />
                <FieldError message={fieldErrors.salary} />
              </Field>

              <Field label="Должность" required>
                <select
                  className={`reg-control${fieldErrors.position ? ' is-invalid' : ''}`}
                  value={form.employment.position}
                  onChange={(e) => setEmployment('position', e.target.value)}
                >
                  <option value="">Выберите...</option>
                  {POSITION_OPTIONS.map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
                <FieldError message={fieldErrors.position} />
              </Field>

              <Field label="Общий стаж (мес.)" required>
                <input
                  className={`reg-control${fieldErrors.workExperienceTotal ? ' is-invalid' : ''}`}
                  type="number"
                  min="0"
                  value={form.employment.workExperienceTotal}
                  onChange={(e) => setEmployment('workExperienceTotal', e.target.value)}
                  placeholder="36"
                />
                <FieldError message={fieldErrors.workExperienceTotal} />
              </Field>

              <Field label="Текущий стаж (мес.)" required>
                <input
                  className={`reg-control${fieldErrors.workExperienceCurrent ? ' is-invalid' : ''}`}
                  type="number"
                  min="0"
                  value={form.employment.workExperienceCurrent}
                  onChange={(e) => setEmployment('workExperienceCurrent', e.target.value)}
                  placeholder="12"
                />
                <FieldError message={fieldErrors.workExperienceCurrent} />
              </Field>
            </SectionCard>

          </div>

          {/* Documents — full-width card below the grid */}
          <div className="reg-docs-section">
            <div className="reg-section-card">
              <h3 className="reg-section-title">
                <span className="reg-section-icon">📄</span>
                Подтверждающие документы
              </h3>
              <p className="reg-docs-hint">
                Для отправки заявки приложите справку 2-НДФЛ и выписку из
                электронной трудовой книжки в формате PDF. Если документы уже
                были загружены ранее, они подтянутся автоматически — повторно
                прикладывать их не нужно.
              </p>
              <DocumentUploader
                compact
                horizontal
                requiredKeys={['NDFL_2', 'EMPLOYMENT_RECORD']}
                showRequiredErrors={showDocsError}
                onChange={(docs) => {
                  setUploadedDocs(docs);
                  if (
                    docs.some((d) => d.documentType === 'NDFL_2') &&
                    docs.some((d) => d.documentType === 'EMPLOYMENT_RECORD')
                  ) {
                    setShowDocsError(false);
                  }
                }}
              />
            </div>
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
