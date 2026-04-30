import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';
import Layout from './shared/ui/Layout/Layout';
import StatementPage from './pages/statement/Statement';
import RegistrationPage from './pages/statement/Registration';
import StatementResumePage from './pages/statement/StatementResume';
import AccountPage from './pages/account/Account';
import LoginPage from './pages/auth/Login.jsx';
import RegisterPage from './pages/auth/Register.jsx';
import HomePage from './pages/Home.jsx';
import { AuthProvider, RequireAuth } from './auth/AuthProvider';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Layout>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/account" element={<RequireAuth><AccountPage /></RequireAuth>} />
            <Route path="/statement" element={<RequireAuth><StatementPage /></RequireAuth>} />
            <Route path="/statement/registration/:statementId" element={<RequireAuth><RegistrationPage /></RequireAuth>} />
            <Route path="/statement/resume/:statementId" element={<RequireAuth><StatementResumePage /></RequireAuth>} />
            <Route path="/auth/login" element={<LoginPage />} />
            <Route path="/auth/register" element={<RegisterPage />} />
          </Routes>
        </Layout>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
