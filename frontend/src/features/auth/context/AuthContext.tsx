import { createContext, useContext, useReducer, useCallback, useEffect, ReactNode } from 'react'
import { authReducer, initialState } from '../reducer'
import { AuthState, Household } from '../types'
import { loginApi, registerApi } from '../api'
import { getUserHouseholdsApi } from '@features/household/api'

interface AuthContextValue {
  user: AuthState['user']
  token: AuthState['token']
  expiresAt: AuthState['expiresAt']
  isLoading: AuthState['isLoading']
  isAuthenticated: AuthState['isAuthenticated']
  error: AuthState['error']
  households: AuthState['households']
  currentHousehold: AuthState['currentHousehold']
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refreshToken: () => Promise<void>
  clearError: () => void
  setCurrentHousehold: (household: Household) => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, initialState)

  // Login action
  const login = useCallback(async (email: string, password: string) => {
    dispatch({ type: 'LOGIN_START' })
    try {
      const result = await loginApi({ email, password })

      if (!result.ok) {
        throw new Error(result.message)
      }

      const expiresAt = new Date(result.data.expiresAt).getTime()

      dispatch({
        type: 'LOGIN_SUCCESS',
        payload: {
          user: {
            id: result.data.userId,
            email: result.data.email,
          },
          token: result.data.token,
          refreshToken: null,
          expiresAt,
        },
      })

      // Fetch households after successful login
      try {
        const householdsResult = await getUserHouseholdsApi()
        if (householdsResult.ok && Array.isArray(householdsResult.data)) {
          const households = householdsResult.data
          const currentHousehold: Household | null = households.length > 0 ? (households[0] ?? null) : null
          dispatch({
            type: 'SET_HOUSEHOLDS',
            payload: {
              households,
              currentHousehold,
            },
          })
        }
      } catch {
        // Silently fail if household fetch fails
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Login failed'
      dispatch({ type: 'LOGIN_ERROR', payload: message })
      throw error
    }
  }, [])

  // Register action
  const register = useCallback(async (email: string, password: string) => {
    dispatch({ type: 'LOGIN_START' })
    try {
      const result = await registerApi({ email, password })

      if (!result.ok) {
        throw new Error(result.message)
      }

      const expiresAt = new Date(result.data.expiresAt).getTime()

      dispatch({
        type: 'REGISTER_SUCCESS',
        payload: {
          user: {
            id: result.data.userId,
            email: result.data.email,
          },
          token: result.data.token,
          refreshToken: null,
          expiresAt,
        },
      })

      // Fetch households after successful registration
      try {
        const householdsResult = await getUserHouseholdsApi()
        if (householdsResult.ok && Array.isArray(householdsResult.data)) {
          const households = householdsResult.data
          const currentHousehold: Household | null = households.length > 0 ? (households[0] ?? null) : null
          dispatch({
            type: 'SET_HOUSEHOLDS',
            payload: {
              households,
              currentHousehold,
            },
          })
        }
      } catch {
        // Silently fail if household fetch fails
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Registration failed'
      dispatch({ type: 'LOGIN_ERROR', payload: message })
      throw error
    }
  }, [])

  // Logout action
  const logout = useCallback(async () => {
    try {
      // Attempt to call logout endpoint if available
      if (state.token) {
        await fetch('/api/auth/logout', {
          method: 'POST',
          headers: { Authorization: `Bearer ${state.token}` },
        }).catch(() => {
          // Silently fail if endpoint doesn't exist
        })
      }
    } finally {
      dispatch({ type: 'LOGOUT' })
    }
  }, [state.token])

  // Set current household action
  const setCurrentHousehold = useCallback((household: Household) => {
    dispatch({ type: 'SET_CURRENT_HOUSEHOLD', payload: household })
  }, [])

  // Refresh token action
  const refreshTokenAsync = useCallback(async () => {
    if (!state.refreshToken) {
      dispatch({ type: 'LOGOUT' })
      return
    }

    dispatch({ type: 'REFRESH_TOKEN_START' })
    try {
      const response = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: state.refreshToken }),
      })

      if (!response.ok) {
        throw new Error('Token refresh failed')
      }

      const data = await response.json()
      const expiresAt = Date.now() + 3600000 // 1 hour from now

      dispatch({
        type: 'REFRESH_TOKEN_SUCCESS',
        payload: {
          token: data.token,
          expiresAt,
        },
      })
    } catch {
      dispatch({ type: 'REFRESH_TOKEN_ERROR' })
    }
  }, [state.refreshToken])

  // Clear error action
  const clearError = useCallback(() => {
    dispatch({ type: 'CLEAR_ERROR' })
  }, [])

  // On mount, restore session from localStorage
  useEffect(() => {
    const savedState = localStorage.getItem('authState')
    if (savedState) {
      try {
        const parsed = JSON.parse(savedState)
        if (parsed.token && parsed.expiresAt) {
          // Check if token is not expired
          if (parsed.expiresAt > Date.now()) {
            dispatch({ type: 'RESTORE_SESSION', payload: parsed })

            // Refetch households after session restore
            getUserHouseholdsApi().then((householdsResult) => {
              if (householdsResult.ok && Array.isArray(householdsResult.data)) {
                const households = householdsResult.data
                const currentHousehold: Household | null = households.length > 0 ? (households[0] ?? null) : null
                dispatch({
                  type: 'SET_HOUSEHOLDS',
                  payload: {
                    households,
                    currentHousehold,
                  },
                })
              }
            }).catch(() => {
              // Silently fail if household fetch fails
            })
          }
        }
      } catch (err) {
        console.error('Failed to restore session:', err)
      }
    }
  }, [])

  // Save auth state to localStorage whenever it changes
  useEffect(() => {
    if (state.isAuthenticated && state.token) {
      const stateToSave = {
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken,
        expiresAt: state.expiresAt,
        isAuthenticated: state.isAuthenticated,
      }
      localStorage.setItem('authState', JSON.stringify(stateToSave))
      // Also set token in a cookie for API requests
      document.cookie = `authToken=${state.token}; path=/; SameSite=Strict`
    } else {
      localStorage.removeItem('authState')
      document.cookie = 'authToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;'
    }
  }, [state.isAuthenticated, state.token, state.user, state.refreshToken, state.expiresAt])

  const contextValue: AuthContextValue = {
    user: state.user,
    token: state.token,
    expiresAt: state.expiresAt,
    isLoading: state.isLoading,
    isAuthenticated: state.isAuthenticated,
    error: state.error,
    households: state.households,
    currentHousehold: state.currentHousehold,
    login,
    register,
    logout,
    refreshToken: refreshTokenAsync,
    clearError,
    setCurrentHousehold,
  }

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
