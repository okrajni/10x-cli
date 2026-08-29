import { useEffect, useState } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import { Button } from '@shared/components'
import { acceptInvitationApi } from '../api'
import { useAuth } from '@features/auth/context/AuthContext'

export default function InvitationAcceptPage() {
  const { token } = useParams<{ token: string }>()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { isAuthenticated } = useAuth()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) {
      setError('Invalid invitation link')
      setLoading(false)
      return
    }

    if (!isAuthenticated) {
      const invitedEmail = searchParams.get('email')
      navigate(`/register?email=${invitedEmail || ''}&inviteToken=${token}`)
      return
    }

    handleAccept()
  }, [token, isAuthenticated])

  const handleAccept = async () => {
    if (!token) return

    setLoading(true)
    try {
      const result = await acceptInvitationApi(token)
      if (!result.ok) {
        if (result.code === 'RESOURCE_EXPIRED') {
          setError('This invitation has expired. Please ask your partner to send a new one.')
        } else if (result.code === 'CONFLICT') {
          setError('This invitation has already been accepted.')
        } else {
          setError(result.message || 'Failed to accept invitation')
        }
        return
      }

      navigate('/dashboard')
    } catch (err) {
      setError('An unexpected error occurred')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-500">Loading...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
        <div className="w-full max-w-md space-y-8 text-center">
          <h2 className="text-3xl font-bold tracking-tight text-gray-900">
            Invitation Error
          </h2>
          <div className="rounded-md bg-red-50 p-4">
            <p className="text-sm text-red-800">{error}</p>
          </div>
          <Button
            variant="primary"
            onClick={() => navigate('/dashboard')}
            className="w-full"
          >
            Go to Dashboard
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <p className="text-gray-500">Accepting invitation...</p>
    </div>
  )
}
