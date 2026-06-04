import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import styles from './EventsPage.module.css'

const BADGE_CONFIG = {
  ACTIVE: { label: 'ACTIVE', bg: '#ECFDF5', color: '#166534' },
  COMPLETED: { label: 'COMPLETED', bg: '#F4F4F5', color: '#52525B' },
  CANCELLED: { label: 'CANCELLED', bg: '#F4F4F5', color: '#52525B' },
}

// Icons (Tabler outline style)
const SearchIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
  </svg>
)

const CalendarIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
  </svg>
)

const MapPinIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>
  </svg>
)

const UserIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
  </svg>
)

// Sample data for static demo
const EVENTS = [
  { id: 1, title: 'Past Event', desc: 'Already happened', date: '3 May 2026, 14:05', location: 'Hall A', organizer: 'Admin User', status: 'COMPLETED', seats: '0/100' },
  { id: 2, title: 'Upcoming Event', desc: 'Future event', date: '23 May 2026, 14:05', location: 'Hall B', organizer: 'Admin User', status: 'ACTIVE', seats: '1/50' },
  { id: 3, title: 'Admin Event', desc: 'Created by admin', date: '20 Jun 2026, 18:00', location: 'Main Hall', organizer: 'Admin User', status: 'ACTIVE', seats: '0/20' },
  { id: 4, title: 'Admin Event Updated', desc: 'Updated', date: '21 Jun 2026, 18:00', location: 'Conference Room', organizer: 'Admin User', status: 'ACTIVE', seats: '1/25' },
  { id: 5, title: 'School Meeting', desc: 'School-wide assembly', date: '1 Jun 2026, 09:30', location: 'School Main Hall', organizer: 'Admin User', status: 'ACTIVE', seats: '0/100' },
]

export default function EventsPage() {
  const { isAuthenticated } = useAuth()
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')

  // Filter events
  const filteredEvents = EVENTS.filter(event => {
    const matchesSearch = !search || 
      event.title.toLowerCase().includes(search.toLowerCase()) ||
      event.location.toLowerCase().includes(search.toLowerCase())
    const matchesStatus = !statusFilter || event.status === statusFilter
    return matchesSearch && matchesStatus
  })

  return (
    <div className={styles.page}>
      {/* Page Header */}
      <header className={styles.header}>
        <h1 className={styles.title}>Upcoming Events</h1>
        <p className={styles.subtitle}>{filteredEvents.length} events available</p>
      </header>

      {/* Search & Filter Bar */}
      <div className={styles.toolbar}>
        <div className={styles.searchBox}>
          <SearchIcon />
          <input
            type="text"
            placeholder="Search by title or location…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className={styles.searchInput}
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className={styles.statusSelect}
        >
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="COMPLETED">Completed</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
      </div>

      {/* Event Cards Grid */}
      <div className={styles.grid}>
        {filteredEvents.map((event, idx) => {
          const badge = BADGE_CONFIG[event.status] || BADGE_CONFIG.COMPLETED
          return (
            <article 
              key={event.id} 
              className={styles.card}
              style={{
                animation: `fadeInUp 240ms ease-out ${idx * 50}ms both`
              }}
            >
              {/* Top row: badge + seats */}
              <div className={styles.cardTop}>
                <span 
                  className={styles.badge}
                  style={{ 
                    backgroundColor: badge.bg, 
                    color: badge.color 
                  }}
                >
                  {badge.label}
                </span>
                <span className={styles.seats}>{event.seats}</span>
              </div>

              {/* Title */}
              <h2 className={styles.cardTitle}>{event.title}</h2>

              {/* Description */}
              <p className={styles.cardDesc}>{event.desc}</p>

              {/* Metadata */}
              <div className={styles.metadata}>
                <div className={styles.metaRow}>
                  <CalendarIcon />
                  <span>{event.date}</span>
                </div>
                <div className={styles.metaRow}>
                  <MapPinIcon />
                  <span>{event.location}</span>
                </div>
                <div className={styles.metaRow}>
                  <UserIcon />
                  <span>{event.organizer}</span>
                </div>
              </div>

              {/* Divider */}
              <div className={styles.divider} />

              {/* CTA Footer (conditional) */}
              {event.status === 'ACTIVE' && !isAuthenticated && (
                <p className={styles.cta}>
                  <Link to="/login" className={styles.ctaLink}>Sign in</Link>
                  {' '}to register for this event
                </p>
              )}
            </article>
          )
        })}
      </div>
    </div>
  )
}
