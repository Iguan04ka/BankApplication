import React from 'react';
import { Link } from 'react-router-dom';
import './Home.css';

export default function Home() {
  return (
    <div className="home-container">
      <div className="home-content">
        <div className="hero-section">
          <div className="hero-title">
            <span className="hero-icon">🏦</span>
            <h1>АтласКредит — система автоматизации заявок на кредит</h1>
          </div>
          <p className="hero-subtitle">Цифровая платформа для автоматизации процесса кредитования</p>

          <div className="hero-description">
            <p>
              АтласКредит помогает перевести ручной процесс оформления кредитов в единый цифровой поток: приём заявок, валидация данных и предварительные расчёты.
            </p>
          </div>

          <div className="hero-actions">
            <Link to="/auth/login" className="btn btn-primary btn-lg">
              Войти
            </Link>
            <Link to="/auth/register" className="btn btn-outline-primary btn-lg">
              Зарегистрироваться
            </Link>
          </div>
        </div>

        <div className="features-section">
          <h2 className="section-title">Преимущества</h2>

          <div className="features-grid">
            <div className="feature-card">
              <div className="feature-icon">🧾</div>
              <h3>Автоматизация заявок</h3>
              <p>Централизованный приём и обработка заявок — меньше ручной работы и ошибок.</p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">⚡</div>
              <h3>Ускорение обработки</h3>
              <p>Предварительные решения и скоринг ускоряют процесс одобрения.</p>
            </div>

            <div className="feature-card">
              <div className="feature-icon">🔒</div>
              <h3>Защита данных</h3>
              <p>Работа через защищённые токены и безопасные API-запросы.</p>
            </div>
          </div>
        </div>

        <div className="cta-section">
          <div className="cta-content">
            <h2>Готовы перевести процесс в цифровой вид?</h2>
            <p>Начните приём заявок онлайн и сократите время обработки до минут.</p>
            <Link to="/auth/register" className="btn btn-primary btn-lg">
              Начать интеграцию
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}


