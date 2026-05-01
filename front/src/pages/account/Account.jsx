import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import client from '../../api/client';
import './Account.css';

// Statuses that allow the user to continue the application
const CONTINUE_ACTIONS = {
  PREAPPROVAL: { label: 'Выбрать предложение', path: (id) => `/statement/resume/${id}` },
  APPROVED:    { label: 'Завершить регистрацию', path: (id) => `/statement/registration/${id}` },
};

const getApiBase = () => (process.env.NODE_ENV === 'development' ? '' : '/api');

const STATUS_LABELS = {
  PREAPPROVAL: 'Предодобрение',
  APPROVED: 'Одобрено',
  CC_APPROVED: 'Одобрено КК',
  CC_DENIED: 'Отказ КК',
  DENIED: 'Отказано',
  DOCUMENT_CREATED: 'Документы созданы',
  DOCUMENT_SIGNED: 'Документы подписаны',
  CREDIT_ISSUED: 'Кредит выдан',
};

const STATUS_CLASS = {
  PREAPPROVAL: 'badge-warning',
  APPROVED: 'badge-success',
  CC_APPROVED: 'badge-success',
  CC_DENIED: 'badge-danger',
  DENIED: 'badge-danger',
  DOCUMENT_CREATED: 'badge-info',
  DOCUMENT_SIGNED: 'badge-info',
  CREDIT_ISSUED: 'badge-success',
};

const DENIABLE_STATUSES = ['PREAPPROVAL', 'APPROVED', 'CC_APPROVED'];

const GENDER_OPTIONS = [
  { value: 'MALE', label: 'Мужской' },
  { value: 'FEMALE', label: 'Женский' },
  { value: 'NON_BINARY', label: 'Другой' },
];

const MARITAL_OPTIONS = [
  { value: 'MARRIED', label: 'Женат/Замужем' },
  { value: 'DIVORCED', label: 'Разведён(а)' },
  { value: 'SINGLE', label: 'Холост/Не замужем' },
  { value: 'WIDOW_WIDOWER', label: 'Вдовец/Вдова' },
];

const EMP_STATUS_OPTIONS = [
  { value: 'UNEMPLOYED', label: 'Безработный' },
  { value: 'SELF_EMPLOYED', label: 'Самозанятый' },
  { value: 'EMPLOYED', label: 'Трудоустроен' },
  { value: 'BUSINESS_OWNER', label: 'Владелец бизнеса' },
];

const EMP_POSITION_OPTIONS = [
  { value: 'WORKER', label: 'Рабочий' },
  { value: 'MID_MANAGER', label: 'Менеджер среднего звена' },
  { value: 'TOP_MANAGER', label: 'Топ-менеджер' },
  { value: 'OWNER', label: 'Владелец' },
];

const labelOf = (options, value) =>
  options.find((o) => o.value === value)?.label || value || '—';

const formatDate = (v) => {
  if (!v) return '—';
  try {
    return new Date(v).toLocaleDateString('ru-RU');
  } catch {
    return String(v);
  }
};

const formatAmount = (v) => {
  if (v == null) return '—';
  return new Intl.NumberFormat('ru-RU', {
    style: 'currency',
    currency: 'RUB',
    maximumFractionDigits: 2,
  }).format(v);
};

function toEditForm(p) {
  return {
    lastName: p.lastName || '',
    firstName: p.firstName || '',
    middleName: p.middleName || '',
    email: p.email || '',
    gender: p.gender || '',
    maritalStatus: p.maritalStatus || '',
    dependentAmount: p.dependentAmount ?? '',
    accountNumber: p.accountNumber || '',
    passport: {
      series: p.passport?.series || '',
      number: p.passport?.number || '',
      issueBranch: p.passport?.issueBranch || '',
      issueDate: p.passport?.issueDate
        ? new Date(p.passport.issueDate).toISOString().slice(0, 10)
        : '',
    },
    employment: {
      status: p.employment?.status || '',
      employer_inn: p.employment?.employer_inn || '',
      salary: p.employment?.salary ?? '',
      position: p.employment?.position || '',
      workExperienceTotal: p.employment?.workExperienceTotal ?? '',
      workExperienceCurrent: p.employment?.workExperienceCurrent ?? '',
    },
  };
}

