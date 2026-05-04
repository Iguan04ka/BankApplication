import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import client from '../../api/client';
import { useAuth } from '../../auth/AuthProvider';
import OtpInput from '../../shared/ui/OtpInput/OtpInput';
import './Settings.css';

const getApiBase = () => (process.env.NODE_ENV === 'development' ? '' : '/api');

const SECTIONS = [
  { key: 'account', label: 'Аккаунт', icon: '👤' },
  { key: 'security', label: 'Безопасность', icon: '🛡️' },
];

// ── Helper: parse error response from gateway ────────────────────────────────

function extractError(err, fallback) {
  const data = err.response?.data;
  if (!data) return err.message || fallback;
  if (typeof data === 'string') {
    try {
      const parsed = JSON.parse(data);
      return parsed.error || parsed.message || data;
    } catch {
      return data;
    }
  }
  if (typeof data === 'object') {
    return data.error || data.message || JSON.stringify(data);
  }
  return fallback;
}

// ── Reusable change form ─────────────────────────────────────────────────────

function ChangeForm({
  title,
  description,
  fields,
  submitLabel,
  loading,
  error,
  success,
  onSubmit,
}) {
  return (
    <form className="settings-form" onSubmit={onSubmit}>
      <div className="settings-form-header">
        <h3 className="settings-form-title">{title}</h3>
        {description && <p className="settings-form-description">{description}</p>}
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="settings-form-body">
        {fields.map((f) => (
          <div className="settings-field" key={f.id}>
            <label htmlFor={f.id} className="settings-label">
              {f.label}
            </label>
            <input
              id={f.id}
              type={f.type || 'text'}
              className="settings-input"
              value={f.value}
              onChange={(e) => f.onChange(e.target.value)}
              placeholder={f.placeholder}
              autoComplete={f.autoComplete}
              required
              minLength={f.minLength}
            />
            {f.hint && <div className="settings-hint">{f.hint}</div>}
          </div>
        ))}
      </div>

      <div className="settings-form-actions">
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? (
            <>
              <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
              Сохранение...
            </>
          ) : (
            submitLabel
          )}
        </button>
      </div>
    </form>
  );
}

// ── Account section ──────────────────────────────────────────────────────────

