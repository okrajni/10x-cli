import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Button, Input } from '@shared/components'
import { sendInvitationApi } from '../api'

export default function InvitePartnerPage() {
  const { householdId } = useParams<{ householdId: string }>()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  if (!householdId) {
    return (
      <div className="text-center py-12">
        <p className="text-red-600">Household not found</p>
      </div>
    )
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess(false)

    if (!email.trim()) {
      setError('Email is required')
      return
    }

    setLoading(true)
    try {
      const result = await sendInvitationApi(householdId, { invitedEmail: email })
      if (!result.ok) {
        setError(result.message || 'Failed to send invitation')
        return
      }

      setSuccess(true)
      setEmail('')
    } catch (err) {
      setError('An unexpected error occurred')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
      <div className="w-full max-w-md space-y-8">
        <div className="text-center">
          <h2 className="mt-6 text-3xl font-bold tracking-tight text-gray-900">
            Invite Your Partner
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            Send an invitation to your partner to join the household
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="rounded-md bg-red-50 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          {success && (
            <div className="rounded-md bg-green-50 p-4">
              <p className="text-sm text-green-800">
                Invitation sent successfully!
              </p>
            </div>
          )}

          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700">
              Partner's Email
            </label>
            <Input
              id="email"
              type="email"
              placeholder="partner@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={loading}
              className="mt-1"
            />
          </div>

          <div className="flex gap-4">
            <Button
              type="submit"
              variant="primary"
              disabled={loading}
              className="flex-1"
            >
              {loading ? 'Sending...' : 'Send Invitation'}
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => navigate('/dashboard')}
              disabled={loading}
              className="flex-1"
            >
              Go to Dashboard
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
