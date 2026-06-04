import api from './axios'

/**
 * Events API — integrates with /api/events/* endpoints
 */

export const getEvents = (params = {}) =>
  api.get('/api/events', { params })

export const getEvent = (id) =>
  api.get(`/api/events/${id}`)

export const getUpcomingEvents = (limit = 5) =>
  api.get('/api/events/upcoming', { params: { limit } })

export const registerForEvent = (id) =>
  api.post(`/api/events/${id}/register`)

export const cancelRegistration = (id) =>
  api.delete(`/api/events/${id}/register`)
