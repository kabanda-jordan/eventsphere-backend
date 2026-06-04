import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Alert from '../components/Alert'
import Spinner from '../components/Spinner'
import Modal, { ConfirmModal } from '../components/Modal'
import FormField, { Input, Select, Textarea } from '../components/FormField'
import {
  getDashboardStats,
  createEvent, updateEvent, deleteEvent,
  getUsers, createUser, updateUser, deleteUser,
  getStudents, updateStudent,
} from '../api/adminApi'
import { getEvents } from '../api/eventsApi'
import styles from './AdminDashboard.module.css'
import mStyles from '../components/Modal.module.css'

// ── Icons ─────────────────────────────────────────────────────
const PlusIcon  = () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
const EditIcon  = () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
const TrashIcon = () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/></svg>
const ChevL     = () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
const ChevR     = () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
const CalIcon   = () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
const UsersIcon = () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
const GradIcon  = () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>
const TickIcon  = () => <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2z"/></svg>
const SearchIcon = () => <svg className={styles.searchIcon} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>

// ── Helpers ───────────────────────────────────────────────────
function fmt(d) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })
}
function toDatetimeLocal(iso) {
  if (!iso) return ''
  const d = new Date(iso), p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`
}

const EV_BADGE = { ACTIVE: styles.badgeActive, CANCELLED: styles.badgeCancelled, COMPLETED: styles.badgeCompleted }
const ROLE_BADGE = { ADMIN: styles.badgeAdmin, STUDENT: styles.badgeStudent }

function Badge({ value, map }) {
  return (
    <span className={`${styles.badge} ${map?.[value] || styles.badgeCompleted}`}>
      <span className={styles.badgeDot} />{value}
    </span>
  )
}

function Pages({ page, totalPages, total, size, onPrev, onNext }) {
  const from = total === 0 ? 0 : page * size + 1
  const to   = Math.min((page + 1) * size, total)
  return (
    <div className={styles.pagination}>
      <span>{total === 0 ? 'No results' : `${from}–${to} of ${total}`}</span>
      <div className={styles.paginationBtns}>
        <button className={styles.pageBtn} disabled={page === 0} onClick={onPrev}><ChevL /> Prev</button>
        <button className={styles.pageBtn} disabled={page >= totalPages - 1} onClick={onNext}>Next <ChevR /></button>
      </div>
    </div>
  )
}

// ── Event Form ────────────────────────────────────────────────
function EventForm({ event, onClose, onSaved }) {
  const isEdit = !!event
  const [form, setForm] = useState({
    title: event?.title || '', description: event?.description || '',
    eventDate: event?.eventDate ? toDatetimeLocal(event.eventDate) : '',
    location: event?.location || '', capacity: event?.capacity || '',
    status: event?.status || 'ACTIVE',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [serverError, setServerError] = useState('')

  const set = (k, v) => { setForm(f => ({ ...f, [k]: v })); setErrors(e => ({ ...e, [k]: '' })) }

  const validate = () => {
    const e = {}
    if (!form.title.trim())                        e.title    = 'Required'
    if (!form.eventDate)                           e.eventDate = 'Required'
    if (!form.location.trim())                     e.location  = 'Required'
    if (!form.capacity || Number(form.capacity)<1) e.capacity  = 'Min 1'
    return e
  }

  const submit = async (e) => {
    e.preventDefault(); setServerError('')
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setLoading(true)
    try {
      const payload = {
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        eventDate: new Date(form.eventDate).toISOString().slice(0, 19),
        location: form.location.trim(),
        capacity: Number(form.capacity),
        status: form.status,
      }
      isEdit ? await updateEvent(event.id, payload) : await createEvent(payload)
      onSaved()
    } catch (err) { setServerError(err.response?.data?.message || 'Save failed.') }
    finally { setLoading(false) }
  }

  return (
    <Modal title={isEdit ? 'Edit Event' : 'New Event'} onClose={onClose} size="lg"
      footer={<>
        <button className={mStyles.btnSecondary} onClick={onClose} disabled={loading}>Cancel</button>
        <button className={mStyles.btnPrimary} onClick={submit} disabled={loading}>
          {loading ? <><span className={mStyles.btnSpinner}/>{isEdit?'Saving...':'Creating...'}</> : isEdit?'Save changes':'Create event'}
        </button>
      </>}>
      {serverError && <Alert type="error" message={serverError}/>}
      <div className={styles.formGrid}>
        <div className={styles.formGridFull}>
          <FormField label="Title" required error={errors.title}>
            <Input value={form.title} onChange={e=>set('title',e.target.value)} placeholder="Event title" disabled={loading} error={errors.title}/>
          </FormField>
        </div>
        <div className={styles.formGridFull}>
          <FormField label="Description">
            <Textarea value={form.description} onChange={e=>set('description',e.target.value)} placeholder="Optional" disabled={loading}/>
          </FormField>
        </div>
        <FormField label="Date & Time" required error={errors.eventDate}>
          <Input type="datetime-local" value={form.eventDate} onChange={e=>set('eventDate',e.target.value)} disabled={loading} error={errors.eventDate}/>
        </FormField>
        <FormField label="Location" required error={errors.location}>
          <Input value={form.location} onChange={e=>set('location',e.target.value)} placeholder="Room / Building" disabled={loading} error={errors.location}/>
        </FormField>
        <FormField label="Capacity" required error={errors.capacity}>
          <Input type="number" min="1" value={form.capacity} onChange={e=>set('capacity',e.target.value)} placeholder="100" disabled={loading} error={errors.capacity}/>
        </FormField>
        <FormField label="Status">
          <Select value={form.status} onChange={e=>set('status',e.target.value)} disabled={loading}>
            <option value="ACTIVE">Active</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="COMPLETED">Completed</option>
          </Select>
        </FormField>
      </div>
    </Modal>
  )
}

// ── User Form ─────────────────────────────────────────────────
function UserForm({ user, onClose, onSaved }) {
  const isEdit = !!user
  const [form, setForm] = useState({
    username: user?.username||'', email: user?.email||'', fullName: user?.fullName||'',
    password: '', role: user?.role||'STUDENT',
    enabled: user?.enabled !== undefined ? String(user.enabled) : 'true',
    studentId: '', department: '', yearOfStudy: '', phone: '',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [serverError, setServerError] = useState('')
  const set = (k,v) => { setForm(f=>({...f,[k]:v})); setErrors(e=>({...e,[k]:''})) }

  const validate = () => {
    const e = {}
    if (!isEdit) {
      if (!form.username.trim()||form.username.length<3) e.username='Min 3 chars'
      if (!form.email.trim()||!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email='Valid email required'
      if (!form.fullName.trim()) e.fullName='Required'
      if (!form.password||form.password.length<8) e.password='Min 8 chars'
    }
    return e
  }

  const submit = async (ev) => {
    ev.preventDefault(); setServerError('')
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setLoading(true)
    try {
      if (isEdit) {
        await updateUser(user.id, { fullName:form.fullName||undefined, email:form.email||undefined, role:form.role, enabled:form.enabled==='true' })
      } else {
        await createUser({
          username:form.username.trim(), email:form.email.trim(), fullName:form.fullName.trim(),
          password:form.password, role:form.role,
          studentId: form.role==='STUDENT' ? form.studentId.trim()||undefined : undefined,
          department:form.department.trim()||undefined,
          yearOfStudy:form.yearOfStudy?Number(form.yearOfStudy):undefined,
          phone:form.phone.trim()||undefined,
        })
      }
      onSaved()
    } catch (err) { setServerError(err.response?.data?.message||'Save failed.') }
    finally { setLoading(false) }
  }

  return (
    <Modal title={isEdit?'Edit User':'New User'} onClose={onClose} size="lg"
      footer={<>
        <button className={mStyles.btnSecondary} onClick={onClose} disabled={loading}>Cancel</button>
        <button className={mStyles.btnPrimary} onClick={submit} disabled={loading}>
          {loading?<><span className={mStyles.btnSpinner}/>{isEdit?'Saving...':'Creating...'}</>:isEdit?'Save changes':'Create user'}
        </button>
      </>}>
      {serverError && <Alert type="error" message={serverError}/>}
      <div className={styles.formGrid}>
        {!isEdit && <>
          <FormField label="Username" required error={errors.username}>
            <Input value={form.username} onChange={e=>set('username',e.target.value)} placeholder="johndoe" disabled={loading} error={errors.username}/>
          </FormField>
          <FormField label="Password" required error={errors.password}>
            <Input type="password" value={form.password} onChange={e=>set('password',e.target.value)} placeholder="Min 8 chars" disabled={loading} error={errors.password}/>
          </FormField>
        </>}
        <FormField label="Full Name" required={!isEdit} error={errors.fullName}>
          <Input value={form.fullName} onChange={e=>set('fullName',e.target.value)} placeholder="John Doe" disabled={loading} error={errors.fullName}/>
        </FormField>
        <FormField label="Email" required={!isEdit} error={errors.email}>
          <Input type="email" value={form.email} onChange={e=>set('email',e.target.value)} placeholder="john@example.com" disabled={loading} error={errors.email}/>
        </FormField>
        <FormField label="Role">
          <Select value={form.role} onChange={e=>set('role',e.target.value)} disabled={loading||isEdit}>
            <option value="STUDENT">Student</option><option value="ADMIN">Admin</option>
          </Select>
        </FormField>
        {isEdit && <FormField label="Status">
          <Select value={form.enabled} onChange={e=>set('enabled',e.target.value)} disabled={loading}>
            <option value="true">Enabled</option><option value="false">Disabled</option>
          </Select>
        </FormField>}
        {!isEdit && form.role==='STUDENT' && <>
          <FormField label="Student ID"><Input value={form.studentId} onChange={e=>set('studentId',e.target.value)} placeholder="STU001" disabled={loading}/></FormField>
          <FormField label="Department"><Input value={form.department} onChange={e=>set('department',e.target.value)} placeholder="Computer Science" disabled={loading}/></FormField>
          <FormField label="Year of Study"><Input type="number" min="1" max="6" value={form.yearOfStudy} onChange={e=>set('yearOfStudy',e.target.value)} placeholder="2" disabled={loading}/></FormField>
          <FormField label="Phone"><Input value={form.phone} onChange={e=>set('phone',e.target.value)} placeholder="+1234567890" disabled={loading}/></FormField>
        </>}
      </div>
    </Modal>
  )
}

// ── Student Edit Form ─────────────────────────────────────────
function StudentForm({ student, onClose, onSaved }) {
  const [form, setForm] = useState({ department:student?.department||'', yearOfStudy:student?.yearOfStudy||'', phone:student?.phone||'' })
  const [loading, setLoading] = useState(false)
  const [serverError, setServerError] = useState('')
  const set = (k,v) => setForm(f=>({...f,[k]:v}))

  const submit = async (e) => {
    e.preventDefault(); setServerError(''); setLoading(true)
    try {
      await updateStudent(student.id, { department:form.department.trim()||undefined, yearOfStudy:form.yearOfStudy?Number(form.yearOfStudy):undefined, phone:form.phone.trim()||undefined })
      onSaved()
    } catch (err) { setServerError(err.response?.data?.message||'Update failed.') }
    finally { setLoading(false) }
  }

  return (
    <Modal title="Edit Student" onClose={onClose}
      footer={<>
        <button className={mStyles.btnSecondary} onClick={onClose} disabled={loading}>Cancel</button>
        <button className={mStyles.btnPrimary} onClick={submit} disabled={loading}>
          {loading?<><span className={mStyles.btnSpinner}/>Saving...</>:'Save changes'}
        </button>
      </>}>
      {serverError && <Alert type="error" message={serverError}/>}
      <div style={{marginBottom:'1rem',padding:'0.75rem 1rem',background:'var(--surface-2)',borderRadius:'var(--radius)',fontSize:'0.875rem'}}>
        <strong>{student.fullName}</strong> &nbsp;·&nbsp; <span style={{color:'var(--text-muted)'}}>{student.studentId}</span>
      </div>
      <div className={styles.formGrid}>
        <FormField label="Department"><Input value={form.department} onChange={e=>set('department',e.target.value)} placeholder="Computer Science" disabled={loading}/></FormField>
        <FormField label="Year of Study"><Input type="number" min="1" max="6" value={form.yearOfStudy} onChange={e=>set('yearOfStudy',e.target.value)} placeholder="2" disabled={loading}/></FormField>
        <FormField label="Phone"><Input value={form.phone} onChange={e=>set('phone',e.target.value)} placeholder="+1234567890" disabled={loading}/></FormField>
      </div>
    </Modal>
  )
}

// ── Events Tab ────────────────────────────────────────────────
function EventsTab() {
  const [events, setEvents] = useState([])
  const [pg, setPg] = useState({ page:0, totalPages:0, total:0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [statusF, setStatusF] = useState('')
  const [page, setPage] = useState(0)
  const [modal, setModal] = useState(null)
  const [delLoading, setDelLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const params = { page, size:10 }
      if (search.trim()) params.search = search.trim()
      if (statusF) params.status = statusF
      const res = await getEvents(params)
      const d = res.data.data
      setEvents(d.content)
      setPg({ page:d.page, totalPages:d.totalPages, total:d.totalElements })
    } catch (err) { setError(err.response?.data?.message||'Failed to load events.') }
    finally { setLoading(false) }
  }, [page, search, statusF])

  useEffect(() => { load() }, [load])
  useEffect(() => { setPage(0) }, [search, statusF])

  const handleDel = async () => {
    setDelLoading(true)
    try { await deleteEvent(modal.event.id); setModal(null); load() }
    catch (err) { setError(err.response?.data?.message||'Delete failed.'); setModal(null) }
    finally { setDelLoading(false) }
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.toolbarLeft}>
          <div className={styles.searchWrap}>
            <SearchIcon/>
            <input className={styles.searchInput} placeholder="Search events..." value={search} onChange={e=>setSearch(e.target.value)}/>
          </div>
          <select className={styles.filterSelect} value={statusF} onChange={e=>setStatusF(e.target.value)}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="CANCELLED">Cancelled</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </div>
        <button className={styles.addBtn} onClick={()=>setModal({type:'create'})}><PlusIcon/> New Event</button>
      </div>
      {error && <Alert type="error" message={error}/>}
      <div className={styles.tableWrap}>
        {loading ? <Spinner text="Loading events..."/> : events.length===0 ? (
          <div className={styles.empty}><p className={styles.emptyTitle}>No events found</p></div>
        ) : (
          <table className={styles.table}>
            <thead><tr><th>Title</th><th>Date</th><th>Location</th><th>Capacity</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {events.map(ev=>(
                <tr key={ev.id}>
                  <td><span className={styles.cellBold}>{ev.title}</span></td>
                  <td><span className={styles.cellMuted}>{fmt(ev.eventDate)}</span></td>
                  <td><span className={styles.cellMuted}>{ev.location}</span></td>
                  <td><span className={styles.cellMuted}>{ev.registrationsCount}/{ev.capacity}</span></td>
                  <td><Badge value={ev.status} map={EV_BADGE}/></td>
                  <td>
                    <div className={styles.actions}>
                      <button className={`${styles.iconBtn} ${styles.iconBtnEdit}`} title="Edit" onClick={()=>setModal({type:'edit',event:ev})}><EditIcon/></button>
                      <button className={`${styles.iconBtn} ${styles.iconBtnDelete}`} title="Delete" onClick={()=>setModal({type:'delete',event:ev})}><TrashIcon/></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && pg.totalPages>0 && <Pages page={page} totalPages={pg.totalPages} total={pg.total} size={10} onPrev={()=>setPage(p=>p-1)} onNext={()=>setPage(p=>p+1)}/>}
      </div>
      {modal?.type==='create' && <EventForm onClose={()=>setModal(null)} onSaved={()=>{setModal(null);load()}}/>}
      {modal?.type==='edit'   && <EventForm event={modal.event} onClose={()=>setModal(null)} onSaved={()=>{setModal(null);load()}}/>}
      {modal?.type==='delete' && <ConfirmModal title="Delete event?" message={`"${modal.event.title}" will be permanently deleted.`} onConfirm={handleDel} onCancel={()=>setModal(null)} loading={delLoading}/>}
    </div>
  )
}

// ── Users Tab ─────────────────────────────────────────────────
function UsersTab() {
  const [users, setUsers] = useState([])
  const [pg, setPg] = useState({ page:0, totalPages:0, total:0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [roleF, setRoleF] = useState('')
  const [page, setPage] = useState(0)
  const [modal, setModal] = useState(null)
  const [delLoading, setDelLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const params = { page, size:10 }
      if (search.trim()) params.search = search.trim()
      if (roleF) params.role = roleF
      const res = await getUsers(params)
      const d = res.data.data
      setUsers(d.content)
      setPg({ page:d.page, totalPages:d.totalPages, total:d.totalElements })
    } catch (err) { setError(err.response?.data?.message||'Failed to load users.') }
    finally { setLoading(false) }
  }, [page, search, roleF])

  useEffect(() => { load() }, [load])
  useEffect(() => { setPage(0) }, [search, roleF])

  const handleDel = async () => {
    setDelLoading(true)
    try { await deleteUser(modal.user.id); setModal(null); load() }
    catch (err) { setError(err.response?.data?.message||'Delete failed.'); setModal(null) }
    finally { setDelLoading(false) }
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.toolbarLeft}>
          <div className={styles.searchWrap}>
            <SearchIcon/>
            <input className={styles.searchInput} placeholder="Search users..." value={search} onChange={e=>setSearch(e.target.value)}/>
          </div>
          <select className={styles.filterSelect} value={roleF} onChange={e=>setRoleF(e.target.value)}>
            <option value="">All roles</option><option value="ADMIN">Admin</option><option value="STUDENT">Student</option>
          </select>
        </div>
        <button className={styles.addBtn} onClick={()=>setModal({type:'create'})}><PlusIcon/> New User</button>
      </div>
      {error && <Alert type="error" message={error}/>}
      <div className={styles.tableWrap}>
        {loading ? <Spinner text="Loading users..."/> : users.length===0 ? (
          <div className={styles.empty}><p className={styles.emptyTitle}>No users found</p></div>
        ) : (
          <table className={styles.table}>
            <thead><tr><th>Name</th><th>Username</th><th>Email</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {users.map(u=>(
                <tr key={u.id}>
                  <td><span className={styles.cellBold}>{u.fullName||'—'}</span></td>
                  <td><span className={styles.cellMono}>{u.username}</span></td>
                  <td><span className={styles.cellMuted}>{u.email}</span></td>
                  <td><Badge value={u.role} map={ROLE_BADGE}/></td>
                  <td><span className={`${styles.badge} ${u.enabled?styles.badgeEnabled:styles.badgeDisabled}`}><span className={styles.badgeDot}/>{u.enabled?'Enabled':'Disabled'}</span></td>
                  <td>
                    <div className={styles.actions}>
                      <button className={`${styles.iconBtn} ${styles.iconBtnEdit}`} title="Edit" onClick={()=>setModal({type:'edit',user:u})}><EditIcon/></button>
                      <button className={`${styles.iconBtn} ${styles.iconBtnDelete}`} title="Delete" onClick={()=>setModal({type:'delete',user:u})}><TrashIcon/></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && pg.totalPages>0 && <Pages page={page} totalPages={pg.totalPages} total={pg.total} size={10} onPrev={()=>setPage(p=>p-1)} onNext={()=>setPage(p=>p+1)}/>}
      </div>
      {modal?.type==='create' && <UserForm onClose={()=>setModal(null)} onSaved={()=>{setModal(null);load()}}/>}
      {modal?.type==='edit'   && <UserForm user={modal.user} onClose={()=>setModal(null)} onSaved={()=>{setModal(null);load()}}/>}
      {modal?.type==='delete' && <ConfirmModal title="Delete user?" message={`"${modal.user.username}" will be permanently deleted.`} onConfirm={handleDel} onCancel={()=>setModal(null)} loading={delLoading}/>}
    </div>
  )
}

// ── Students Tab ──────────────────────────────────────────────
function StudentsTab() {
  const [students, setStudents] = useState([])
  const [pg, setPg] = useState({ page:0, totalPages:0, total:0 })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [modal, setModal] = useState(null)

  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const params = { page, size:10 }
      if (search.trim()) params.search = search.trim()
      const res = await getStudents(params)
      const d = res.data.data
      setStudents(d.content)
      setPg({ page:d.page, totalPages:d.totalPages, total:d.totalElements })
    } catch (err) { setError(err.response?.data?.message||'Failed to load students.') }
    finally { setLoading(false) }
  }, [page, search])

  useEffect(() => { load() }, [load])
  useEffect(() => { setPage(0) }, [search])

  return (
    <div>
      <div className={styles.toolbar}>
        <div className={styles.toolbarLeft}>
          <div className={styles.searchWrap}>
            <SearchIcon/>
            <input className={styles.searchInput} placeholder="Search students..." value={search} onChange={e=>setSearch(e.target.value)}/>
          </div>
        </div>
      </div>
      {error && <Alert type="error" message={error}/>}
      <div className={styles.tableWrap}>
        {loading ? <Spinner text="Loading students..."/> : students.length===0 ? (
          <div className={styles.empty}><p className={styles.emptyTitle}>No students found</p></div>
        ) : (
          <table className={styles.table}>
            <thead><tr><th>Name</th><th>Student ID</th><th>Department</th><th>Year</th><th>Phone</th><th>Actions</th></tr></thead>
            <tbody>
              {students.map(s=>(
                <tr key={s.id}>
                  <td>
                    <span className={styles.cellBold}>{s.fullName}</span>
                    <div className={styles.cellMuted} style={{fontSize:'0.75rem'}}>{s.email}</div>
                  </td>
                  <td><span className={styles.cellMono}>{s.studentId}</span></td>
                  <td><span className={styles.cellMuted}>{s.department||'—'}</span></td>
                  <td><span className={styles.cellMuted}>{s.yearOfStudy||'—'}</span></td>
                  <td><span className={styles.cellMuted}>{s.phone||'—'}</span></td>
                  <td>
                    <div className={styles.actions}>
                      <button className={`${styles.iconBtn} ${styles.iconBtnEdit}`} title="Edit" onClick={()=>setModal({type:'edit',student:s})}><EditIcon/></button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && pg.totalPages>0 && <Pages page={page} totalPages={pg.totalPages} total={pg.total} size={10} onPrev={()=>setPage(p=>p-1)} onNext={()=>setPage(p=>p+1)}/>}
      </div>
      {modal?.type==='edit' && <StudentForm student={modal.student} onClose={()=>setModal(null)} onSaved={()=>{setModal(null);load()}}/>}
    </div>
  )
}

// ── Stat Card ─────────────────────────────────────────────────
function StatCard({ icon, cls, value, label }) {
  return (
    <div className={styles.statCard}>
      <div className={`${styles.statIcon} ${cls}`}>{icon}</div>
      <div className={styles.statValue}>{value ?? '—'}</div>
      <div className={styles.statLabel}>{label}</div>
    </div>
  )
}

// ── Main Page ─────────────────────────────────────────────────
const TABS = [
  { id:'events',   label:'Events',   icon:<CalIcon/> },
  { id:'users',    label:'Users',    icon:<UsersIcon/> },
  { id:'students', label:'Students', icon:<GradIcon/> },
]

export default function AdminDashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [tab, setTab] = useState('events')
  const [stats, setStats] = useState(null)

  useEffect(() => {
    if (user && user.role !== 'ADMIN') navigate('/events', { replace:true })
  }, [user, navigate])

  useEffect(() => {
    getDashboardStats()
      .then(r => setStats(r.data.data))
      .catch(() => {})
  }, [])

  const totalEvents = stats
    ? Object.values(stats.eventsByStatus || {}).reduce((a,b) => a+b, 0)
    : null

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1 className={styles.pageTitle}>Dashboard</h1>
          <p className={styles.pageSub}>Manage events, users, and students</p>
        </div>
      </div>

      {stats && (
        <div className={styles.statsGrid}>
          <StatCard icon={<CalIcon/>}   cls={styles.statIconPurple} value={totalEvents}                    label="Total Events"/>
          <StatCard icon={<CalIcon/>}   cls={styles.statIconGreen}  value={stats.eventsByStatus?.ACTIVE??0} label="Active Events"/>
          <StatCard icon={<GradIcon/>}  cls={styles.statIconBlue}   value={stats.totalStudents}             label="Students"/>
          <StatCard icon={<TickIcon/>}  cls={styles.statIconOrange} value={stats.totalRegistrations}        label="Registrations"/>
        </div>
      )}

      <div className={styles.tabs}>
        {TABS.map(t => (
          <button key={t.id} className={`${styles.tab} ${tab===t.id?styles.tabActive:''}`} onClick={()=>setTab(t.id)}>
            {t.icon}{t.label}
          </button>
        ))}
      </div>

      {tab==='events'   && <EventsTab/>}
      {tab==='users'    && <UsersTab/>}
      {tab==='students' && <StudentsTab/>}
    </div>
  )
}
