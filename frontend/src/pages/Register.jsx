import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { UserPlus } from 'lucide-react'

export default function Register({ showToast }) {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)

    try {
      const response = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password })
      })

      const data = await response.json()
      if (response.ok && data.code === 200) {
        showToast('Đăng ký thành công! Hãy đăng nhập.')
        navigate('/login')
      } else {
        showToast(data.message || 'Lỗi đăng ký tài khoản!', 'error')
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
        <h2>Đăng Ký</h2>
        <p>Tạo tài khoản UMDane OJ của bạn</p>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">Tên tài khoản</label>
          <input
            type="text"
            className="form-control"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="Từ 3 đến 50 ký tự"
            required
            disabled={loading}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Email</label>
          <input
            type="email"
            className="form-control"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="email@example.com"
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
            placeholder="Ít nhất 6 ký tự"
            required
            disabled={loading}
          />
        </div>

        <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '1rem' }} disabled={loading}>
          <UserPlus size={18} />
          {loading ? 'Đang xử lý...' : 'Đăng Ký'}
        </button>
      </form>

      <div className="auth-footer">
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </div>
    </div>
  )
}