function AccountSection({ onLogoutAfterPasswordChange }) {
  const base = getApiBase();
  const auth = useAuth();

  // Sub change
  const [newSub, setNewSub] = useState('');
  const [subPassword, setSubPassword] = useState('');
  const [subLoading, setSubLoading] = useState(false);
  const [subError, setSubError] = useState(null);
  const [subSuccess, setSubSuccess] = useState(null);

  // Email change
  const [newEmail, setNewEmail] = useState('');
  const [emailPassword, setEmailPassword] = useState('');
  const [emailLoading, setEmailLoading] = useState(false);
  const [emailError, setEmailError] = useState(null);
  const [emailSuccess, setEmailSuccess] = useState(null);

  // Password change
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [pwConfirm, setPwConfirm] = useState('');
  const [pwLoading, setPwLoading] = useState(false);
  const [pwError, setPwError] = useState(null);
  const [pwSuccess, setPwSuccess] = useState(null);

  const submitSub = async (e) => {
    e.preventDefault();
    setSubLoading(true);
    setSubError(null);
    setSubSuccess(null);
    try {
      const res = await client.post(`${base}/account/settings/change-sub`, {
        newSub: newSub.trim(),
        currentPassword: subPassword,
      });
      // Server returns new sub + new tokens (sub is part of JWT)
      const { accessToken, refreshToken, sub } = res.data || {};
      if (accessToken && refreshToken) {
        auth.login({ accessToken, refreshToken });
      }
      setSubSuccess(`Логин успешно изменён на «${sub || newSub.trim()}»`);
      setNewSub('');
      setSubPassword('');
    } catch (err) {
      setSubError(extractError(err, 'Не удалось сменить логин'));
    } finally {
      setSubLoading(false);
    }
  };

  const submitEmail = async (e) => {
    e.preventDefault();
    setEmailLoading(true);
    setEmailError(null);
    setEmailSuccess(null);
    try {
      const res = await client.post(`${base}/account/settings/change-email`, {
        newEmail: newEmail.trim(),
        currentPassword: emailPassword,
      });
      const updated = res.data?.email || newEmail.trim();
      setEmailSuccess(`Email обновлён: ${updated}`);
      setNewEmail('');
      setEmailPassword('');
    } catch (err) {
      setEmailError(extractError(err, 'Не удалось сменить email'));
    } finally {
      setEmailLoading(false);
    }
  };

  const submitPassword = async (e) => {
    e.preventDefault();
    setPwError(null);
    setPwSuccess(null);
    if (newPassword.length < 6) {
      setPwError('Пароль должен быть не короче 6 символов');
      return;
    }
    if (newPassword !== pwConfirm) {
      setPwError('Пароли не совпадают');
      return;
    }
    setPwLoading(true);
    try {
      await client.post(`${base}/account/settings/change-password`, {
        currentPassword: oldPassword,
        newPassword,
      });
      setPwSuccess('Пароль успешно изменён. Сейчас вы будете перенаправлены на страницу входа.');
      setOldPassword('');
      setNewPassword('');
      setPwConfirm('');
      setTimeout(() => onLogoutAfterPasswordChange(), 2200);
    } catch (err) {
      setPwError(extractError(err, 'Не удалось сменить пароль'));
    } finally {
      setPwLoading(false);
    }
  };

  return (
    <div className="settings-section">
      <div className="settings-section-header">
        <h2 className="settings-section-title">
          <span className="settings-section-icon">👤</span>
          Аккаунт
        </h2>
        <p className="settings-section-subtitle">
          Управляйте логином, электронной почтой и паролем вашей учётной записи.
          Для подтверждения каждого изменения требуется текущий пароль.
        </p>
      </div>

      <ChangeForm
        title="Логин"
        description="Логин используется для входа в систему. После смены логина текущая сессия будет автоматически обновлена."
        loading={subLoading}
        error={subError}
        success={subSuccess}
        onSubmit={submitSub}
        submitLabel="Сменить логин"
        fields={[
          {
            id: 'newSub',
            label: 'Новый логин',
            value: newSub,
            onChange: setNewSub,
            placeholder: 'Придумайте новый логин',
            autoComplete: 'off',
          },
          {
            id: 'subPassword',
            label: 'Текущий пароль',
            type: 'password',
            value: subPassword,
            onChange: setSubPassword,
            placeholder: 'Введите текущий пароль',
            autoComplete: 'current-password',
          },
        ]}
      />

      <ChangeForm
        title="Электронная почта"
        description="Email используется для уведомлений и восстановления пароля."
        loading={emailLoading}
        error={emailError}
        success={emailSuccess}
        onSubmit={submitEmail}
        submitLabel="Сменить email"
        fields={[
          {
            id: 'newEmail',
            label: 'Новый email',
            type: 'email',
            value: newEmail,
            onChange: setNewEmail,
            placeholder: 'example@mail.ru',
            autoComplete: 'off',
          },
          {
            id: 'emailPassword',
            label: 'Текущий пароль',
            type: 'password',
            value: emailPassword,
            onChange: setEmailPassword,
            placeholder: 'Введите текущий пароль',
            autoComplete: 'current-password',
          },
        ]}
      />

      <ChangeForm
        title="Пароль"
        description="После смены пароля потребуется заново войти в систему."
        loading={pwLoading}
        error={pwError}
        success={pwSuccess}
        onSubmit={submitPassword}
        submitLabel="Сменить пароль"
        fields={[
          {
            id: 'oldPassword',
            label: 'Текущий пароль',
            type: 'password',
            value: oldPassword,
            onChange: setOldPassword,
            placeholder: 'Введите текущий пароль',
            autoComplete: 'current-password',
          },
          {
            id: 'newPassword',
            label: 'Новый пароль',
            type: 'password',
            value: newPassword,
            onChange: setNewPassword,
            placeholder: 'Минимум 6 символов',
            autoComplete: 'new-password',
            minLength: 6,
            hint: 'Используйте не менее 6 символов',
          },
          {
            id: 'pwConfirm',
            label: 'Повторите новый пароль',
            type: 'password',
            value: pwConfirm,
            onChange: setPwConfirm,
            placeholder: 'Повторите новый пароль',
            autoComplete: 'new-password',
            minLength: 6,
          },
        ]}
      />
    </div>
  );
}

// ── Security section ─────────────────────────────────────────────────────────

