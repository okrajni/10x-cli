import { useAuth } from '../context/AuthContext'

export default function SettingsPage() {
  const { user } = useAuth()

  return (
    <div>
      <h1 className="text-3xl font-bold mb-6">Settings</h1>

      <div className="bg-white p-6 rounded-lg shadow max-w-lg">
        <h2 className="text-xl font-semibold mb-4">User Profile</h2>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700">Email</label>
            <p className="mt-1 text-gray-900">{user?.email}</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">Name</label>
            <p className="mt-1 text-gray-900">{user?.name || 'Not set'}</p>
          </div>
        </div>

        <p className="mt-6 text-sm text-gray-500">More settings coming in future versions.</p>
      </div>
    </div>
  )
}
