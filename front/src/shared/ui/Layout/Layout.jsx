import React from 'react';
import Header from '../Header/Header';
import Footer from '../Footer/Footer';
import './Layout.css';

export default function Layout({ children }) {
  return (
    <div className="layout-wrapper">
      <Header />
      <main className="layout-main">
        <div className="layout-container">
          {children}
        </div>
      </main>
      <Footer />
    </div>
  );
}
