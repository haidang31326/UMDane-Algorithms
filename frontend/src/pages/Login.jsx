import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { LogIn } from 'lucide-react'

export default function Login({ onLogin, showToast }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      })

      const data = await response.json()
      if (response.ok && data.code === 200) {
        onLogin(data.data)
        navigate('/dashboard')
      } else {
        showToast(data.message || 'Sai tên đăng nhập hoặc mật khẩu!', 'error')
      }
    } catch (err) {
      showToast('Không thể kết nối đến máy chủ!', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-wrapper glass-panel">
      <div className="auth-header">
        <h2>Đăng Nhập</h2>
        <p>Truy cập tài khoản UMDane OJ của bạn</p>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">Tên tài khoản</label>
          <input
            type="text"
            className="form-control"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Nhập username"
            required
            disabled={loading}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Mật khẩu</label>
          <input
            type="password"
            className="form-control"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Nhập mật khẩu"
            required
            disabled={loading}
          />
        </div>

        <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1rem' }} disabled={loading}>
          <LogIn size={18} />
          {loading ? 'Đang xử lý...' : 'Đăng Nhập'}
        </button>
      </form>

      <div className="auth-footer">
        Chưa có tài khoản? <Link to="/register">Đăng ký ngay</Link>
      </div>
    </div>
  )
}
