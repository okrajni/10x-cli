import { useAuth } from '@features/auth/context/AuthContext'
import { Navigate } from 'react-router-dom'
import { ReactNode } from 'react'
import { Spinner } from '@shared/components'

interface ProtectedRouteProps {
  children: ReactNode
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading } = useAuth()

  if (isLoading) {
    return <Spinner fullScreen />
  }

  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}
