import styles from './Spinner.module.css'

export default function Spinner({ size = 'md', text = 'Loading...' }) {
  return (
    <div className={styles.wrapper}>
      <div className={`${styles.spinner} ${styles[size]}`} />
      {text && <p className={styles.text}>{text}</p>}
    </div>
  )
}
