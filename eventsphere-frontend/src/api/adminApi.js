import api from './axios'

// ── Dashboard ─────────────────────────────────────────────────
export const getDashboardStats = () =>
  api.get('/api/admin/dashboard')

// ── Events CRUD ───────────────────────────────────────────────
export const createEvent = (data) =>
  api.post('/api/events', data)

export const updateEvent = (id, data) =>
  api.put(`/api/events/${id}`, data)

export const deleteEvent = (id) =>
  api.delete(`/api/events/${id}`)

// ── Users CRUD ────────────────────────────────────────────────
export const getUsers = (params = {}) =>
  api.get('/api/users', { params })

export const getUser = (id) =>
  api.get(`/api/users/${id}`)

export const createUser = (data) =>
  api.post('/api/users', data)

export const updateUser = (id, data) =>
  api.put(`/api/users/${id}`, data)

export const deleteUser = (id) =>
  api.delete(`/api/users/${id}`)

// ── Students ──────────────────────────────────────────────────
export const getStudents = (params = {}) =>
  api.get('/api/students', { params })

export const getStudent = (id) =>
  api.get(`/api/students/${id}`)

export const updateStudent = (id, data) =>
  api.patch(`/api/students/${id}`, data)
