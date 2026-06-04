import { createContext, useContext, useState, useCallback } from 'react'
import { logout as apiLogout } from '../api/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('user')
      return stored ? JSON.parse(stored) : null
    } catch {
      return null
    }
  })

  const [token, setToken] = useState(() => localStorage.getItem('accessToken'))

  const login = useCallback((authData) => {
    localStorage.setItem('accessToken', authData.accessToken)
    localStorage.setItem('user', JSON.stringify(authData.user))
    setToken(authData.accessToken)
    setUser(authData.user)
  }, [])

  const logout = useCallback(async () => {
    try { await apiLogout() } catch { /* ignore */ }
    localStorage.removeItem('accessToken')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
