import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import styles from './Navbar.module.css'

const CalendarIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
    <line x1="16" y1="2" x2="16" y2="6"/>
    <line x1="8" y1="2" x2="8" y2="6"/>
    <line x1="3" y1="10" x2="21" y2="10"/>
  </svg>
)

export default function Navbar() {
  const { isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => { 
    await logout()
    navigate('/login') 
  }

  return (
    <nav className={styles.nav}>
      <div className={styles.inner}>
        {/* Left: Logo + Brand */}
        <Link to="/events" className={styles.brand}>
          <div className={styles.logoBox}>
            <CalendarIcon />
          </div>
          <span className={styles.brandName}>EventSphere</span>
        </Link>

        {/* Right: Links + Auth */}
        <div className={styles.right}>
          <Link to="/events" className={styles.link}>Events</Link>
          <div className={styles.divider} />
          {isAuthenticated ? (
            <button onClick={handleLogout} className={styles.link}>Sign out</button>
          ) : (
            <>
              <Link to="/login" className={styles.link}>Sign in</Link>
              <Link to="/register" className={styles.btnPrimary}>Get started</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
