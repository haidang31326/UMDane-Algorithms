import React, { useState, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { Calendar, Flame, Trophy, Award, CheckCircle2, ChevronRight, Activity, Clock, Shield } from 'lucide-react'

export default function Profile({ user, showToast }) {
  const [submissions, setSubmissions] = useState([])
  const [problems, setProblems] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [subRes, probRes] = await Promise.all([
          fetch('/api/submissions', {
            headers: user ? { 'Authorization': `Bearer ${user.token}` } : {}
          }),
          fetch('/api/problems', {
            headers: user ? { 'Authorization': `Bearer ${user.token}` } : {}
          })
        ])

        const subData = await subRes.json()
        const probData = await probRes.json()

        if (subRes.ok && subData.code === 200) {
          setSubmissions(subData.data)
        }
        if (probRes.ok && probData.code === 200) {
          setProblems(probData.data)
        }
      } catch (err) {
        showToast('Lỗi khi tải thông tin cá nhân!', 'error')
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [user])

  // Get user's submissions
  const userSubmissions = useMemo(() => {
    return submissions.filter(sub => sub.userId === user.id)
  }, [submissions, user.id])

  // Get unique solved problem IDs
  const solvedProblemIds = useMemo(() => {
    const solved = new Set()
    userSubmissions.forEach(sub => {
      if (sub.status === 'ACCEPTED') {
        solved.add(sub.problemId)
      }
    })
    return solved
  }, [userSubmissions])

  // Calculate stats by difficulty
  const difficultyStats = useMemo(() => {
    let easy = 0, medium = 0, hard = 0
    solvedProblemIds.forEach(id => {
      const prob = problems.find(p => p.id === id)
      if (prob) {
        const diff = prob.difficulty?.toUpperCase()
        if (diff === 'EASY') easy++
        else if (diff === 'HARD') hard++
        else medium++
      }
    })
    const total = solvedProblemIds.size
    return { easy, medium, hard, total }
  }, [solvedProblemIds, problems])

  // Calculate Streaks
  const streakStats = useMemo(() => {
    const acDates = new Set()
    userSubmissions.forEach(sub => {
      if (sub.status === 'ACCEPTED' && sub.createdAt) {
        const dateStr = new Date(sub.createdAt).toDateString()
        acDates.add(dateStr)
      }
    })

    if (acDates.size === 0) {
      return { currentStreak: 0, maxStreak: 0 }
    }

    const sortedDates = Array.from(acDates).map(d => new Date(d)).sort((a, b) => a - b)

    // Max Streak
    let maxStreak = 1
    let currentRun = 1
    for (let i = 1; i < sortedDates.length; i++) {
      const diffTime = Math.abs(sortedDates[i] - sortedDates[i - 1])
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
      if (diffDays === 1) {
        currentRun++
        if (currentRun > maxStreak) {
          maxStreak = currentRun
        }
      } else if (diffDays > 1) {
        currentRun = 1
      }
    }

    // Current Streak
    let currentStreak = 0
    const oneDay = 24 * 60 * 60 * 1000
    const today = new Date(new Date().toDateString())
    const yesterday = new Date(today.getTime() - oneDay)

    let checkDate = today
    if (!acDates.has(today.toDateString())) {
      if (acDates.has(yesterday.toDateString())) {
        checkDate = yesterday
      } else {
        checkDate = null
      }
    }

    if (checkDate) {
      currentStreak = 1
      let previousDate = new Date(checkDate.getTime() - oneDay)
      while (acDates.has(previousDate.toDateString())) {
        currentStreak++
        previousDate = new Date(previousDate.getTime() - oneDay)
      }
    }

    return { currentStreak, maxStreak }
  }, [userSubmissions])

  // Generate last 365 days contribution data aligned to Sunday
  const calendarData = useMemo(() => {
    const dailyCount = {}
    userSubmissions.forEach(sub => {
      if (sub.status === 'ACCEPTED' && sub.createdAt) {
        const dateStr = new Date(sub.createdAt).toDateString()
        dailyCount[dateStr] = (dailyCount[dateStr] || 0) + 1
      }
    })

    const dates = []
    const today = new Date()
    const startDate = new Date()
    startDate.setDate(today.getDate() - 364) // 52 weeks
    const startDay = startDate.getDay()
    startDate.setDate(startDate.getDate() - startDay) // Align to Sunday

    const currentDate = new Date(startDate)
    while (currentDate <= today) {
      const dateStr = currentDate.toDateString()
      const count = dailyCount[dateStr] || 0
      
      let level = 0
      if (count > 0 && count <= 2) level = 1
      else if (count > 2 && count <= 4) level = 2
      else if (count > 4 && count <= 6) level = 3
      else if (count > 6) level = 4

      dates.push({
        dateStr: currentDate.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }),
        count,
        level
      })
      currentDate.setDate(currentDate.getDate() + 1)
    }
    return dates
  }, [userSubmissions])

  // Get recently solved problems list
  const recentSolved = useMemo(() => {
    const solvedSet = new Set()
    const list = []
    // userSubmissions is sorted by ID desc (latest first)
    userSubmissions.forEach(sub => {
      if (sub.status === 'ACCEPTED') {
        if (!solvedSet.has(sub.problemId)) {
          solvedSet.add(sub.problemId)
          const prob = problems.find(p => p.id === sub.problemId)
          if (prob) {
            list.push({
              id: prob.id,
              title: prob.title,
              difficulty: prob.difficulty,
              solvedAt: new Date(sub.createdAt).toLocaleDateString('vi-VN')
            })
          }
        }
      }
    })
    return list.slice(0, 5)
  }, [userSubmissions, problems])

  // Calculate percentages for difficulty bars
  const totalCount = problems.length || 1
  const easyTotal = problems.filter(p => p.difficulty?.toUpperCase() === 'EASY').length || 1
  const mediumTotal = problems.filter(p => p.difficulty?.toUpperCase() === 'MEDIUM' || !p.difficulty).length || 1
  const hardTotal = problems.filter(p => p.difficulty?.toUpperCase() === 'HARD').length || 1

  const easyPercent = ((difficultyStats.easy / easyTotal) * 100).toFixed(0)
  const mediumPercent = ((difficultyStats.medium / mediumTotal) * 100).toFixed(0)
  const hardPercent = ((difficultyStats.hard / hardTotal) * 100).toFixed(0)

  if (loading) {
    return <p style={{ color: 'var(--text-muted)' }}>Đang tải thông tin cá nhân...</p>
  }

  return (
    <div className="streak-container">
      
      {/* Title */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <Award size={28} style={{ color: '#fbbf24' }} />
        <h1 style={{ fontSize: '1.75rem', fontWeight: 800, margin: 0 }}>Trang cá nhân & Tiến độ</h1>
      </div>

      <div className="profile-grid">
        
        {/* Left Column: User Card & Stats */}
        <div className="glass-panel profile-card">
          <div className="avatar-circle">
            {user.username.slice(0, 2).toUpperCase()}
          </div>
          <div className="profile-name">{user.username}</div>
          <div className="profile-role">
            <Shield size={10} style={{ marginRight: '4px', display: 'inline' }} />
            {user.role}
          </div>

          <div className="stats-mini-grid" style={{ marginBottom: '2rem' }}>
            <div className="stat-mini-box">
              <span className="stat-mini-val">{difficultyStats.total}</span>
              <span className="stat-mini-lbl">Đã giải</span>
            </div>
            <div className="stat-mini-box">
              <span className="stat-mini-val">{userSubmissions.length}</span>
              <span className="stat-mini-lbl">Bài nộp</span>
            </div>
          </div>

          {/* Difficulty breakdown */}
          <div style={{ width: '100%', textAlign: 'left' }}>
            <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '1rem', color: 'var(--text-muted)' }}>Tiến trình giải bài</h3>
            
            <div className="diff-bar-container">
              <div className="diff-bar-item">
                <div className="diff-bar-lbl">
                  <span style={{ color: '#10b981', fontWeight: 600 }}>Dễ</span>
                  <span>{difficultyStats.easy}/{easyTotal} ({easyPercent}%)</span>
                </div>
                <div className="diff-bar-track">
                  <div className="diff-bar-fill" style={{ width: `${easyPercent}%`, backgroundColor: '#10b981' }}></div>
                </div>
              </div>

              <div className="diff-bar-item">
                <div className="diff-bar-lbl">
                  <span style={{ color: '#f59e0b', fontWeight: 600 }}>Trung bình</span>
                  <span>{difficultyStats.medium}/{mediumTotal} ({mediumPercent}%)</span>
                </div>
                <div className="diff-bar-track">
                  <div className="diff-bar-fill" style={{ width: `${mediumPercent}%`, backgroundColor: '#f59e0b' }}></div>
                </div>
              </div>

              <div className="diff-bar-item">
                <div className="diff-bar-lbl">
                  <span style={{ color: '#ef4444', fontWeight: 600 }}>Khó</span>
                  <span>{difficultyStats.hard}/{hardTotal} ({hardPercent}%)</span>
                </div>
                <div className="diff-bar-track">
                  <div className="diff-bar-fill" style={{ width: `${hardPercent}%`, backgroundColor: '#ef4444' }}></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Coding Streak Calendar Grid */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          {/* Streak calendar card */}
          <div className="glass-panel card streak-calendar-panel" style={{ marginBottom: 0 }}>
            <div className="streak-header">
              <h2 className="card-title" style={{ margin: 0, fontSize: '1.15rem' }}>
                <Calendar size={18} style={{ color: '#3b82f6' }} />
                Lịch sử hoạt động (365 ngày qua)
              </h2>
              <div className="streak-stats-row">
                <div className="streak-stat-item">
                  <Flame size={16} style={{ color: '#ef4444' }} />
                  Streak hiện tại: <span>{streakStats.currentStreak} ngày</span>
                </div>
                <div className="streak-stat-item">
                  <Trophy size={16} style={{ color: '#fbbf24' }} />
                  Kỷ lục: <span>{streakStats.maxStreak} ngày</span>
                </div>
              </div>
            </div>

            {/* Grid Container */}
            <div className="streak-grid-wrapper">
              <div className="streak-grid">
                {calendarData.map((day, idx) => (
                  <div
                    key={idx}
                    className={`streak-square level-${day.level}`}
                    title={`${day.dateStr}: ${day.count} bài giải thành công (AC)`}
                  />
                ))}
              </div>
            </div>

            {/* Legend */}
            <div className="streak-legend">
              <span>Ít</span>
              <div className="legend-square level-0" />
              <div className="legend-square level-1" />
              <div className="legend-square level-2" />
              <div className="legend-square level-3" />
              <div className="legend-square level-4" />
              <span>Nhiều</span>
            </div>
          </div>

          {/* Recent solved list */}
          <div className="glass-panel card" style={{ marginBottom: 0 }}>
            <h2 className="card-title" style={{ fontSize: '1.15rem' }}>
              <Activity size={18} style={{ color: '#a78bfa' }} />
              Các bài vừa giải thành công gần đây
            </h2>
            {recentSolved.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>
                Bạn chưa giải thành công bài tập nào. Hãy bắt đầu luyện tập ngay!
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {recentSolved.map((prob) => (
                  <div
                    key={prob.id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '0.75rem 1rem',
                      background: 'rgba(255, 255, 255, 0.02)',
                      border: '1px solid var(--border-color)',
                      borderRadius: '8px',
                      fontSize: '0.9rem'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                      <CheckCircle2 size={16} style={{ color: '#10b981', flexShrink: 0 }} />
                      <span style={{ fontWeight: 600 }}>{prob.title}</span>
                      <span className={`difficulty-badge diff-${(prob.difficulty || 'medium').toLowerCase()}`} style={{ fontSize: '0.7rem', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>
                        {prob.difficulty || 'MEDIUM'}
                      </span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', color: 'var(--text-muted)' }}>
                      <span style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', fontSize: '0.8rem' }}>
                        <Clock size={12} />
                        {prob.solvedAt}
                      </span>
                      <Link to={`/problem/${prob.id}`} className="btn btn-secondary" style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem', display: 'flex', alignItems: 'center' }}>
                        Xem lại
                        <ChevronRight size={12} />
                      </Link>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

        </div>

      </div>
    </div>
  )
}
