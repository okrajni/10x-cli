import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, TaskCard } from '@shared/components'
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
    <div className="bg-white p-6 rounded-lg shadow">
      {/* Header with Title and Refresh Button */}
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold">📅 Today & Overdue</h2>
        <Button
          variant="secondary"
          size="sm"
          onClick={fetchTasks}
          disabled={isLoading}
        >
          {isLoading ? 'Refreshing...' : 'Refresh'}
        </Button>
      </div>

      {lastRefresh && (
        <p className="text-xs text-gray-500 mb-4">
          Last updated: {lastRefresh.toLocaleTimeString()}
        </p>
      )}

      {/* Error State */}
      {error && (
        <div className="mb-4 p-4 bg-red-50 text-red-800 border border-red-200 rounded-lg flex justify-between items-center">
          <span>{error}</span>
          <button
            onClick={() => setError(null)}
            className="font-medium underline hover:no-underline"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Loading State */}
      {isLoading && (
        <div className="text-center py-8">
          <p className="text-gray-500">Loading today&apos;s tasks...</p>
        </div>
      )}

      {/* Task List */}
      {!isLoading && (
        <>
          {todayAndOverdue.length === 0 ? (
            <div className="text-center py-12 bg-gray-50 rounded-lg border border-gray-200">
              <p className="text-gray-500 mb-4">✓ All caught up! No tasks for today.</p>
              <Button
                onClick={() => navigate('/task/create')}
                size="sm"
              >
                Create a new task
              </Button>
            </div>
          ) : (
            <div className="max-h-96 overflow-y-auto space-y-3 pr-2">
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

      {/* Delete Dialog */}
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
