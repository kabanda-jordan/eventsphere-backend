import { useState, useEffect, useCallback } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { verify2FA, sendOtp } from '../api/authApi'
import { useAuth } from '../context/AuthContext'
import Alert from '../components/Alert'
import styles from './AuthPage.module.css'

const RESEND_COOLDOWN = 60 // seconds before resend is allowed again

export default function Verify2FAPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { login: authLogin } = useAuth()

  const username = location.state?.username || ''

  const [otpCode, setOtpCode] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // Resend state
  const [resendLoading, setResendLoading] = useState(false)
  const [resendMsg, setResendMsg] = useState('')
  const [cooldown, setCooldown] = useState(0) // seconds remaining

  // Countdown ticker
  useEffect(() => {
    if (cooldown <= 0) return
    const id = setTimeout(() => setCooldown(c => c - 1), 1000)
    return () => clearTimeout(id)
  }, [cooldown])

  const handleResend = useCallback(async () => {
    if (!username || cooldown > 0) return
    setResendLoading(true)
    setResendMsg('')
    setError('')
    try {
      const res = await sendOtp(username)
      setResendMsg(res.data.message || 'A new code has been sent to your email.')
      setCooldown(RESEND_COOLDOWN)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to resend OTP. Please try again.')
    } finally {
      setResendLoading(false)
    }
  }, [username, cooldown])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!otpCode.trim() || otpCode.length !== 6) {
      setError('Please enter the 6-digit OTP code')
      return
    }
    setLoading(true)
    setError('')
    setResendMsg('')
    try {
      const res = await verify2FA({ username, otpCode })
      authLogin(res.data.data)
      navigate('/events')
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid or expired OTP. Please try again.')
      setOtpCode('')
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
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
            </svg>
          </div>
          <h1 className={styles.title}>Check your email</h1>
          <p className={styles.subtitle}>
            We sent a 6-digit code to
            {username
              ? <><br /><strong style={{ color: 'var(--text)' }}>{username}</strong></>
              : ' your email address'}
          </p>
        </div>

        {resendMsg && <Alert type="success" message={resendMsg} />}
        {error     && <Alert type="error"   message={error} />}

        <form onSubmit={handleSubmit} noValidate className={styles.form}>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="otpCode">
              Verification code
            </label>
            <input
              id="otpCode"
              className={styles.input}
              type="text"
              inputMode="numeric"
              maxLength={6}
              value={otpCode}
              onChange={(e) => {
                setOtpCode(e.target.value.replace(/\D/g, ''))
                setError('')
              }}
              placeholder="000000"
              autoFocus
              disabled={loading}
              style={{
                fontSize: '1.75rem',
                letterSpacing: '0.6rem',
                textAlign: 'center',
                fontWeight: '700',
                padding: '0.75rem 1rem',
              }}
            />
            <span className={styles.fieldError} style={{ color: 'var(--text-muted)', fontWeight: 400 }}>
              Code expires in 5 minutes
            </span>
          </div>

          <button type="submit" className={styles.submitBtn} disabled={loading}>
            {loading ? (
              <><span className={styles.btnSpinner} /> Verifying...</>
            ) : (
              'Verify code'
            )}
          </button>
        </form>

        {/* Resend section */}
        <div className={styles.footer}>
          Didn't receive it?{' '}
          {cooldown > 0 ? (
            <span style={{ color: 'var(--text-muted)' }}>
              Resend in {cooldown}s
            </span>
          ) : (
            <button
              type="button"
              onClick={handleResend}
              disabled={resendLoading || !username}
              style={{
                background: 'none',
                border: 'none',
                padding: 0,
                color: 'var(--primary)',
                fontWeight: 600,
                fontSize: 'inherit',
                cursor: resendLoading ? 'not-allowed' : 'pointer',
                opacity: resendLoading ? 0.6 : 1,
              }}
            >
              {resendLoading ? 'Sending...' : 'Resend code'}
            </button>
          )}
        </div>

        <p className={styles.footer} style={{ marginTop: '0.5rem' }}>
          <Link to="/login" className={styles.footerLink}>← Back to sign in</Link>
        </p>
      </div>
    </div>
  )
}
