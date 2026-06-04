import { useEffect } from 'react'
import styles from './Modal.module.css'

const CloseIcon = () => (
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
  </svg>
)

export default function Modal({ title, onClose, children, size = 'md', footer }) {
  // Close on Escape
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [onClose])

  return (
    <div className={styles.overlay} onClick={(e) => { if (e.target === e.currentTarget) onClose() }}>
      <div className={`${styles.modal} ${size === 'lg' ? styles.modalLg : ''}`} role="dialog" aria-modal="true">
        <div className={styles.header}>
          <span className={styles.title}>{title}</span>
          <button className={styles.closeBtn} onClick={onClose} aria-label="Close">
            <CloseIcon />
          </button>
        </div>
        <div className={styles.body}>{children}</div>
        {footer && <div className={styles.footer}>{footer}</div>}
      </div>
    </div>
  )
}

export function ConfirmModal({ title, message, onConfirm, onCancel, loading, danger = true }) {
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onCancel() }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [onCancel])

  return (
    <div className={styles.overlay} onClick={(e) => { if (e.target === e.currentTarget) onCancel() }}>
      <div className={styles.modal} role="dialog" aria-modal="true">
        <div className={styles.body}>
          <div className={`${styles.confirmIcon} ${danger ? styles.confirmIconDanger : ''}`}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
          <p className={styles.confirmTitle}>{title}</p>
          <p className={styles.confirmText}>{message}</p>
        </div>
        <div className={styles.footer}>
          <button className={styles.btnSecondary} onClick={onCancel} disabled={loading}>Cancel</button>
          <button
            className={danger ? styles.btnDanger : styles.btnPrimary}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? <><span className={styles.btnSpinner} /> Deleting...</> : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  )
}
