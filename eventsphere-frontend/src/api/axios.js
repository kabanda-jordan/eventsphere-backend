import axios from 'axios'

// Use empty baseURL so ALL requests go through the Vite dev proxy (/api → localhost:8081)
// This eliminates ALL CORS issues during development.
// In production, set VITE_API_BASE_URL to your deployed backend URL.
const BASE_URL = import.meta.env.PROD
  ? (import.meta.env.VITE_API_BASE_URL || '')
  : ''

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
  withCredentials: false,
})

// Attach JWT token to every request if present
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Global response error handler
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || ''
    const isAuthEndpoint = url.includes('/api/auth/')

    // Only force-logout on 401 for non-auth endpoints
    // (so login/register errors don't redirect to /login)
    if (error.response?.status === 401 && !isAuthEndpoint) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
