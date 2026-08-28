import { lazy, Suspense } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from '@lib/router/ProtectedRoute'
import Layout from '@shared/Layout'
import { Button } from '@shared/components'

// Lazy-loaded pages (code-splitting)
const LoginPage = lazy(() => import('@features/auth/pages/LoginPage'))
const RegisterPage = lazy(() => import('@features/auth/pages/RegisterPage'))
const DashboardPage = lazy(() => import('@features/tasks/pages/DashboardPage'))
const SettingsPage = lazy(() => import('@features/auth/pages/SettingsPage'))

// eslint-disable-next-line react-refresh/only-export-components
const SuspenseWrapper = ({ children }: { children: React.ReactNode }) => (
  <Suspense
    fallback={
      <div className="flex items-center justify-center h-screen">
        <p className="text-gray-500">Loading...</p>
      </div>
    }
  >
    {children}
  </Suspense>
)

// eslint-disable-next-line react-refresh/only-export-components
function ErrorPage() {
  return (
    <div className="flex items-center justify-center h-screen">
      <div className="text-center">
        <h1 className="text-3xl font-bold text-red-600 mb-2">Error</h1>
        <p className="text-gray-600">Something went wrong. Please try again.</p>
      </div>
    </div>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
function NotFoundPage() {
  return (
    <div className="flex items-center justify-center h-screen">
      <div className="text-center">
        <h1 className="text-3xl font-bold mb-2">404</h1>
        <p className="text-gray-600 mb-4">Page not found</p>
        <Button variant="primary" onClick={() => (window.location.href = '/dashboard')}>
          Go Home
        </Button>
      </div>
    </div>
  )
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    errorElement: <ErrorPage />,
    children: [
      // Public routes
      {
        path: 'login',
        element: (
          <SuspenseWrapper>
            <LoginPage />
          </SuspenseWrapper>
        ),
      },
      {
        path: 'register',
        element: (
          <SuspenseWrapper>
            <RegisterPage />
          </SuspenseWrapper>
        ),
      },

      // Protected routes
      {
        path: 'dashboard',
        element: (
          <ProtectedRoute>
            <SuspenseWrapper>
              <DashboardPage />
            </SuspenseWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'settings',
        element: (
          <ProtectedRoute>
            <SuspenseWrapper>
              <SettingsPage />
            </SuspenseWrapper>
          </ProtectedRoute>
        ),
      },

      // Redirect root to dashboard if authenticated, login otherwise
      {
        path: '',
        element: <Navigate to="/dashboard" replace />,
      },

      // Catch-all
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
])
