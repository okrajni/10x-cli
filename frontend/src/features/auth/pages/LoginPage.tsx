import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Input, Button, Notice } from '@shared/components'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const navigate = useNavigate()
  const { login, error, isLoading, clearError } = useAuth()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    clearError()

    try {
      await login(email, password)
      navigate('/dashboard')
    } catch {
      // Error is in context
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-4 py-8">
      <div className="w-full max-w-md space-y-6">
        <div className="space-y-2 text-center">
          <h1 className="wordmark text-5xl sm:text-6xl">Done yet?</h1>
          <p className="text-xs sm:text-sm font-light text-accent/70">Sign in to your account</p>
        </div>

        <form onSubmit={handleSubmit} className="surface space-y-5 p-6 sm:p-8">
          {error && <Notice tone="error">{error}</Notice>}

          <Input
            id="email"
            label="Email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            required
          />

          <Input
            id="password"
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            required
          />

          <Button type="submit" variant="primary" disabled={isLoading} className="w-full">
            {isLoading ? 'Signing in…' : 'Sign In'}
          </Button>
        </form>

        <p className="text-center text-sm font-light text-accent/75">
          Don&apos;t have an account?{' '}
          <Link to="/register" className="link">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  )
}
