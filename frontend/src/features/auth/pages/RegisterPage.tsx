import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Button, Input } from '@shared/components'
import { registerApi } from '../api'
import { useAuth } from '../context/AuthContext'
import { acceptInvitationApi, getUserHouseholdsApi } from '@features/household/api'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { clearError } = useAuth()

  const inviteEmail = searchParams.get('email') || ''
  const inviteToken = searchParams.get('inviteToken') || ''

  const [email, setEmail] = useState(inviteEmail)
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    clearError()

    if (!email || !password) {
      setError('Email and password are required')
      return
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    if (password.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }

    setLoading(true)
    try {
      const result = await registerApi({ email, password })
      if (!result.ok) {
        setError(result.message || 'Registration failed')
        return
      }

      // Store token in localStorage similar to how login does it
      localStorage.setItem('authToken', result.data.token)

      // If there's an invitation token, accept it automatically
      if (inviteToken) {
        const inviteResult = await acceptInvitationApi(inviteToken)
        if (!inviteResult.ok) {
          console.error('Failed to accept invitation:', inviteResult.message)
          // Still redirect to dashboard even if invitation acceptance fails
        }
      }

      // Fetch households to decide where to redirect
      const householdsResult = await getUserHouseholdsApi()
      if (householdsResult.ok && householdsResult.data.length === 0) {
        // If no households, redirect to creation
        navigate('/household/create')
      } else {
        navigate('/dashboard')
      }
    } catch (err) {
      setError('An unexpected error occurred')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 to-blue-600 flex items-center justify-center">
      <div className="w-full max-w-md">
        <div className="bg-white rounded-lg shadow-lg p-8 space-y-6">
          <div className="text-center">
            <h1 className="text-3xl font-bold text-gray-900">done yet?</h1>
            <p className="text-gray-600 mt-2">Create your account</p>
            {inviteEmail && (
              <p className="text-sm text-gray-500 mt-1">
                Joining via invitation to <strong>{inviteEmail}</strong>
              </p>
            )}
          </div>

          <form className="space-y-4" onSubmit={handleSubmit}>
            {error && (
              <div className="rounded-md bg-red-50 p-3">
                <p className="text-sm text-red-800">{error}</p>
              </div>
            )}

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                Email
              </label>
              <Input
                id="email"
                type="email"
                placeholder="your@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={loading || !!inviteEmail}
                className="mt-1"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                Password
              </label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                className="mt-1"
              />
            </div>

            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
                Confirm Password
              </label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="••••••••"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                disabled={loading}
                className="mt-1"
              />
            </div>

            <Button variant="primary" disabled={loading} className="w-full">
              {loading ? 'Creating account...' : 'Sign Up'}
            </Button>
          </form>

          <p className="text-center text-sm text-gray-600">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-600 hover:underline font-medium">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
