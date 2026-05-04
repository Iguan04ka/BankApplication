import React from 'react';
import { Link } from 'react-router-dom';
import './Footer.css';

export default function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className="site-footer">
      <div className="footer-inner">

        <div className="footer-brand">
          <span className="footer-brand-icon">🏦</span>
          <span className="footer-brand-text">АтласКредит</span>
          <p className="footer-brand-tagline">
            Надёжное кредитование для&nbsp;вас
          </p>
        </div>

        <div className="footer-links">
          <h4 className="footer-links-title">Навигация</h4>
          <ul className="footer-links-list">
            <li><Link to="/" className="footer-link">Главная</Link></li>
            <li><Link to="/statement" className="footer-link">Подать заявку</Link></li>
            <li><Link to="/account" className="footer-link">Личный кабинет</Link></li>
          </ul>
        </div>

        <div className="footer-links">
          <h4 className="footer-links-title">Аккаунт</h4>
          <ul className="footer-links-list">
            <li><Link to="/auth/login" className="footer-link">Войти</Link></li>
            <li><Link to="/auth/register" className="footer-link">Регистрация</Link></li>
            <li><Link to="/auth/forgot-password" className="footer-link">Восстановление пароля</Link></li>
          </ul>
        </div>

        <div className="footer-links">
          <h4 className="footer-links-title">Документы</h4>
          <ul className="footer-links-list">
            <li><Link to="/legal/privacy-policy" className="footer-link">Политика конфиденциальности</Link></li>
            <li><Link to="/legal/personal-data-policy" className="footer-link">Обработка персональных данных</Link></li>
          </ul>
        </div>

        <div className="footer-contacts">
          <h4 className="footer-links-title">Контакты</h4>
          <ul className="footer-links-list">
            <li className="footer-contact-item">
              <span className="footer-contact-icon">📞</span>
              8 800 100-00-00
            </li>
            <li className="footer-contact-item">
              <span className="footer-contact-icon">✉️</span>
              support@atlaskredit.ru
            </li>
            <li className="footer-contact-item">
              <span className="footer-contact-icon">🕐</span>
              Пн–Пт, 9:00–20:00
            </li>
          </ul>
        </div>

      </div>

      <div className="footer-bottom">
        <span>© {year} АтласКредит. Все права защищены.</span>
        <span className="footer-bottom-sep">·</span>
        <span>Лицензия ЦБ РФ №0000</span>
      </div>
    </footer>
  );
}
