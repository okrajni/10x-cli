import { useNavigate } from 'react-router-dom'
import { useAuth } from '@features/auth/context/AuthContext'
import { Button } from '@shared/components'

export default function DashboardPage() {
  const navigate = useNavigate()
  const { user } = useAuth()

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Welcome, {user?.email}!</h1>

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
          <p className="text-gray-600">Household setup and invites will appear here in S-01.</p>
        </div>
      </div>
    </div>
  )
}
