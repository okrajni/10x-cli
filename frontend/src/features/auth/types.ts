export interface User {
  id: string
  email: string
  name?: string
  householdId?: string
}

export interface AuthState {
  user: User | null
  token: string | null
  refreshToken: string | null
  expiresAt: number | null
  isLoading: boolean
  isAuthenticated: boolean
  error: string | null
}

export type AuthAction =
  | { type: 'LOGIN_START' }
  | { type: 'LOGIN_SUCCESS'; payload: { user: User; token: string; refreshToken: string; expiresAt: number } }
  | { type: 'LOGIN_ERROR'; payload: string }
  | { type: 'LOGOUT' }
  | { type: 'REFRESH_TOKEN_START' }
  | { type: 'REFRESH_TOKEN_SUCCESS'; payload: { token: string; expiresAt: number } }
  | { type: 'REFRESH_TOKEN_ERROR' }
  | { type: 'CLEAR_ERROR' }
  | { type: 'RESTORE_SESSION'; payload: AuthState }
