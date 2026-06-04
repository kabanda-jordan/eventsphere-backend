import { useState, useEffect, useRef } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { getCaptcha, register } from '../api/authApi'
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

function Field({ name, label, type = 'text', placeholder, required, value, onChange, error, disabled }) {
  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={name}>
        {label}{required && <span className={styles.required}>*</span>}
      </label>
      <input
        id={name}
        className={`${styles.input} ${error ? styles.inputError : ''}`}
        type={type}
        name={name}
        value={value}
        onChange={onChange}
        placeholder={placeholder}
        autoComplete={type === 'password' ? 'new-password' : 'off'}
        disabled={disabled}
      />
      {error && <span className={styles.fieldError}>{error}</span>}
    </div>
  )
}

export default function RegisterPage() {
  const navigate = useNavigate()
  const { login: authLogin, isAuthenticated } = useAuth()

  const [form, setForm] = useState({
    username: '', email: '', fullName: '', password: '',
    studentId: '', department: '', yearOfStudy: '', phone: '',
    captchaAnswer: '',
  })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')
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
      if (!token) {
        setCaptchaError('CAPTCHA token missing from server response.')
        return
      }
      const url = URL.createObjectURL(res.data)
      prevCaptchaUrl.current = url
      setCaptchaToken(token)
      setCaptchaUrl(url)
      setForm((f) => ({ ...f, captchaAnswer: '' }))
    } catch (err) {
      setCaptchaError(
        err.code === 'ERR_NETWORK'
          ? 'Cannot reach the server. Make sure the backend is running on port 8081.'
          : `Failed to load CAPTCHA: ${err.message}`
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
    if (!form.username.trim() || form.username.length < 3) e.username = 'At least 3 characters'
    if (!form.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email = 'Valid email required'
    if (!form.fullName.trim()) e.fullName = 'Full name is required'
    if (!form.password || form.password.length < 8) e.password = 'At least 8 characters'
    if (!form.studentId.trim()) e.studentId = 'Student ID is required'
    if (!form.captchaAnswer.trim()) e.captchaAnswer = 'Please type the CAPTCHA text'
    return e
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setForm((f) => ({ ...f, [name]: value }))
    setErrors((er) => ({ ...er, [name]: '' }))
    setServerError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setServerError('')
    setSuccessMsg('')
    const validationErrors = validate()
    if (Object.keys(validationErrors).length > 0) { setErrors(validationErrors); return }
    if (!captchaToken) { setCaptchaError('CAPTCHA not loaded. Click refresh.'); return }

    setLoading(true)
    try {
      const res = await register({
        username: form.username.trim(),
        email: form.email.trim(),
        fullName: form.fullName.trim(),
        password: form.password,
        studentId: form.studentId.trim(),
        department: form.department.trim() || undefined,
        yearOfStudy: form.yearOfStudy ? parseInt(form.yearOfStudy, 10) : undefined,
        phone: form.phone.trim() || undefined,
        captchaToken,
        captchaAnswer: form.captchaAnswer.trim(),
      })
      setSuccessMsg('Account created! Redirecting...')
      authLogin(res.data.data)
      setTimeout(() => navigate('/events'), 800)
    } catch (err) {
      const status = err.response?.status
      const msg = err.response?.data?.message
      if (status === 400 && msg?.toLowerCase().includes('captcha')) {
        setErrors((er) => ({ ...er, captchaAnswer: 'Wrong CAPTCHA — a new one has been loaded' }))
        loadCaptcha()
      } else if (status === 409) {
        setServerError(msg || 'An account with these details already exists.')
        loadCaptcha()
      } else if (status === 400) {
        setServerError(msg || 'Please check your input and try again.')
        loadCaptcha()
      } else if (err.code === 'ERR_NETWORK') {
        setServerError('Cannot reach the server. Make sure the backend is running.')
      } else {
        setServerError(msg || 'Registration failed. Please try again.')
        loadCaptcha()
      }
    } finally {
      setLoading(false)
    }
  }

  const fieldProps = (name) => ({
    name,
    value: form[name],
    onChange: handleChange,
    error: errors[name],
    disabled: loading,
  })

  return (
    <div className={styles.page}>
      <div className={`${styles.card} ${styles.cardWide}`}>
        <div className={styles.cardHeader}>
          <div className={styles.logoMark}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </div>
          <h1 className={styles.title}>Create your account</h1>
          <p className={styles.subtitle}>Join EventSphere as a student</p>
        </div>

        {serverError && <Alert type="error" message={serverError} />}
        {successMsg && <Alert type="success" message={successMsg} />}

        <form onSubmit={handleSubmit} noValidate className={styles.form}>
          <div className={styles.grid2}>
            <Field {...fieldProps('username')} label="Username" placeholder="johndoe" required />
            <Field {...fieldProps('email')} label="Email" type="email" placeholder="john@example.com" required />
            <Field {...fieldProps('fullName')} label="Full Name" placeholder="John Doe" required />

            {/* Password */}
            <div className={styles.field}>
              <label className={styles.label} htmlFor="password">
                Password<span className={styles.required}>*</span>
              </label>
              <div className={styles.passwordWrapper}>
                <input
                  id="password"
                  className={`${styles.input} ${styles.inputWithBtn} ${errors.password ? styles.inputError : ''}`}
                  type={showPassword ? 'text' : 'password'}
                  name="password"
                  value={form.password}
                  onChange={handleChange}
                  placeholder="Min 8 characters"
                  autoComplete="new-password"
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

            <Field {...fieldProps('studentId')} label="Student ID" placeholder="STU001" required />
            <Field {...fieldProps('department')} label="Department" placeholder="Computer Science" />
            <Field {...fieldProps('yearOfStudy')} label="Year of Study" type="number" placeholder="2" />
            <Field {...fieldProps('phone')} label="Phone" placeholder="+1234567890" />
          </div>

          {/* CAPTCHA */}
          <div className={styles.field}>
            <label className={styles.label}>
              CAPTCHA<span className={styles.required}>*</span>{' '}
              <span className={styles.hint}>— type the characters you see</span>
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
                Creating account...
              </>
            ) : (
              'Create account'
            )}
          </button>
        </form>

        <p className={styles.footer}>
          Already have an account?{' '}
          <Link to="/login" className={styles.footerLink}>Sign in</Link>
        </p>
      </div>
    </div>
  )
}
