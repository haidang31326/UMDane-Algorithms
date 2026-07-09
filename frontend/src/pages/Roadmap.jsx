import React, { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Trophy, Lock, Play, Sparkles, CheckCircle2, ChevronRight } from 'lucide-react'

export default function Roadmap({ user, showToast }) {
  const [nodes, setNodes] = useState([])
  const [loading, setLoading] = useState(true)
  const [generatingNodeId, setGeneratingNodeId] = useState(null)
  const navigate = useNavigate()

  const fetchRoadmap = async () => {
    try {
      const response = await fetch('/api/roadmap/nodes', {
        headers: user ? { 'Authorization': `Bearer ${user.token}` } : {}
      })
      const data = await response.json()
      if (response.ok && data.code === 200) {
        setNodes(data.data)
      } else {
        showToast('Không thể tải dữ liệu lộ trình!', 'error')
      }
    } catch (err) {
      showToast('Lỗi kết nối máy chủ!', 'error')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRoadmap()
  }, [user])

  // Group nodes by phase
  const phases = useMemo(() => {
    const grouped = {}
    nodes.forEach(node => {
      if (!grouped[node.phase]) {
        grouped[node.phase] = []
      }
      grouped[node.phase].push(node)
    })
    return Object.entries(grouped).map(([phaseNum, phaseNodes]) => ({
      phaseNumber: parseInt(phaseNum),
      name: getPhaseName(parseInt(phaseNum)),
      nodes: phaseNodes
    }))
  }, [nodes])

  function getPhaseName(phaseNum) {
    switch (phaseNum) {
      case 1: return 'Chặng 1: Mảng & Chuỗi (Arrays & Strings)'
      case 2: return 'Chặng 2: Kỹ thuật Hai con trỏ (Two Pointers)'
      case 3: return 'Chặng 3: Cửa sổ trượt (Sliding Window)'
      case 4: return 'Chặng 4: Bảng băm (Hash Table)'
      case 5: return 'Chặng 5: Ngăn xếp & Hàng đợi (Stack & Queue)'
      case 6: return 'Chặng 6: Tìm kiếm nhị phân (Binary Search)'
      case 7: return 'Chặng 7: Cây & Đồ thị (Tree & Graph BFS/DFS)'
      case 8: return 'Chặng 8: Quy hoạch động (Dynamic Programming)'
      default: return `Chặng ${phaseNum}`
    }
  }

  // Calculate phase progress and locks
  const phaseStats = useMemo(() => {
    const stats = {}
    phases.forEach(phase => {
      const total = phase.nodes.length
      const solved = phase.nodes.filter(n => n.solved).length
      const percentage = total > 0 ? Math.round((solved / total) * 100) : 0
      stats[phase.phaseNumber] = { total, solved, percentage }
    })
    return stats
  }, [phases])

  // Check if a phase is locked (locked if previous phase is < 50% solved)
  const isPhaseLocked = (phaseNum) => {
    if (phaseNum === 1) return false
    const prevStats = phaseStats[phaseNum - 1]
    if (!prevStats) return true
    return prevStats.percentage < 50
  }

  const handleNodeClick = async (node) => {
    if (isPhaseLocked(node.phase)) {
      showToast('Vui lòng hoàn thành ít nhất 50% chặng trước để mở khóa chặng này!', 'error')
      return
    }

    if (!user) {
      showToast('Vui lòng đăng nhập để mở khóa bài tập!', 'error')
      navigate('/login')
      return
    }

    if (node.problemId) {
      // Already generated, redirect directly
      navigate(`/problem/${node.problemId}`)
      return
    }

    // Trigger AI generation
    setGeneratingNodeId(node.nodeId)
    try {
      const response = await fetch(`/api/roadmap/nodes/${node.nodeId}/generate`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${user.token}`
        }
      })
      const data = await response.json()
      if (response.ok && data.code === 200) {
        showToast('Mở khóa bài tập thành công!')
        navigate(`/problem/${data.data}`)
      } else {
        showToast(data.message || 'Lỗi khi sinh đề bài bằng AI!', 'error')
      }
    } catch (err) {
      showToast('Lỗi kết nối máy chủ!', 'error')
    } finally {
      setGeneratingNodeId(null)
    }
  }

  // Global Progress Stats
  const globalStats = useMemo(() => {
    const total = nodes.length
    const solved = nodes.filter(n => n.solved).length
    const percentage = total > 0 ? Math.round((solved / total) * 100) : 0
    return { total, solved, percentage }
  }, [nodes])

  if (loading) {
    return (
      <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
        Đang tải thông tin lộ trình...
      </div>
    )
  }

  return (
    <div className="roadmap-container" style={{ paddingBottom: '3rem' }}>
      
      {/* Header Dashboard */}
      <div className="glass-panel" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1.5rem', marginBottom: '2rem' }}>
        <div style={{ textAlign: 'left' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
            <Trophy size={24} style={{ color: '#fbbf24' }} />
            <h1 style={{ fontSize: '1.5rem', fontWeight: 800, margin: 0 }}>UMDane 75: Lộ trình thuật toán</h1>
          </div>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: 0, maxWidth: '600px' }}>
            Lộ trình học tập chuẩn hóa gồm 75 bài toán sắp xếp tăng dần từ cơ bản đến nâng cao. Đề bài được sinh tự động bằng AI và kiểm thử an toàn qua Docker sandbox.
          </p>
        </div>

        {/* Global Progress Bar */}
        <div style={{ minWidth: '240px', flex: '1', maxWidth: '360px', textAlign: 'right' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.35rem' }}>
            <span>Đã hoàn thành:</span>
            <span style={{ color: '#10b981' }}>{globalStats.solved}/{globalStats.total} bài ({globalStats.percentage}%)</span>
          </div>
          <div style={{ height: '8px', background: 'rgba(255,255,255,0.05)', borderRadius: '99px', overflow: 'hidden' }}>
            <div style={{ height: '100%', width: `${globalStats.percentage}%`, background: 'linear-gradient(90deg, #10b981, #059669)', borderRadius: '99px' }}></div>
          </div>
        </div>
      </div>

      {/* List of Phases */}
      <div className="phases-list" style={{ display: 'flex', flexDirection: 'column', gap: '2rem' }}>
        {phases.map((phase) => {
          const stats = phaseStats[phase.phaseNumber]
          const locked = isPhaseLocked(phase.phaseNumber)

          return (
            <div 
              key={phase.phaseNumber} 
              className={`glass-panel phase-card ${locked ? 'phase-locked' : ''}`}
              style={{ 
                padding: '1.5rem', 
                opacity: locked ? 0.45 : 1, 
                transition: 'opacity 0.3s',
                position: 'relative'
              }}
            >
              {locked && (
                <div style={{ 
                  position: 'absolute', 
                  top: '1.25rem', 
                  right: '1.5rem', 
                  display: 'flex', 
                  alignItems: 'center', 
                  gap: '0.35rem', 
                  color: '#ef4444', 
                  fontSize: '0.8rem', 
                  fontWeight: 700,
                  background: 'rgba(239, 68, 68, 0.1)',
                  padding: '0.25rem 0.6rem',
                  borderRadius: '99px'
                }}>
                  <Lock size={12} />
                  Bị khóa
                </div>
              )}

              {/* Phase Title Info */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '1rem', marginBottom: '1.5rem' }}>
                <div style={{ textAlign: 'left' }}>
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {phase.name}
                  </h3>
                  <p style={{ fontSize: '0.75rem', color: 'var(--text-muted)', margin: '0.25rem 0 0 0' }}>
                    Hãy hoàn thành tối thiểu 50% chặng này để kích hoạt chặng sau.
                  </p>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <span style={{ fontSize: '0.85rem', fontWeight: 700, color: locked ? 'var(--text-muted)' : '#3b82f6' }}>
                    Tiến độ: {stats.solved}/{stats.total} bài ({stats.percentage}%)
                  </span>
                </div>
              </div>

              {/* Grid of Nodes */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(110px, 1fr))', gap: '1rem' }}>
                {phase.nodes.map((node) => {
                  const isNodeGenerating = generatingNodeId === node.nodeId
                  let btnClass = 'node-btn'
                  let titleTip = `${node.title}\n[Chủ đề: ${node.topic} | Độ khó: ${node.difficulty}]`
                  
                  if (node.solved) {
                    btnClass += ' node-solved'
                    titleTip += '\n- Đã hoàn thành (AC) ✅'
                  } else if (node.problemId) {
                    btnClass += ' node-generated'
                    titleTip += '\n- Đang mở khóa (Nhấp để giải)'
                  } else {
                    btnClass += ' node-ungenerated'
                    titleTip += '\n- Nhấp để AI khởi tạo đề bài ❄️'
                  }

                  return (
                    <button
                      key={node.nodeId}
                      className={btnClass}
                      onClick={() => handleNodeClick(node)}
                      disabled={generatingNodeId !== null}
                      title={titleTip}
                      style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        aspectRatio: '1',
                        borderRadius: '12px',
                        border: '1px solid var(--border-color)',
                        cursor: locked ? 'not-allowed' : 'pointer',
                        padding: '0.5rem',
                        position: 'relative',
                        transition: 'transform 0.2s, box-shadow 0.2s',
                      }}
                    >
                      {/* Node Number */}
                      <span style={{ fontSize: '1.25rem', fontWeight: 800 }}>{node.nodeId}</span>

                      {/* Small metadata icon under number */}
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem', marginTop: '0.25rem' }}>
                        {node.solved ? (
                          <CheckCircle2 size={12} style={{ color: '#10b981' }} />
                        ) : node.problemId ? (
                          <Play size={10} style={{ color: '#f59e0b' }} />
                        ) : (
                          <Sparkles size={10} style={{ color: '#a78bfa' }} />
                        )}
                        <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)', fontWeight: 600 }}>
                          {node.difficulty}
                        </span>
                      </div>

                      {/* Small badge for topic */}
                      <span className="node-topic-badge" style={{ 
                        position: 'absolute', 
                        bottom: '0.35rem', 
                        fontSize: '0.55rem', 
                        padding: '0.1rem 0.25rem', 
                        borderRadius: '3px',
                        maxWidth: '90%',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        background: 'rgba(255,255,255,0.03)',
                        color: 'var(--text-muted)'
                      }}>
                        {node.topic}
                      </span>
                    </button>
                  )
                })}
              </div>
            </div>
          )
        })}
      </div>

      {/* AI Generating Loading Overlay */}
      {generatingNodeId !== null && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100vw',
          height: '100vh',
          background: 'rgba(15, 23, 42, 0.85)',
          backdropFilter: 'blur(8px)',
          zIndex: 9999,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '2rem'
        }}>
          <div className="spinner-glow" style={{ marginBottom: '1.5rem' }}>
            <Sparkles size={48} className="spin-slow" style={{ color: '#a78bfa' }} />
          </div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 800, color: '#ffffff', margin: '0 0 0.5rem 0' }}>
            Đang sinh đề bài & xác thực tự động...
          </h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: 0, maxWidth: '400px', textAlign: 'center', lineHeight: '1.5' }}>
            AI đang biên soạn thử thách thuật toán chuẩn hóa. Trình biên dịch đang kiểm thử lời giải mẫu qua Docker sandbox để xác thực đáp án tối ưu. Vui lòng đợi trong giây lát!
          </p>
        </div>
      )}
    </div>
  )
}
