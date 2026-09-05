import { useEffect, useState } from 'react'
import { useAuth } from '@features/auth/context/AuthContext'
import { listTasks, deleteTask, updateTask, Task } from '../api'
import { Button, Notice, Spinner, TaskCard } from '@shared/components'

export default function TodayDashboardContainer() {
  const { currentHousehold } = useAuth()
  const [tasks, setTasks] = useState<Task[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (currentHousehold?.householdId) {
      loadTasks()
    }
  }, [currentHousehold?.householdId])

  const loadTasks = async () => {
    setIsLoading(true)
    setError(null)

    try {
      const result = await listTasks()
      if (result.ok) {
        setTasks(result.data)
      } else {
        setError(result.message || 'Failed to load tasks')
      }
    } catch {
      setError('An error occurred while loading tasks')
    } finally {
      setIsLoading(false)
    }
  }

  const getTodayAndOverdue = () => {
    const today = new Date().toISOString().split('T')[0] ?? ''

    const formatDate = (dateStr: string | undefined): string => {
      return (dateStr ?? '').split('T')[0] ?? ''
    }

    const overdue = tasks
      .filter((t) => {
        const dueStr = formatDate(t.dueDate)
        return dueStr < today && !t.completedAt
      })
      .sort((a, b) => new Date(a.dueDate ?? '').getTime() - new Date(b.dueDate ?? '').getTime())

    const todayOnly = tasks
      .filter((t) => {
        const dueStr = formatDate(t.dueDate)
        return dueStr === today && !t.completedAt
      })
      .sort((a, b) => a.category.localeCompare(b.category))

    const completedOverdueAndToday = tasks
      .filter((t) => {
        const dueStr = formatDate(t.dueDate)
        return dueStr <= today && t.completedAt
      })
      .sort((a, b) => new Date(a.dueDate ?? '').getTime() - new Date(b.dueDate ?? '').getTime())

    return [...overdue, ...todayOnly, ...completedOverdueAndToday]
  }

  const handleComplete = async (taskId: string) => {
    const task = tasks.find((t) => t.id === taskId)
    if (!task) return

    const optimisticTasks = tasks.filter((t) => t.id !== taskId)
    setTasks(optimisticTasks)

    try {
      const result = await updateTask(taskId, {
        completedAt: new Date().toISOString(),
      })

      if (!result.ok) {
        setTasks(tasks)
        setError('Failed to complete task')
      }
    } catch {
      setTasks(tasks)
      setError('An error occurred while completing the task')
    }
  }

  const handleDelete = async (taskId: string) => {
    const optimisticTasks = tasks.filter((t) => t.id !== taskId)
    setTasks(optimisticTasks)

    try {
      const result = await deleteTask(taskId)
      if (!result.ok) {
        setTasks(tasks)
        setError('Failed to delete task')
      }
    } catch {
      setTasks(tasks)
      setError('An error occurred while deleting the task')
    }
  }

  const handleMoveToTomorrow = async (taskId: string) => {
    const tomorrow = new Date(new Date().getTime() + 86400000).toISOString().split('T')[0]
    const optimisticTasks = tasks.filter((t) => t.id !== taskId)
    setTasks(optimisticTasks)

    try {
      const result = await updateTask(taskId, { dueDate: tomorrow })
      if (!result.ok) {
        setTasks(tasks)
        setError('Failed to move task to tomorrow')
      }
    } catch {
      setTasks(tasks)
      setError('An error occurred while moving the task')
    }
  }

  const todayAndOverdue = getTodayAndOverdue()

  return (
    <div className="surface flex flex-col p-8">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold text-accent">Today & Overdue</h2>
        <Button variant="secondary" size="sm" onClick={loadTasks} disabled={isLoading}>
          Refresh
        </Button>
      </div>

      {error && (
        <Notice tone="error" className="mb-4" onDismiss={() => setError(null)}>
          {error}
        </Notice>
      )}

      {isLoading ? (
        <div className="py-6 text-center">
          <Spinner />
        </div>
      ) : todayAndOverdue.length === 0 ? (
        <div className="rounded-card border border-accent/20 bg-accent/[0.02] p-8 text-center">
          <p className="text-sm text-accent/70">All caught up! No tasks for today.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {todayAndOverdue.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onComplete={() => handleComplete(task.id)}
              onDelete={() => handleDelete(task.id)}
              onMoveToTomorrow={() => handleMoveToTomorrow(task.id)}
            />
          ))}
        </div>
      )}
    </div>
  )
}