// ── Shared small components ──────────────────────────────────────────────────

function Loader({ text }) {
  return (
    <div className="loader-state">
      <div className="spinner-border text-primary" role="status" aria-hidden="true" />
      <p className="loader-text">{text}</p>
    </div>
  );
}

function EmptyState({ icon, text, children }) {
  return (
    <div className="empty-state">
      <div className="empty-state-icon">{icon}</div>
      <p className="empty-state-text">{text}</p>
      {children}
    </div>
  );
}

function InfoRow({ label, value }) {
  return (
    <div className="info-row">
      <span className="info-label">{label}</span>
      <span className="info-value">{value ?? '—'}</span>
    </div>
  );
}

function SectionCard({ title, icon, children }) {
  return (
    <div className="section-card">
      <h3 className="section-card-title">
        <span className="section-icon">{icon}</span>
        {title}
      </h3>
      {children}
    </div>
  );
}

// ── Profile View ─────────────────────────────────────────────────────────────

function ProfileView({ profile, onEdit }) {
  return (
    <div className="profile-view">
      <div className="profile-view-header">
        <h2 className="profile-view-name">
          {[profile.lastName, profile.firstName, profile.middleName]
            .filter(Boolean)
            .join(' ') || '—'}
        </h2>
        <button className="btn btn-primary" onClick={onEdit}>
          Редактировать профиль
        </button>
      </div>

      <div className="profile-grid">
        <SectionCard title="Личные данные" icon="👤">
          <InfoRow label="Фамилия" value={profile.lastName} />
          <InfoRow label="Имя" value={profile.firstName} />
          <InfoRow label="Отчество" value={profile.middleName} />
          <InfoRow label="Дата рождения" value={formatDate(profile.birthDate)} />
          <InfoRow label="Email" value={profile.email} />
          <InfoRow label="Пол" value={labelOf(GENDER_OPTIONS, profile.gender)} />
          <InfoRow
            label="Семейное положение"
            value={labelOf(MARITAL_OPTIONS, profile.maritalStatus)}
          />
          <InfoRow label="Кол-во иждивенцев" value={profile.dependentAmount} />
          <InfoRow label="Номер счёта" value={profile.accountNumber} />
        </SectionCard>

        <SectionCard title="Паспорт" icon="🪪">
          <InfoRow label="Серия" value={profile.passport?.series} />
          <InfoRow label="Номер" value={profile.passport?.number} />
          <InfoRow label="Дата выдачи" value={formatDate(profile.passport?.issueDate)} />
          <InfoRow label="Отдел выдачи" value={profile.passport?.issueBranch} />
        </SectionCard>

        <SectionCard title="Трудоустройство" icon="💼">
          <InfoRow
            label="Статус"
            value={labelOf(EMP_STATUS_OPTIONS, profile.employment?.status)}
          />
          <InfoRow label="ИНН работодателя" value={profile.employment?.employer_inn} />
          <InfoRow
            label="Зарплата"
            value={
              profile.employment?.salary != null
                ? formatAmount(profile.employment.salary)
                : null
            }
          />
          <InfoRow
            label="Должность"
            value={labelOf(EMP_POSITION_OPTIONS, profile.employment?.position)}
          />
          <InfoRow
            label="Общий стаж (мес.)"
            value={profile.employment?.workExperienceTotal}
          />
          <InfoRow
            label="Текущий стаж (мес.)"
            value={profile.employment?.workExperienceCurrent}
          />
        </SectionCard>
      </div>
    </div>
  );
}

// ── Profile Edit Form ────────────────────────────────────────────────────────

