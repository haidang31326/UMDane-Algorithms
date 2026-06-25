import React, { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Play, ClipboardList, BookOpen } from 'lucide-react'

export default function Dashboard({ user, showToast }) {
  const [problems, setProblems] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [loadingProblems, setLoadingProblems] = useState(true)
  const [loadingSubmissions, setLoadingSubmissions] = useState(true)

  const fetchProblems = async () => {
    try {
      const response = await fetch('/api/problems')
      const data = await response.json()
      if (response.ok && data.code === 200) {
        setProblems(data.data)
      } else {
        showToast('Không thể lấy danh sách bài tập!', 'error')
      }
    } catch (err) {
      showToast('Lỗi khi tải danh sách bài tập!', 'error')
    } finally {
      setLoadingProblems(false)
    }
  }

  const fetchSubmissions = async () => {
    try {
      const response = await fetch('/api/submissions', {
        headers: user ? { 'Authorization': `Bearer ${user.token}` } : {}
      })
      const data = await response.json()
      if (response.ok && data.code === 200) {
        setSubmissions(data.data)
      }
    } catch (err) {
      console.error('Error fetching submissions:', err)
    } finally {
      setLoadingSubmissions(false)
    }
  }

  useEffect(() => {
    fetchProblems()
    fetchSubmissions()

    // Poll submissions every 3 seconds to support live updates
    const interval = setInterval(() => {
      fetchSubmissions()
    }, 3000)

    return () => clearInterval(interval)
  }, [user])

  const getProblemTitle = (problemId) => {
    const prob = problems.find(p => p.id === problemId)
    return prob ? prob.title : `Bài toán #${problemId}`
  }

  const formatTime = (timeStr) => {
    if (!timeStr) return ''
    const date = new Date(timeStr)
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  }

  return (
    <div className="grid-2">
      {/* Problems List */}
      <div className="glass-panel card">
        <h2 className="card-title">
          <BookOpen size={20} style={{ color: '#3b82f6' }} />
          Danh sách bài tập
        </h2>
        {loadingProblems ? (
          <p style={{ color: 'var(--text-muted)' }}>Đang tải đề bài...</p>
        ) : problems.length === 0 ? (
          <p style={{ color: 'var(--text-muted)' }}>Hiện chưa có bài tập nào.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Tên bài tập</th>
                <th>Chủ đề</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {problems.map((prob) => (
                <tr key={prob.id}>
                  <td style={{ fontWeight: 600 }}>{prob.title}</td>
                  <td>
                    <span className="topic-badge">{prob.topic}</span>
                  </td>
                  <td>
                    <Link to={`/problem/${prob.id}`} className="btn btn-primary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}>
                      <Play size={12} />
                      Làm bài
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Submissions List */}
      <div className="glass-panel card">
        <h2 className="card-title">
          <ClipboardList size={20} style={{ color: '#a78bfa' }} />
          Nhật ký chấm bài (Real-time)
        </h2>
        {loadingSubmissions ? (
          <p style={{ color: 'var(--text-muted)' }}>Đang tải trạng thái...</p>
        ) : submissions.length === 0 ? (
          <p style={{ color: 'var(--text-muted)' }}>Chưa có bài nộp nào trên hệ thống.</p>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Mã bài</th>
                  <th>Thời gian</th>
                  <th>Trạng thái</th>
                  <th>Runtime</th>
                </tr>
              </thead>
              <tbody>
                {submissions.slice(0, 10).map((sub) => (
                  <tr key={sub.id}>
                    <td>
                      <Link to={`/problem/${sub.problemId}`} style={{ color: 'var(--text-main)', textDecoration: 'none', fontWeight: 600 }}>
                        {getProblemTitle(sub.problemId)}
                      </Link>
                    </td>
                    <td style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      {formatTime(sub.createdAt)}
                    </td>
                    <td>
                      <span className={`status-badge status-${sub.status}`}>
                        {sub.status === 'ACCEPTED' ? 'AC' :
                         sub.status === 'WRONG_ANSWER' ? 'WA' :
                         sub.status === 'TIME_LIMIT_EXCEEDED' ? 'TLE' :
                         sub.status === 'COMPILE_ERROR' ? 'CE' :
                         sub.status === 'RUNTIME_ERROR' ? 'RE' : 'PENDING'}
                      </span>
                    </td>
                    <td style={{ fontFamily: 'var(--font-mono)', fontSize: '0.85rem' }}>
                      {sub.status === 'PENDING' ? '-' : `${sub.runtimeMs} ms`}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
