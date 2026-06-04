import styles from './FormField.module.css'

export default function FormField({ label, required, error, children, hint }) {
  return (
    <div className={styles.field}>
      {label && (
        <label className={styles.label}>
          {label}
          {required && <span className={styles.required}>*</span>}
          {hint && <span className={styles.hint}>{hint}</span>}
        </label>
      )}
      {children}
      {error && <span className={styles.error}>{error}</span>}
    </div>
  )
}

export function Input({ error, ...props }) {
  return (
    <input
      className={`${styles.input} ${error ? styles.inputError : ''}`}
      {...props}
    />
  )
}

export function Select({ error, children, ...props }) {
  return (
    <select
      className={`${styles.select} ${error ? styles.inputError : ''}`}
      {...props}
    >
      {children}
    </select>
  )
}

export function Textarea({ error, ...props }) {
  return (
    <textarea
      className={`${styles.textarea} ${error ? styles.inputError : ''}`}
      rows={3}
      {...props}
    />
  )
}
