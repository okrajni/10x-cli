import { AuthState, AuthAction } from './types'

const initialState: AuthState = {
  user: null,
  token: null,
  refreshToken: null,
  expiresAt: null,
  isLoading: false,
  isAuthenticated: false,
  error: null,
  households: [],
  currentHousehold: null,
}

export function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN_START':
      return { ...state, isLoading: true, error: null }
    case 'LOGIN_SUCCESS':
    case 'REGISTER_SUCCESS':
      return {
        ...state,
        user: action.payload.user,
        token: action.payload.token,
        refreshToken: action.payload.refreshToken,
        expiresAt: action.payload.expiresAt,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      }
    case 'LOGIN_ERROR':
      return { ...state, isLoading: false, error: action.payload, isAuthenticated: false }
    case 'LOGOUT':
      return { ...initialState }
    case 'REFRESH_TOKEN_SUCCESS':
      return {
        ...state,
        token: action.payload.token,
        expiresAt: action.payload.expiresAt,
      }
    case 'REFRESH_TOKEN_ERROR':
      return { ...initialState, error: 'Session expired. Please log in again.' }
    case 'CLEAR_ERROR':
      return { ...state, error: null }
    case 'RESTORE_SESSION':
      return action.payload
    case 'SET_HOUSEHOLDS':
      return {
        ...state,
        households: action.payload.households,
        currentHousehold: action.payload.currentHousehold,
      }
    case 'SET_CURRENT_HOUSEHOLD':
      return {
        ...state,
        currentHousehold: action.payload,
      }
    default:
      return state
  }
}

export { initialState }
