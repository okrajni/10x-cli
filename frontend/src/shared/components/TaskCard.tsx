import clsx from 'clsx'
import { Task } from '@features/tasks/api'
import { Button } from './Button'

interface TaskCardProps {
  task: Task
  onComplete?: (taskId: string) => void
  onEdit?: (task: Task) => void
  onDelete?: (taskId: string) => void
  isLoading?: boolean
}

const categoryColors: Record<Task['category'], string> = {
  cleaning: 'bg-blue-100 text-blue-800',
  shopping: 'bg-green-100 text-green-800',
  laundry: 'bg-purple-100 text-purple-800',
  maintenance: 'bg-yellow-100 text-yellow-800',
  bills: 'bg-red-100 text-red-800',
}

export function TaskCard({
  task,
  onComplete,
  onEdit,
  onDelete,
  isLoading = false,
}: TaskCardProps) {
  const isCompleted = task.status === 'completed'

  return (
    <div
      className={clsx(
        'p-4 border rounded-lg shadow-sm hover:shadow-md transition bg-white',
        isCompleted && 'opacity-60'
      )}
    >
      {/* Header: Title + Category Badge */}
      <div className="flex justify-between items-start mb-2">
        <h3 className={clsx('font-semibold text-gray-900', isCompleted && 'line-through')}>
          {task.title}
        </h3>
        <span className={clsx('text-xs px-2 py-1 rounded-full font-medium', categoryColors[task.category])}>
          {task.category}
        </span>
      </div>

      {/* Description */}
      {task.description && <p className="text-sm text-gray-600 mb-3">{task.description}</p>}

      {/* Due Date + Assignee */}
      <div className="flex gap-4 text-sm text-gray-500 mb-4">
        {task.dueDate && (
          <span>{new Date(task.dueDate).toLocaleDateString()}</span>
        )}
        <span className="font-medium text-blue-600">
          {task.assignedTo === 'me' ? '👤 Me' : '👥 Partner'}
        </span>
      </div>

      {/* Action Buttons */}
      <div className="flex gap-2 justify-end">
        <Button
          variant="primary"
          size="sm"
          onClick={() => onComplete?.(task.id)}
          disabled={isLoading}
          aria-label={`Mark ${task.title} complete`}
        >
          {isCompleted ? '✓ Done' : 'Complete'}
        </Button>

        {onEdit && (
          <Button
            variant="secondary"
            size="sm"
            onClick={() => onEdit(task)}
            disabled={isLoading}
            aria-label={`Edit ${task.title}`}
          >
            Edit
          </Button>
        )}

        {onDelete && (
          <Button
            variant="danger"
            size="sm"
            onClick={() => onDelete(task.id)}
            disabled={isLoading}
            aria-label={`Delete ${task.title}`}
          >
            Delete
          </Button>
        )}
      </div>
    </div>
  )
}
