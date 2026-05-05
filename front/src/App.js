import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';
import Layout from './shared/ui/Layout/Layout';
import StatementPage from './pages/statement/Statement';
import RegistrationPage from './pages/statement/Registration';
import ConfirmationPage from './pages/statement/Confirmation';
import StatementResumePage from './pages/statement/StatementResume';
import AccountPage from './pages/account/Account';
import SettingsPage from './pages/account/Settings';
import LoginPage from './pages/auth/Login.jsx';
import RegisterPage from './pages/auth/Register.jsx';
import ForgotPasswordPage from './pages/auth/ForgotPassword.jsx';
import ResetPasswordPage from './pages/auth/ResetPassword.jsx';
import HomePage from './pages/Home.jsx';
import PrivacyPolicyPage from './pages/legal/PrivacyPolicy.jsx';
import PersonalDataPolicyPage from './pages/legal/PersonalDataPolicy.jsx';
import { AuthProvider, RequireAuth, RequireAdmin } from './auth/AuthProvider';
import AdminLayout from './pages/admin/AdminLayout';
import AdminDashboard from './pages/admin/Dashboard';
import AdminStatements from './pages/admin/Statements';
import AdminUsers from './pages/admin/Users';
import AdminCredits from './pages/admin/Credits';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          {/* Admin section: own layout, no main public Layout */}
          <Route
            path="/admin"
            element={
              <RequireAdmin>
                <AdminLayout />
              </RequireAdmin>
            }
          >
            <Route index element={<Navigate to="dashboard" replace />} />
            <Route path="dashboard" element={<AdminDashboard />} />
            <Route path="statements" element={<AdminStatements />} />
            <Route path="users" element={<AdminUsers />} />
            <Route path="credits" element={<AdminCredits />} />
          </Route>

          {/* Public + user app inside the standard Layout */}
          <Route
            path="*"
            element={
              <Layout>
                <Routes>
                  <Route path="/" element={<HomePage />} />
                  <Route path="/account" element={<RequireAuth><AccountPage /></RequireAuth>} />
                  <Route path="/account/settings" element={<RequireAuth><SettingsPage /></RequireAuth>} />
                  <Route path="/statement" element={<RequireAuth><StatementPage /></RequireAuth>} />
                  <Route path="/statement/registration/:statementId" element={<RequireAuth><RegistrationPage /></RequireAuth>} />
                  <Route path="/statement/confirmation/:statementId" element={<RequireAuth><ConfirmationPage /></RequireAuth>} />
                  <Route path="/statement/resume/:statementId" element={<RequireAuth><StatementResumePage /></RequireAuth>} />
                  <Route path="/legal/privacy-policy" element={<PrivacyPolicyPage />} />
                  <Route path="/legal/personal-data-policy" element={<PersonalDataPolicyPage />} />
                  <Route path="/auth/login" element={<LoginPage />} />
                  <Route path="/auth/register" element={<RegisterPage />} />
                  <Route path="/auth/forgot-password" element={<ForgotPasswordPage />} />
                  <Route path="/auth/reset-password/:token" element={<ResetPasswordPage />} />
                </Routes>
              </Layout>
            }
          />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
