import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import CreatePassword from './pages/CreatePassword';
import Dashboard from './pages/Dashboard';
import Records from './pages/Records';
import RecordDetail from './pages/RecordDetail';
import Reports from './pages/Reports';
import UserManagement from './pages/UserManagement';
import Settings from './pages/Settings';
import ChatPage from './pages/ChatPage';
import PublicChatPage from './pages/PublicChatPage';
import ContributionsOverview from './pages/ContributionsOverview';
import CapturePayment from './pages/CapturePayment';
import PaymentsList from './pages/PaymentsList';
import ProjectsManagement from './pages/ProjectsManagement';
import EventsManagement from './pages/EventsManagement';
import MyContributions from './pages/MyContributions';
import NotificationsPage from './pages/NotificationsPage';
import AdminVerification from './pages/AdminVerification';
import AdminScan from './pages/AdminScan';
import Layout from './components/layout/Layout';
import ProtectedRoute from './components/common/ProtectedRoute';

const defaultRoute = import.meta.env.VITE_DEFAULT_ROUTE;

function App() {
  return (
    <Router
      future={{
        v7_startTransition: true,
        v7_relativeSplatPath: true,
      }}
    >
      <Routes>
        {defaultRoute ? (
          <Route path="/" element={<Navigate to={defaultRoute} replace />} />
        ) : null}
        <Route path="/login" element={<Login />} />
        <Route path="/otp-login" element={<Navigate to="/login" replace />} />
        <Route path="/create-password" element={<CreatePassword />} />
        <Route path="/chat" element={<PublicChatPage />} />
        <Route path="/staff-chat" element={<ProtectedRoute><ChatPage /></ProtectedRoute>} />
        <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="records" element={<Records />} />
          <Route path="records/:id" element={<RecordDetail />} />
          <Route path="reports" element={<Reports />} />
          <Route path="admin/verifications" element={<AdminVerification />} />
          <Route path="admin/scan" element={<AdminScan />} />
          <Route path="users" element={<UserManagement />} />
          <Route path="settings" element={<Settings />} />
          
          {/* Contributions & Projects Routes */}
          <Route path="contributions/overview" element={<ContributionsOverview />} />
          <Route path="contributions/capture" element={<CapturePayment />} />
          <Route path="contributions/payments" element={<PaymentsList />} />
          <Route path="contributions/projects" element={<ProjectsManagement />} />
          <Route path="contributions/events" element={<EventsManagement />} />
          <Route path="contributions/my-contributions" element={<MyContributions />} />
          <Route path="contributions/notifications" element={<NotificationsPage />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;

