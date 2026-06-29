import React from 'react'
import { Link, NavLink } from 'react-router-dom'
import { Code2, LogOut, LogIn, UserPlus } from 'lucide-react'

export default function Navbar({ user, onLogout }) {
  return (
    <nav className="navbar">
      <Link to="/dashboard" className="nav-brand">
        <Code2 size={24} />
        <span>UMDane OJ</span>
      </Link>
      
      <div className="nav-links">
        <NavLink 
          to="/dashboard" 
          className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}
        >
          Bài tập
        </NavLink>

        {user && (
          <NavLink 
            to="/profile" 
            className={({ isActive }) => isActive ? "nav-link active" : "nav-link"}
          >
            Trang cá nhân
          </NavLink>
        )}
        
        {user ? (
          <div className="user-profile">
            <Link to="/profile" className="user-tag" style={{ textDecoration: 'none', cursor: 'pointer' }}>
              {user.username}
              <span className="role-badge">{user.role}</span>
            </Link>
            <button className="btn btn-secondary" onClick={onLogout} style={{ padding: '0.4rem 0.8rem' }}>
              <LogOut size={16} />
              Đăng xuất
            </button>
          </div>
        ) : (
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <Link to="/login" className="btn btn-secondary" style={{ padding: '0.4rem 0.8rem' }}>
              <LogIn size={16} />
              Đăng nhập
            </Link>
            <Link to="/register" className="btn btn-primary" style={{ padding: '0.4rem 0.8rem' }}>
              <UserPlus size={16} />
              Đăng ký
            </Link>
          </div>
        )}
      </div>
    </nav>
  )
}