function ProfileEditForm({ form, loading, error, onField, onNested, onSubmit, onCancel }) {
  return (
    <form className="profile-edit-form" onSubmit={onSubmit}>
      <div className="profile-edit-header">
        <h2 className="profile-edit-title">Редактирование профиля</h2>
      </div>

      {error && <div className="alert alert-danger mt-2">{error}</div>}

      <div className="profile-grid">
        <SectionCard title="Личные данные" icon="👤">
          <div className="edit-field">
            <label className="form-label">Фамилия</label>
            <input
              className="form-control"
              value={form.lastName}
              onChange={(e) => onField('lastName', e.target.value)}
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Имя</label>
            <input
              className="form-control"
              value={form.firstName}
              onChange={(e) => onField('firstName', e.target.value)}
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Отчество</label>
            <input
              className="form-control"
              value={form.middleName}
              onChange={(e) => onField('middleName', e.target.value)}
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Email</label>
            <input
              className="form-control"
              type="email"
              value={form.email}
              onChange={(e) => onField('email', e.target.value)}
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Пол</label>
            <select
              className="form-control"
              value={form.gender}
              onChange={(e) => onField('gender', e.target.value)}
            >
              <option value="">Выберите...</option>
              {GENDER_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
          <div className="edit-field">
            <label className="form-label">Семейное положение</label>
            <select
              className="form-control"
              value={form.maritalStatus}
              onChange={(e) => onField('maritalStatus', e.target.value)}
            >
              <option value="">Выберите...</option>
              {MARITAL_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
          <div className="edit-field">
            <label className="form-label">Кол-во иждивенцев</label>
            <input
              className="form-control"
              type="number"
              min="0"
              value={form.dependentAmount}
              onChange={(e) =>
                onField(
                  'dependentAmount',
                  e.target.value === '' ? '' : parseInt(e.target.value, 10),
                )
              }
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Номер счёта</label>
            <input
              className="form-control"
              value={form.accountNumber}
              onChange={(e) => onField('accountNumber', e.target.value)}
            />
          </div>
        </SectionCard>

        <SectionCard title="Паспорт" icon="🪪">
          <div className="edit-field">
            <label className="form-label">Серия</label>
            <input
              className="form-control"
              value={form.passport?.series || ''}
              onChange={(e) => onNested('passport', 'series', e.target.value)}
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Номер</label>
            <input
              className="form-control"
              value={form.passport?.number || ''}
              onChange={(e) => onNested('passport', 'number', e.target.value)}
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Дата выдачи</label>
            <input
              className="form-control"
              type="date"
              value={form.passport?.issueDate || ''}
              onChange={(e) => onNested('passport', 'issueDate', e.target.value)}
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Отдел выдачи</label>
            <input
              className="form-control"
              value={form.passport?.issueBranch || ''}
              onChange={(e) => onNested('passport', 'issueBranch', e.target.value)}
            />
          </div>
        </SectionCard>

        <SectionCard title="Трудоустройство" icon="💼">
          <div className="edit-field">
            <label className="form-label">Статус занятости</label>
            <select
              className="form-control"
              value={form.employment?.status || ''}
              onChange={(e) => onNested('employment', 'status', e.target.value)}
            >
              <option value="">Выберите...</option>
              {EMP_STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
          <div className="edit-field">
            <label className="form-label">ИНН работодателя</label>
            <input
              className="form-control"
              value={form.employment?.employer_inn || ''}
              onChange={(e) => onNested('employment', 'employer_inn', e.target.value)}
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Зарплата (руб.)</label>
            <input
              className="form-control"
              type="number"
              min="0"
              value={form.employment?.salary ?? ''}
              onChange={(e) =>
                onNested(
                  'employment',
                  'salary',
                  e.target.value === '' ? '' : parseFloat(e.target.value),
                )
              }
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Должность</label>
            <select
              className="form-control"
              value={form.employment?.position || ''}
              onChange={(e) => onNested('employment', 'position', e.target.value)}
            >
              <option value="">Выберите...</option>
              {EMP_POSITION_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
          <div className="edit-field">
            <label className="form-label">Общий стаж (мес.)</label>
            <input
              className="form-control"
              type="number"
              min="0"
              value={form.employment?.workExperienceTotal ?? ''}
              onChange={(e) =>
                onNested(
                  'employment',
                  'workExperienceTotal',
                  e.target.value === '' ? '' : parseInt(e.target.value, 10),
                )
              }
            />
          </div>
          <div className="edit-field">
            <label className="form-label">Текущий стаж (мес.)</label>
            <input
              className="form-control"
              type="number"
              min="0"
              value={form.employment?.workExperienceCurrent ?? ''}
              onChange={(e) =>
                onNested(
                  'employment',
                  'workExperienceCurrent',
                  e.target.value === '' ? '' : parseInt(e.target.value, 10),
                )
              }
            />
          </div>
        </SectionCard>
      </div>

      <div className="edit-actions">
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? (
            <>
              <span
                className="spinner-border spinner-border-sm"
                role="status"
                aria-hidden="true"
              />
              Сохранение...
            </>
          ) : (
            'Сохранить изменения'
          )}
        </button>
        <button
          type="button"
          className="btn btn-outline"
          onClick={onCancel}
          disabled={loading}
        >
          Отмена
        </button>
      </div>
    </form>
  );
}

// ── Statement Detail ─────────────────────────────────────────────────────────

function StatementDetailView({ detail }) {
  const offer = detail.appliedOffer;
  const history = detail.statusHistory || [];

  return (
    <div className="detail-view">
      <div className="detail-grid">
        <div>
          <h4 className="detail-section-title">Информация о заявке</h4>
          <InfoRow label="ID заявки" value={detail.statementId} />
          <InfoRow label="Дата создания" value={formatDate(detail.creationDate)} />
          <InfoRow label="Дата подписания" value={formatDate(detail.signDate)} />
        </div>

        {offer && (
          <div>
            <h4 className="detail-section-title">Выбранное предложение</h4>
            {offer.requestedAmount != null && (
              <InfoRow label="Запрошенная сумма" value={formatAmount(offer.requestedAmount)} />
            )}
            {offer.totalAmount != null && (
              <InfoRow label="Полная сумма" value={formatAmount(offer.totalAmount)} />
            )}
            {offer.term != null && (
              <InfoRow label="Срок" value={`${offer.term} мес.`} />
            )}
            {offer.monthlyPayment != null && (
              <InfoRow label="Платёж/мес." value={formatAmount(offer.monthlyPayment)} />
            )}
            {offer.rate != null && (
              <InfoRow label="Ставка" value={`${offer.rate}%`} />
            )}
            {offer.psk != null && (
              <InfoRow label="ПСК" value={`${offer.psk}%`} />
            )}
            {offer.isInsuranceEnabled != null && (
              <InfoRow label="Страховка" value={offer.isInsuranceEnabled ? 'Да' : 'Нет'} />
            )}
            {offer.isSalaryClient != null && (
              <InfoRow
                label="Зарплатный клиент"
                value={offer.isSalaryClient ? 'Да' : 'Нет'}
              />
            )}
          </div>
        )}
      </div>

      {detail.credit && (
        <div className="mt-3">
          <h4 className="detail-section-title">Кредит по заявке</h4>
          <div className="detail-grid">
            <div>
              <InfoRow label="Сумма кредита" value={formatAmount(detail.credit.amount)} />
              <InfoRow label="Срок" value={`${detail.credit.term} мес.`} />
              <InfoRow label="Ставка" value={`${detail.credit.rate}%`} />
            </div>
            <div>
              <InfoRow label="Платёж/мес." value={formatAmount(detail.credit.monthlyPayment)} />
              <InfoRow label="ПСК" value={`${detail.credit.psk}%`} />
              <InfoRow
                label="Статус кредита"
                value={STATUS_LABELS[detail.credit.creditStatus] || detail.credit.creditStatus}
              />
            </div>
          </div>
        </div>
      )}

      {history.length > 0 && (
        <div className="mt-3">
          <h4 className="detail-section-title">История статусов</h4>
          <div className="status-history">
            {history.map((h, i) => (
              <div key={i} className="history-item">
                <span className={`status-badge ${STATUS_CLASS[h.status] || 'badge-info'}`}>
                  {STATUS_LABELS[h.status] || h.status}
                </span>
                <span className="history-time">{formatDate(h.time)}</span>
                {h.changeType && <span className="history-change">{h.changeType}</span>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ── Credit Detail ────────────────────────────────────────────────────────────

function CreditDetailView({ detail }) {
  let schedule = null;
  try {
    schedule = detail.paymentSchedule;
    if (typeof schedule === 'string') schedule = JSON.parse(schedule);
  } catch {
    schedule = null;
  }

  return (
    <div className="detail-view">
      <div className="detail-grid">
        <div>
          <h4 className="detail-section-title">Параметры кредита</h4>
          <InfoRow label="ID кредита" value={detail.creditId} />
          <InfoRow label="Сумма" value={formatAmount(detail.amount)} />
          <InfoRow label="Срок" value={`${detail.term} мес.`} />
          <InfoRow label="Ставка" value={`${detail.rate}%`} />
        </div>
        <div>
          <InfoRow label="Платёж/мес." value={formatAmount(detail.monthlyPayment)} />
          <InfoRow label="ПСК" value={`${detail.psk}%`} />
          <InfoRow label="Страховка" value={detail.isInsuranceEnabled ? 'Да' : 'Нет'} />
          <InfoRow
            label="Зарплатный клиент"
            value={detail.isSalaryClient ? 'Да' : 'Нет'}
          />
        </div>
      </div>

      {Array.isArray(schedule) && schedule.length > 0 && (
        <div className="mt-3">
          <h4 className="detail-section-title">График платежей</h4>
          <div className="schedule-table-wrapper">
            <table className="schedule-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Дата</th>
                  <th>Платёж</th>
                  <th>Основной долг</th>
                  <th>Проценты</th>
                  <th>Остаток</th>
                </tr>
              </thead>
              <tbody>
                {schedule.map((row, i) => (
                  <tr key={i}>
                    <td>{row.number ?? i + 1}</td>
                    <td>{formatDate(row.date)}</td>
                    <td>{formatAmount(row.totalPayment)}</td>
                    <td>{formatAmount(row.debtPayment)}</td>
                    <td>{formatAmount(row.interestPayment)}</td>
                    <td>{formatAmount(row.remainingDebt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

// ── Statement Card ───────────────────────────────────────────────────────────

function StatementCard({ stmt, isOpen, detail, detailLoading, denyLoading, onToggle, onDeny, onContinue }) {
  const canDeny = DENIABLE_STATUSES.includes(stmt.status);
  const continueAction = CONTINUE_ACTIONS[stmt.status];

  return (
    <div className={`list-card${isOpen ? ' list-card--open' : ''}`}>
      <div className="list-card-header" onClick={onToggle}>
        <div className="list-card-main">
          <span className="list-card-id">#{String(stmt.statementId).slice(0, 8)}…</span>
          <span className={`status-badge ${STATUS_CLASS[stmt.status] || 'badge-info'}`}>
            {STATUS_LABELS[stmt.status] || stmt.status}
          </span>
        </div>
        <div className="list-card-meta">
          <span>
            <strong>Сумма:</strong> {formatAmount(stmt.requestedAmount)}
          </span>
          <span>
            <strong>Срок:</strong> {stmt.term} мес.
          </span>
          <span>
            <strong>Дата:</strong> {formatDate(stmt.creationDate)}
          </span>
        </div>
        <div className="list-card-actions" onClick={(e) => e.stopPropagation()}>
          {continueAction && (
            <button
              className="btn btn-continue btn-sm"
              onClick={onContinue}
              disabled={denyLoading}
            >
              {continueAction.label} →
            </button>
          )}
          {canDeny && (
            <button
              className="btn btn-danger btn-sm"
              onClick={onDeny}
              disabled={denyLoading}
            >
              {denyLoading ? 'Отмена...' : 'Отказаться'}
            </button>
          )}
          <span className="expand-chevron">{isOpen ? '▲' : '▼'}</span>
        </div>
      </div>

      {isOpen && (
        <div className="list-card-body">
          {detailLoading ? (
            <div className="loading-inline">
              <span
                className="spinner-border spinner-border-sm"
                role="status"
                aria-hidden="true"
              />
              Загрузка деталей...
            </div>
          ) : detail ? (
            <StatementDetailView detail={detail} />
          ) : null}
        </div>
      )}
    </div>
  );
}

// ── Credit Card ──────────────────────────────────────────────────────────────

function CreditCard({ credit, isOpen, detail, detailLoading, onToggle }) {
  return (
    <div className={`list-card${isOpen ? ' list-card--open' : ''}`}>
      <div className="list-card-header" onClick={onToggle}>
        <div className="list-card-main">
          <span className="list-card-id">#{String(credit.creditId).slice(0, 8)}…</span>
          <span className={`status-badge ${STATUS_CLASS[credit.creditStatus] || 'badge-info'}`}>
            {STATUS_LABELS[credit.creditStatus] || credit.creditStatus}
          </span>
        </div>
        <div className="list-card-meta">
          <span>
            <strong>Сумма:</strong> {formatAmount(credit.amount)}
          </span>
          <span>
            <strong>Срок:</strong> {credit.term} мес.
          </span>
          <span>
            <strong>Ставка:</strong> {credit.rate}%
          </span>
          <span>
            <strong>Платёж:</strong> {formatAmount(credit.monthlyPayment)}/мес.
          </span>
        </div>
        <span className="expand-chevron">{isOpen ? '▲' : '▼'}</span>
      </div>

      {isOpen && (
        <div className="list-card-body">
          {detailLoading ? (
            <div className="loading-inline">
              <span
                className="spinner-border spinner-border-sm"
                role="status"
                aria-hidden="true"
              />
              Загрузка деталей...
            </div>
          ) : detail ? (
            <CreditDetailView detail={detail} />
          ) : null}
        </div>
      )}
    </div>
  );
}

// ── Main Page ────────────────────────────────────────────────────────────────

export default function AccountPage() {
  const [tab, setTab] = useState('profile');
  const base = getApiBase();
  const navigate = useNavigate();

  // Profile state
  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState(null);
  const [editMode, setEditMode] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [editLoading, setEditLoading] = useState(false);
  const [editError, setEditError] = useState(null);
  const [editSuccess, setEditSuccess] = useState(null);

  // Statements state
  const [statements, setStatements] = useState([]);
  const [stmtLoading, setStmtLoading] = useState(false);
  const [stmtError, setStmtError] = useState(null);
  const [stmtFetched, setStmtFetched] = useState(false);
  const [openStmt, setOpenStmt] = useState(null);
  const [stmtDetail, setStmtDetail] = useState(null);
  const [stmtDetailLoading, setStmtDetailLoading] = useState(false);
  const [denyLoading, setDenyLoading] = useState(null);
  const [denyError, setDenyError] = useState(null);

  // Credits state
  const [credits, setCredits] = useState([]);
  const [creditLoading, setCreditLoading] = useState(false);
  const [creditError, setCreditError] = useState(null);
  const [creditFetched, setCreditFetched] = useState(false);
  const [openCredit, setOpenCredit] = useState(null);
  const [creditDetail, setCreditDetail] = useState(null);
  const [creditDetailLoading, setCreditDetailLoading] = useState(false);

  // ── Profile ──────────────────────────────────────────────────────────────
  useEffect(() => {
    (async () => {
      try {
        const res = await client.get(`${base}/deal/client/me`);
        setProfile(res.data);
        setEditForm(toEditForm(res.data));
      } catch (e) {
        setProfileError(
          e.response?.data?.message || e.message || 'Ошибка загрузки профиля',
        );
      } finally {
        setProfileLoading(false);
      }
    })();
  }, [base]);

  // ── Statements ───────────────────────────────────────────────────────────
  const fetchStatements = useCallback(async () => {
    setStmtLoading(true);
    setStmtError(null);
    try {
      const res = await client.get(`${base}/deal/client/me/statements`);
      setStatements(res.data);
      setStmtFetched(true);
    } catch (e) {
      setStmtError(
        e.response?.data?.message || e.message || 'Ошибка загрузки заявок',
      );
    } finally {
      setStmtLoading(false);
    }
  }, [base]);

  useEffect(() => {
    if (tab === 'statements' && !stmtFetched) fetchStatements();
  }, [tab, stmtFetched, fetchStatements]);

  const toggleStatement = async (id) => {
    if (openStmt === id) {
      setOpenStmt(null);
      setStmtDetail(null);
      return;
    }
    setOpenStmt(id);
    setStmtDetail(null);
    setStmtDetailLoading(true);
    try {
      const res = await client.get(`${base}/deal/client/me/statements/${id}`);
      setStmtDetail(res.data);
    } finally {
      setStmtDetailLoading(false);
    }
  };

  const denyStatement = async (id) => {
    setDenyLoading(id);
    setDenyError(null);
    try {
      await client.post(`${base}/deal/client/me/statements/${id}/deny`);
      const res = await client.get(`${base}/deal/client/me/statements`);
      setStatements(res.data);
      if (openStmt === id) {
        const det = await client.get(`${base}/deal/client/me/statements/${id}`);
        setStmtDetail(det.data);
      }
    } catch (e) {
      setDenyError(
        e.response?.data?.message || e.message || 'Ошибка отказа от заявки',
      );
    } finally {
      setDenyLoading(null);
    }
  };

  // ── Credits ──────────────────────────────────────────────────────────────
  const fetchCredits = useCallback(async () => {
    setCreditLoading(true);
    setCreditError(null);
    try {
      const res = await client.get(`${base}/deal/client/me/credits`);
      setCredits(res.data);
      setCreditFetched(true);
    } catch (e) {
      setCreditError(
        e.response?.data?.message || e.message || 'Ошибка загрузки кредитов',
      );
    } finally {
      setCreditLoading(false);
    }
  }, [base]);

  useEffect(() => {
    if (tab === 'credits' && !creditFetched) fetchCredits();
  }, [tab, creditFetched, fetchCredits]);

  const toggleCredit = async (id) => {
    if (openCredit === id) {
      setOpenCredit(null);
      setCreditDetail(null);
      return;
    }
    setOpenCredit(id);
    setCreditDetail(null);
    setCreditDetailLoading(true);
    try {
      const res = await client.get(`${base}/deal/client/me/credits/${id}`);
      setCreditDetail(res.data);
    } finally {
      setCreditDetailLoading(false);
    }
  };

  // ── Edit Profile ─────────────────────────────────────────────────────────
  const handleEditField = (field, value) =>
    setEditForm((f) => ({ ...f, [field]: value }));

  const handleEditNested = (section, field, value) =>
    setEditForm((f) => ({ ...f, [section]: { ...f[section], [field]: value } }));

  const submitEdit = async (e) => {
    e.preventDefault();
    setEditLoading(true);
    setEditError(null);
    try {
      const res = await client.patch(`${base}/deal/client/me`, editForm);
      setProfile(res.data);
      setEditForm(toEditForm(res.data));
      setEditMode(false);
      setEditSuccess('Профиль успешно обновлён');
      setTimeout(() => setEditSuccess(null), 4000);
    } catch (e) {
      setEditError(
        e.response?.data?.message || e.message || 'Ошибка обновления профиля',
      );
    } finally {
      setEditLoading(false);
    }
  };

  // ── Render ───────────────────────────────────────────────────────────────
  const fullName =
    profile
      ? [profile.lastName, profile.firstName, profile.middleName]
          .filter(Boolean)
          .join(' ')
      : null;

  return (
    <div className="account-page">
      <div className="account-content">
        {/* Page header */}
        <div className="account-page-header">
          <div className="account-avatar">👤</div>
          <div className="account-page-heading">
            <h1 className="account-page-title">Личный кабинет</h1>
            <p className="account-page-subtitle">
              {profileLoading ? 'Загрузка...' : fullName || 'Профиль пользователя'}
            </p>
          </div>
          <Link to="/account/settings" className="account-settings-link" title="Настройки">
            <span className="account-settings-icon" aria-hidden="true">
              {/* Gear icon */}
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="3" />
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
              </svg>
            </span>
            <span className="account-settings-label">Настройки</span>
          </Link>
        </div>

        {/* Tab navigation */}
        <div className="account-tabs">
          {[
            { key: 'profile', label: 'Профиль', icon: '🪪' },
            { key: 'statements', label: 'Заявки', icon: '📋' },
            { key: 'credits', label: 'Кредиты', icon: '💳' },
          ].map((t) => (
            <button
              key={t.key}
              className={`account-tab${tab === t.key ? ' account-tab--active' : ''}`}
              onClick={() => setTab(t.key)}
            >
              <span className="tab-icon">{t.icon}</span>
              {t.label}
            </button>
          ))}
        </div>

        {/* Profile tab */}
        {tab === 'profile' && (
          <div className="tab-panel">
            {editSuccess && <div className="alert alert-success">{editSuccess}</div>}
            {profileLoading ? (
              <Loader text="Загрузка профиля..." />
            ) : profileError ? (
              <div className="alert alert-danger">{profileError}</div>
            ) : profile && (
              editMode ? (
                <ProfileEditForm
                  form={editForm}
                  loading={editLoading}
                  error={editError}
                  onField={handleEditField}
                  onNested={handleEditNested}
                  onSubmit={submitEdit}
                  onCancel={() => {
                    setEditMode(false);
                    setEditError(null);
                    setEditForm(toEditForm(profile));
                  }}
                />
              ) : (
                <ProfileView profile={profile} onEdit={() => setEditMode(true)} />
              )
            )}
          </div>
        )}

        {/* Statements tab */}
        {tab === 'statements' && (
          <div className="tab-panel">
            {denyError && <div className="alert alert-danger">{denyError}</div>}
            {stmtLoading ? (
              <Loader text="Загрузка заявок..." />
            ) : stmtError ? (
              <div className="alert alert-danger">{stmtError}</div>
            ) : statements.length === 0 ? (
              <EmptyState icon="📋" text="У вас пока нет заявок">
                <a href="/statement" className="btn btn-primary mt-3">
                  Подать заявку
                </a>
              </EmptyState>
            ) : (
              <div className="card-list">
                {statements.map((stmt) => (
                  <StatementCard
                    key={stmt.statementId}
                    stmt={stmt}
                    isOpen={openStmt === stmt.statementId}
                    detail={openStmt === stmt.statementId ? stmtDetail : null}
                    detailLoading={openStmt === stmt.statementId && stmtDetailLoading}
                    denyLoading={denyLoading === stmt.statementId}
                    onToggle={() => toggleStatement(stmt.statementId)}
                    onDeny={() => denyStatement(stmt.statementId)}
                    onContinue={() => {
                      const action = CONTINUE_ACTIONS[stmt.status];
                      if (action) navigate(action.path(stmt.statementId), {
                        state: {
                          requestedAmount: stmt.requestedAmount,
                          requestedTerm: stmt.term,
                        },
                      });
                    }}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* Credits tab */}
        {tab === 'credits' && (
          <div className="tab-panel">
            {creditLoading ? (
              <Loader text="Загрузка кредитов..." />
            ) : creditError ? (
              <div className="alert alert-danger">{creditError}</div>
            ) : credits.length === 0 ? (
              <EmptyState icon="💳" text="У вас пока нет активных кредитов" />
            ) : (
              <div className="card-list">
                {credits.map((credit) => (
                  <CreditCard
                    key={credit.creditId}
                    credit={credit}
                    isOpen={openCredit === credit.creditId}
                    detail={openCredit === credit.creditId ? creditDetail : null}
                    detailLoading={openCredit === credit.creditId && creditDetailLoading}
                    onToggle={() => toggleCredit(credit.creditId)}
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
