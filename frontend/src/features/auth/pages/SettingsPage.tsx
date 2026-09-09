import { useAuth } from '../context/AuthContext'
import { PageHeading } from '@shared/components'

export default function SettingsPage() {
  const { user } = useAuth()

  return (
    <div>
      <PageHeading title="Settings" subtitle="Your account details." />

      <div className="surface max-w-lg space-y-8 p-8">
        <h2 className="text-lg font-semibold text-accent">User Profile</h2>

        <div className="space-y-6">
          <div>
            <p className="label">Email</p>
            <p className="text-sm text-accent">{user?.email}</p>
          </div>

          <div>
            <p className="label">Name</p>
            <p className="text-sm text-accent">{user?.name || 'Not set'}</p>
          </div>
        </div>

        <div className="divider pt-6">
          <p className="hint">More settings coming in future versions.</p>
        </div>
      </div>
    </div>
  )
}
