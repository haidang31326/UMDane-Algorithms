import React, { useState, useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import Navbar from './components/Navbar.jsx'
import Dashboard from './pages/Dashboard.jsx'
import ProblemDetails from './pages/ProblemDetails.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Profile from './pages/Profile.jsx'

export default function App() {
  const [user, setUser] = useState(null)
  const [toast, setToast] = useState(null)

  useEffect(() => {
    const storedUser = localStorage.getItem('umdane_user')
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser))
      } catch (e) {
        localStorage.removeItem('umdane_user')
      }
    }

    // Intercept global fetch to handle 401/403 (token expiration)
    const originalFetch = window.fetch
    window.fetch = async (...args) => {
      try {
        const response = await originalFetch(...args)
        if (response.status === 401 || response.status === 403) {
          const userObj = localStorage.getItem('umdane_user')
          if (userObj) {
            localStorage.removeItem('umdane_user')
            setUser(null)
            showToast('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại!', 'error')
          }
        }
        return response
      } catch (error) {
        throw error
      }
    }

    return () => {
      window.fetch = originalFetch
    }
  }, [])

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => {
      setToast(null)
    }, 4000)
  }

  const handleLogin = (userData) => {
    localStorage.setItem('umdane_user', JSON.stringify(userData))
    setUser(userData)
    showToast(`Chào mừng ${userData.username} trở lại!`)
  }

  const handleLogout = () => {
    localStorage.removeItem('umdane_user')
    setUser(null)
    showToast('Đã đăng xuất thành công!')
  }

  return (
    <Router>
      <div className="app-container">
        <Navbar user={user} onLogout={handleLogout} />
        <main className="main-content">
          <Routes>
            <Route 
              path="/login" 
              element={user ? <Navigate to="/dashboard" /> : <Login onLogin={handleLogin} showToast={showToast} />} 
            />
            <Route 
              path="/register" 
              element={user ? <Navigate to="/dashboard" /> : <Register showToast={showToast} />} 
            />
            <Route 
              path="/dashboard" 
              element={<Dashboard user={user} showToast={showToast} />} 
            />
            <Route 
              path="/profile" 
              element={user ? <Profile user={user} showToast={showToast} /> : <Navigate to="/login" />} 
            />
            <Route 
              path="/problem/:id" 
              element={<ProblemDetails user={user} showToast={showToast} />} 
            />
            <Route path="*" element={<Navigate to="/dashboard" />} />
          </Routes>
        </main>

        {toast && (
          <div 
            className="toast" 
            style={{ 
              backgroundColor: toast.type === 'error' ? 'var(--error-bg)' : 'var(--success-bg)',
              color: toast.type === 'error' ? 'var(--error)' : 'var(--success)',
              border: `1px solid ${toast.type === 'error' ? 'rgba(244, 63, 94, 0.2)' : 'rgba(16, 185, 129, 0.2)'}`
            }}
          >
            <span>{toast.type === 'error' ? '⚠️' : '✅'}</span>
            <span>{toast.message}</span>
          </div>
        )}
      </div>
    </Router>
  )
}
