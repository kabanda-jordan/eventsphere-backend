import api from './axios'

/**
 * Auth API — integrates with /api/auth/* endpoints
 */

export const getCaptcha = () =>
  api.get('/api/auth/captcha', { responseType: 'blob' })

export const login = (data) =>
  api.post('/api/auth/login', data)

export const register = (data) =>
  api.post('/api/auth/register', data)

export const verify2FA = (data) =>
  api.post('/api/auth/verify-2fa', data)

export const sendOtp = (usernameOrEmail) =>
  api.post('/api/auth/send-otp', { usernameOrEmail })

export const logout = () =>
  api.post('/api/auth/logout')