function SecuritySection() {
  const base = getApiBase();

  // Server state
  const [enabled, setEnabled] = useState(false);
  const [email, setEmail] = useState('');
  const [statusLoading, setStatusLoading] = useState(true);
  const [statusError, setStatusError] = useState(null);

  // Local state (checkbox)
  const [checked, setChecked] = useState(false);

  // Apply / verification flow
  const [phase, setPhase] = useState('idle'); // idle | requesting | verifying | success
  const [pendingAction, setPendingAction] = useState(null); // 'enable' | 'disable'
  const [code, setCode] = useState('');
  const [actionError, setActionError] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [resendInfo, setResendInfo] = useState(null);

  const loadStatus = useCallback(async () => {
    setStatusLoading(true);
    setStatusError(null);
    try {
      const res = await client.get(`${base}/account/settings/2fa/status`);
      const e = !!res.data?.enabled;
      const em = res.data?.email || '';
      setEnabled(e);
      setChecked(e);
      setEmail(em);
    } catch (err) {
      setStatusError(extractError(err, 'Не удалось загрузить настройки безопасности'));
    } finally {
      setStatusLoading(false);
    }
  }, [base]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  const apply = async () => {
    setActionError(null);
    setResendInfo(null);
    if (checked === enabled) return;

    const action = checked ? 'enable' : 'disable';
    setPendingAction(action);
    setActionLoading(true);
    try {
      const path = action === 'enable'
        ? `${base}/account/settings/2fa/request-enable`
        : `${base}/account/settings/2fa/request-disable`;
      await client.post(path);
      setPhase('verifying');
      setCode('');
    } catch (err) {
      setActionError(extractError(err, 'Не удалось отправить код'));
      setPendingAction(null);
    } finally {
      setActionLoading(false);
    }
  };

  const submitCode = async (e) => {
    e.preventDefault();
    setActionError(null);
    if (code.length !== 6) {
      setActionError('Введите 6-значный код');
      return;
    }
    setActionLoading(true);
    try {
      const path = pendingAction === 'enable'
        ? `${base}/account/settings/2fa/confirm-enable`
        : `${base}/account/settings/2fa/confirm-disable`;
      await client.post(path, { code });
      setPhase('success');
    } catch (err) {
      setActionError(extractError(err, 'Неверный или просроченный код'));
    } finally {
      setActionLoading(false);
    }
  };

  const resendCode = async () => {
    setResendInfo(null);
    setActionError(null);
    try {
      const path = pendingAction === 'enable'
        ? `${base}/account/settings/2fa/request-enable`
        : `${base}/account/settings/2fa/request-disable`;
      await client.post(path);
      setResendInfo('Новый код отправлен на почту.');
    } catch (err) {
      setActionError(extractError(err, 'Не удалось отправить код повторно'));
    }
  };

  const cancelVerification = () => {
    setPhase('idle');
    setCode('');
    setPendingAction(null);
    setActionError(null);
    setResendInfo(null);
    setChecked(enabled);
  };

  const finishSuccess = async () => {
    setPhase('idle');
    setCode('');
    setPendingAction(null);
    setActionError(null);
    setResendInfo(null);
    await loadStatus();
  };

  // ── Render

  if (statusLoading) {
    return (
      <div className="settings-section">
        <div className="settings-section-header">
          <h2 className="settings-section-title">
            <span className="settings-section-icon">🛡️</span>
            Безопасность
          </h2>
        </div>
        <div className="settings-form">
          <div className="settings-form-description">Загрузка...</div>
        </div>
      </div>
    );
  }

  if (statusError) {
    return (
      <div className="settings-section">
        <div className="settings-section-header">
          <h2 className="settings-section-title">
            <span className="settings-section-icon">🛡️</span>
            Безопасность
          </h2>
        </div>
        <div className="settings-form">
          <div className="alert alert-danger">{statusError}</div>
        </div>
      </div>
    );
  }

  if (phase === 'verifying') {
    return (
      <div className="settings-section">
        <div className="settings-section-header">
          <h2 className="settings-section-title">
            <span className="settings-section-icon">🛡️</span>
            Безопасность
          </h2>
          <p className="settings-section-subtitle">
            Мы отправили 6-значный код на адрес <strong>{email}</strong>.
            Введите его, чтобы {pendingAction === 'enable' ? 'включить' : 'отключить'} двухфакторную аутентификацию.
          </p>
        </div>

        <form className="settings-form" onSubmit={submitCode}>
          {actionError && <div className="alert alert-danger">{actionError}</div>}
          {resendInfo && <div className="alert alert-success">{resendInfo}</div>}

          <OtpInput value={code} onChange={setCode} disabled={actionLoading} />

          <div className="settings-hint" style={{ textAlign: 'center', marginBottom: '1rem' }}>
            Код действителен 5 минут с момента отправки.
          </div>

          <div className="settings-form-actions" style={{ gap: '0.75rem' }}>
            <button
              type="button"
              className="btn btn-back-mini"
              onClick={cancelVerification}
              disabled={actionLoading}
            >
              ← Отмена
            </button>
            <button
              type="button"
              className="btn btn-back-mini"
              onClick={resendCode}
              disabled={actionLoading}
            >
              Отправить код повторно
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={actionLoading || code.length !== 6}
            >
              {actionLoading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                  Подтверждение...
                </>
              ) : (
                'Подтвердить'
              )}
            </button>
          </div>
        </form>
      </div>
    );
  }

  if (phase === 'success') {
    const wasEnable = pendingAction === 'enable';
    return (
      <div className="settings-section">
        <div className="settings-section-header">
          <h2 className="settings-section-title">
            <span className="settings-section-icon">🛡️</span>
            Безопасность
          </h2>
        </div>

        <div className="settings-form">
          <div className="settings-success">
            <div className="settings-success-icon">{wasEnable ? '✅' : '🔓'}</div>
            <h3 className="settings-success-title">
              {wasEnable
                ? 'Двухфакторная аутентификация включена'
                : 'Двухфакторная аутентификация отключена'}
            </h3>
            <p className="settings-success-text">
              {wasEnable
                ? 'Теперь при каждом входе мы будем отправлять одноразовый код на вашу почту.'
                : 'Вход в аккаунт снова будет выполняться только по логину и паролю.'}
            </p>
            <div className="settings-success-actions">
              <button type="button" className="btn btn-primary" onClick={finishSuccess}>
                ← Вернуться к настройкам
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // idle — main view
  const dirty = checked !== enabled;

  return (
    <div className="settings-section">
      <div className="settings-section-header">
        <h2 className="settings-section-title">
          <span className="settings-section-icon">🛡️</span>
          Безопасность
        </h2>
        <p className="settings-section-subtitle">
          Управляйте дополнительными механизмами защиты вашего аккаунта.
        </p>
      </div>

      <div className="settings-form">
        <div className="settings-form-header">
          <h3 className="settings-form-title">Двухфакторная аутентификация</h3>
          <p className="settings-form-description">
            При включённой двухфакторной аутентификации после ввода логина и пароля мы отправим 6-значный код
            на вашу электронную почту. Без правильного кода вход невозможен — это защищает аккаунт даже в случае
            утечки пароля. Вторым фактором служит ваша почта{' '}
            <strong>{email || '—'}</strong>.
          </p>
        </div>

        {actionError && <div className="alert alert-danger">{actionError}</div>}

        <label className="settings-toggle-row">
          <input
            type="checkbox"
            className="settings-toggle-checkbox"
            checked={checked}
            onChange={(e) => setChecked(e.target.checked)}
            disabled={actionLoading}
          />
          <span className="settings-toggle-text">
            <span className="settings-toggle-label">
              Двухфакторная аутентификация
            </span>
            <span className={`settings-toggle-state ${enabled ? 'is-on' : 'is-off'}`}>
              {enabled ? 'Включена' : 'Выключена'}
            </span>
          </span>
        </label>

        <div className="settings-form-actions">
          <button
            type="button"
            className="btn btn-primary"
            onClick={apply}
            disabled={!dirty || actionLoading}
          >
            {actionLoading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                Отправка кода...
              </>
            ) : (
              'Применить'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Page ─────────────────────────────────────────────────────────────────────

export default function Settings() {
  const [activeSection, setActiveSection] = useState('account');
  const navigate = useNavigate();
  const auth = useAuth();

  const handleLogoutAfterPasswordChange = () => {
    auth.logout();
    navigate('/auth/login');
  };

  return (
    <div className="settings-page">
      <div className="settings-shell">
        <aside className="settings-sidebar">
          <div className="settings-sidebar-header">
            <h2 className="settings-sidebar-title">⚙️ Настройки</h2>
          </div>

          <nav className="settings-nav">
            {SECTIONS.map((s) => (
              <button
                key={s.key}
                type="button"
                className={`settings-nav-item${activeSection === s.key ? ' settings-nav-item--active' : ''}`}
                onClick={() => setActiveSection(s.key)}
              >
                <span className="settings-nav-icon">{s.icon}</span>
                {s.label}
              </button>
            ))}
          </nav>

          <button
            type="button"
            className="settings-back"
            onClick={() => navigate('/account')}
          >
            ← К личному кабинету
          </button>
        </aside>

        <main className="settings-main">
          {activeSection === 'account' && (
            <AccountSection
              onLogoutAfterPasswordChange={handleLogoutAfterPasswordChange}
            />
          )}
          {activeSection === 'security' && <SecuritySection />}
        </main>
      </div>
    </div>
  );
}
