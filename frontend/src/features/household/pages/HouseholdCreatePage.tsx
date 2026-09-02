import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input, Notice, Spinner } from '@shared/components'
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
  const { refetchHouseholds } = useAuth()
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
    } catch {
      setError('An unexpected error occurred')
    } finally {
      setLoading(false)
    }
  }

  if (householdsLoading) {
    return <Spinner fullScreen label="Loading household" />
  }

  if (households.length > 0) {
    const household = households[0]!
    return (
      <div className="flex min-h-screen items-center justify-center bg-canvas px-6 py-16">
        <div className="w-full max-w-md space-y-10">
          <div className="space-y-3 text-center">
            <h1 className="text-3xl font-semibold text-accent">Your Household</h1>
            <p className="text-sm font-light text-accent/70">
              You have one household. Create tasks to get started.
            </p>
          </div>

          <div className="surface space-y-6 p-8">
            <div>
              <p className="label">Household Name</p>
              <p className="font-serif text-lg font-semibold text-accent">{household.name}</p>
            </div>

            <div>
              <p className="label">Household ID</p>
              <p className="break-all font-mono text-xs text-accent/75">{household.householdId}</p>
            </div>

            <div className="divider pt-6">
              <Button variant="primary" className="w-full" onClick={() => navigate('/dashboard')}>
                Go to Dashboard
              </Button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-canvas px-6 py-16">
      <div className="w-full max-w-md space-y-10">
        <div className="space-y-3 text-center">
          <h1 className="text-3xl font-semibold text-accent">Create Your Household</h1>
          <p className="text-sm font-light text-accent/70">
            Set up your household to start sharing tasks.
          </p>
        </div>

        <form className="surface space-y-6 p-8 sm:p-10" onSubmit={handleSubmit}>
          {error && <Notice tone="error">{error}</Notice>}

          <Input
            id="name"
            label="Household Name"
            type="text"
            placeholder="e.g., Smith Household"
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={loading}
          />

          <Button type="submit" variant="primary" disabled={loading} className="w-full">
            {loading ? 'Creating…' : 'Create Household'}
          </Button>
        </form>
      </div>
    </div>
  )
}
