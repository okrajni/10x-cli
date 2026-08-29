import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input } from '@shared/components'
import { createHouseholdApi, getUserHouseholdsApi } from '../api'
import { useAuth } from '@features/auth/context/AuthContext'

interface Household {
  householdId: string
  name: string
  createdBy: string
  createdAt: string
}

export default function HouseholdCreatePage() {
  const navigate = useNavigate()
  const { setCurrentHousehold, refetchHouseholds } = useAuth()
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [households, setHouseholds] = useState<Household[]>([])
  const [householdsLoading, setHouseholdsLoading] = useState(true)

  useEffect(() => {
    const fetchHouseholds = async () => {
      try {
        const result = await getUserHouseholdsApi()
        if (result.ok) {
          setHouseholds(result.data || [])
        }
      } catch (err) {
        console.error('Failed to fetch households:', err)
      } finally {
        setHouseholdsLoading(false)
      }
    }

    fetchHouseholds()
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!name.trim()) {
      setError('Household name is required')
      return
    }

    setLoading(true)
    try {
      const result = await createHouseholdApi({ name })
      if (!result.ok) {
        setError(result.message || 'Failed to create household')
        return
      }

      // Refetch households in auth context to update global state
      await refetchHouseholds()

      // Also update local state for UI
      const householdsResult = await getUserHouseholdsApi()
      if (householdsResult.ok && householdsResult.data && householdsResult.data.length > 0) {
        setHouseholds(householdsResult.data)
      }

      // Redirect to dashboard after successful creation and households are synced
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError('An unexpected error occurred')
    } finally {
      setLoading(false)
    }
  }

  if (householdsLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
        <div className="w-full max-w-md space-y-8">
          <div className="text-center">
            <p className="text-sm text-gray-600">Loading...</p>
          </div>
        </div>
      </div>
    )
  }

  if (households.length > 0) {
    const household = households[0]!
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
        <div className="w-full max-w-md space-y-8">
          <div className="text-center">
            <h2 className="mt-6 text-3xl font-bold tracking-tight text-gray-900">
              Your Household
            </h2>
            <p className="mt-2 text-sm text-gray-600">
              You have one household. Create tasks to get started.
            </p>
          </div>

          <div className="mt-8 space-y-6 rounded-lg border border-gray-200 bg-white p-6">
            <div>
              <p className="text-sm font-medium text-gray-500">Household Name</p>
              <p className="mt-1 text-lg font-semibold text-gray-900">{household.name}</p>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-500">Household ID</p>
              <p className="mt-1 font-mono text-sm text-gray-700">{household.householdId}</p>
            </div>
            <div className="pt-4">
              <Button
                variant="primary"
                className="w-full"
                onClick={() => navigate('/dashboard')}
              >
                Go to Dashboard
              </Button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
      <div className="w-full max-w-md space-y-8">
        <div className="text-center">
          <h2 className="mt-6 text-3xl font-bold tracking-tight text-gray-900">
            Create Your Household
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            Set up your household
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="rounded-md bg-red-50 p-4">
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700">
              Household Name
            </label>
            <Input
              id="name"
              type="text"
              placeholder="e.g., Smith Household"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={loading}
              className="mt-1"
            />
          </div>

          <Button
            type="submit"
            variant="primary"
            disabled={loading}
            className="w-full"
          >
            {loading ? 'Creating...' : 'Create Household'}
          </Button>
        </form>
      </div>
    </div>
  )
}
