import { useState, useEffect, useRef } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getCaptcha, login } from '../api/authApi'
import { useAuth } from '../context/AuthContext'
import Alert from '../components/Alert'
import styles from './AuthPage.module.css'

const EyeIcon = ({ open }) =>
  open ? (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
      <line x1="1" y1="1" x2="23" y2="23"/>
    </svg>
  ) : (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
      <circle cx="12" cy="12" r="3"/>
    </svg>
  )

const RefreshIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="23 4 23 10 17 10"/>
    <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
  </svg>
)

export default function LoginPage() {
  const navigate = useNavigate()
  const { login: authLogin, isAuthenticated } = useAuth()

  const [form, setForm] = useState({ usernameOrEmail: '', password: '', captchaAnswer: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const [captchaUrl, setCaptchaUrl] = useState('')
  const [captchaToken, setCaptchaToken] = useState('')
  const [captchaLoading, setCaptchaLoading] = useState(false)
  const [captchaError, setCaptchaError] = useState('')
  const prevCaptchaUrl = useRef('')

  useEffect(() => {
    if (isAuthenticated) navigate('/events', { replace: true })
  }, [isAuthenticated, navigate])

  const loadCaptcha = async () => {
    setCaptchaLoading(true)
    setCaptchaError('')
    try {
      const res = await getCaptcha()
      if (prevCaptchaUrl.current) URL.revokeObjectURL(prevCaptchaUrl.current)
      const token = res.headers['x-captcha-token']
      const url = URL.createObjectURL(res.data)
      prevCaptchaUrl.current = url
      setCaptchaToken(token)
      setCaptchaUrl(url)
      setForm((f) => ({ ...f, captchaAnswer: '' }))
    } catch (err) {
      setCaptchaError(
        err.code === 'ERR_NETWORK'
          ? 'Cannot reach the server. Make sure the backend is running on port 8081.'
          : 'Failed to load CAPTCHA. Try refreshing.'
      )
    } finally {
      setCaptchaLoading(false)
    }
  }

  useEffect(() => {
    loadCaptcha()
    return () => { if (prevCaptchaUrl.current) URL.revokeObjectURL(prevCaptchaUrl.current) }
  }, [])

  const validate = () => {
    const e = {}
    if (!form.usernameOrEmail.trim()) e.usernameOrEmail = 'Username or email is required'
    if (!form.password) e.password = 'Password is required'
    if (!form.captchaAnswer.trim()) e.captchaAnswer = 'Please type the CAPTCHA text'
    return e
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
    setErrors((er) => ({ ...er, [name]: '' }))
    if (name === 'captchaAnswer') setServerError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setServerError('')
    const validationErrors = validate()
    if (Object.keys(validationErrors).length > 0) { setErrors(validationErrors); return }
    if (!captchaToken) { setCaptchaError('CAPTCHA not loaded. Click refresh.'); return }

    setLoading(true)
    try {
      const res = await login({
        usernameOrEmail: form.usernameOrEmail.trim(),
        password: form.password,
        captchaToken,
        captchaAnswer: form.captchaAnswer.trim(),
      })
      const data = res.data.data
      if (data.requiresTwoFactor) {
        navigate('/verify-2fa', { state: { username: data.user.username } })
      } else {
        authLogin(data)
        navigate('/events')
      }
    } catch (err) {
      const status = err.response?.status
      const msg = err.response?.data?.message
      if (status === 400 && msg?.toLowerCase().includes('captcha')) {
        setErrors((e) => ({ ...e, captchaAnswer: 'Wrong CAPTCHA — a new one has been loaded' }))
        loadCaptcha()
      } else if (status === 401 || (status === 400 && msg?.toLowerCase().includes('password'))) {
        setServerError('Invalid username or password.')
        loadCaptcha()
      } else if (err.code === 'ERR_NETWORK') {
        setServerError('Cannot reach the server. Make sure the backend is running.')
      } else {
        setServerError(msg || 'Login failed. Please try again.')
        loadCaptcha()
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.cardHeader}>
          <div className={styles.logoMark}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
          </div>
          <h1 className={styles.title}>Welcome back</h1>
          <p className={styles.subtitle}>Sign in to your EventSphere account</p>
        </div>

        {serverError && <Alert type="error" message={serverError} />}

        <form onSubmit={handleSubmit} noValidate className={styles.form}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="usernameOrEmail">
              Username or Email
            </label>
            <input
              id="usernameOrEmail"
              className={`${styles.input} ${errors.usernameOrEmail ? styles.inputError : ''}`}
              type="text"
              name="usernameOrEmail"
              value={form.usernameOrEmail}
              onChange={handleChange}
              placeholder="you@example.com"
              autoComplete="username"
              autoFocus
              disabled={loading}
            />
            {errors.usernameOrEmail && <span className={styles.fieldError}>{errors.usernameOrEmail}</span>}
          </div>

          <div className={styles.field}>
            <label className={styles.label} htmlFor="password">Password</label>
            <div className={styles.passwordWrapper}>
              <input
                id="password"
                className={`${styles.input} ${styles.inputWithBtn} ${errors.password ? styles.inputError : ''}`}
                type={showPassword ? 'text' : 'password'}
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="••••••••"
                autoComplete="current-password"
                disabled={loading}
              />
              <button
                type="button"
                className={styles.eyeBtn}
                onClick={() => setShowPassword((v) => !v)}
                tabIndex={-1}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                <EyeIcon open={showPassword} />
              </button>
            </div>
            {errors.password && <span className={styles.fieldError}>{errors.password}</span>}
          </div>

          <div className={styles.field}>
            <label className={styles.label}>
              CAPTCHA <span className={styles.hint}>— type the characters you see</span>
            </label>

            {captchaError && <Alert type="error" message={captchaError} />}

            <div className={styles.captchaRow}>
              {captchaLoading ? (
                <div className={styles.captchaPlaceholder}>
                  <span className={styles.captchaSpinner} /> Loading...
                </div>
              ) : captchaUrl ? (
                <img
                  src={captchaUrl}
                  alt="CAPTCHA"
                  className={styles.captchaImg}
                  onClick={loadCaptcha}
                  title="Click to refresh"
                />
              ) : (
                <div className={styles.captchaPlaceholder}>No image</div>
              )}
              <button
                type="button"
                className={styles.refreshBtn}
                onClick={loadCaptcha}
                disabled={captchaLoading}
                title="Get a new CAPTCHA"
                aria-label="Refresh CAPTCHA"
              >
                <RefreshIcon />
              </button>
            </div>

            <input
              className={`${styles.input} ${errors.captchaAnswer ? styles.inputError : ''}`}
              type="text"
              name="captchaAnswer"
              value={form.captchaAnswer}
              onChange={handleChange}
              placeholder="Type the characters above"
              autoComplete="off"
              spellCheck={false}
              disabled={loading || captchaLoading}
            />
            {errors.captchaAnswer && <span className={styles.fieldError}>{errors.captchaAnswer}</span>}
          </div>

          <button type="submit" className={styles.submitBtn} disabled={loading || captchaLoading}>
            {loading ? (
              <>
                <span className={styles.btnSpinner} />
                Signing in...
              </>
            ) : (
              'Sign in'
            )}
          </button>
        </form>

        <p className={styles.footer}>
          Don't have an account?{' '}
          <Link to="/register" className={styles.footerLink}>Create one</Link>
        </p>
      </div>
    </div>
  )
}
