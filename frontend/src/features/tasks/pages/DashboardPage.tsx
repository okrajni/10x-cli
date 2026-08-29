import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@features/auth/context/AuthContext'
import { Button } from '@shared/components'

export default function DashboardPage() {
  const navigate = useNavigate()
  const { user, households, currentHousehold, setCurrentHousehold } = useAuth()

  useEffect(() => {
    // If user has no households, redirect to creation
    if (households.length === 0) {
      navigate('/household/create', { replace: true })
    }
  }, [households, navigate])

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Welcome, {user?.email}!</h1>

      {currentHousehold && (
        <div className="mb-6 bg-blue-50 border border-blue-200 rounded-lg p-4">
          <p className="text-sm text-blue-600">
            <strong>Household:</strong> {currentHousehold.name}
          </p>
          {households.length > 1 && (
            <div className="mt-3 flex gap-2">
              {households.map((h) => (
                <Button
                  key={h.householdId}
                  variant={h.householdId === currentHousehold.householdId ? 'primary' : 'secondary'}
                  size="sm"
                  onClick={() => setCurrentHousehold(h)}
                >
                  {h.name}
                </Button>
              ))}
            </div>
          )}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-2">📋 Tasks</h2>
          <p className="text-gray-600 mb-4">Manage your household tasks.</p>
          <Button onClick={() => navigate('/task')} size="sm">
            Go to Tasks
          </Button>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-2">📊 Dashboard Analytics</h2>
          <p className="text-gray-600">Today&apos;s tasks will appear here in S-06.</p>
        </div>

        <div className="bg-white p-6 rounded-lg shadow">
          <h2 className="text-xl font-semibold mb-2">👥 Household</h2>
          {currentHousehold && (
            <div className="space-y-3">
              <p className="text-gray-600">
                Household: <strong>{currentHousehold.name}</strong>
              </p>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => navigate(`/household/${currentHousehold.householdId}/invite`)}
              >
                Invite Partner
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
