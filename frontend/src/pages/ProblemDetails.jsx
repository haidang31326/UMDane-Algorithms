import React, { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { Play, ArrowLeft, Lightbulb, HelpCircle, Terminal, RotateCcw, Sparkles } from 'lucide-react'

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

export default function ProblemDetails({ user, showToast }) {
  const { id } = useParams()
  const [problem, setProblem] = useState(null)
  const [code, setCode] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const [showHint, setShowHint] = useState(false)
  const [running, setRunning] = useState(false)
  const [runInput, setRunInput] = useState('')
  const [runResults, setRunResults] = useState(null)
  const [activeTabIdx, setActiveTabIdx] = useState(0)
  const [showConsole, setShowConsole] = useState(false)
  const [editorRef, setEditorRef] = useState(null)
  const currentSampleTestCase = problem?.sampleTestCases?.find(tc => tc.inputData === runInput)

  const defaultTemplate = `import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // Nhập dữ liệu và xử lý tại đây
        
    }
}`

  useEffect(() => {
    const fetchProblem = async () => {
      try {
        const response = await fetch(`/api/problems/${id}`, {
          headers: user ? { 'Authorization': `Bearer ${user.token}` } : {}
        })
        if (response.ok) {
          const data = await response.json()
          setProblem(data)
          const draftKey = `umdane_draft_${user ? user.id : 'anon'}_${id}`
          const savedDraft = localStorage.getItem(draftKey)
          if (savedDraft) {
            setCode(savedDraft)
          } else {
            setCode(data.userTemplate ? data.userTemplate : defaultTemplate)
          }
          if (data.sampleTestCases && data.sampleTestCases.length > 0) {
            setRunInput(data.sampleTestCases[0].inputData)
          }
        } else {
          try {
            const errData = await response.json()
            showToast(errData.message || 'Không tìm thấy bài tập!', 'error')
          } catch (e) {
            showToast('Không tìm thấy bài tập!', 'error')
          }
        }
      } catch (err) {
        showToast('Lỗi khi tải thông tin bài tập!', 'error')
      } finally {
        setLoading(false)
      }
    }

    fetchProblem()
  }, [id])

  const handleReset = () => {
    if (!problem) return
    if (window.confirm('Bạn có chắc chắn muốn đặt lại code về trạng thái ban đầu không?')) {
      const initialTemplate = problem.userTemplate ? problem.userTemplate : defaultTemplate
      setCode(initialTemplate)
      const draftKey = `umdane_draft_${user ? user.id : 'anon'}_${id}`
      localStorage.removeItem(draftKey)
      showToast('Đã đặt lại code về trạng thái mặc định!')
    }
  }

  const formatJavaCodeAndComments = (rawCode) => {
    if (!rawCode) return "";
    const lines = rawCode.split("\n");
    const formattedLines = lines.map(line => {
      // 1. Format single-line comments (ensure space after //)
      let formatted = line.replace(/(^|[^:])\/\/([^\s/])/g, '$1// $2');
      
      // 2. Format block comment lines starting with '*' (ensure space after *)
      formatted = formatted.replace(/^(\s*\*)\s*([^\s*/])/g, '$1 $2');
      
      return formatted;
    });
    return formattedLines.join("\n");
  };

  const handleFormat = () => {
    if (editorRef) {
      const currentCode = editorRef.getValue()
      const commentFormattedCode = formatJavaCodeAndComments(currentCode)
      setCode(commentFormattedCode)
      
      // Give React a small tick to update the state before running Monaco's formatter
      setTimeout(() => {
        if (editorRef) {
          editorRef.getAction('editor.action.formatDocument').run()
        }
      }, 50)
      showToast('Đã định dạng code và bình luận thành công!')
    } else {
      showToast('Không thể định dạng code tại thời điểm này!', 'error')
    }
  }

  const handleSubmit = async () => {
    if (!user) {
      showToast('Làm ơn đăng nhập để nộp bài!', 'error')
      return
    }

    setSubmitting(true)
    setResult(null)

    try {
      const response = await fetch('/api/submissions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${user.token}`
        },
        body: JSON.stringify({
          problemId: parseInt(id),
          code: code,
          language: 'JAVA'
        })
      })

      const data = await response.json()
      if (response.ok && data.code === 200) {
        setResult(data.data)
        if (data.data.submission.status === 'ACCEPTED') {
          showToast('Chúc mừng! Bài giải chính xác (AC).')
        } else {
          showToast(`Kết quả: ${data.data.submission.status}`, 'error')
        }
      } else {
        showToast(data.message || 'Lỗi khi gửi code!', 'error')
      }
    } catch (err) {
      showToast('Không thể kết nối đến máy chủ để chấm bài!', 'error')
    } finally {
      setSubmitting(false)
    }
  }

  const handleRun = async () => {
    setRunning(true)
    setRunResults(null)
    setShowConsole(true)

    // Build the inputs list: all sample test cases plus the custom runInput
    const sampleInputs = problem?.sampleTestCases?.map(tc => tc.inputData) || []
    const inputs = [...sampleInputs, runInput || '']

    try {
      const response = await fetch('/api/submissions/run', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': user ? `Bearer ${user.token}` : ''
        },
        body: JSON.stringify({
          problemId: parseInt(id),
          code: code,
          inputs: inputs,
          language: 'JAVA'
        })
      })

      const data = await response.json()
      if (response.ok && data.code === 200) {
        setRunResults(data.data)
        const anyFailed = data.data.some(r => r.status !== 'ACCEPTED')
        if (!anyFailed) {
          showToast('Chạy thử hoàn tất thành công!')
        } else {
          showToast('Có test case chạy thử thất bại!', 'error')
        }
      } else {
        showToast(data.message || 'Lỗi khi chạy thử code!', 'error')
      }
    } catch (err) {
      showToast('Không thể kết nối đến máy chủ để chạy thử!', 'error')
    } finally {
      setRunning(false)
    }
  }

  const renderDistributionChart = (distribution, userRuntime) => {
    const bins = Object.entries(distribution)
      .map(([rt, cnt]) => ({ runtime: parseInt(rt), count: cnt }))
      .sort((a, b) => a.runtime - b.runtime)

    if (bins.length === 0) return null

    const maxCount = Math.max(...bins.map(b => b.count), 1)
    const svgWidth = 500
    const svgHeight = 160
    const paddingLeft = 40
    const paddingRight = 40
    const paddingTop = 30
    const paddingBottom = 30

    const graphWidth = svgWidth - paddingLeft - paddingRight
    const graphHeight = svgHeight - paddingTop - paddingBottom

    const totalBars = bins.length
    const barWidth = Math.min(50, Math.max(8, Math.floor(graphWidth / (totalBars || 1)) - 6))
    const gap = totalBars <= 1 ? 0 : (graphWidth - (barWidth * totalBars)) / (totalBars - 1)

    return (
      <div style={{ width: '100%', overflowX: 'auto', display: 'flex', justifyContent: 'center', margin: '0.5rem 0' }}>
        <svg viewBox={`0 0 ${svgWidth} ${svgHeight}`} width="100%" maxWidth="460px" height="160px" style={{ overflow: 'visible' }}>
          <defs>
            <linearGradient id="userBarGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#a78bfa" />
              <stop offset="100%" stopColor="#7c3aed" />
            </linearGradient>
            <linearGradient id="otherBarGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="rgba(255, 255, 255, 0.15)" />
              <stop offset="100%" stopColor="rgba(255, 255, 255, 0.03)" />
            </linearGradient>
          </defs>

          {/* Grid lines */}
          <line x1={paddingLeft} y1={paddingTop} x2={svgWidth - paddingRight} y2={paddingTop} stroke="rgba(255, 255, 255, 0.03)" strokeWidth="1" />
          <line x1={paddingLeft} y1={paddingTop + graphHeight / 2} x2={svgWidth - paddingRight} y2={paddingTop + graphHeight / 2} stroke="rgba(255, 255, 255, 0.03)" strokeWidth="1" />
          <line x1={paddingLeft} y1={svgHeight - paddingBottom} x2={svgWidth - paddingRight} y2={svgHeight - paddingBottom} stroke="rgba(255, 255, 255, 0.1)" strokeWidth="1" />

          {/* Bars */}
          {bins.map((bin, idx) => {
            const isUserBin = bin.runtime === userRuntime
            const barHeight = (bin.count / maxCount) * graphHeight
            const x = paddingLeft + idx * (barWidth + gap)
            const y = svgHeight - paddingBottom - barHeight

            return (
              <g key={idx}>
                {/* Bar */}
                <rect
                  x={x}
                  y={y}
                  width={barWidth}
                  height={Math.max(2, barHeight)}
                  rx="2"
                  fill={isUserBin ? 'url(#userBarGrad)' : 'url(#otherBarGrad)'}
                  stroke={isUserBin ? 'rgba(167, 139, 250, 0.8)' : 'transparent'}
                  strokeWidth="1"
                  style={{
                    transition: 'all 0.5s ease-out'
                  }}
                >
                  <title>{`Thời gian: ${bin.runtime} ms, Số lượng: ${bin.count} bài nộp`}</title>
                </rect>

                {/* Marker Arrow above User Bar */}
                {isUserBin && (
                  <path
                    d={`M ${x + barWidth / 2} ${y - 4} L ${x + barWidth / 2 - 3} ${y - 9} L ${x + barWidth / 2 + 3} ${y - 9} Z`}
                    fill="#a78bfa"
                  />
                )}

                {/* X-axis Label (Runtime in ms) */}
                {(totalBars <= 8 || idx % Math.ceil(totalBars / 6) === 0 || isUserBin) && (
                  <text
                    x={x + barWidth / 2}
                    y={svgHeight - 12}
                    fill={isUserBin ? '#a78bfa' : 'var(--text-muted)'}
                    fontSize="9px"
                    fontWeight={isUserBin ? '700' : 'normal'}
                    textAnchor="middle"
                  >
                    {`${bin.runtime} ms`}
                  </text>
                )}
              </g>
            )
          })}
        </svg>
      </div>
    )
  }

  if (loading) {
    return <p style={{ color: 'var(--text-muted)' }}>Đang tải thông tin bài tập...</p>
  }

  if (!problem) {
    return (
      <div className="glass-panel card" style={{ textAlign: 'center', padding: '3rem' }}>
        <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>Không tìm thấy bài tập yêu cầu.</p>
        <Link to="/dashboard" className="btn btn-secondary">
          <ArrowLeft size={16} /> Quay lại Dashboard
        </Link>
      </div>
    )
  }

  return (
    <div className="problem-workspace">
      {/* Left panel: Problem Details */}
      <div className="workspace-left">
        <div style={{ marginBottom: '0.5rem' }}>
          <Link to="/dashboard" className="btn btn-secondary" style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem' }}>
            <ArrowLeft size={14} /> Quay lại danh sách
          </Link>
        </div>

        <div className="glass-panel problem-description">
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '0.5rem' }}>{problem.title}</h1>
          <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', alignItems: 'center' }}>
            <span className="topic-badge">{getNormalizedTopicName(problem.topic)}</span>
            <span className="topic-badge" style={{ background: 'rgba(6, 182, 212, 0.1)', color: '#22d3ee', borderColor: 'rgba(6, 182, 212, 0.2)' }}>
              {problem.keyword}
            </span>
            <span className={`difficulty-badge diff-${(problem.difficulty || 'medium').toLowerCase()}`} style={{
              display: 'inline-block',
              padding: '0.2rem 0.5rem',
              borderRadius: '4px',
              fontSize: '0.75rem',
              fontWeight: 700,
              backgroundColor: problem.difficulty === 'EASY' ? 'rgba(16, 185, 129, 0.15)' : problem.difficulty === 'HARD' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(245, 158, 11, 0.15)',
              color: problem.difficulty === 'EASY' ? '#10b981' : problem.difficulty === 'HARD' ? '#ef4444' : '#f59e0b',
              border: problem.difficulty === 'EASY' ? '1px solid rgba(16, 185, 129, 0.3)' : problem.difficulty === 'HARD' ? '1px solid rgba(239, 68, 68, 0.3)' : '1px solid rgba(245, 158, 11, 0.3)'
            }}>
              {problem.difficulty || 'MEDIUM'}
            </span>
          </div>

          <div style={{ display: 'flex', gap: '1.25rem', marginBottom: '1.5rem', fontSize: '0.85rem', color: 'var(--text-muted)', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
            <div>
              <span style={{ fontWeight: 600, color: 'var(--text-main)' }}>Thời gian chạy tối đa:</span> {problem.timeLimit ? (problem.timeLimit / 1000).toFixed(1) : '2.0'}s
            </div>
            <div>
              <span style={{ fontWeight: 600, color: 'var(--text-main)' }}>Giới hạn bộ nhớ:</span> {problem.memoryLimit || '128'}MB
            </div>
          </div>

          <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--text-muted)' }}>Mô tả bài toán</h3>
          <div className="problem-desc-text" style={{ marginBottom: '1.5rem' }}>{problem.description}</div>

          <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--text-muted)' }}>Ràng buộc (Constraints)</h3>
          <div 
            className="glass-panel" 
            style={{ 
              padding: '1rem', 
              fontFamily: 'var(--font-mono)', 
              fontSize: '0.85rem', 
              color: '#a7f3d0', 
              background: 'rgba(16, 185, 129, 0.02)', 
              borderColor: 'rgba(16, 185, 129, 0.15)',
              whiteSpace: 'pre-wrap',
              marginBottom: '1.5rem'
            }}
          >
            {problem.constraints || 'Không có ràng buộc đặc biệt nào.'}
          </div>

          {problem.hint && (
            <div style={{ marginTop: '2rem', borderTop: '1px solid var(--border-color)', paddingTop: '1.5rem' }}>
              <button className="btn btn-secondary" onClick={() => setShowHint(!showHint)} style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}>
                <Lightbulb size={14} style={{ color: '#f59e0b' }} />
                {showHint ? 'Ẩn gợi ý' : 'Hiển thị gợi ý'}
              </button>
              {showHint && (
                <div 
                  className="glass-panel" 
                  style={{ 
                    marginTop: '0.75rem', 
                    padding: '1rem', 
                    background: 'rgba(245, 158, 11, 0.05)', 
                    borderColor: 'rgba(245, 158, 11, 0.2)',
                    fontSize: '0.9rem',
                    color: '#fcd34d'
                  }}
                >
                  {problem.hint}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Right panel: Editor & Results */}
      <div className="workspace-right">
        {/* Editor */}
        <div className="editor-container">
          <div className="editor-header">
            <div className="editor-title">
              <Terminal size={14} style={{ color: '#10b981' }} />
              Trình soạn thảo Java (JDK 21)
            </div>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button 
                className="btn btn-secondary" 
                onClick={handleFormat} 
                disabled={running || submitting}
                title="Định dạng code (Format)"
                style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem', borderColor: 'var(--border-color)', background: 'transparent', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
              >
                <Sparkles size={12} style={{ color: '#c084fc' }} />
                <span>Định dạng</span>
              </button>
              <button 
                className="btn btn-secondary" 
                onClick={handleReset} 
                disabled={running || submitting}
                title="Đặt lại code ban đầu (Reset)"
                style={{ padding: '0.4rem 0.8rem', fontSize: '0.85rem', borderColor: 'var(--border-color)', background: 'transparent', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
              >
                <RotateCcw size={12} style={{ color: '#ef4444' }} />
                <span>Đặt lại</span>
              </button>
              <button 
                className="btn btn-secondary" 
                onClick={handleRun} 
                disabled={running || submitting} 
                style={{ padding: '0.4rem 1rem', fontSize: '0.85rem', borderColor: 'var(--border-color)', background: 'transparent', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
              >
                <Terminal size={12} style={{ color: '#60a5fa' }} />
                {running ? 'Đang chạy...' : 'Chạy thử'}
              </button>
              <button 
                className="btn btn-primary" 
                onClick={handleSubmit} 
                disabled={submitting || running} 
                style={{ padding: '0.4rem 1rem', fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}
              >
                <Play size={12} />
                {submitting ? 'Đang chấm...' : 'Nộp bài'}
              </button>
            </div>
          </div>
          <div style={{ flex: 1, minHeight: '350px' }}>
            <Editor
              height="100%"
              defaultLanguage="java"
              theme="vs-dark"
              value={code}
              onChange={(value) => {
                const val = value || ''
                setCode(val)
                const draftKey = `umdane_draft_${user ? user.id : 'anon'}_${id}`
                localStorage.setItem(draftKey, val)
              }}
              onMount={(editor) => {
                setEditorRef(editor)
              }}
              options={{
                fontSize: 14,
                minimap: { enabled: false },
                automaticLayout: true,
                padding: { top: 12, bottom: 12 }
              }}
            />
          </div>
        </div>

        {/* Stdin Input Console */}
        <div className="glass-panel" style={{ marginTop: '1rem', padding: '1rem' }}>
          <div 
            onClick={() => setShowConsole(!showConsole)} 
            style={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              alignItems: 'center', 
              cursor: 'pointer',
              fontWeight: 600,
              fontSize: '0.9rem'
            }}
          >
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#60a5fa' }}>
              <Terminal size={16} />
              Bảng chạy thử (Custom Input)
            </span>
            <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
              {showConsole ? 'Thu gọn ▲' : 'Mở rộng ▼'}
            </span>
          </div>

          {showConsole && (
            <div style={{ marginTop: '1.25rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label className="form-label" style={{ fontSize: '0.85rem', marginBottom: '0.5rem', display: 'block', color: 'var(--text-muted)' }}>
                  Dữ liệu đầu vào mẫu & Tùy chỉnh (Sample Test Cases)
                </label>
                
                {/* Tab Selector */}
                <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem', flexWrap: 'wrap', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                  {problem.sampleTestCases?.map((tc, idx) => (
                    <button
                      key={idx}
                      type="button"
                      className="btn"
                      onClick={() => setActiveTabIdx(idx)}
                      style={{
                        padding: '0.35rem 0.75rem',
                        fontSize: '0.75rem',
                        borderRadius: '4px',
                        background: activeTabIdx === idx ? 'rgba(59, 130, 246, 0.15)' : 'rgba(255, 255, 255, 0.05)',
                        color: activeTabIdx === idx ? '#60a5fa' : 'var(--text-muted)',
                        border: activeTabIdx === idx ? '1px solid rgba(59, 130, 246, 0.3)' : '1px solid var(--border-color)',
                        cursor: 'pointer',
                        fontWeight: activeTabIdx === idx ? 700 : 500
                      }}
                    >
                      Ví dụ {idx + 1}
                    </button>
                  ))}
                  <button
                    type="button"
                    className="btn"
                    onClick={() => setActiveTabIdx(problem.sampleTestCases?.length || 0)}
                    style={{
                      padding: '0.35rem 0.75rem',
                      fontSize: '0.75rem',
                      borderRadius: '4px',
                      background: activeTabIdx === (problem.sampleTestCases?.length || 0) ? 'rgba(59, 130, 246, 0.15)' : 'rgba(255, 255, 255, 0.05)',
                      color: activeTabIdx === (problem.sampleTestCases?.length || 0) ? '#60a5fa' : 'var(--text-muted)',
                      border: activeTabIdx === (problem.sampleTestCases?.length || 0) ? '1px solid rgba(59, 130, 246, 0.3)' : '1px solid var(--border-color)',
                      cursor: 'pointer',
                      fontWeight: activeTabIdx === (problem.sampleTestCases?.length || 0) ? 700 : 500
                    }}
                  >
                    Tùy chỉnh
                  </button>
                </div>

                {/* Tab Contents */}
                {activeTabIdx < (problem.sampleTestCases?.length || 0) ? (
                  // Sample Test Case Tab
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                    <div>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.25rem' }}>
                        Dữ liệu đầu vào (Input):
                      </span>
                      <pre style={{ background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--border-color)', padding: '0.6rem 0.8rem', borderRadius: '4px', fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: 'var(--text-main)', whiteSpace: 'pre-wrap', margin: 0 }}>
                        {problem.sampleTestCases[activeTabIdx]?.inputData || '(Rỗng)'}
                      </pre>
                    </div>
                    <div>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.25rem' }}>
                        Kết quả đầu ra mong đợi (Expected Output):
                      </span>
                      <pre style={{ background: 'rgba(255, 255, 255, 0.02)', border: '1px solid var(--border-color)', padding: '0.6rem 0.8rem', borderRadius: '4px', fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: '#60a5fa', whiteSpace: 'pre-wrap', margin: 0 }}>
                        {problem.sampleTestCases[activeTabIdx]?.expectedOutput || '(Rỗng)'}
                      </pre>
                    </div>
                  </div>
                ) : (
                  // Custom Input Tab
                  <div>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.25rem' }}>
                      Nhập đầu vào tùy chỉnh (Custom Input):
                    </span>
                    <textarea 
                      className="form-control" 
                      rows={4} 
                      value={runInput} 
                      onChange={(e) => { setRunInput(e.target.value); setRunResults(null); }} 
                      placeholder="Nhập input dữ liệu ở đây... Ví dụ: 5 10"
                      style={{ fontFamily: 'var(--font-mono)', fontSize: '0.85rem', background: 'rgba(15, 23, 42, 0.6)' }}
                    />
                  </div>
                )}
              </div>

              {/* Display Result for Active Tab */}
              {runResults && runResults[activeTabIdx] && (
                <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '1rem', marginTop: '0.5rem' }}>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.5rem' }}>Kết quả chạy thử:</h4>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem' }}>
                    <span className={`status-badge status-${runResults[activeTabIdx].status}`}>
                      {runResults[activeTabIdx].status === 'ACCEPTED' ? 'SUCCESS' :
                       runResults[activeTabIdx].status === 'COMPILE_ERROR' ? 'COMPILE ERROR (CE)' :
                       runResults[activeTabIdx].status === 'RUNTIME_ERROR' ? 'RUNTIME ERROR (RE)' :
                       runResults[activeTabIdx].status === 'TIME_LIMIT_EXCEEDED' ? 'TIME LIMIT EXCEEDED (TLE)' : runResults[activeTabIdx].status}
                    </span>
                    <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      Thời gian chạy: <strong style={{ color: 'var(--text-main)' }}>{runResults[activeTabIdx].runtimeMs} ms</strong>
                    </span>
                  </div>

                  {runResults[activeTabIdx].status === 'ACCEPTED' && (
                    <div>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Đầu ra (Standard Output):</p>
                      <pre className="console-output" style={{ background: 'rgba(16, 185, 129, 0.05)', borderColor: 'rgba(16, 185, 129, 0.2)', padding: '0.75rem', borderRadius: '4px', whiteSpace: 'pre-wrap', fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: '#34d399' }}>
                        {runResults[activeTabIdx].output || '(Không có dữ liệu in ra)'}
                      </pre>
                    </div>
                  )}

                  {(runResults[activeTabIdx].errorOutput || runResults[activeTabIdx].status === 'COMPILE_ERROR' || runResults[activeTabIdx].status === 'RUNTIME_ERROR') && (
                    <div>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Chi tiết lỗi:</p>
                      <pre className="console-output console-error" style={{ padding: '0.75rem', borderRadius: '4px', whiteSpace: 'pre-wrap', fontFamily: 'var(--font-mono)', fontSize: '0.85rem' }}>
                        {runResults[activeTabIdx].errorOutput}
                      </pre>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Evaluation Results */}
        {result && result.submission && (
          <div className="glass-panel results-container">
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <HelpCircle size={18} />
              Kết quả chấm bài
            </h3>
            
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
              <span className={`status-badge status-${result.submission.status}`}>
                {result.submission.status === 'ACCEPTED' ? 'ACCEPTED (AC)' :
                 result.submission.status === 'WRONG_ANSWER' ? 'WRONG ANSWER (WA)' :
                 result.submission.status === 'TIME_LIMIT_EXCEEDED' ? 'TIME LIMIT EXCEEDED (TLE)' :
                 result.submission.status === 'COMPILE_ERROR' ? 'COMPILE ERROR (CE)' :
                 result.submission.status === 'RUNTIME_ERROR' ? 'RUNTIME ERROR (RE)' : result.submission.status}
              </span>
              <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                Thời gian chạy: <strong style={{ color: 'var(--text-main)' }}>{result.submission.runtimeMs} ms</strong>
              </span>
            </div>

            {result.submission.errorMessage && (
              <div>
                <p style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>Chi tiết lỗi:</p>
                <pre className="console-output console-error">
                  {result.submission.errorMessage}
                </pre>
              </div>
            )}

            {result.submission.status === 'ACCEPTED' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem', marginTop: '0.5rem' }}>
                <div 
                  style={{ 
                    background: 'rgba(16, 185, 129, 0.05)', 
                    border: '1px solid rgba(16, 185, 129, 0.2)', 
                    padding: '1rem', 
                    borderRadius: '6px',
                    color: '#a7f3d0',
                    fontSize: '0.9rem'
                  }}
                >
                  🎉 Tất cả test case đều chạy chính xác! Thật tuyệt vời!
                </div>

                {/* Beats Display */}
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  padding: '1rem',
                  borderRadius: '8px',
                  background: 'rgba(167, 139, 250, 0.06)',
                  border: '1px solid rgba(167, 139, 250, 0.15)',
                }}>
                  <div style={{ fontSize: '2.4rem', fontWeight: 900, color: '#a78bfa', lineHeight: 1 }}>
                    {result.beatsPercentage}%
                  </div>
                  <div style={{ textAlign: 'left' }}>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--text-main)' }}>
                      Vượt qua thời gian chạy (Beats)
                    </div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      Của tất cả các bài nộp Java cho bài tập này trên hệ thống.
                    </div>
                  </div>
                </div>

                {/* Distribution Histogram Chart */}
                {result.runtimeDistribution && Object.keys(result.runtimeDistribution).length > 0 && (
                  <div style={{ 
                    padding: '1rem', 
                    borderRadius: '8px', 
                    background: 'rgba(255,255,255,0.01)', 
                    border: '1px solid rgba(255,255,255,0.05)' 
                  }}>
                    <h4 style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.75rem', textAlign: 'left' }}>
                      Phân phối thời gian chạy (Runtime Distribution)
                    </h4>
                    {renderDistributionChart(result.runtimeDistribution, result.submission.runtimeMs)}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
