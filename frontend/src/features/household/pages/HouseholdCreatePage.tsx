import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input } from '@shared/components'
import { createHouseholdApi, getUserHouseholdsApi } from '../api'
import { useAuth } from '@features/auth/context/AuthContext'

export default function HouseholdCreatePage() {
  const navigate = useNavigate()
  const { setCurrentHousehold } = useAuth()
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

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

      // Refetch households to update auth context
      const householdsResult = await getUserHouseholdsApi()
      if (householdsResult.ok && householdsResult.data.length > 0) {
        // Set the current household to the newly created one
        const newHousehold = householdsResult.data.find((h) => h.householdId === result.data.householdId)
        if (newHousehold) {
          setCurrentHousehold(newHousehold)
        }
      }

      navigate(`/household/${result.data.householdId}/invite`)
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
            Create Your Household
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            Set up your household and invite your partner
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
