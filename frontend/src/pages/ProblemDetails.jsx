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
  const [runResult, setRunResult] = useState(null)
  const [showConsole, setShowConsole] = useState(false)
  const [editorRef, setEditorRef] = useState(null)

  const defaultTemplate = `import java.util.Scanner;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // Nhập dữ liệu và xử lý tại đây
        
    }
}`

  useEffect(() => {
    const fetchProblem = async () => {
      try {
        const response = await fetch(`/api/problems/${id}`)
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
          showToast('Không tìm thấy bài tập!', 'error')
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
        if (data.data.status === 'ACCEPTED') {
          showToast('Chúc mừng! Bài giải chính xác (AC).')
        } else {
          showToast(`Kết quả: ${data.data.status}`, 'error')
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
    setRunResult(null)
    setShowConsole(true)

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
          inputData: runInput,
          language: 'JAVA'
        })
      })

      const data = await response.json()
      if (response.ok && data.code === 200) {
        setRunResult(data.data)
        if (data.data.status === 'ACCEPTED') {
          showToast('Chạy thử hoàn tất thành công!')
        } else {
          showToast(`Chạy thử thất bại: ${data.data.status}`, 'error')
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
                  Dữ liệu đầu vào mẫu (Sample Test Cases)
                </label>
                {problem.sampleTestCases && problem.sampleTestCases.length > 0 ? (
                  <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem', flexWrap: 'wrap' }}>
                    {problem.sampleTestCases.map((tc, idx) => (
                      <button
                        key={idx}
                        type="button"
                        className="btn"
                        onClick={() => setRunInput(tc.inputData)}
                        style={{
                          padding: '0.3rem 0.6rem',
                          fontSize: '0.75rem',
                          borderRadius: '4px',
                          background: runInput === tc.inputData ? 'rgba(16, 185, 129, 0.15)' : 'rgba(255, 255, 255, 0.05)',
                          color: runInput === tc.inputData ? '#10b981' : 'var(--text-muted)',
                          border: runInput === tc.inputData ? '1px solid rgba(16, 185, 129, 0.3)' : '1px solid var(--border-color)',
                          cursor: 'pointer',
                          fontWeight: runInput === tc.inputData ? 700 : 500
                        }}
                      >
                        Ví dụ {idx + 1}
                      </button>
                    ))}
                  </div>
                ) : (
                  <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.5rem' }}>
                    Không có test case mẫu nào được cấu hình.
                  </span>
                )}
                <textarea 
                  className="form-control" 
                  rows={4} 
                  value={runInput} 
                  onChange={(e) => setRunInput(e.target.value)} 
                  placeholder="Nhập input dữ liệu ở đây... Ví dụ: 5 10"
                  style={{ fontFamily: 'var(--font-mono)', fontSize: '0.85rem', background: 'rgba(15, 23, 42, 0.6)' }}
                />
              </div>

              {runResult && (
                <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '1rem' }}>
                  <h4 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.5rem' }}>Kết quả chạy thử:</h4>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem' }}>
                    <span className={`status-badge status-${runResult.status}`}>
                      {runResult.status === 'ACCEPTED' ? 'SUCCESS' :
                       runResult.status === 'COMPILE_ERROR' ? 'COMPILE ERROR (CE)' :
                       runResult.status === 'RUNTIME_ERROR' ? 'RUNTIME ERROR (RE)' :
                       runResult.status === 'TIME_LIMIT_EXCEEDED' ? 'TIME LIMIT EXCEEDED (TLE)' : runResult.status}
                    </span>
                    <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      Thời gian chạy: <strong style={{ color: 'var(--text-main)' }}>{runResult.runtimeMs} ms</strong>
                    </span>
                  </div>

                  {runResult.status === 'ACCEPTED' && (
                    <div>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Đầu ra (Standard Output):</p>
                      <pre className="console-output" style={{ background: 'rgba(16, 185, 129, 0.05)', borderColor: 'rgba(16, 185, 129, 0.2)', padding: '0.75rem', borderRadius: '4px', whiteSpace: 'pre-wrap', fontFamily: 'var(--font-mono)', fontSize: '0.85rem', color: '#34d399' }}>
                        {runResult.output || '(Không có dữ liệu in ra)'}
                      </pre>
                    </div>
                  )}

                  {(runResult.errorOutput || runResult.status === 'COMPILE_ERROR' || runResult.status === 'RUNTIME_ERROR') && (
                    <div>
                      <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.25rem' }}>Chi tiết lỗi:</p>
                      <pre className="console-output console-error" style={{ padding: '0.75rem', borderRadius: '4px', whiteSpace: 'pre-wrap', fontFamily: 'var(--font-mono)', fontSize: '0.85rem' }}>
                        {runResult.errorOutput}
                      </pre>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Evaluation Results */}
        {result && (
          <div className="glass-panel results-container">
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <HelpCircle size={18} />
              Kết quả chấm bài
            </h3>
            
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem' }}>
              <span className={`status-badge status-${result.status}`}>
                {result.status === 'ACCEPTED' ? 'ACCEPTED (AC)' :
                 result.status === 'WRONG_ANSWER' ? 'WRONG ANSWER (WA)' :
                 result.status === 'TIME_LIMIT_EXCEEDED' ? 'TIME LIMIT EXCEEDED (TLE)' :
                 result.status === 'COMPILE_ERROR' ? 'COMPILE ERROR (CE)' :
                 result.status === 'RUNTIME_ERROR' ? 'RUNTIME ERROR (RE)' : result.status}
              </span>
              <span style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                Thời gian chạy: <strong style={{ color: 'var(--text-main)' }}>{result.runtimeMs} ms</strong>
              </span>
            </div>

            {result.errorMessage && (
              <div>
                <p style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>Chi tiết lỗi:</p>
                <pre className={`console-output ${result.status === 'COMPILE_ERROR' ? 'console-error' : 'console-error'}`}>
                  {result.errorMessage}
                </pre>
              </div>
            )}

            {result.status === 'ACCEPTED' && (
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
            )}
          </div>
        )}
      </div>
    </div>
  )
}
