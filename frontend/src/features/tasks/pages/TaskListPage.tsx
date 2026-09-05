import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, TaskCard, Notice, PageHeading } from '@shared/components'
import { listTasks, Task, completeTask, deleteTask } from '../api'
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
      const result = await completeTask(taskId)

      if (result.ok) {
        setTasks(
          tasks.map((t) => (t.id === taskId ? result.data : t))
        )
      } else {
        setError(result.message || 'Failed to complete task')
      }
    } catch {
      setError('An error occurred while completing the task')
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
    <div>
      <PageHeading
        title="Tasks"
        subtitle="Everything your household is keeping track of."
        action={<Button onClick={() => navigate('/task/create')}>+ New Task</Button>}
      />

      {error && (
        <Notice tone="error" className="mb-6" onDismiss={() => setError(null)}>
          {error}
        </Notice>
      )}

      {/* Active / completed switch — pill segmented control */}
      <div className="mb-8 inline-flex rounded-pill border border-accent/40 p-1">
        <button
          onClick={() => setShowCompleted(false)}
          aria-pressed={!showCompleted}
          className={
            !showCompleted
              ? 'rounded-pill bg-accent px-5 py-1.5 text-xs font-medium text-canvas transition'
              : 'rounded-pill px-5 py-1.5 text-xs font-light text-accent transition hover:bg-accent/10'
          }
        >
          Active ({activeTasks.length})
        </button>
        <button
          onClick={() => setShowCompleted(true)}
          aria-pressed={showCompleted}
          className={
            showCompleted
              ? 'rounded-pill bg-accent px-5 py-1.5 text-xs font-medium text-canvas transition'
              : 'rounded-pill px-5 py-1.5 text-xs font-light text-accent transition hover:bg-accent/10'
          }
        >
          Completed ({completedTasks.length})
        </button>
      </div>

      {isLoading && (
        <p className="py-16 text-center text-xs font-light uppercase tracking-label text-accent/70">
          Loading tasks
        </p>
      )}

      {!isLoading && (
        <>
          {displayTasks.length === 0 ? (
            <div className="surface space-y-5 py-16 text-center">
              <p className="text-sm font-light text-accent/75">
                {showCompleted ? 'No completed tasks yet.' : 'No active tasks.'}
              </p>
              {!showCompleted && (
                <Button onClick={() => navigate('/task/create')}>Create Your First Task</Button>
              )}
            </div>
          ) : (
            <div className="space-y-4">
              {displayTasks
                .sort((a, b) => new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime())
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
