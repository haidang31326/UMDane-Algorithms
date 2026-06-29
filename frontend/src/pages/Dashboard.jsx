import React, { useState, useEffect, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { Play, ClipboardList, BookOpen, Sparkles, ChevronDown, ChevronUp, CheckCircle2, Trash2, Shuffle } from 'lucide-react'

const getNormalizedTopicName = (rawTopic) => {
  if (!rawTopic) return 'Khác';
  const clean = rawTopic.trim().toLowerCase();

  // Mapping rules
  if (clean.includes('array') || clean.includes('mảng') || clean.includes('list')) {
    return 'Array';
  }
  if (clean.includes('greedy') || clean.includes('tham lam')) {
    return 'Greedy';
  }
  if (clean.includes('dynamic') || clean.includes('programming') || clean === 'dp' || clean.includes('quy hoạch động')) {
    return 'Dynamic Programming';
  }
  if (clean.includes('graph') || clean.includes('tree') || clean.includes('đồ thị') || clean.includes('cây')) {
    return 'Graph & Tree';
  }
  if (clean.includes('math') || clean.includes('toán') || clean.includes('number') || clean.includes('prime')) {
    return 'Mathematics';
  }
  if (clean.includes('string') || clean.includes('chuỗi') || clean.includes('xâu')) {
    return 'String';
  }
  if (clean.includes('sort') || clean.includes('search') || clean.includes('sắp xếp') || clean.includes('tìm kiếm')) {
    return 'Sorting & Searching';
  }

  // Default pretty casing: Capitalize First Letter of Each Word
  return rawTopic
    .trim()
    .split(/\s+/)
    .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
    .join(' ');
};

export default function Dashboard({ user, showToast }) {
  const [problems, setProblems] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [loadingProblems, setLoadingProblems] = useState(true)
  const [loadingSubmissions, setLoadingSubmissions] = useState(true)
  const [activeFilter, setActiveFilter] = useState('ALL')
  const [searchTerm, setSearchTerm] = useState('')

  // AI Generator state
  const [topic, setTopic] = useState('Greedy')
  const [keyword, setKeyword] = useState('tiktok')
  const [difficulty, setDifficulty] = useState('MEDIUM')
  const [generating, setGenerating] = useState(false)

  const [expandedTopics, setExpandedTopics] = useState({})

  const toggleTopic = (topicName) => {
    setExpandedTopics(prev => ({
      ...prev,
      [topicName]: prev[topicName] === false ? true : false
    }))
  }

  const solvedProblemIds = useMemo(() => {
    const solved = new Set()
    submissions.forEach(sub => {
      if (sub.status === 'ACCEPTED' && (!user || sub.userId === user.id)) {
        solved.add(sub.problemId)
      }
    })
    return solved
  }, [submissions, user])

  const filteredProblems = useMemo(() => {
    return problems.filter(prob => {
      const isSolved = solvedProblemIds.has(prob.id)
      
      // Filter by AC/UNSOLVED status
      if (activeFilter === 'AC' && !isSolved) return false
      if (activeFilter === 'UNSOLVED' && isSolved) return false
      
      // Filter by topic search term
      if (searchTerm.trim() !== '') {
        const query = searchTerm.toLowerCase().trim()
        const probTopic = (prob.topic || '').toLowerCase()
        const probTitle = (prob.title || '').toLowerCase()
        return probTopic.includes(query) || probTitle.includes(query)
      }
      
      return true
    })
  }, [problems, solvedProblemIds, activeFilter, searchTerm])

  const groupedProblems = useMemo(() => {
    const groups = filteredProblems.reduce((acc, prob) => {
      const t = getNormalizedTopicName(prob.topic)
      if (!acc[t]) {
        acc[t] = []
      }
      acc[t].push(prob)
      return acc
    }, {})

    const getDiffWeight = (d) => {
      switch (d?.toUpperCase()) {
        case 'EASY': return 1
        case 'MEDIUM': return 2
        case 'HARD': return 3
        default: return 2
      }
    }

    Object.keys(groups).forEach(t => {
      groups[t].sort((a, b) => getDiffWeight(a.difficulty) - getDiffWeight(b.difficulty))
    })

    return groups
  }, [filteredProblems])

  const uniqueSubmissions = useMemo(() => {
    const latestMap = new Map()
    // submissions is sorted desc (latest first)
    submissions.forEach(sub => {
      if (!latestMap.has(sub.problemId)) {
        latestMap.set(sub.problemId, sub)
      }
    })
    return Array.from(latestMap.values())
  }, [submissions])

  const fetchProblems = async () => {
    try {
      const response = await fetch('/api/problems', {
        headers: user ? { 'Authorization': `Bearer ${user.token}` } : {}
      })
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

  const RANDOM_TOPICS = [
    'Greedy', 
    'Dynamic Programming', 
    'Array', 
    'String', 
    'Graph & Tree', 
    'Mathematics', 
    'Sorting & Searching', 
    'Hash Table', 
    'Binary Search', 
    'Sliding Window', 
    'Two Pointers', 
    'Stack & Queue',
    'Recursion',
    'Bit Manipulation'
  ];

  const RANDOM_KEYWORDS = [
    'TikTok',
    'Grab',
    'Shopee',
    'Netflix',
    'Spotify',
    'SpaceX',
    'NASA',
    'Tesla',
    'Facebook',
    'Instagram',
    'Youtube',
    'ChatGPT',
    'Minecraft',
    'League of Legends',
    'Valorant',
    'FIFA World Cup',
    'Apple Store',
    'Google Maps',
    'Steam Game',
    'Tiki',
    'Garena',
    'Momo',
    'Binance',
    'Lazada'
  ];

  const handleGenerate = async (optTopic, optKeyword) => {
    const finalTopic = optTopic || topic;
    const finalKeyword = optKeyword || keyword;

    if (!finalTopic.trim() || !finalKeyword.trim()) {
      showToast('Vui lòng nhập đầy đủ chủ đề và từ khóa!', 'error')
      return
    }

    setGenerating(true)
    try {
      const response = await fetch(`/api/problems/generate?topic=${encodeURIComponent(finalTopic)}&keyword=${encodeURIComponent(finalKeyword)}&difficulty=${encodeURIComponent(difficulty)}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${user.token}`
        }
      })

      const data = await response.json()
      if (response.ok && data.code === 200) {
        showToast('Đã tạo đề bài và test cases thành công!')
        fetchProblems() // Refresh problems list
      } else {
        showToast(data.message || 'Lỗi khi tạo đề bài bằng AI!', 'error')
      }
    } catch (err) {
      showToast('Không thể kết nối đến máy chủ!', 'error')
    } finally {
      setGenerating(false)
    }
  }

  const handleRandomGenerate = () => {
    const randTopic = RANDOM_TOPICS[Math.floor(Math.random() * RANDOM_TOPICS.length)];
    const randKeyword = RANDOM_KEYWORDS[Math.floor(Math.random() * RANDOM_KEYWORDS.length)];
    
    setTopic(randTopic);
    setKeyword(randKeyword);
    
    // Trigger generation immediately
    handleGenerate(randTopic, randKeyword);
  };

  const handleDelete = async (problemId) => {
    if (!window.confirm("Bạn có chắc chắn muốn xóa bài tập này không? Hành động này sẽ xóa cả test cases và submissions liên quan.")) {
      return
    }

    try {
      const response = await fetch(`/api/problems/${problemId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${user.token}`
        }
      })

      const data = await response.json()
      if (response.ok && data.code === 200) {
        showToast('Xóa bài tập thành công!')
        fetchProblems() // Refresh problems list
        fetchSubmissions() // Refresh submissions list
      } else {
        showToast(data.message || 'Lỗi khi xóa bài tập!', 'error')
      }
    } catch (err) {
      showToast('Không thể kết nối đến máy chủ để xóa!', 'error')
    }
  }

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
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      
      {/* AI Problem Generator Form - Only shown for logged-in users */}
      {user && (
        <div className="glass-panel card" style={{ marginBottom: 0 }}>
          <h2 className="card-title" style={{ color: '#fbbf24' }}>
            <Sparkles size={20} />
            Tạo bài tập tự động bằng AI (Gemini)
          </h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '1.25rem' }}>
            Nhập chủ đề thuật toán và bối cảnh bất kỳ để AI tự động biên soạn đề bài lập trình tiếng Việt và sinh test cases kiểm thử.
          </p>
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div className="form-group" style={{ marginBottom: 0, flex: 1, minWidth: '200px' }}>
              <label className="form-label">Chủ đề thuật toán (Topic)</label>
              <input 
                type="text" 
                className="form-control" 
                value={topic} 
                onChange={(e) => setTopic(e.target.value)} 
                placeholder="Ví dụ: Greedy, Dynamic Programming, Array..."
                disabled={generating}
              />
            </div>
            <div className="form-group" style={{ marginBottom: 0, flex: 1, minWidth: '200px' }}>
              <label className="form-label">Từ khóa/Ngữ cảnh (Keyword)</label>
              <input 
                type="text" 
                className="form-control" 
                value={keyword} 
                onChange={(e) => setKeyword(e.target.value)} 
                placeholder="Ví dụ: tiktok, shopee, grab..."
                disabled={generating}
              />
            </div>
            <div className="form-group" style={{ marginBottom: 0, flex: 1, minWidth: '150px' }}>
              <label className="form-label">Độ khó (Difficulty)</label>
              <select 
                className="form-control" 
                value={difficulty} 
                onChange={(e) => setDifficulty(e.target.value)} 
                disabled={generating}
                style={{ 
                  background: 'rgba(30, 41, 59, 0.7)', 
                  color: 'var(--text-main)', 
                  border: '1px solid var(--border-color)', 
                  height: '42px', 
                  padding: '0 0.75rem', 
                  borderRadius: '6px',
                  width: '100%'
                }}
              >
                <option value="EASY">Dễ (EASY)</option>
                <option value="MEDIUM">Trung bình (MEDIUM)</option>
                <option value="HARD">Khó (HARD)</option>
              </select>
            </div>
            <button 
              className="btn btn-secondary" 
              onClick={handleRandomGenerate} 
              disabled={generating}
              style={{ 
                height: '42px', 
                borderColor: '#10b981',
                color: '#10b981',
                background: 'transparent',
                display: 'flex',
                alignItems: 'center',
                gap: '0.35rem',
                padding: '0 1.25rem',
                fontSize: '0.85rem'
              }}
              title="AI tự chọn Topic và Keyword ngẫu nhiên dựa trên độ khó đã chọn"
            >
              <Shuffle size={14} />
              {generating ? 'Đang tạo...' : 'Sinh ngẫu nhiên'}
            </button>
            <button 
              className="btn btn-primary" 
              onClick={() => handleGenerate()} 
              disabled={generating}
              style={{ 
                height: '42px', 
                background: 'linear-gradient(135deg, #a78bfa, #4f46e5)',
                boxShadow: '0 0 15px rgba(167, 139, 250, 0.3)'
              }}
            >
              <Sparkles size={16} />
              {generating ? 'Đang thiết kế đề bài...' : 'Tạo bài tập bằng AI'}
            </button>
          </div>
        </div>
      )}

      <div className="grid-2">
        {/* Problems List */}
        <div className="glass-panel card">
          <h2 className="card-title">
            <BookOpen size={20} style={{ color: '#3b82f6' }} />
            Danh sách bài tập
          </h2>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              {user && (
                <>
                  <button 
                    onClick={() => setActiveFilter('ALL')}
                    className="btn"
                    style={{ 
                      padding: '0.4rem 0.85rem', 
                      fontSize: '0.8rem', 
                      borderRadius: '6px',
                      background: activeFilter === 'ALL' ? 'linear-gradient(135deg, #3b82f6, #1d4ed8)' : 'rgba(255, 255, 255, 0.05)',
                      border: activeFilter === 'ALL' ? 'none' : '1px solid rgba(255, 255, 255, 0.1)',
                      color: activeFilter === 'ALL' ? '#ffffff' : 'var(--text-muted)',
                      cursor: 'pointer'
                    }}
                  >
                    Tất cả
                  </button>
                  <button 
                    onClick={() => setActiveFilter('AC')}
                    className="btn"
                    style={{ 
                      padding: '0.4rem 0.85rem', 
                      fontSize: '0.8rem', 
                      borderRadius: '6px',
                      background: activeFilter === 'AC' ? 'linear-gradient(135deg, #10b981, #047857)' : 'rgba(255, 255, 255, 0.05)',
                      border: activeFilter === 'AC' ? 'none' : '1px solid rgba(255, 255, 255, 0.1)',
                      color: activeFilter === 'AC' ? '#ffffff' : 'var(--text-muted)',
                      cursor: 'pointer'
                    }}
                  >
                    Đã AC ({solvedProblemIds.size})
                  </button>
                  <button 
                    onClick={() => setActiveFilter('UNSOLVED')}
                    className="btn"
                    style={{ 
                      padding: '0.4rem 0.85rem', 
                      fontSize: '0.8rem', 
                      borderRadius: '6px',
                      background: activeFilter === 'UNSOLVED' ? 'linear-gradient(135deg, #f59e0b, #b45309)' : 'rgba(255, 255, 255, 0.05)',
                      border: activeFilter === 'UNSOLVED' ? 'none' : '1px solid rgba(255, 255, 255, 0.1)',
                      color: activeFilter === 'UNSOLVED' ? '#ffffff' : 'var(--text-muted)',
                      cursor: 'pointer'
                    }}
                  >
                    Chưa giải
                  </button>
                </>
              )}
            </div>
            
            <div style={{ flex: 1, minWidth: '180px', display: 'flex', justifyContent: 'flex-end' }}>
              <input 
                type="text" 
                className="form-control" 
                value={searchTerm} 
                onChange={(e) => setSearchTerm(e.target.value)} 
                placeholder="Tìm chủ đề hoặc tên bài..."
                style={{ 
                  padding: '0.4rem 0.75rem', 
                  fontSize: '0.8rem', 
                  height: '34px',
                  background: 'rgba(255, 255, 255, 0.03)',
                  borderColor: 'rgba(255, 255, 255, 0.1)',
                  borderRadius: '6px',
                  width: '100%',
                  maxWidth: '240px'
                }}
              />
            </div>
          </div>
          {loadingProblems ? (
            <p style={{ color: 'var(--text-muted)' }}>Đang tải đề bài...</p>
          ) : Object.keys(groupedProblems).length === 0 ? (
            <p style={{ color: 'var(--text-muted)' }}>Hiện chưa có bài tập nào.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {Object.entries(groupedProblems).map(([topicName, topicProbs]) => {
                const isExpanded = expandedTopics[topicName] !== false;
                return (
                  <div key={topicName} className="glass-panel" style={{ padding: 0, overflow: 'hidden', border: '1px solid rgba(255,255,255,0.05)', background: 'rgba(255,255,255,0.01)', borderRadius: '8px' }}>
                    {/* Accordion Header */}
                    <div 
                      onClick={() => toggleTopic(topicName)}
                      style={{ 
                        display: 'flex', 
                        justifyContent: 'space-between', 
                        alignItems: 'center', 
                        padding: '0.85rem 1.1rem', 
                        cursor: 'pointer', 
                        background: 'rgba(255, 255, 255, 0.02)',
                        userSelect: 'none',
                        transition: 'background 0.2s'
                      }}
                      className="accordion-header-hover"
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <span style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-main)' }}>{topicName}</span>
                        <span className="topic-badge" style={{ fontSize: '0.7rem', padding: '0.1rem 0.4rem', borderRadius: '4px' }}>
                          {topicProbs.length} bài tập
                        </span>
                      </div>
                      <div style={{ color: 'var(--text-muted)' }}>
                        {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                      </div>
                    </div>

                    {/* Accordion Table Content */}
                    {isExpanded && (
                      <div style={{ padding: '0.5rem 1.1rem 1.1rem 1.1rem', borderTop: '1px solid rgba(255,255,255,0.03)' }}>
                        <table className="data-table" style={{ margin: 0 }}>
                          <thead>
                            <tr>
                              <th>Tên bài tập</th>
                              <th>Độ khó</th>
                              <th>Thao tác</th>
                            </tr>
                          </thead>
                          <tbody>
                            {topicProbs.map((prob) => (
                              <tr key={prob.id}>
                                <td style={{ fontWeight: 600 }}>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    {solvedProblemIds.has(prob.id) && (
                                      <CheckCircle2 size={16} style={{ color: '#10b981', flexShrink: 0 }} title="Đã giải thành công" />
                                    )}
                                    {prob.title}
                                  </div>
                                </td>
                                <td>
                                  <span className={`difficulty-badge diff-${(prob.difficulty || 'medium').toLowerCase()}`} style={{
                                    display: 'inline-block',
                                    padding: '0.2rem 0.5rem',
                                    borderRadius: '4px',
                                    fontSize: '0.75rem',
                                    fontWeight: 700,
                                    backgroundColor: prob.difficulty === 'EASY' ? 'rgba(16, 185, 129, 0.15)' : prob.difficulty === 'HARD' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                                    color: prob.difficulty === 'EASY' ? '#10b981' : prob.difficulty === 'HARD' ? '#ef4444' : '#f59e0b',
                                    border: prob.difficulty === 'EASY' ? '1px solid rgba(16, 185, 129, 0.3)' : prob.difficulty === 'HARD' ? '1px solid rgba(239, 68, 68, 0.3)' : '1px solid rgba(245, 158, 11, 0.3)'
                                  }}>
                                    {prob.difficulty || 'MEDIUM'}
                                  </span>
                                </td>
                                <td>
                                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                                    <Link to={`/problem/${prob.id}`} className="btn btn-primary" style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}>
                                      <Play size={12} />
                                      Làm bài
                                    </Link>
                                    {user && (
                                      <button 
                                        className="btn btn-secondary" 
                                        onClick={() => handleDelete(prob.id)} 
                                        style={{ padding: '0.35rem 0.5rem', borderColor: 'rgba(239, 68, 68, 0.3)', color: '#ef4444', background: 'transparent' }}
                                        title="Xóa bài tập"
                                      >
                                        <Trash2 size={12} />
                                      </button>
                                    )}
                                  </div>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
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
          ) : uniqueSubmissions.length === 0 ? (
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
                  {uniqueSubmissions.slice(0, 10).map((sub) => (
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
    </div>
  )
}
