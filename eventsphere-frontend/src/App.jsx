import { Routes, Route, Navigate } from 'react-router-dom'
import Navbar from './components/Navbar'
import EventsPage from './pages/EventsPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import Verify2FAPage from './pages/Verify2FAPage'
import AdminDashboard from './pages/AdminDashboard'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'

export default function App() {
  return (
    <AuthProvider>
      <Navbar />
      <main style={{ minHeight: 'calc(100vh - 60px)' }}>
        <Routes>
          <Route path="/" element={<Navigate to="/events" replace />} />
          <Route path="/events" element={<EventsPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-2fa" element={<Verify2FAPage />} />
          <Route path="/admin" element={
            <ProtectedRoute><AdminDashboard /></ProtectedRoute>
          } />
          <Route path="*" element={<Navigate to="/events" replace />} />
        </Routes>
      </main>
    </AuthProvider>
  )
}
