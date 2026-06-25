import React, { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { Play, ArrowLeft, Lightbulb, HelpCircle, Terminal } from 'lucide-react'

export default function ProblemDetails({ user, showToast }) {
  const { id } = useParams()
  const [problem, setProblem] = useState(null)
  const [code, setCode] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState(null)
  const [showHint, setShowHint] = useState(false)

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
          setCode(defaultTemplate)
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
          <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
            <span className="topic-badge">{problem.topic}</span>
            <span className="topic-badge" style={{ background: 'rgba(6, 182, 212, 0.1)', color: '#22d3ee', borderColor: 'rgba(6, 182, 212, 0.2)' }}>
              {problem.keyword}
            </span>
          </div>

          <h3 style={{ fontSize: '1.05rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--text-muted)' }}>Mô tả bài toán</h3>
          <div className="problem-desc-text">{problem.description}</div>

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
            <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting} style={{ padding: '0.4rem 1rem', fontSize: '0.85rem' }}>
              <Play size={12} />
              {submitting ? 'Đang chấm bài...' : 'Nộp bài'}
            </button>
          </div>
          <div style={{ flex: 1, minHeight: '350px' }}>
            <Editor
              height="100%"
              defaultLanguage="java"
              theme="vs-dark"
              value={code}
              onChange={(value) => setCode(value || '')}
              options={{
                fontSize: 14,
                minimap: { enabled: false },
                automaticLayout: true,
                padding: { top: 12, bottom: 12 }
              }}
            />
          </div>
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
