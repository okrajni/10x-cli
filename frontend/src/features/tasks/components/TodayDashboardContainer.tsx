import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, TaskCard, Notice } from '@shared/components'
import { listTasks, Task, updateTask, deleteTask } from '../api'
import TaskDeleteDialog from './TaskDeleteDialog'

export default function TodayDashboardContainer() {
  const navigate = useNavigate()
  const [tasks, setTasks] = useState<Task[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastRefresh, setLastRefresh] = useState<Date | null>(null)
  const [deleteDialog, setDeleteDialog] = useState<{ taskId: string; taskTitle: string } | null>(null)
  const [actionInProgress, setActionInProgress] = useState<string | null>(null)

  useEffect(() => {
    fetchTasks()
  }, [])

  const fetchTasks = async () => {
    setIsLoading(true)
    setError(null)

    try {
      const result = await listTasks()

      if (result.ok) {
        setTasks(result.data)
        setLastRefresh(new Date())
      } else {
        setError(result.message || 'Failed to fetch tasks')
      }
    } catch {
      setError('An error occurred while fetching tasks')
    } finally {
      setIsLoading(false)
    }
  }

  // Filter tasks for today and overdue (not completed)
  const getTodayAndOverdue = () => {
    const today = new Date()
    const todayStr = today.toISOString().split('T')[0] // YYYY-MM-DD

    return tasks
      .filter((t) => {
        if (t.completedAt || t.deletedAt) return false
        const dueStr = t.dueDate?.split('T')[0]
        return dueStr && dueStr <= (todayStr ?? '')
      })
      .sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime())
  }

  const todayAndOverdue = getTodayAndOverdue()

  const handleComplete = async (taskId: string) => {
    const task = tasks.find((t) => t.id === taskId)
    if (!task) return

    setActionInProgress(taskId)
    try {
      const result = await updateTask(taskId, {
        completedAt: task.completedAt ? undefined : new Date().toISOString(),
      })

      if (result.ok) {
        setTasks(tasks.map((t) => (t.id === taskId ? result.data : t)))
      } else {
        setError(result.message || 'Failed to update task')
      }
    } catch {
      setError('An error occurred while updating the task')
    } finally {
      setActionInProgress(null)
    }
  }

  const handleDeleteConfirm = async () => {
    if (!deleteDialog) return

    setActionInProgress(deleteDialog.taskId)
    try {
      const result = await deleteTask(deleteDialog.taskId)

      if (result.ok) {
        setTasks(tasks.filter((t) => t.id !== deleteDialog.taskId))
        setDeleteDialog(null)
      } else {
        setError(result.message || 'Failed to delete task')
      }
    } catch {
      setError('An error occurred while deleting the task')
    } finally {
      setActionInProgress(null)
    }
  }

  return (
    <div className="surface p-8">
      {/* Header */}
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-accent">Today &amp; Overdue</h2>
          {lastRefresh && (
            <p className="hint mt-1">Last updated {lastRefresh.toLocaleTimeString()}</p>
          )}
        </div>

        <Button variant="secondary" size="sm" onClick={fetchTasks} disabled={isLoading}>
          {isLoading ? 'Refreshing…' : 'Refresh'}
        </Button>
      </div>

      {error && (
        <Notice tone="error" className="mb-6" onDismiss={() => setError(null)}>
          {error}
        </Notice>
      )}

      {isLoading && (
        <p className="py-10 text-center text-xs font-light uppercase tracking-label text-accent/70">
          Loading today&apos;s tasks
        </p>
      )}

      {!isLoading && (
        <>
          {todayAndOverdue.length === 0 ? (
            <div className="surface-inset space-y-5 py-12 text-center">
              <p className="text-sm font-light text-accent/75">
                All caught up — no tasks for today.
              </p>
              <Button onClick={() => navigate('/task/create')} size="sm">
                Create a new task
              </Button>
            </div>
          ) : (
            <div className="max-h-96 space-y-3 overflow-y-auto pr-2">
              {todayAndOverdue.map((task) => (
                <TaskCard
                  key={task.id}
                  task={task}
                  onComplete={() => handleComplete(task.id)}
                  onDelete={() => setDeleteDialog({ taskId: task.id, taskTitle: task.title })}
                  isLoading={actionInProgress === task.id}
                />
              ))}
            </div>
          )}
        </>
      )}

      {deleteDialog && (
        <TaskDeleteDialog
          taskTitle={deleteDialog.taskTitle}
          onConfirm={handleDeleteConfirm}
          onCancel={() => setDeleteDialog(null)}
        />
      )}
    </div>
  )
}
