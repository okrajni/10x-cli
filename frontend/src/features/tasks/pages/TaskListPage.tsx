import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, TaskCard } from '@shared/components'
import { listTasks, Task, updateTask, deleteTask } from '../api'
import TaskDeleteDialog from '../components/TaskDeleteDialog'

export default function TaskListPage() {
  const navigate = useNavigate()
  const [tasks, setTasks] = useState<Task[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [showCompleted, setShowCompleted] = useState(false)
  const [deleteDialog, setDeleteDialog] = useState<{ taskId: string; taskTitle: string } | null>(
    null
  )
  const [error, setError] = useState<string | null>(null)

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
      } else {
        setError(result.message || 'Failed to fetch tasks')
      }
    } catch {
      setError('An error occurred while fetching tasks')
    } finally {
      setIsLoading(false)
    }
  }

  const handleComplete = async (taskId: string) => {
    const task = tasks.find((t) => t.id === taskId)
    if (!task) return

    try {
      const result = await updateTask(taskId, {
        completedAt: task.completedAt ? undefined : new Date().toISOString(),
      })

      if (result.ok) {
        setTasks(
          tasks.map((t) => (t.id === taskId ? result.data : t))
        )
      } else {
        setError(result.message || 'Failed to update task')
      }
    } catch {
      setError('An error occurred while updating the task')
    }
  }

  const handleDelete = async () => {
    if (!deleteDialog) return

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
    }
  }

  const activeTasks = tasks.filter((t) => !t.completedAt && !t.deletedAt)
  const completedTasks = tasks.filter((t) => t.completedAt && !t.deletedAt)

  const displayTasks = showCompleted ? completedTasks : activeTasks

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Tasks</h1>
          <Button onClick={() => navigate('/task/create')}>
            + New Task
          </Button>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-4 p-4 bg-red-50 text-red-800 border border-red-200 rounded-lg">
            {error}
            <button
              onClick={() => setError(null)}
              className="ml-2 font-medium underline"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Toggle for Completed Tasks */}
        <div className="mb-6 flex items-center gap-4">
          <button
            onClick={() => setShowCompleted(!showCompleted)}
            className="text-sm font-medium text-blue-600 hover:text-blue-700"
          >
            {showCompleted
              ? `← Back to Active (${activeTasks.length})`
              : `Show Completed (${completedTasks.length})`}
          </button>
        </div>

        {/* Loading State */}
        {isLoading && (
          <div className="text-center py-12">
            <p className="text-gray-500">Loading tasks...</p>
          </div>
        )}

        {/* Task List */}
        {!isLoading && (
          <>
            {displayTasks.length === 0 ? (
              <div className="text-center py-12 bg-white rounded-lg border border-gray-200">
                <p className="text-gray-500 mb-4">
                  {showCompleted ? 'No completed tasks yet' : 'No active tasks'}
                </p>
                {!showCompleted && (
                  <Button onClick={() => navigate('/task/create')}>
                    Create Your First Task
                  </Button>
                )}
              </div>
            ) : (
              <div className="space-y-4">
                {displayTasks
                  .sort(
                    (a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime()
                  )
                  .map((task) => (
                    <TaskCard
                      key={task.id}
                      task={task}
                      onComplete={() => handleComplete(task.id)}
                      onEdit={() => navigate(`/task/${task.id}/edit`)}
                      onDelete={() =>
                        setDeleteDialog({
                          taskId: task.id,
                          taskTitle: task.title,
                        })
                      }
                    />
                  ))}
              </div>
            )}
          </>
        )}
      </div>

      {/* Delete Dialog */}
      {deleteDialog && (
        <TaskDeleteDialog
          taskTitle={deleteDialog.taskTitle}
          onConfirm={handleDelete}
          onCancel={() => setDeleteDialog(null)}
        />
      )}
    </div>
  )
}
