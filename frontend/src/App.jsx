import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import Layout from './components/Layout/Layout';
import { Toaster } from 'react-hot-toast';

// Auth Pages
import RoleSelect from './pages/auth/RoleSelect';
import Login from './pages/auth/Login';
import Signup from './pages/auth/Signup';
import ForgotPassword from './pages/auth/ForgotPassword';
import VerifyOTP from './pages/auth/VerifyOTP';
import ResetPassword from './pages/auth/ResetPassword';

// Admin Pages
import AdminDashboard from './pages/admin/Dashboard';
import ManageDoctors from './pages/admin/ManageDoctors';
import ManageStaff from './pages/admin/ManageStaff';
import Approvals from './pages/admin/Approvals';
import AdminProfile from './pages/admin/Profile';

// Doctor Pages
import DoctorDashboard from './pages/doctor/Dashboard';
import DoctorPatients from './pages/doctor/Patients';
import DoctorAlerts from './pages/doctor/Alerts';
import DoctorProfile from './pages/doctor/Profile';

// Staff Pages
import StaffDashboard from './pages/staff/Dashboard';
import StaffPatients from './pages/staff/Patients';
import StaffAlerts from './pages/staff/Alerts';
import StaffProfile from './pages/staff/Profile';

// Patient Pages
import AddPatient from './pages/patients/AddPatient';
import PatientDetail from './pages/patients/PatientDetail';
import ReassessmentChecklist from './pages/patients/ReassessmentChecklist';

// Settings Page
import Settings from './pages/settings/Settings';
import ClinicalGuidelines from './pages/settings/ClinicalGuidelines';
import HelpSupport from './pages/settings/HelpSupport';

// Notifications
import Notifications from './pages/Notifications';

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Toaster position="top-right" toastOptions={{
          style: { background: 'var(--bg-surface)', color: 'var(--text-primary)', border: '1px solid var(--border)' }
        }}/>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<RoleSelect />} />
          <Route path="/login" element={<Login />} />
          <Route path="/login/:role" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/verify-otp" element={<VerifyOTP />} />
          <Route path="/reset-password" element={<ResetPassword />} />

          {/* Protected Routes inside Layout */}
          <Route element={<Layout />}>
            
            {/* Admin Routes */}
            <Route path="/admin/dashboard" element={
              <ProtectedRoute allowedRoles={['admin']}><AdminDashboard /></ProtectedRoute>
            } />
            <Route path="/admin/doctors" element={
              <ProtectedRoute allowedRoles={['admin']}><ManageDoctors /></ProtectedRoute>
            } />
            <Route path="/admin/staff" element={
              <ProtectedRoute allowedRoles={['admin']}><ManageStaff /></ProtectedRoute>
            } />
            <Route path="/admin/approvals" element={
              <ProtectedRoute allowedRoles={['admin']}><Approvals /></ProtectedRoute>
            } />
            <Route path="/admin/profile" element={
              <ProtectedRoute allowedRoles={['admin']}><AdminProfile /></ProtectedRoute>
            } />

            {/* Doctor Routes */}
            <Route path="/doctor/dashboard" element={
              <ProtectedRoute allowedRoles={['doctor']}><DoctorDashboard /></ProtectedRoute>
            } />
            <Route path="/doctor/patients" element={
              <ProtectedRoute allowedRoles={['doctor']}><DoctorPatients /></ProtectedRoute>
            } />
            <Route path="/doctor/alerts" element={
              <ProtectedRoute allowedRoles={['doctor']}><DoctorAlerts /></ProtectedRoute>
            } />
            <Route path="/doctor/profile" element={
              <ProtectedRoute allowedRoles={['doctor']}><DoctorProfile /></ProtectedRoute>
            } />

            {/* Staff Routes */}
            <Route path="/staff/dashboard" element={
              <ProtectedRoute allowedRoles={['staff']}><StaffDashboard /></ProtectedRoute>
            } />
            <Route path="/staff/patients" element={
              <ProtectedRoute allowedRoles={['staff']}><StaffPatients /></ProtectedRoute>
            } />
            <Route path="/staff/alerts" element={
              <ProtectedRoute allowedRoles={['staff']}><StaffAlerts /></ProtectedRoute>
            } />
            <Route path="/staff/profile" element={
              <ProtectedRoute allowedRoles={['staff']}><StaffProfile /></ProtectedRoute>
            } />
            {/* Shared Patient Routes */}
            <Route path="/add-patient" element={
              <ProtectedRoute allowedRoles={['doctor', 'staff']}><AddPatient /></ProtectedRoute>
            } />
            <Route path="/patients/:id" element={
              <ProtectedRoute allowedRoles={['doctor', 'staff']}><PatientDetail /></ProtectedRoute>
            } />
            <Route path="/patients/:id/reassessment" element={
              <ProtectedRoute allowedRoles={['staff']}><ReassessmentChecklist /></ProtectedRoute>
            } />
            
            {/* Shared Settings Route */}
            <Route path="/settings" element={
              <ProtectedRoute allowedRoles={['admin', 'doctor', 'staff']}><Settings /></ProtectedRoute>
            } />
            <Route path="/settings/guidelines" element={
              <ProtectedRoute allowedRoles={['admin', 'doctor', 'staff']}><ClinicalGuidelines /></ProtectedRoute>
            } />
            <Route path="/settings/help" element={
              <ProtectedRoute allowedRoles={['admin', 'doctor', 'staff']}><HelpSupport /></ProtectedRoute>
            } />

            {/* Notifications */}
            <Route path="/notifications" element={
              <ProtectedRoute allowedRoles={['admin', 'doctor', 'staff']}><Notifications /></ProtectedRoute>
            } />

          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
