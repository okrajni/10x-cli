import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@features/auth/context/AuthContext'
import { Button, PageHeading, Spinner } from '@shared/components'
import TodayDashboardContainer from '../components/TodayDashboardContainer'
import { SuggestedTasksList } from '../components/SuggestedTasksList'

export default function DashboardPage() {
  const navigate = useNavigate()
  const { user, households, currentHousehold } = useAuth()

  useEffect(() => {
    // If user has no households, redirect to creation
    if (households && households.length === 0) {
      navigate('/household/create', { replace: true })
    }
  }, [households, navigate])

  // If households exist but currentHousehold isn't set yet, wait for it
  if (households && households.length > 0 && !currentHousehold) {
    return <Spinner label="Loading household data" />
  }

  // If no households at all, this will redirect via useEffect above
  if (!households || households.length === 0) {
    return <Spinner />
  }

  return (
    <div>
      <PageHeading title="Welcome back" subtitle={user?.email} />

      <div className="mb-6">
        <div className="surface flex flex-col items-start gap-4 p-8">
          <h2 className="text-lg font-semibold text-accent">All Tasks</h2>
          <p className="flex-1 text-sm font-light text-accent/70">
            Browse, edit and complete every task in your household.
          </p>
          <div className="flex gap-2">
            <Button onClick={() => navigate('/task/create')} size="sm">
              Create task
            </Button>
            <Button onClick={() => navigate('/task')} size="sm" variant="secondary">
              Go to Tasks
            </Button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <TodayDashboardContainer />

        <div className="surface flex flex-col p-8">
          <div className="flex-1">
            <SuggestedTasksList />
          </div>
        </div>
      </div>
    </div>
  )
}
