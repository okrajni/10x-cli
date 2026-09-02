import { lazy, Suspense } from 'react'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import { ProtectedRoute } from '@lib/router/ProtectedRoute'
import Layout from '@shared/Layout'
import { Button, Spinner } from '@shared/components'

// Lazy-loaded pages (code-splitting)
const LoginPage = lazy(() => import('@features/auth/pages/LoginPage'))
const RegisterPage = lazy(() => import('@features/auth/pages/RegisterPage'))
const DashboardPage = lazy(() => import('@features/tasks/pages/DashboardPage'))
const TaskListPage = lazy(() => import('@features/tasks/pages/TaskListPage'))
const TaskCreatePage = lazy(() => import('@features/tasks/pages/TaskCreatePage'))
const TaskEditPage = lazy(() => import('@features/tasks/pages/TaskEditPage'))
const SettingsPage = lazy(() => import('@features/auth/pages/SettingsPage'))
const HouseholdCreatePage = lazy(() => import('@features/household/pages/HouseholdCreatePage'))

// eslint-disable-next-line react-refresh/only-export-components
const SuspenseWrapper = ({ children }: { children: React.ReactNode }) => (
  <Suspense fallback={<Spinner />}>{children}</Suspense>
)

// eslint-disable-next-line react-refresh/only-export-components
function ErrorPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-6">
      <div className="surface max-w-md space-y-3 p-10 text-center">
        <h1 className="text-3xl font-semibold text-accent">Something went wrong</h1>
        <p className="text-sm font-light text-accent/70">
          The page could not be loaded. Please try again.
        </p>
      </div>
    </div>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-6">
      <div className="surface max-w-md space-y-5 p-10 text-center">
        <p className="font-serif text-5xl font-semibold text-accent">404</p>
        <p className="text-sm font-light text-accent/70">
          We couldn&apos;t find the page you were looking for.
        </p>
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
        path: 'household/create',
        element: (
          <ProtectedRoute>
            <SuspenseWrapper>
              <HouseholdCreatePage />
            </SuspenseWrapper>
          </ProtectedRoute>
        ),
      },
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
        path: 'task/create',
        element: (
          <ProtectedRoute>
            <SuspenseWrapper>
              <TaskCreatePage />
            </SuspenseWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'task/:id/edit',
        element: (
          <ProtectedRoute>
            <SuspenseWrapper>
              <TaskEditPage />
            </SuspenseWrapper>
          </ProtectedRoute>
        ),
      },
      {
        path: 'task',
        element: (
          <ProtectedRoute>
            <SuspenseWrapper>
              <TaskListPage />
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
